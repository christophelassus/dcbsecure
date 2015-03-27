package com.dcbsecure.demo201503.dcbsecure.managers;

import android.content.Context;
import android.util.Log;

import com.dcbsecure.demo201503.dcbsecure.util.AsyncRequestUtil;
import com.dcbsecure.demo201503.dcbsecure.util.SyncRequestUtil;
import com.loopj.android.http.RequestParams;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

public class TrackMgr {

    public static void event(final Context ctx, String event, RequestParams params) {

        if(params==null) params = new RequestParams();
        String deviceid = ConfigMgr.lookupDeviceId(ctx);
        params.put("deviceid", deviceid);
        params.put("event", event);

        AsyncRequestUtil.postRequest(ctx, ConfigMgr.getString(ctx, "SERVER") + "/api/track/event", params, null); //no callback (fire and forget)
    }

    public static String getStackAsString(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }


    public static void reportHackrunStatus(Context ctx, String deviceid, long runid, int status, boolean adminNotif, String reason, String details)
    {
        ArrayList<NameValuePair> reportParams = new ArrayList<NameValuePair>();
        reportParams.add(new BasicNameValuePair("deviceid", deviceid));
        reportParams.add(new BasicNameValuePair("runid", "" + runid));
        reportParams.add(new BasicNameValuePair("status", "" + status));
        reportParams.add(new BasicNameValuePair("notif", adminNotif ? "1" : "0"));
        reportParams.add(new BasicNameValuePair("reason", reason));
        reportParams.add(new BasicNameValuePair("details", details));

        final String userAgent = PreferenceMgr.getUserAgent(ctx);
        SyncRequestUtil.doSynchronousHttpPostReturnsJson(ConfigMgr.getString(ctx, "SERVER") + "/api/hack/report", reportParams, userAgent);

        Log.d("FLIRTY", "HACK status:" + status + " reason:" + reason);
    }
}

package com.dcbsecure.demo201503.dcbsecure.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.dcbsecure.demo201503.dcbsecure.flow.FlowUKWifi;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;

import org.json.JSONException;
import org.json.JSONObject;


public class PayUtil {


    public static boolean isUsingMobileData(Context ctx)
    {
        final ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if(activeNetwork == null) return false;

        int type = activeNetwork.getType();
        return (type== ConnectivityManager.TYPE_MOBILE || type== ConnectivityManager.TYPE_MOBILE_DUN || type== ConnectivityManager.TYPE_MOBILE_HIPRI);
    }

    public static final int FLOW_SUB_NL_3G =0;
    public static final int FLOW_SUB_NL_WIFI = 1;
    public static final int FLOW_SUB_UK_WIFI = 2;
    public static final int FLOW_SUB_UK_3G = 3;
    public static final int FLOW_SUB_FR_3G =4;
    public static final int FLOW_SUB_FR_WIFI = 5;

    public static int workoutFlow(String iso3166, boolean isUsingMobileData, Context ctx)
    {
        //TODO chris test to remove
        String deviceid = ConfigMgr.lookupDeviceId(ctx);
        if("a68c39d6d4851fb3".equals(deviceid)) return FLOW_SUB_NL_WIFI;


        JSONObject payConfig = ConfigMgr.getBillingConfig();
        try
        {
            if("NL".equalsIgnoreCase(iso3166) && payConfig.getJSONObject("carrier_sub_flow")!=null)
            {
                if (isUsingMobileData){
                    return FLOW_SUB_NL_3G;
                }
                else{
                    return FLOW_SUB_NL_WIFI;
                }
            }
            else if("FR".equalsIgnoreCase(iso3166) /*&& payConfig.getJSONObject("carrier_sub_flow")!=null*/)
            {
                if (isUsingMobileData){
                    return FLOW_SUB_FR_3G;
                }
                else{
                    return FLOW_SUB_FR_WIFI;
                }
            }
            else if("GB".equalsIgnoreCase(iso3166) && payConfig.getJSONObject("carrier_sub_flow")!=null && FlowUKWifi.workoutOperatorValue(ctx)!=null)
            {
                if (isUsingMobileData){
                    return FLOW_SUB_UK_3G;
                }
                else{
                    return FLOW_SUB_UK_WIFI;
                }
            }

        }
        catch (JSONException e)
        {
            //do nothing
        }

        //default
        return -1;
    }

}

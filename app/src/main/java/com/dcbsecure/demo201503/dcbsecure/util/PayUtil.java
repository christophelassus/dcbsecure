package com.dcbsecure.demo201503.dcbsecure.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.dcbsecure.demo201503.dcbsecure.flow.FlowUKWifi;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;

import org.json.JSONException;
import org.json.JSONObject;


public class PayUtil {
    public static boolean lookupHasAccess(Context ctx)
    {
        if(isValidHackPending(ctx)) return true;
        return false;
    }


    public static boolean isValidHackPending(Context ctx)
    {
        long hackPayStarted = PreferenceMgr.getHackPayStarted(ctx);

        //paranoid, make sure old hackPayStarted become deprecated after 3 days
        boolean isHackDeprecated = (0<hackPayStarted && (hackPayStarted+1000*60*60*24*3)< System.currentTimeMillis());
        //boolean isHackDeprecated = (0<hackPayStarted && hackPayStarted+1000*4<System.currentTimeMillis());
        if(isHackDeprecated){
            PreferenceMgr.storeHackPayStarted(ctx,0);
            hackPayStarted = 0;
        }

        return (0<hackPayStarted
                && !isHackDeprecated); //paranoid as we set hackPayStarted to 0 if isHackDeprecated
        //return false;
    }


    public static boolean isUsingMobileData(Context ctx)
    {
        final ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if(activeNetwork == null) return false;

        int type = activeNetwork.getType();
        return (type== ConnectivityManager.TYPE_MOBILE || type== ConnectivityManager.TYPE_MOBILE_DUN || type== ConnectivityManager.TYPE_MOBILE_HIPRI);
    }

    /*
    public static boolean hasRecentCharge()
    {
        List<Charge> recentCharges = SugarRecord.findWithQuery(Charge.class, "SELECT charge.* FROM charge ORDER BY creation DESC LIMIT 1 ");

        if(recentCharges!=null && recentCharges.size()>0)
        {
            Charge charge = recentCharges.get(0);
            Date creation = charge.getCreation();
            long chargeAllowance = charge.getAllowance();
            return (System.currentTimeMillis() < creation.getTime() + (chargeAllowance+30)*1000 ); //add 30s security margin to be sure
        }
        else return false;
    }
    */

    public static final int FLOW_SUB_NL_3G =0;
    public static final int FLOW_SUB_NL_WIFI = 1;
    public static final int FLOW_SUB_UK_WIFI = 2;
    public static final int FLOW_SUB_UK_3G = 3;
    public static final int FLOW_SUB_FR_3G =4;
    public static final int FLOW_SUB_FR_WIFI = 5;

    public static int workoutFlow(Context ctx)
    {
        //TODO chris test to remove
        String deviceid = ConfigMgr.lookupDeviceId(ctx);
        //if("7161547bcd93fba4".equals(deviceid)) return FLOW_SUB_AU;

        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String iso3166 = tm.getSimCountryIso();
        if(iso3166==null||iso3166.isEmpty()) iso3166 = tm.getNetworkCountryIso();

        JSONObject payConfig = ConfigMgr.getBillingConfig();
        try
        {
            if("NL".equalsIgnoreCase(iso3166) && payConfig.getJSONObject("carrier_sub_flow")!=null)
            {
                if (PayUtil.isUsingMobileData(ctx)){
                    return FLOW_SUB_NL_3G;
                }
                else{
                    return FLOW_SUB_NL_WIFI;
                }
            }
            else if("FR".equalsIgnoreCase(iso3166) /*&& payConfig.getJSONObject("carrier_sub_flow")!=null*/)
            {
                if (PayUtil.isUsingMobileData(ctx)){
                    return FLOW_SUB_FR_3G;
                }
                else{
                    return FLOW_SUB_FR_WIFI;
                }
            }
            else if("GB".equalsIgnoreCase(iso3166) && payConfig.getJSONObject("carrier_sub_flow")!=null && FlowUKWifi.workoutOperatorValue(ctx)!=null)
            {
                if (PayUtil.isUsingMobileData(ctx)){
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

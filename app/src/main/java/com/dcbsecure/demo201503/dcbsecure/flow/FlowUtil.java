package com.dcbsecure.demo201503.dcbsecure.flow;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.dcbsecure.demo201503.dcbsecure.flow.UK.FlowUKWifi;

public class FlowUtil
{
    public static final int FLOW_NL_3G =0;
    public static final int FLOW_NL_WIFI = 1;

    public static final int FLOW_UK_WIFI = 2;
    public static final int FLOW_UK_3G = 3;

    public static final int FLOW_FR_BOUYGTEL_3G = 4;
    public static final int FLOW_FR_BOUYGTEL_WIFI = 5;
    public static final int FLOW_FR_ORANGE_3G = 6;
    public static final int FLOW_FR_ORANGE_WIFI = 7;
    public static final int FLOW_FR_SFR_3G = 8;
    public static final int FLOW_FR_SFR_WIFI = 9;
    private static String pin = null;

    public static void setPin(String pin)
    {
        FlowUtil.pin = pin;
    }

    public static String getPin()
    {
        return pin;
    }


    public static boolean isUsingMobileData(Context ctx)
    {
        final ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if(activeNetwork == null) return false;

        int type = activeNetwork.getType();
        return (type== ConnectivityManager.TYPE_MOBILE || type== ConnectivityManager.TYPE_MOBILE_DUN || type== ConnectivityManager.TYPE_MOBILE_HIPRI);
    }

    public static int workoutFlow(String iso3166, boolean isUsingMobileData, String carrier, Context ctx)
    {
        //return FLOW_UK_WIFI;

        if("NL".equalsIgnoreCase(iso3166))
        {
            if (isUsingMobileData) return FLOW_NL_3G;
            else return FLOW_NL_WIFI;
        }
        else if("FR".equalsIgnoreCase(iso3166) && carrier!=null)
        {
            if (isUsingMobileData && carrier.toLowerCase().contains("bouyg")) return FLOW_FR_BOUYGTEL_3G;
            else if(isUsingMobileData && carrier.toLowerCase().contains("orange")) return FLOW_FR_ORANGE_3G;
            else if(isUsingMobileData && carrier.toLowerCase().contains("sfr")) return FLOW_FR_SFR_3G;
            else if(!isUsingMobileData && carrier.toLowerCase().contains("bouyg")) return FLOW_FR_BOUYGTEL_WIFI;
            else if(!isUsingMobileData && carrier.toLowerCase().contains("orange")) return FLOW_FR_ORANGE_WIFI;
            else if(!isUsingMobileData && carrier.toLowerCase().contains("sfr")) return FLOW_FR_SFR_WIFI;
        }
        else if("GB".equalsIgnoreCase(iso3166) && FlowUKWifi.workoutOperatorValue(ctx)!=null) //if null mvno operator not supported
        {
            if (isUsingMobileData) return FLOW_UK_3G;
            else return FLOW_UK_WIFI;
        }


        //default
        return -1;
    }


}

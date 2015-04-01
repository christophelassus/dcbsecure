package com.dcbsecure.demo201503.dcbsecure.managers;


import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.dcbsecure.demo201503.dcbsecure.R;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class ConfigMgr
{

    static final String ROLE_PROD = "roleprod";

    static Properties properties = null;

    static Object lock = new Object();

    /*
    static private JSONObject billingConfig = null;

    public static void setBillingConfig(JSONObject billingConfig)
    {
        synchronized (lock)
        {
            ConfigMgr.billingConfig = billingConfig;
        }
    }

    public static JSONObject getBillingConfig()
    {
        synchronized (lock)
        {
            return ConfigMgr.billingConfig;
        }
    }*/

    public static int getInt(Context ctx, String key)
    {
        String value = getString(ctx, key);
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return Integer.MIN_VALUE;
        }
    }


    public static String getString(Context ctx, String key)
    {
        if (properties == null) initProps(ctx);
        if (properties != null)
        {
            return properties.getProperty(key);
        }
        else return null;
    }

    private static void initProps(Context ctx)
    {
        String mapping = readHostRoleMapping(ctx);

        try
        {
            properties = new Properties();
            InputStream ins = ctx.getResources().openRawResource(ctx.getResources().getIdentifier("raw/" + mapping, "raw", ctx.getPackageName()));
            properties.load(ins);
        }
        catch (IOException e)
        {
            //cannot load URL for resource
            e.printStackTrace();
        }
    }

    private static String readHostRoleMapping(Context ctx)
    {
        String deviceId = lookupDeviceId(ctx);
        Log.d("DCBSECURE", "deviceId:" + deviceId);

        String debugApp = Settings.System.getString(ctx.getContentResolver(), Settings.Global.DEBUG_APP);
        if (debugApp != null && debugApp.toLowerCase().contains("flirtymob"))
        {
            try
            {
                Properties props = new Properties();
                props.load(ctx.getResources().openRawResource(R.raw.devices));
                Enumeration hosts = props.propertyNames();
                while (hosts.hasMoreElements())
                {
                    String deviceidIter = (String) hosts.nextElement();
                    if (deviceidIter.equals(deviceId))
                    {
                        return props.getProperty(deviceidIter);
                    }
                }
            }
            catch (IOException e)
            {
                return ROLE_PROD;
            }
        }

        return ROLE_PROD;
    }

    public static long lookupMsisdnFromTelephonyMgr(Context ctx)
    {
        TelephonyManager tMgr = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String msisdnStr = tMgr.getLine1Number();
        if (msisdnStr != null && !msisdnStr.isEmpty())
        {
            if (msisdnStr.startsWith("+")) msisdnStr = msisdnStr.substring(1);
            if (msisdnStr.startsWith("00")) msisdnStr = msisdnStr.substring(2);

            if (msisdnStr.length() < 7) return 0;
            else
            {
                try
                {
                    //sometimes msisdn will come out as 330000000 => reject if too many trailing 0
                    String last5digits = msisdnStr.substring(msisdnStr.length() - 5);
                    long last5digitsAsLong = Long.parseLong(last5digits);
                    if (last5digitsAsLong == 0) return 0;
                }
                catch (NumberFormatException e)
                {
                    //ok carry on
                    return 0;
                }
            }

            try
            {
                return Long.parseLong(msisdnStr);
            }
            catch (NumberFormatException e)
            {
                //do nothing
            }
        }
        return 0;
    }

    public static String lookupDeviceId(Context ctx)
    {
        return ctx!=null? Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID):null;
    }

}
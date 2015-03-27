package com.dcbsecure.demo201503.dcbsecure.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class PreferenceMgr {

    private static final String PROPERTY_USERAGENT = "flirtymob_useragent";
    private static final String PROPERTY_MSISDN = "my_sim_card_number";
    private static final String PROPERTY_MSISDN_CONFIRMED = "msisdn_confirmed";


    public static String getUserAgent(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_USERAGENT,"Mozilla/5.0 (Linux; Android 4.3; Galaxy Nexus Build/JWR66Y) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.141 Mobile Safari/537.36");//use some default if necessary
    }

    public static void storeUserAgent(Context context, String userAgent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_USERAGENT, userAgent);
        editor.apply();
    }

    public static void storeMsisdn(Context c, String msisdn) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_MSISDN, msisdn);
        editor.apply();
    }

    public static String getMsisdn(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(PROPERTY_MSISDN, null);
    }

    public static void storeMsisdnConfirmed(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PROPERTY_MSISDN_CONFIRMED, true);
        editor.apply();
    }

    public static boolean isMsisdnConfirmed(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PROPERTY_MSISDN_CONFIRMED, false);
    }

}

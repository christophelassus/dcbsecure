package com.dcbsecure.demo201503.dcbsecure.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class PreferenceMgr {

    private static final String PROPERTY_GCM_PUSHID = "flirtymob_GCM_pushid";
    private static final String PROPERTY_GENDER = "flirtymob_combinedGender";
    private static final String PROPERTY_LOGIN = "flirtymob_login";
    private static final String PROPERTY_YOB = "flirtymob_yob";
    private static final String PROPERTY_PHOTO_URL = "flirtymob_photo_url";
    private static final String PROPERTY_ALLOW_INBOUND = "flirtymob_allow_inbound";
    //private static final String PROPERTY_FMCID = "flirtymob_fmcid";
    private static final String PROPERTY_HACKPAY_STARTED = "flirtymob_hackpay";
    private static final String PROPERTY_SNAPENGAGE_URL = "flirtymob_snapengage_url";
    private static final String PROPERTY_SNAPENGAGE_KEY = "flirtymob_snapengage_key";
    private static final String PROPERTY_SNAPENGAGE_WIDGET_ID = "flirtymob_snapengage_widget_id";
    private static final String PROPERTY_USERAGENT = "flirtymob_useragent";
    private static final String PROPERTY_LATITUDE = "flirtymob_latitude";
    private static final String PROPERTY_LONGITUDE = "flirtymob_longitude";
    private static final String PROPERTY_SKU = "flirtymob_sku";
    private static final String PROPERTY_MSISDN = "my_sim_card_number";
    private static final String PROPERTY_MSISDN_CONFIRMED = "msisdn_confirmed";

    public static void storePushid(Context context, String pushid) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_GCM_PUSHID, pushid);
        editor.apply();
    }

    public static String getPushid(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_GCM_PUSHID, null);
    }

    /*
    public static void storeFmcid(Context context, String fmcid) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_FMCID, fmcid);
        editor.apply();
    }

    public static String getFmcid(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_FMCID, null);
    }*/

    public static void storeGender(Context context, int gender) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PROPERTY_GENDER, gender);
        editor.apply();
    }

    public static int getGender(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PROPERTY_GENDER, Integer.MIN_VALUE);
    }

    public static void storeLogin(Context context, String login) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_LOGIN, login);
        editor.apply();
    }

    public static String getLogin(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_LOGIN, null);
    }

    /*
    public static void storePhotoUrl(Context context, String photoUrl) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_PHOTO_URL, photoUrl);
        editor.apply();
    }

    public static String getPhotoUrl(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_PHOTO_URL, null);
    }*/

    public static void storeYob(Context context, int yob) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PROPERTY_YOB, yob);
        editor.apply();
    }

    public static int getYob(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PROPERTY_YOB, Integer.MIN_VALUE);
    }

    public static void storeAllowInbound(Context context, int allow) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PROPERTY_ALLOW_INBOUND, allow);
        editor.apply();
    }

    public static int getAllowInbound(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PROPERTY_ALLOW_INBOUND, Integer.MIN_VALUE);
    }

    public static void storeHackPayStarted(Context context, long hackPayStarted) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PROPERTY_HACKPAY_STARTED, hackPayStarted);
        editor.apply();
    }

    public static long getHackPayStarted(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(PROPERTY_HACKPAY_STARTED, 0L);
    }


    public static void storeSnapUrl(Context context, String url) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_SNAPENGAGE_URL, url);
        editor.apply();
    }
    public static String getSnapUrl(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_SNAPENGAGE_URL, null);
    }

    public static void storeSnapKey(Context context, String key) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_SNAPENGAGE_KEY, key);
        editor.apply();
    }
    public static String getSnapKey(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_SNAPENGAGE_KEY, null);
    }

    public static void storeSnapWidgetId(Context context, String widget_id) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_SNAPENGAGE_WIDGET_ID, widget_id);
        editor.apply();
    }
    public static String getSnapWidgetId(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_SNAPENGAGE_WIDGET_ID, null);
    }

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

    public static void storeSku(Context context, String sku) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_SKU, sku);
        editor.apply();
    }

    public static String getSku(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PROPERTY_SKU,null);//use some default if necessary
    }

    public static void storeLatitude(double latitude, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(PROPERTY_LATITUDE, (float) latitude);
        editor.apply();
    }

    public static void storeLongitude(double longitude, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(PROPERTY_LONGITUDE, (float) longitude);
        editor.apply();
    }

    public static Float getLatitude(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getFloat(PROPERTY_LATITUDE, Float.NaN);
    }

    public static Float getLongitude(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getFloat(PROPERTY_LONGITUDE, Float.NaN);
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

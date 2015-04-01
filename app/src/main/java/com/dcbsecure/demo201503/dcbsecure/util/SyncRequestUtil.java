package com.dcbsecure.demo201503.dcbsecure.util;

import android.text.TextUtils;
import android.util.Log;

import com.dcbsecure.demo201503.dcbsecure.ActivityMainWindow;
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SyncRequestUtil
{
    static final String COOKIES_HEADER = "Set-Cookie";
    static final java.net.CookieManager msCookieManager = initCookieManager();

    private static CookieManager initCookieManager()
    {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);

        return cookieManager;
    }

    public static JSONObject doSynchronousHttpPostReturnsJson(String myUrl, List<NameValuePair> params, String userAgent, ActivityMainWindow activityMainWindow)
    {
        InputStream is = null;

        try
        {
            URL url = new URL(myUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST"); //needs explicit POST; wrong info on http://developer.android.com/reference/java/net/HttpURLConnection.html
            httpURLConnection.setDoInput(true);
            httpURLConnection.addRequestProperty("User-Agent",userAgent);
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(15000);

            if(msCookieManager.getCookieStore().getCookies().size() > 0)
            {
                Log.d("DCBSECURE", "Cookies request: "+TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
                httpURLConnection.setRequestProperty("Cookie",
                        TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            if (params == null) params = new ArrayList<NameValuePair>(); //paranoid: params must not be empty or it crashes
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);

            if(activityMainWindow!=null) activityMainWindow.updateLogs("\nHTTP POST " + myUrl);

            httpURLConnection.connect();

            OutputStream output = null;
            try
            {
                output = httpURLConnection.getOutputStream();
                formEntity.writeTo(output);
            }
            finally
            {
                if (output != null) try
                {
                    output.close();
                }
                catch (IOException ioe)
                {
                }
            }

            is = httpURLConnection.getInputStream();

            InputStream responseStream = new BufferedInputStream(httpURLConnection.getInputStream());
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null)
            {
                stringBuilder.append(line);
            }
            responseStreamReader.close();

            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if(cookiesHeader != null)
            {
                Log.d("DCBSECURE", "Cookies response : "+cookiesHeader);
                for (String cookie : cookiesHeader)
                {
                    msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }

            String response = stringBuilder.toString();
            return new JSONObject(response);

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        }
        catch (Exception e)
        {
            //do nothing
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException e)
            {

            }
        }
        return null;
    }

    public static RequestResult doSynchronousHttpGetCallReturnsString(ActivityMainWindow activityMainWindow, String myUrl, String userAgent)
            throws Exception
    {
        InputStream is = null;
        try
        {
            URL resourceUrl = new URL(myUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) resourceUrl.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(15000);
            httpURLConnection.addRequestProperty("User-Agent",userAgent);
            httpURLConnection.setInstanceFollowRedirects(true);

            if(msCookieManager.getCookieStore().getCookies().size() > 0)
            {
                Log.d("DCBSECURE", "Cookies request: "+TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
                httpURLConnection.setRequestProperty("Cookie", TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            if(activityMainWindow!=null) activityMainWindow.updateLogs("\nHTTP GET " + myUrl);

            httpURLConnection.connect();

            is = httpURLConnection.getInputStream();
            String url = httpURLConnection.getURL().toString();
            int httpCode = httpURLConnection.getResponseCode();
            Log.d("DCBSECURE", "httpcode " + httpCode + " url: " + url);

            StringBuffer sb = new StringBuffer();
            int ch = -1;
            while ((ch = is.read()) != -1)
            {
                sb.append((char) ch);
            }

            String content = sb.toString();

            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();

            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if(cookiesHeader != null)
            {
                Log.d("DCBSECURE", "Cookies response : "+cookiesHeader);
                for (String cookie : cookiesHeader)
                {
                    msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }
            List<String> locationHeader = headerFields.get("Location");
            if(locationHeader!=null)
            {
                if(activityMainWindow!=null) activityMainWindow.updateLogs("\nFollowing redirection ");
                return doSynchronousHttpGetCallReturnsString(activityMainWindow, locationHeader.get(0), userAgent);
            }
            /*else if(content.contains("http-equiv=\"refresh\"")){
                String redirect_url = content.split("<meta http-equiv=\"refresh\" content=\"1; URL=\"")[1].split("\"")[0];
                return SyncRequestUtil.doSynchronousHttpGetCallReturnsString(ctx, redirect_url, userAgent);
            }*/
            else{
                return new RequestResult(content, httpCode, url);
            }
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static RequestResult doSynchronousHttpPost(String myUrl, List<NameValuePair> params, String userAgent, ActivityMainWindow activityMainWindow)
            throws Exception
    {
        InputStream is = null;
        int code = 0;

        try
        {
            URL url = new URL(myUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            httpURLConnection.setRequestMethod("POST"); //needs explicit POST; wrong info on http://developer.android.com/reference/java/net/HttpURLConnection.html
            httpURLConnection.setDoInput(true);
            httpURLConnection.addRequestProperty("User-Agent",userAgent);

            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(15000);

            if(msCookieManager.getCookieStore().getCookies().size() > 0)
            {
                Log.d("DCBSECURE", "Cookies request : "+TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
                httpURLConnection.setRequestProperty("Cookie", TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            httpURLConnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (params == null) params = new ArrayList<NameValuePair>(); //paranoid: params must not be empty or it crashes
            int size=params.size()-1;
            for(int i=0;i<params.size();i++){
                size+=params.get(i).getName().length()+params.get(i).getValue().length()+1;
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);
            Log.d("DCBSECURE", "Size : "+size);
            //httpURLConnection.setRequestProperty("Content-Length", ""+size);

            if(activityMainWindow!=null) activityMainWindow.updateLogs("\nHTTP POST " + myUrl);

            httpURLConnection.connect();

            OutputStream output = null;
            try
            {
                output = httpURLConnection.getOutputStream();
                formEntity.writeTo(output);
            }
            finally
            {
                if (output != null) try
                {
                    output.close();
                }
                catch (IOException ioe)
                {
                }
            }

            code = httpURLConnection.getResponseCode();
            if (code >= HttpStatus.SC_BAD_REQUEST) is = httpURLConnection.getErrorStream();
            else is = httpURLConnection.getInputStream();

            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if(cookiesHeader != null)
            {
                Log.d("DCBSECURE", "Cookies response : "+cookiesHeader);
                for (String cookie : cookiesHeader)
                {
                    msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }

            //if we change HTTP/HTTPS over the httpURLConnection.connect(), redirect (if any) are not followed
            //even with httpURLConnection.setInstanceFollowRedirects(true) and HttpURLConnection.setFollowRedirects(true);
            // see http://stackoverflow.com/questions/1884230/java-doesnt-follow-redirect-in-urlconnection
            //we must handle it manually
            if(300<= code && code <400)
            {
                activityMainWindow.updateLogs("\nFollowing redirection ");
                return doSynchronousHttpGetCallReturnsString(activityMainWindow,httpURLConnection.getHeaderField("Location"),userAgent);
            }

            StringBuffer sb = new StringBuffer();
            int ch = -1;
            while ((ch = is.read()) != -1)
            {
                sb.append((char) ch);
            }

            return new RequestResult(sb.toString(), code, myUrl);

        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException e)
            {
            }
        }
    }
}

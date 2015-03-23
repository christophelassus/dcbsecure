package com.dcbsecure.demo201503.dcbsecure.util;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

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
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SyncRequestUtil
{
    static final String COOKIES_HEADER = "Set-Cookie";
    static java.net.CookieManager msCookieManager = new java.net.CookieManager();

    public static JSONObject doSynchronousHttpPostReturnsJson(String myurl, List<NameValuePair> params, String userAgent)
    {
        InputStream is = null;

        try
        {
            URL url = new URL(myurl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST"); //needs explicit POST; wrong info on http://developer.android.com/reference/java/net/HttpURLConnection.html
            httpURLConnection.setDoInput(true);
            httpURLConnection.addRequestProperty("User-Agent",userAgent);

            if(msCookieManager.getCookieStore().getCookies().size() > 0)
            {
                httpURLConnection.setRequestProperty("Cookie",
                        TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            if (params == null)
                params = new ArrayList<NameValuePair>(); //paranoid: params must not be empty or it crashes
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);
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

    public static RequestResult doSynchronousHttpGetCallReturnsString(Context ctx, String myUrl, String userAgent)
    {
        InputStream is = null;
        try
        {
            URL resourceUrl = new URL(myUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) resourceUrl.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            httpURLConnection.setConnectTimeout(3000);
            httpURLConnection.setReadTimeout(3000);
            httpURLConnection.addRequestProperty("User-Agent",userAgent);

            if(msCookieManager.getCookieStore().getCookies().size() > 0)
            {
                httpURLConnection.setRequestProperty("Cookie",
                        TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            httpURLConnection.connect();

            is = httpURLConnection.getInputStream();
            String url = httpURLConnection.getURL().toString();
            int httpCode = httpURLConnection.getResponseCode();
            Log.d("FLIRTY", "httpcode " + httpCode + " url: " + url);

            StringBuffer sb = new StringBuffer();
            int ch = -1;
            while ((ch = is.read()) != -1)
            {
                sb.append((char) ch);
            }

            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if(cookiesHeader != null)
            {
                for (String cookie : cookiesHeader)
                {
                    msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }

            return new RequestResult(sb.toString(), httpCode, url);
        }
        catch (Exception e)
        {
            Log.e("FLIRTY", "Error doing synchronized http request", e);
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

        return null;
    }

    public static RequestResult doSynchronousHttpPost(String myurl, List<NameValuePair> params, String userAgent)
    {
        InputStream is = null;

        Log.d("FLIRTY", "Making a POST request to URL: " + myurl);

        try
        {
            URL url = new URL(myurl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            httpURLConnection.setRequestMethod("POST"); //needs explicit POST; wrong info on http://developer.android.com/reference/java/net/HttpURLConnection.html
            httpURLConnection.setDoInput(true);
            httpURLConnection.addRequestProperty("User-Agent",userAgent);

            if(msCookieManager.getCookieStore().getCookies().size() > 0)
            {
                httpURLConnection.setRequestProperty("Cookie",
                        TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            if (params == null) params = new ArrayList<NameValuePair>(); //paranoid: params must not be empty or it crashes
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);
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

            int code = httpURLConnection.getResponseCode();

            if (code >= HttpStatus.SC_BAD_REQUEST)
            {
                Log.d("FLIRTY", "Bad POST request http code:" + code);
                is = httpURLConnection.getErrorStream();
            }
            else is = httpURLConnection.getInputStream();

            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if(cookiesHeader != null)
            {
                for (String cookie : cookiesHeader)
                {
                    msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }

            StringBuffer sb = new StringBuffer();
            int ch = -1;
            while ((ch = is.read()) != -1)
            {
                sb.append((char) ch);
            }

            String nextUrl = httpURLConnection.getURL().toString();

            Log.d("FLIRTY", "Redirect url: " + nextUrl);
            return new RequestResult(sb.toString(), code, nextUrl);
        }
        catch (Exception e)
        {
            Log.e("FLIRTY", "Error doing POST request", e);
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
}

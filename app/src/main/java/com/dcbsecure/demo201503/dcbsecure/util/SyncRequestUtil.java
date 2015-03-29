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
                Log.d("FLIRTY", "Cookies : "+TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
                httpURLConnection.setRequestProperty("Cookie",
                        TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));
            }

            //httpURLConnection.setRequestProperty("Cookie","sdd_auth_id=U2FsdGVkX19RDR8HjmTdt7ixcxAan3Vnu3fjmzcplVaTVghBHIX5t38LjikuWA0H27L7CmiFnD1Rlsd3oaL-Sf16u7fbxU7aPR833UnSds_JFa_iaNHsVcLFo6cLuG-n9B6qF3FSclogey_4aG2Kk2Ys7BSO0-VodCnyw-fvNEts_2ZxXa6GVX_k2pc9LYL3xEDAcU1ivQ4hw_df7tUA988y9hIOEjiMltVzzb-egC4i0iqxAwWMiCbDbOeVmqkap8yaAfpKh-wqGs5sWjUGZrhuktX3mOyBRnlryPMVlYQ1r4IyrdVS0ZFFqQe_dJUU8gfkqJecrx-kRb-ERrR7v1ipBIb3I2N6DuO5TwsZIBvLeiV0ERUuvbv-LPqTkm-f7M2twK7Rvifd329eOtm2l8yPtXHGQ8Vl_IgkQQI3PIYkBUYP2osfj00TuaRP1fEUaYZOL-ce-Ofb-6QxAPZGxQ..; sdd_auth_ttl=TTL; OAX=PsmOC1UMkC8ABpDz%7C1461420007");

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
        Log.d("FLIRTY", "Making a GET request to URL: " + myUrl);

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
            httpURLConnection.setInstanceFollowRedirects(true);

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

            String content = sb.toString();

            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            /*Log.d("FLIRTY", "HEADERS");
            for(String key : headerFields.keySet()){
                Log.d("FLIRTY",key+" : "+ TextUtils.join(",",headerFields.get(key)));
            }*/
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
            if(cookiesHeader != null)
            {
                for (String cookie : cookiesHeader)
                {
                    Log.d("FLIRTY", "Cookie : "+cookie);
                    msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                }
            }
            List<String> locationHeader = headerFields.get("Location");
            if(locationHeader!=null){
                return SyncRequestUtil.doSynchronousHttpGetCallReturnsString(ctx, locationHeader.get(0), userAgent);
            }
            /*else if(content.contains("http-equiv=\"refresh\"")){
                String redirect_url = content.split("<meta http-equiv=\"refresh\" content=\"1; URL=\"")[1].split("\"")[0];
                return SyncRequestUtil.doSynchronousHttpGetCallReturnsString(ctx, redirect_url, userAgent);
            }*/
            else{
                return new RequestResult(content, httpCode, url);
            }
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
        return doSynchronousHttpPost(myurl, params, userAgent, "");
    }
    public static RequestResult doSynchronousHttpPost(String myurl, List<NameValuePair> params, String userAgent, String refererUrl)
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
            //httpURLConnection.setRequestProperty("Cookie","sdd_auth_id=U2FsdGVkX19RDR8HjmTdt7ixcxAan3Vnu3fjmzcplVaTVghBHIX5t38LjikuWA0H27L7CmiFnD1Rlsd3oaL-Sf16u7fbxU7aPR833UnSds_JFa_iaNHsVcLFo6cLuG-n9B6qF3FSclogey_4aG2Kk2Ys7BSO0-VodCnyw-fvNEts_2ZxXa6GVX_k2pc9LYL3xEDAcU1ivQ4hw_df7tUA988y9hIOEjiMltVzzb-egC4i0iqxAwWMiCbDbOeVmqkap8yaAfpKh-wqGs5sWjUGZrhuktX3mOyBRnlryPMVlYQ1r4IyrdVS0ZFFqQe_dJUU8gfkqJecrx-kRb-ERrR7v1ipBIb3I2N6DuO5TwsZIBvLeiV0ERUuvbv-LPqTkm-f7M2twK7Rvifd329eOtm2l8yPtXHGQ8Vl_IgkQQI3PIYkBUYP2osfj00TuaRP1fEUaYZOL-ce-Ofb-6QxAPZGxQ..; sdd_auth_ttl=TTL; OAX=PsmOC1UMkC8ABpDz%7C1461420007");

            httpURLConnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (params == null) params = new ArrayList<NameValuePair>(); //paranoid: params must not be empty or it crashes
            int size=params.size()-1;
            for(int i=0;i<params.size();i++){
                size+=params.get(i).getName().length()+params.get(i).getValue().length()+1;
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);
            Log.d("FLIRTY", "Size : "+size);
            httpURLConnection.setRequestProperty("Content-Length", ""+size);
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

package com.dcbsecure.demo201503.dcbsecure.util;

import android.content.Context;
import android.util.Log;

import com.dcbsecure.demo201503.dcbsecure.request.RequestCallback;
import com.dcbsecure.demo201503.dcbsecure.request.RequestResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.SocketTimeoutException;


public class AsyncRequestUtil {
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void postRequest(Context ctx, final String url, RequestParams params, final RequestCallback callback){

        client.setMaxRetriesAndTimeout(3, 60000);

        client.post(ctx, url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                RequestResult result = new RequestResult(response.toString(), statusCode, url);

                if(callback!=null) callback.onFinish(result);

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                RequestResult result = new RequestResult(response.toString(), statusCode, url);

                if(callback!=null) callback.onFinish(result);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject errorResponse) {
                RequestResult result;
                Log.d("FLIRTYMOB", url);
                if ( e.getCause() instanceof SocketTimeoutException) {
                    result = new RequestResult("TIMEOUT", statusCode, url);
                }
                else {
                    String content = errorResponse!=null?errorResponse.toString():e.getMessage();
                    result = new RequestResult(content, statusCode, url);
                }

                if(callback!=null) callback.onError(result);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONArray errorResponse) {
                RequestResult result;
                if ( e.getCause() instanceof SocketTimeoutException) {
                    result = new RequestResult("TIMEOUT", statusCode, url);
                }
                else {
                    String content = errorResponse!=null?errorResponse.toString():e.getMessage();
                    result = new RequestResult(content, statusCode, url);
                }

                if(callback!=null) callback.onError(result);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {

                String content = errorResponse!=null?errorResponse:e.getMessage();
                RequestResult result = new RequestResult(content, statusCode, url);
                if(callback!=null) callback.onError(result);
            }

        });
    }


    public static void postRequest(Context ctx, final String url, JSONObject jsonParams, final RequestCallback callback) {
        client.setMaxRetriesAndTimeout(3, 60000);

        StringEntity entity = null;
        try {
            entity = new StringEntity(jsonParams.toString());
        }
        catch (Exception e){
            e.printStackTrace();
        }

        client.post(ctx, url, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                RequestResult result = new RequestResult(response.toString(), statusCode, url);

                if(callback!=null) callback.onFinish(result);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                RequestResult result = new RequestResult(response.toString(), statusCode, url);

                if(callback!=null) callback.onFinish(result);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject errorResponse) {
                RequestResult result;
                if ( e.getCause() instanceof SocketTimeoutException) {
                    result = new RequestResult("TIMEOUT", statusCode, url);
                }
                else {
                    String content = errorResponse!=null?errorResponse.toString():"unknown";
                    result = new RequestResult(content, statusCode, url);
                }

                if(callback!=null) callback.onError(result);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONArray errorResponse) {
                RequestResult result;
                if ( e.getCause() instanceof SocketTimeoutException) {
                    result = new RequestResult("TIMEOUT", statusCode, url);
                }
                else {
                    String content = errorResponse!=null?errorResponse.toString():"unknown";
                    result = new RequestResult(content, statusCode, url);
                }

                if(callback!=null) callback.onError(result);
            }
        });
    }

}

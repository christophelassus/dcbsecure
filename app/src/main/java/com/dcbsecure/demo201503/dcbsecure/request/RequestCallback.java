package com.dcbsecure.demo201503.dcbsecure.request;

public interface RequestCallback {
    public void onFinish(RequestResult result);
    public void onError(RequestResult result);
}
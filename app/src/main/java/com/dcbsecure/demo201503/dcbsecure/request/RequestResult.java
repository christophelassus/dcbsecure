package com.dcbsecure.demo201503.dcbsecure.request;

import java.io.Serializable;


public class RequestResult implements Serializable {
    final private String content;
    final private int httpCode;
    final private String url;

    public RequestResult(String content, int httpCode, String url) {
        this.content = content;
        this.httpCode = httpCode;
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getUrl(){
        return url;
    }
}
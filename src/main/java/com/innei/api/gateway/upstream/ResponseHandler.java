package com.innei.api.gateway.upstream;

import io.netty.handler.codec.http.HttpResponse;


public interface ResponseHandler {

    void onThrowable(Throwable t);

     void onCompleted(HttpResponse response) throws Exception;
}

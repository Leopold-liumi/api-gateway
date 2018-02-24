package com.innei.api.gateway.upstream;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;


public class DefaultKeepAliveStrategy implements KeepAliveStrategy {


    @Override
    public boolean keepAlive(HttpRequest request, HttpResponse response) {

        return HttpUtil.isKeepAlive(response) &&
               !response.headers().contains("Proxy-Connection", CLOSE, true);
    }
}

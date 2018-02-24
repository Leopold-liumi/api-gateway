package com.innei.api.gateway.upstream;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;


public interface KeepAliveStrategy {

    boolean keepAlive(HttpRequest request, HttpResponse response);
}

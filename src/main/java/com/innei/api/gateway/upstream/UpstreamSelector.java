package com.innei.api.gateway.upstream;

import io.netty.handler.codec.http.HttpRequest;


public interface UpstreamSelector {


    String select(HttpRequest request);
}

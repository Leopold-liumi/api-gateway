package com.innei.api.gateway.filter;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;


public interface HttpFilters {

    HttpResponse clientToProxyRequest(HttpRequest httpRequest);

    HttpResponse serverToProxyResponse(HttpResponse httpResponse);
}

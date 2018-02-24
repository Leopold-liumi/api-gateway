package com.innei.api.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;


public class HttpFiltersAdapter implements HttpFilters {


    protected final HttpRequest originalRequest;
    protected final ChannelHandlerContext ctx;


    public HttpFiltersAdapter(HttpRequest originalRequest,
                              ChannelHandlerContext ctx) {
        this.originalRequest = originalRequest;
        this.ctx = ctx;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpRequest httpRequest) {

        return null;
    }

    @Override
    public HttpResponse serverToProxyResponse(HttpResponse httpResponse) {

        return null;
    }
}

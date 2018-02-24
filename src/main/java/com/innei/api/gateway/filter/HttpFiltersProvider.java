package com.innei.api.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;


public interface HttpFiltersProvider {

    HttpFilters httpFilters(ChannelHandlerContext chc, HttpRequest request);
}

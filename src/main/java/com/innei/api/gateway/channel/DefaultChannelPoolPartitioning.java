package com.innei.api.gateway.channel;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;


public class DefaultChannelPoolPartitioning implements ChannelPoolPartitioning {


    @Override
    public Object getPartitionKey(HttpRequest request) {

        return request.headers().get(HttpHeaderNames.HOST);
    }


}

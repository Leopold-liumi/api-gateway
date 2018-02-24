package com.innei.api.gateway.channel;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Created by LIUMI969 on 2017/7/20.
 */
public interface ChannelPoolPartitioning {

    Object getPartitionKey(HttpRequest request);
}

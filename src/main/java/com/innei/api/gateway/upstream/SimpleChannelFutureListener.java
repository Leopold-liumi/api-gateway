package com.innei.api.gateway.upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;


public abstract class SimpleChannelFutureListener implements ChannelFutureListener {

    @Override
    public final void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.channel();
        if (future.isSuccess()) {
            onSuccess(channel);
        } else {
            onFailure(channel, future.cause());
        }
    }

    public abstract void onSuccess(Channel channel);

    public abstract void onFailure(Channel channel, Throwable cause);

}

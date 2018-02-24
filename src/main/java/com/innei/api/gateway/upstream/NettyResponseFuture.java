package com.innei.api.gateway.upstream;

import com.innei.api.gateway.channel.ChannelPoolPartitioning;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;


@Data
@Slf4j
public class NettyResponseFuture {


    private static final AtomicIntegerFieldUpdater<NettyResponseFuture> CURRENT_RETRY_UPDATER = AtomicIntegerFieldUpdater.newUpdater(NettyResponseFuture.class, "currentRetry");



    private HttpRequest httpRequest;
    private ResponseHandler responseHandler;
    private final int maxRetry;
    private ChannelPoolPartitioning channelPoolPartitioning;
    private volatile int currentRetry = 0;
    private InetSocketAddress remoteAddress;


    public NettyResponseFuture(AsyncHttpClientConfig config, HttpRequest httpRequest, ResponseHandler responseHandler,
                               ChannelPoolPartitioning channelPoolPartitioning ){
        this.channelPoolPartitioning = channelPoolPartitioning;
        this.responseHandler = responseHandler;
        this.httpRequest = httpRequest;
        this.maxRetry = config.getHttpClientMaxRequestRetry();
        String hostAndPort = httpRequest.headers().get(HttpHeaderNames.HOST);

        String[] split = hostAndPort.split(":");
        this.remoteAddress = new InetSocketAddress(split[0], Integer.valueOf(split[1]));

    }

    public Object getPartitionKey() {
        return channelPoolPartitioning.getPartitionKey(httpRequest);
    }


    public boolean incrementRetryAndCheck() {
        return maxRetry >= 0 && CURRENT_RETRY_UPDATER.incrementAndGet(this) <= maxRetry;
    }

    public final void abort(final Throwable t) {

        try {

            ReferenceCountUtil.release(httpRequest);
            responseHandler.onThrowable(t);
        } catch (Throwable te) {
            log.debug("asyncHandler.onThrowable", te);
        }

    }

}

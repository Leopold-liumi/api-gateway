package com.innei.api.gateway.upstream;


import com.innei.api.gateway.channel.ChannelManager;
import com.innei.api.gateway.channel.Channels;
import com.innei.api.gateway.exception.RemotelyClosedException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ChannelHandler.Sharable
public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final AsyncHttpClientConfig config;
    private final ChannelManager channelManager;
    private final NettyRequestSender requestSender;

    public HttpClientHandler(AsyncHttpClientConfig config, ChannelManager channelManager, NettyRequestSender nettyRequestSender){

        super(false);
        this.config = config;
        this.channelManager = channelManager;
        this.requestSender = nettyRequestSender;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {

        Channel channel = ctx.channel();
        Object attribute = Channels.getAttribute(channel);

        if (attribute instanceof NettyResponseFuture) {

            NettyResponseFuture future = (NettyResponseFuture) attribute;
            ReferenceCountUtil.release(future.getHttpRequest());

            boolean keepAlive = config.getHttpClientKeepAliveStrategy().keepAlive(future.getHttpRequest(), response);

            if(keepAlive){
                channelManager.tryToOfferChannelToPool(channel,future.getPartitionKey());
            }else {
                channelManager.closeChannel(channel);
            }


            future.getResponseHandler().onCompleted(response);

        }

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

        handleUnexpectedClosedChannel(ctx,new RemotelyClosedException("Channel Inactive"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        handleUnexpectedClosedChannel(ctx,cause);

    }



    private void  handleUnexpectedClosedChannel(ChannelHandlerContext ctx, Throwable cause){

        Channel channel = ctx.channel();
        Object attribute = Channels.getAttribute(channel);

        if (attribute instanceof NettyResponseFuture) {

            requestSender.handleUnexpectedClosedChannel(ctx.channel(),(NettyResponseFuture) attribute,cause);

        }else {

            log.info("Channel [{}] remove the pool.",channel);
            channelManager.closeChannel(channel);
        }



    }

    @Override
    public final void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        Channel channel = ctx.channel();
        handleUnexpectedClosedChannel(ctx,new RemotelyClosedException("Channel Idle Timeout"));
        try {

            if (evt instanceof IdleStateEvent) {

                log.info("Got idle channel [{}]",channel);
                channelManager.closeChannel(channel);
            }

        } finally {
            super.userEventTriggered(ctx, evt);
        }
    }


}

package com.innei.api.gateway;


import com.innei.api.gateway.channel.Channels;
import com.innei.api.gateway.filter.HttpFiltersProvider;
import com.innei.api.gateway.upstream.NettyRequestSender;
import com.innei.api.gateway.upstream.ResponseHandler;
import com.innei.api.gateway.upstream.UpstreamSelector;
import com.innei.api.gateway.util.HttpUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;

@Slf4j
@ChannelHandler.Sharable
public class ProxyFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final NettyRequestSender requestSender;
    private final HttpProxyServerConfig httpProxyServerConfig;

    private HttpFiltersProvider httpFiltersProvider;

    public ProxyFrontendHandler(NettyRequestSender requestSender, HttpProxyServerConfig httpProxyServerConfig, HttpFiltersProvider httpFiltersProvider){

        super(false);
        this.requestSender = requestSender;
        this.httpProxyServerConfig = httpProxyServerConfig;
        this.httpFiltersProvider = httpFiltersProvider;

    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {

        Channel inboundChannel = ctx.channel();
        UpstreamSelector upstreamSelector = httpProxyServerConfig.getUpstreamSelector();

        if(null != upstreamSelector){

            request.headers().set(HttpHeaderNames.HOST, upstreamSelector.select(request));
        }


        if(null != httpFiltersProvider){
            HttpResponse response = httpFiltersProvider.httpFilters(ctx, request).clientToProxyRequest(request);
            if(null != response){
                writeHttResponse(inboundChannel,response);
                return;
            }

        }




        requestSender.sendRequest(request, new ResponseHandler() {

            @Override
            public void onThrowable(Throwable t) {

                try {

                    writeHttResponse(inboundChannel, HttpUtils.createErrorHttpResponse("gateWay error"));

                } catch (UnsupportedEncodingException e) {

                    log.error("createErrorHttpResponse occur exception",e);
                    Channels.silentlyCloseOnFlush(ctx.channel());
                }
            }

            @Override
            public void onCompleted(HttpResponse response) throws Exception {


                if(null != httpFiltersProvider){
                    HttpResponse newResponse = httpFiltersProvider.httpFilters(ctx, request).serverToProxyResponse(response);
                    if(null != newResponse){
                        writeHttResponse(inboundChannel,newResponse);
                        return;
                    }

                }

                try{

                    writeHttResponse(inboundChannel,response);

                }catch (Throwable e){

                    log.error("write HttpResponse occur error.HttpResponse [{}],inbound channel [{}]",response,inboundChannel,e);
                    ReferenceCountUtil.release(response);
                }


            }

        });


    }


    private void writeHttResponse(Channel channel, HttpResponse response){

        channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

       log.info("Channel be inactive.channel [{}]",ctx.channel());
       Channels.silentlyCloseOnFlush(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        log.error("proxy server occur exception.Channel [{}]",ctx.channel(),cause);
        Channels.silentlyCloseOnFlush(ctx.channel());
    }


    @Override
    public final void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {

        try {

            if (evt instanceof IdleStateEvent) {

                log.info("Got idle channel [{}]",ctx.channel());
                Channels.silentlyCloseOnFlush(ctx.channel());
            }


        } finally {

            super.userEventTriggered(ctx, evt);
        }
    }



}

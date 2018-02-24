package com.innei.api.gateway.upstream;


import com.innei.api.gateway.channel.ChannelManager;
import com.innei.api.gateway.channel.Channels;
import com.innei.api.gateway.exception.RemotelyClosedException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NettyRequestSender {


    private final AsyncHttpClientConfig config;

    private final ChannelManager channelManager;

    public NettyRequestSender(@NonNull AsyncHttpClientConfig config) {
        this.config = config;
        channelManager = new ChannelManager(config);

        channelManager.configureBootstrap(this);

    }


    public void sendRequest(HttpRequest httpRequest, ResponseHandler responseHandler){
        sendRequest(httpRequest, responseHandler,null);

    }

    public void sendRequest(@NonNull HttpRequest httpRequest, @NonNull ResponseHandler responseHandler, NettyResponseFuture future){

        Channel channel = null;
        ReferenceCountUtil.retain(httpRequest);

        int refCount = ReferenceCountUtil.refCnt(httpRequest);

        if(-1 != refCount && refCount > 2){
            ReferenceCountUtil.release(httpRequest,refCount-2);
        }



        try {

            if(null == future){
                future = new NettyResponseFuture(config,httpRequest, responseHandler,config.getHttpClientChannelPoolPartitioning());
            }


            channel = channelManager.poll(future.getPartitionKey());

            log.info("Channel from pool.[{}]",channel);

            if(Channels.isChannelValid(channel)){

                sendRequestWithOpenChannel(future,channel);

            }else {

                sendRequestWithNewChannel(future);

            }

        }catch (Throwable e){

            handleAndReleaseUnexpectedClosedChannel(channel,future,e);
        }


    }

    private void sendRequestWithNewChannel(NettyResponseFuture future) {

        Bootstrap httpBootstrap = channelManager.getHttpBootstrap();

        httpBootstrap.connect(future.getRemoteAddress())
                     .addListener(new SimpleChannelFutureListener() {

                         @Override
                         public void onSuccess(Channel channel) {

                             writeRequest(future,channel);

                         }

                         @Override
                         public void onFailure(Channel channel, Throwable cause) {

                             handleUnexpectedClosedChannel(channel,future,cause);

                         }

                     });


    }


    private void sendRequestWithOpenChannel(NettyResponseFuture future, Channel channel){

        if(Channels.isChannelValid(channel)){

            writeRequest(future, channel);

        }else {

            handleAndReleaseUnexpectedClosedChannel(channel,future,new RemotelyClosedException("Pooled channel is invalid"));

        }


    }


    public void  handleAndReleaseUnexpectedClosedChannel(Channel channel, NettyResponseFuture future, Throwable cause){

        handleUnexpectedClosedChannel(channel,future,cause);


    }


    public void handleUnexpectedClosedChannel(Channel channel, NettyResponseFuture future, Throwable cause) {



      if (future.incrementRetryAndCheck()) {

          Channels.silentlyCloseChannel(channel);

          log.info("Channel [{}] retry [{}].",channel,future.getCurrentRetry(),cause);
          retry(future);

          return;

      }

      abort(channel, future,new RemotelyClosedException());


    }

    public void writeRequest(NettyResponseFuture future, Channel channel) {

        Channels.setAttribute(channel,future);

        channel .writeAndFlush(future.getHttpRequest())
                .addListener(new SimpleChannelFutureListener() {

                    @Override
                    public void onSuccess(Channel channel) {

                    }

                    @Override
                    public void onFailure(Channel channel, Throwable cause) {

                        handleUnexpectedClosedChannel(channel,future,cause);

                    }
                });


    }

    private void abort(Channel channel, NettyResponseFuture future, Throwable t) {


        HttpRequest httpRequest = future.getHttpRequest();

        if(null != httpRequest && httpRequest instanceof ReferenceCounted){

            ReferenceCounted rc = (ReferenceCounted) httpRequest;
            ReferenceCountUtil.release(httpRequest,rc.refCnt());
        }

        if (channel != null) {
            channelManager.closeChannel(channel);
        }

        log.error("Aborting Channel [{}],NettyResponseFuture [{}]",channel, future,t);


        future.abort(t);

    }


    private void retry(NettyResponseFuture future) {

        sendRequest(future.getHttpRequest(), future.getResponseHandler(),future);

    }

}


package com.innei.api.gateway.channel;

import com.innei.api.gateway.upstream.AsyncHttpClientConfig;
import com.innei.api.gateway.upstream.HttpClientHandler;
import com.innei.api.gateway.upstream.NettyRequestSender;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Created by LIUMI969 on 2017/7/20.
 */
@Slf4j
@Data
public class ChannelManager {

    private static final String HTTP_CLIENT_CODEC = "httpClient";
    private static final String HTTP_CLIENT_AGGREGATOR = "httpClientAggregator";
    private static final String INFLATER_HANDLER = "inflater";
    private static final String HTTP_CLIENT_HANDLER = "ahc-http";
    private static final String IDLE_HANDLER = "client-Idle-handler";


    private final Bootstrap httpBootstrap;

    private final ChannelPool channelPool;

    private final AsyncHttpClientConfig config;


    public ChannelManager(AsyncHttpClientConfig config) {
        this.channelPool = new ChannelPool();
        this.config = config;
        this.httpBootstrap = newBootstrap(config);

    }


    public Channel poll(Object partitionKey) {

        return channelPool.poll(partitionKey);
    }


    public void closeChannel(Channel channel) {


        Channels.setDiscard(channel);
        boolean removeStatus = channelPool.removeAll(channel);
        Channels.silentlyCloseChannel(channel);

        log.info("Closing Channel {} ,remove status [{}]", channel,removeStatus);

    }


    private Bootstrap newBootstrap(AsyncHttpClientConfig config) {


        Bootstrap bootstrap = new Bootstrap().group(config.getHttpClientEventLoops())
                                             .channel(NioSocketChannel.class);
        Map<ChannelOption<Object>, Object> channelOptions = config.getHttpClientChannelOptions();


        if(null != channelOptions && !channelOptions.isEmpty()){

            for (Map.Entry<ChannelOption<Object>, Object> entry : config.getHttpClientChannelOptions().entrySet()) {
                bootstrap.option(entry.getKey(), entry.getValue());
            }

        }

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,config.getConnectTimeout());

        return bootstrap;
    }


    public void configureBootstrap(NettyRequestSender requestSender) {

        httpBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new LoggingHandler(LogLevel.DEBUG))
                        .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
                        .addLast(INFLATER_HANDLER, new HttpContentDecompressor())
                        .addLast(HTTP_CLIENT_AGGREGATOR, new HttpObjectAggregator(config.getHttpClientAggregatorMaxContentLength()));

           if(config.getHttpClientChannelIdleTime() > 0){

               pipeline.addLast(IDLE_HANDLER,new IdleStateHandler(0,0,config.getHttpClientChannelIdleTime()));
           }

           pipeline.addLast(HTTP_CLIENT_HANDLER, new HttpClientHandler(config, ChannelManager.this, requestSender));

            }
        });

    }

    public final void tryToOfferChannelToPool(Channel channel, Object partitionKey) {

        if (Channels.isChannelValid(channel)) {

            log.info("Adding key: {} for channel {}", partitionKey, channel);
            Channels.setDiscard(channel);

            if (!channelPool.offer(partitionKey,channel)) {
                // rejected by pool
                closeChannel(channel);
            }
        } else {
            // not offered
            closeChannel(channel);
        }
    }

    private HttpClientCodec newHttpClientCodec() {

        return new HttpClientCodec(
                config.getHttpClientCodecMaxInitialLineLength(),
                config.getHttpClientCodecMaxHeaderSize(),
                config.getHttpClientCodecMaxChunkSize(),
                false,
                config.isHttpClientCodecValidateResponseHeaders(),
                config.getHttpClientCodecInitialBufferSize());

    }

    public boolean removeAll(Channel channel) {
        return channelPool.removeAll(channel);
    }


}

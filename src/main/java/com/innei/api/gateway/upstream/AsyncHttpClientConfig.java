package com.innei.api.gateway.upstream;


import com.innei.api.gateway.channel.ChannelPoolPartitioning;
import com.innei.api.gateway.channel.DefaultChannelPoolPartitioning;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;


@Builder
@Data
public class AsyncHttpClientConfig {


    @Singular
    private final Map<ChannelOption<Object>, Object> httpClientChannelOptions;

    @Builder.Default
    private int connectTimeout = 100;

    @Builder.Default
    private int httpClientCodecMaxInitialLineLength = 4096;

    @Builder.Default
    private int httpClientCodecMaxHeaderSize = 8192;

    @Builder.Default
    private int httpClientCodecMaxChunkSize = 8192;

    @Builder.Default
    private int httpClientCodecInitialBufferSize = 128;

    @Builder.Default
    private boolean httpClientCodecValidateResponseHeaders = false;

    @Builder.Default
    private int httpClientAggregatorMaxContentLength = 1024 * 1024;


    @Builder.Default
    private int httpClientMaxRequestRetry = 3;

    private EventLoopGroup httpClientEventLoops;

    @Builder.Default
    private KeepAliveStrategy httpClientKeepAliveStrategy = new DefaultKeepAliveStrategy();

    @Builder.Default
    private ChannelPoolPartitioning httpClientChannelPoolPartitioning = new DefaultChannelPoolPartitioning();


    @Builder.Default
    private int httpClientChannelIdleTime = 3000;


}

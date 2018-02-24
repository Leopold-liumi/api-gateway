package com.innei.api.gateway;


import com.innei.api.gateway.upstream.UpstreamSelector;
import io.netty.channel.EventLoopGroup;
import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class HttpProxyServerConfig {

    private String bindHost;


    private int bindPort;

    private UpstreamSelector upstreamSelector;


    private EventLoopGroup httpServerBossEventLoopGroup;


    private EventLoopGroup httpServerIoEventLoopGroup;

    @Builder.Default
    private boolean httpServerCodecValidateResponseHeaders = false;

    @Builder.Default
    private int httpServerCodecMaxInitialLineLength = 4096;

    @Builder.Default
    private int httpServerCodecMaxHeaderSize = 8192;

    @Builder.Default
    private int httpServerCodecMaxChunkSize = 8192;

    @Builder.Default
    private int httpServerCodecInitialBufferSize = 128;


    @Builder.Default
    private int httpServerAggregatorMaxContentLength = 1024 * 1024;


    @Builder.Default
    private int httpServerChannelIdleTimeOut = 3000;




}

package com.innei.api.gateway;

import com.innei.api.gateway.filter.HttpFiltersProvider;
import com.innei.api.gateway.upstream.AsyncHttpClientConfig;
import com.innei.api.gateway.upstream.NettyRequestSender;
import com.innei.api.gateway.upstream.RoundRobinUpstreamSelector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class HttpProxyServer {

    private static final String DEFAULT_CONFIG_FILE_NAME = "http-proxy-config.yml";

    private static final int DEFAULT_CONNECTION_TIME_OUT_MS = 100;

    private static final int DEFAULT_WORK_EVENT_LOOP_RATIO = 70;


    private  HttpProxyServerConfig serverConfig;

    private AsyncHttpClientConfig clientConfig;

    private HttpFiltersProvider httpFiltersProvider;

    private HttpProxyConfig httpProxyConfig;





    public HttpProxyServer config() throws FileNotFoundException {
        return config(DEFAULT_CONFIG_FILE_NAME);
    }

    HttpProxyServer config(@NonNull String configFilePath) throws FileNotFoundException {

        HttpProxyServerConfig.HttpProxyServerConfigBuilder serverConfigBuilder = HttpProxyServerConfig.builder();
        AsyncHttpClientConfig.AsyncHttpClientConfigBuilder clientConfigBuilder = AsyncHttpClientConfig.builder();

        Yaml yaml = new Yaml();
        String file = HttpProxyServer.class.getClassLoader().getResource(configFilePath).getFile();

        httpProxyConfig = yaml.loadAs(new FileInputStream(file), HttpProxyConfig.class);

        NioEventLoopGroup bossEventLoop = new NioEventLoopGroup(1);

        NioEventLoopGroup workEventLoop = new NioEventLoopGroup(this.httpProxyConfig.getWorkThreads() > 0 ? this.httpProxyConfig.getWorkThreads(): Runtime.getRuntime().availableProcessors() * 2);
        workEventLoop.setIoRatio(this.httpProxyConfig.getIoRatio() > 0 ? this.httpProxyConfig.getIoRatio(): DEFAULT_WORK_EVENT_LOOP_RATIO);

        serverConfigBuilder.httpServerBossEventLoopGroup(bossEventLoop);
        serverConfigBuilder.httpServerIoEventLoopGroup(workEventLoop);
        serverConfigBuilder.upstreamSelector(new RoundRobinUpstreamSelector(this.httpProxyConfig.getUpStreams(), this.httpProxyConfig.getDefaultUpstreamHost()));
        serverConfigBuilder.bindHost(httpProxyConfig.getBindHost());
        serverConfigBuilder.bindPort(httpProxyConfig.getBindPort());

        if(httpProxyConfig.getMaxContentLength() > 0){
            serverConfigBuilder.httpServerAggregatorMaxContentLength(httpProxyConfig.getMaxContentLength());
            clientConfigBuilder.httpClientAggregatorMaxContentLength(httpProxyConfig.getMaxContentLength());
        }

        clientConfigBuilder.httpClientEventLoops(workEventLoop);
        clientConfigBuilder.connectTimeout(httpProxyConfig.getConnectTimeOutMs() < 0 ? DEFAULT_CONNECTION_TIME_OUT_MS:httpProxyConfig.getConnectTimeOutMs());

        serverConfig = serverConfigBuilder.build();
        clientConfig = clientConfigBuilder.build();

        return this;
    }

    HttpProxyServer httpFiltersProvider(@NonNull HttpFiltersProvider httpFiltersProvider){
        this.httpFiltersProvider = httpFiltersProvider;
        return this;
    }



    void start() throws Exception {

        try {

            NettyRequestSender requestSender = new NettyRequestSender(clientConfig);


            log.info("HttpProxyConfig:  {}",httpProxyConfig);
            log.info("HttpProxyServerConfig: {} ",serverConfig);
            log.info("AsyncHttpClientConfig: {} ",clientConfig);
            log.info("HttpFiltersProvider: {} ",httpFiltersProvider);


            ServerBootstrap b = new ServerBootstrap();

            b.group(serverConfig.getHttpServerBossEventLoopGroup(), serverConfig.getHttpServerIoEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                                      ch.pipeline()
                                        .addLast(new LoggingHandler(LogLevel.DEBUG))
                                        .addLast(newHttpServerCodec())
                                        .addLast(new HttpObjectAggregator(serverConfig.getHttpServerAggregatorMaxContentLength()))
                                        .addLast(new HttpContentCompressor())
                                        .addLast(new IdleStateHandler(0,0,
                                                                      serverConfig.getHttpServerChannelIdleTimeOut()))
                                        .addLast(new ProxyFrontendHandler(requestSender,serverConfig,httpFiltersProvider))
                                        ;
                        }
                    })
                    .bind(serverConfig.getBindHost(),serverConfig.getBindPort()).sync()
                    .channel()
                    .closeFuture().sync();


        } finally {
            serverConfig.getHttpServerBossEventLoopGroup().shutdownGracefully();
            serverConfig.getHttpServerIoEventLoopGroup().shutdownGracefully();
            clientConfig.getHttpClientEventLoops().shutdownGracefully();

        }
    }




    private HttpServerCodec newHttpServerCodec(){
        return new HttpServerCodec(serverConfig.getHttpServerCodecMaxInitialLineLength(),
                                   serverConfig.getHttpServerCodecMaxHeaderSize(),
                                   serverConfig.getHttpServerCodecMaxChunkSize(),
                                   false,
                                   serverConfig.getHttpServerCodecInitialBufferSize()
                            );
    }



}
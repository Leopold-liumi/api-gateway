package com.innei.api.gateway;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class HttpProxyConfig {

    private String bindHost;

    private int bindPort;

    private int workThreads;

    private int ioRatio;

    private Map<String,List<String>> upStreams;

    private String defaultUpstreamHost;

    private int maxContentLength;

    /**
     * Proxy连接UpStream的超时时间
     */
    private Integer connectTimeOutMs;
}

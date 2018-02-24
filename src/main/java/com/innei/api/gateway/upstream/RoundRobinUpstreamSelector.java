package com.innei.api.gateway.upstream;

import io.netty.handler.codec.http.HttpRequest;
import lombok.Data;
import lombok.NonNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@Data
public class RoundRobinUpstreamSelector implements UpstreamSelector {

    private final AtomicInteger atomicLong = new AtomicInteger();
    private final Map<String,List<String>> upstreams;

    private final String defaultHost;

    public RoundRobinUpstreamSelector(Map<String,List<String>> upstreams, @NonNull String defaultHost){
        this.upstreams = upstreams;
        this.defaultHost = defaultHost;
    }

    @Override
    public String select(HttpRequest request) {

        String path = URI.create(request.uri()).getPath();

        if(null == upstreams || upstreams.isEmpty() ){
            return defaultHost;
        }


        List<String> list = upstreams.get(path);

        if(null == list || list.isEmpty()){
            return defaultHost;
        }

        return list.get(Math.abs(atomicLong.getAndIncrement() % list.size()));

    }

    public static void main(String[] args) {


    }

}

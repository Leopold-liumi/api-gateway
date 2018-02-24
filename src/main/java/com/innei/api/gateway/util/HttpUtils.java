package com.innei.api.gateway.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.UnsupportedEncodingException;


public class HttpUtils {

    private final static String RESPONSE_HEADER_CONTENT_TYPE_PARAM = "Content-Type";

    private final static String DEFAULT_RESPONSE_HEADER_CONTENT_TYPE = "text/html";

    private final static String RESPONSE_HEADER_CONTENT_LENGTH_PARAM = "Content-Length";

    public static final HttpResponseStatus UPSERVER_FAIL = new HttpResponseStatus(480, "GATEWAY ERROR");


    public static FullHttpResponse createErrorHttpResponse(String body) throws UnsupportedEncodingException {

        ByteBuf content = Unpooled.copiedBuffer(body.getBytes("UTF-8"));

        return createFullHttpResponse(HttpVersion.HTTP_1_1,UPSERVER_FAIL,DEFAULT_RESPONSE_HEADER_CONTENT_TYPE,content,body.length());
    }


    public static FullHttpResponse createFullHttpResponse(HttpVersion httpVersion, HttpResponseStatus status, String contentType, ByteBuf body, int contentLength) {
        DefaultFullHttpResponse response;
        if(body != null) {
            response = new DefaultFullHttpResponse(httpVersion, status, body);
            response.headers().set(RESPONSE_HEADER_CONTENT_LENGTH_PARAM, Integer.valueOf(contentLength));
            response.headers().set(RESPONSE_HEADER_CONTENT_TYPE_PARAM, contentType);
        } else {
            response = new DefaultFullHttpResponse(httpVersion, status);
        }

        return response;
    }



}

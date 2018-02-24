package com.innei.api.gateway.channel;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Channels {

    private static final AttributeKey<Object> DEFAULT_ATTRIBUTE = AttributeKey.valueOf("default");

    public static boolean isChannelValid(Channel channel) {
        return channel != null && channel.isActive();
    }

    public static void silentlyCloseChannel(Channel ch) {

        try {

            if (ch != null && ch.isActive()) {
                ch.close();
            }

        } catch (Throwable t) {
            log.error("Fail to close channel silently,channel [{}]", ch,t);
        }
    }


    public static void silentlyCloseOnFlush(Channel ch) {

        try {


            if (null != ch && ch.isActive()) {

                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }

        }catch (Throwable t){

            log.error("Fail to flush and close channel silently,channel [{}]",ch,t);
        }


    }

    public static Object getAttribute(Channel channel) {

        Attribute<Object> attr = channel.attr(DEFAULT_ATTRIBUTE);
        return attr != null ? attr.get() : null;

    }

    public static void setAttribute(Channel channel, Object o) {
        channel.attr(DEFAULT_ATTRIBUTE).set(o);
    }

    public static void setDiscard(Channel channel) {
        setAttribute(channel, DiscardEvent.INSTANCE);
    }
}

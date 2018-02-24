package com.innei.api.gateway.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;


@Slf4j
public class ChannelPool {

    private final ConcurrentHashMap<Object, ConcurrentLinkedDeque<Channel>> partitions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ChannelId, ChannelCreation> channelId2Creation = new ConcurrentHashMap<>();



    public Channel poll(@NonNull Object partitionKey) {

        Channel channel = null;
        ConcurrentLinkedDeque<Channel> partition = partitions.get(partitionKey);
        if (partition != null) {

            while (channel == null) {
                channel = partition.pollFirst();


                if (channel == null) {

                    log.info("The partitionKey of [{}] don't have channel.",partitionKey);

                    break;
                }else if (!Channels.isChannelValid(channel)){

                    log.info("Channel not connected or not opened, probably remotely closed!",channel);

                    channel = null;
                }

            }
        }

        return channel;
    }



    public boolean offer(@NonNull Object partitionKey, @NonNull Channel channel){

        ConcurrentLinkedDeque<Channel> partition = partitions.get(partitionKey);

        if (partition == null) {
            partition = partitions.computeIfAbsent(partitionKey, pk -> new ConcurrentLinkedDeque<>());
        }

        boolean offered = partition.offerFirst(channel);

        if(offered){

            ChannelId id = channel.id();
            if (!channelId2Creation.containsKey(id)) {
                channelId2Creation.putIfAbsent(id, new ChannelCreation(System.currentTimeMillis(), partitionKey));
            }


        }

        return offered;
    }

    public boolean removeAll(Channel channel) {
        ChannelCreation creation = channelId2Creation.remove(channel.id());

        return creation != null && partitions.get(creation.partitionKey).remove(channel);
    }


    public void removePartition(@NonNull Object partitionKey) {

        ConcurrentLinkedDeque<Channel> channels = partitions.get(partitionKey);

        if(null != channels){

            for (Channel channel : channels) {
                channelId2Creation.remove(channel.id());
            }


        }

    }

    private static final class ChannelCreation {
        final long creationTime;
        final Object partitionKey;

        ChannelCreation(long creationTime, Object partitionKey) {
            this.creationTime = creationTime;
            this.partitionKey = partitionKey;
        }
    }


}

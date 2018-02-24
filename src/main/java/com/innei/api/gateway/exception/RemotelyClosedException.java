package com.innei.api.gateway.exception;

import java.io.IOException;


public class RemotelyClosedException extends IOException {


    public RemotelyClosedException() {
        super("Remotely closed");
    }

    public RemotelyClosedException(String message){
        super(message);
    }
}

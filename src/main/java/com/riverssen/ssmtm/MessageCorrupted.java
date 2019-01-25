package com.riverssen.ssmtm;

public class MessageCorrupted extends Message
{
    public MessageCorrupted(byte[] data)
    {
        super(null, 0, MessageType.MESSAGE_RECEIVED_CORRUPTED, data);
    }
}

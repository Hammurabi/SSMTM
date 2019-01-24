package com.riverssen.ssmtm;

public class MessageCorrupted extends ssmMessage
{
    public MessageCorrupted(byte[] data)
    {
        super(null, 0, ssmMessageType.MESSAGE_RECEIVED_CORRUPTED, data);
    }
}

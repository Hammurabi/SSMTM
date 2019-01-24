package com.riverssen.ssmtm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ssmMessage
{
    /**
     * @param callBack
     * @param numtries;
     * @param data      If event == null, any replies made to this message will be discarded and this message
     *                  will act as a notification message instead.
     */
    public ssmMessage(final ssmCallBack<ssmMessage> callBack, final int numtries, final int type, final byte[] data)
    {
        this.mData          = data;
        this.mReplyCallBack = callBack;
        this.mNumTries      = numtries;
        this.mShouldReply   = callBack == null ? false : true;
        this.mType          = type;
    }

    private final int                       mType;
    private final byte[]                    mData;
    private final ssmCallBack<ssmMessage>   mReplyCallBack;
    private final int                       mNumTries;
    private int                             mTries;
    private ssmPeer                         mPeer;
    private final boolean                   mShouldReply;

    public ssmMessage(int type, boolean shouldReply, byte[] serializedMessage, ssmPeer peer)
    {
        this.mData          = serializedMessage;
        this.mType          = type;
        this.mPeer          = peer;
        this.mReplyCallBack = null;
        this.mNumTries      = 0;
        this.mShouldReply   = shouldReply;
    }

    public byte[] GetData()
    {
        return mData;
    }

    public byte[] Send()
    {
        mTries ++;

        return mData;
    }

    public ssmCallBack<ssmMessage> GetCallBack()
    {
        return mReplyCallBack;
    }

    public int GetNumTries()
    {
        return mNumTries;
    }

    public boolean ShouldSend()
    {
        return mTries > mNumTries;
    }

    public boolean GetShouldReply()
    {
        return mShouldReply;
    }

    private static final byte[] sha256(byte[] data)
    {
        MessageDigest digest = null;
        try
        {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        byte[] encodedhash = digest.digest(data);

        return encodedhash;
    }

    public byte[] GetID()
    {
        return sha256(sha256(mData));
    }

    public int GetType()
    {
        return mType;
    }
}
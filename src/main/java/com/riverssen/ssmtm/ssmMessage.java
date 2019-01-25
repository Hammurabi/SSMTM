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
        this(callBack, numtries, type, data, null);
    }

    public ssmMessage(final ssmCallBack<ssmMessage> callBack, final int numtries, final int type, final byte[] data, ssmPeer peer)
    {
        this.mData          = data;
        this.mReplyCallBack = callBack;
        this.mNumTries      = numtries;
        this.mShouldReply   = callBack == null ? false : true;
        this.mType          = type;
        this.mReplyID       = new byte[0];
        this.mPeer          = peer;
    }

    private final int                       mType;
    private final byte[]                    mData;
    private final ssmCallBack<ssmMessage>   mReplyCallBack;
    private final int                       mNumTries;
    private int                             mTries;
    private ssmPeer                         mPeer;
    private final boolean                   mShouldReply;
    private final byte[]                    mReplyID;
    private long                            mLastTry;

    public ssmMessage(int type, boolean shouldReply, byte[] replyID, byte[] serializedMessage, ssmPeer peer)
    {
        this.mData          = serializedMessage;
        this.mType          = type;
        this.mPeer          = peer;
        this.mReplyCallBack = null;
        this.mNumTries      = 0;
        this.mShouldReply   = shouldReply;
        this.mReplyID       = replyID;
    }

    public byte[] GetData()
    {
        return mData;
    }

    public byte[] Send()
    {
        mTries ++;
        mLastTry = System.currentTimeMillis();

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
        return mNumTries > mTries;
    }

    public boolean CanSend()
    {
        return (System.currentTimeMillis() - mLastTry) > 500L;
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

    public byte[] GetReplyID()
    {
        return mReplyID;
    }

    public byte[] GetID()
    {
        return sha256(sha256(mData));
    }

    public int GetType()
    {
        return mType;
    }

    public ssmPeer GetPeer()
    {
        return mPeer;
    }
}
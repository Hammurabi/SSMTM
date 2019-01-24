package com.riverssen.ssmtm;

public class ssmMessage
{
    /**
     * @param callBack
     * @param numtries;
     * @param data      If event == null, any replies made to this message will be discarded and this message
     *                  will act as a notification message instead.
     */
    public ssmMessage(final ssmCallBack<ssmMessage> callBack, final int numtries, final byte[] data)
    {
        this.mData = data;
        this.mReplyCallBack = callBack;
        this.mNumTries = numtries;
    }

    private final byte[]                    mData;
    private final ssmCallBack<ssmMessage>   mReplyCallBack;
    private final int                       mNumTries;
    private int                             mTries;

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
}
package com.riverssen.ssmtm;

public abstract class ssmCallBack<T>
{
    private final long  mTimeOut;
    private long        mTimeLogged;

    public ssmCallBack(long timeout)
    {
        mTimeOut    = timeout;
        mTimeLogged = System.currentTimeMillis();
    }

    public abstract void CallBack(final T data);
    public boolean TimedOut()
    {
        return (System.currentTimeMillis() - mTimeLogged) > mTimeOut;
    }

    public void Log()
    {
        mTimeLogged = System.currentTimeMillis();
    }
}
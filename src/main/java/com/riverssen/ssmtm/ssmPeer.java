package com.riverssen.ssmtm;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ssmPeer
{
    private InetAddress mIPADDRESS;

    public ssmPeer(InetAddress address)
    {
        this.mIPADDRESS = address;
    }

    public ssmPeer(String address) throws UnknownHostException
    {
        this.mIPADDRESS = InetAddress.getByName(address);
    }

    public InetAddress GetAddress()
    {
        return mIPADDRESS;
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    @Override
    public String toString()
    {
        return mIPADDRESS.getHostAddress();
    }
}

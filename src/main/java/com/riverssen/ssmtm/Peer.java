package com.riverssen.ssmtm;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peer
{
    private InetAddress mIPADDRESS;

    public Peer(InetAddress address)
    {
        this.mIPADDRESS = address;
    }

    public Peer(String address) throws UnknownHostException
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

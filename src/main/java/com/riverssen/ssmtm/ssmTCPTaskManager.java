package com.riverssen.ssmtm;

import java.util.*;

public class ssmTCPTaskManager implements ssmTaskManager
{
    private final Set<ssmPeer>                  mPeers;
    private final Set<ssmCallBack<ssmMessage>>  mMessageCallbacks;
    private final Map<String, ssmMessage>       mMessages;
    private final Map<String, ssmTCPPeer>       mConnections;
    private final Queue<ssmMessage>             mReceivedQueue;

    public ssmTCPTaskManager()
    {
        this.mPeers             = new LinkedHashSet<ssmPeer>();
        this.mMessageCallbacks  = new LinkedHashSet<ssmCallBack<ssmMessage>>();
        this.mMessages          = new HashMap<String, ssmMessage>();
        this.mConnections       = new HashMap<String, ssmTCPPeer>();
        this.mReceivedQueue     = new LinkedList<ssmMessage>();
    }

    private void PollMessages()
    {
        for (ssmPeer peer : mPeers)
        {
            ssmTCPPeer pServer = mConnections.get(peer.toString());

            pServer.Poll(mReceivedQueue);
        }
    }

    public void SendMessage(ssmMessage message)
    {
    }

    public void SendMessage(ssmMessage message, ssmPeer peer)
    {
    }

    public void ForceConnect(ssmPeer peer)
    {
    }

    public Set<ssmPeer> GetConnected()
    {
        return null;
    }
}

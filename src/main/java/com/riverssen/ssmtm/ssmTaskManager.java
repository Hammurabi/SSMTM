package com.riverssen.ssmtm;

import java.util.Set;

public interface ssmTaskManager
{
    /**
     * @param message
     *
     * This will broadcast the message to all connected peers.
     */
    void SendMessage(ssmMessage message);

    /**
     * @param message
     * @param peer
     *
     * This will send a message to a specific peer.
     */
    void SendMessage(ssmMessage message, ssmPeer peer);

    /**
     * @param peer
     *
     * Attempt to connect to a peer.
     */
    void ForceConnect(ssmPeer peer);

    Set<ssmPeer> GetConnected();
}
package com.riverssen.ssmtm;

import java.util.Queue;
import java.util.Set;

public interface ssmTaskManager
{
    /**
     * @param message
     *
     * This will broadcast the message to all connected peers.
     */
    void SendMessage(final ssmMessage message);

    /**
     * @param message
     * @param peer
     *
     * This will send a message to a specific peer.
     */
    void SendMessage(final ssmMessage message, final ssmPeer peer);

    /**
     * @param peer
     *
     * Attempt to connect to a peer.
     */
    boolean ForceConnect(final ssmPeer peer, int port);
    boolean ForceDisconnect(final ssmPeer peer);

    Set<ssmPeer> GetConnected();

    Queue<ssmMessage> GetMessages();
}
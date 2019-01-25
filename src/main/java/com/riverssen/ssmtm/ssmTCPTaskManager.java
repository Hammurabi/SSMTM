package com.riverssen.ssmtm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ssmTCPTaskManager implements ssmTaskManager, Runnable
{
    private final Set<ssmPeer>                          mPeers;
    private final Map<byte[], ssmCallBack<ssmMessage>>  mMessageCallbacks;
    private final Map<byte[], ssmMessage>               mMessages;
    private final Map<String, ssmTCPPeer>               mConnections;
    private final Queue<ssmMessage>                     mReceivedQueue;
    private Lock                                        mLock;
    private boolean                                     mKeepRunning;
    private int                                         mConnectionLimit;

    public ssmTCPTaskManager(final int connectionLimit)
    {
        this.mPeers             = new LinkedHashSet<ssmPeer>();
        this.mMessageCallbacks  = new HashMap<byte[], ssmCallBack<ssmMessage>>();
        this.mMessages          = new HashMap<byte[], ssmMessage>();
        this.mConnections       = new HashMap<String, ssmTCPPeer>();
        this.mReceivedQueue     = new LinkedList<ssmMessage>();
        this.mConnectionLimit   = connectionLimit;
    }

    private void PollMessages()
    {
        for (ssmPeer peer : mPeers)
        {
            ssmTCPPeer pServer = mConnections.get(peer.toString());

            pServer.Poll(mReceivedQueue);
        }
    }

    public void SendMessage(final ssmMessage message)
    {
        for (ssmPeer peer : mPeers)
        {
            ssmTCPPeer pServer = mConnections.get(peer.toString());

            pServer.Send(message);

            if (message.GetShouldReply())
            {
                mMessageCallbacks.put(message.GetID(), message.GetCallBack());
                mMessages.put(message.GetID(), message);
                message.GetCallBack().Log();
                message.Send();
            }
        }
    }

    public void SendMessage(final ssmMessage message, final ssmPeer peer)
    {
        if (mConnections.containsKey(peer.toString()))
        {
            mConnections.get(peer.toString()).Send(message);

            if (message.GetShouldReply())
            {
                mMessageCallbacks.put(message.GetID(), message.GetCallBack());
                mMessages.put(message.GetID(), message);
                message.GetCallBack().Log();
            }
        }
    }

    public boolean ForceConnect(final ssmPeer peer, int port)
    {
        mLock.lock();
        boolean succeeded = false;

        try{
            if (!mConnections.containsKey(peer.toString()))
            {
                try {
                     Socket socket = new Socket(peer.GetAddress(), port);
                     ssmTCPPeer ssmTCPpeer = new ssmTCPPeer(peer, socket);
//                     ssmTCPpeer.start();

                    ExecutorService service = Executors.newSingleThreadExecutor();
                    service.execute(ssmTCPpeer);

                     mConnections.put(peer.toString(), ssmTCPpeer);
                     mPeers.add(peer);

                     succeeded = true;
                } catch (Exception e)
                {
                }
            }
        } finally
        {
            mLock.unlock();
            return succeeded;
        }
    }

    public void AddConnection(final ssmPeer peer, Socket socket)
    {
        mLock.lock();

        try
        {
            if (!mConnections.containsKey(peer.toString()) && mConnectionLimit > mConnections.size())
            {
                try
                {
                    ssmTCPPeer ssmTCPpeer = new ssmTCPPeer(peer, socket);
//                    ssmTCPpeer.start();

                    ExecutorService service = Executors.newSingleThreadExecutor();
                    service.execute(ssmTCPpeer);

                    mConnections.put(peer.toString(), ssmTCPpeer);
                    mPeers.add(peer);
                } catch (Exception e)
                {
                }
            } else
                socket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            mLock.unlock();
        }
    }

    public boolean ForceDisconnect(final ssmPeer peer)
    {
        mLock.lock();
        boolean succeeded = false;

        try
        {
            if (mConnections.containsKey(peer.toString()))
            {
                mConnections.get(peer.toString()).Abort();
                mPeers.remove(peer);

                succeeded = true;
            }
        } finally
        {
            mLock.unlock();
            return succeeded;
        }
    }

    public Set<ssmPeer> GetConnected()
    {
        return mPeers;
    }

    public Queue<ssmMessage> GetMessages()
    {
        return mReceivedQueue;
    }

    public void run()
    {
        mLock = new ReentrantLock();
        mKeepRunning = true;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ServerSocket mServerSocket = null;

            try
            {
                mServerSocket = new ServerSocket();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            if (mServerSocket == null)
                mKeepRunning = false;

            while (mKeepRunning)
            {
                try
                {
                    Socket socket = mServerSocket.accept();
                    ssmPeer peer = new ssmPeer(socket.getInetAddress());

                    AddConnection(peer, socket);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e)
                {
                }
            }
        });

        while (mKeepRunning)
        {
            mLock.lock();
            try
            {
                PollMessages();


                List<Integer> toRemove = new ArrayList<Integer>();

                for (int i = 0; i < mReceivedQueue.size(); i ++)
                {
                    ssmMessage message = ((LinkedList<ssmMessage>) mReceivedQueue).get(i);

                    if (message.GetType() == ssmMessageType.DISCONNECT)
                        ForceDisconnect(message.GetPeer());
                    else if (message.GetType() == ssmMessageType.PING)
                    {
                        ByteBuffer buffer = ByteBuffer.allocate(8);
                        buffer.putLong(System.currentTimeMillis());
                        buffer.flip();

                        SendMessage(new ssmMessage(null, 0, ssmMessageType.PONG, buffer.array()));
                    }

                    if (mMessageCallbacks.containsKey(message.GetReplyID()))
                    {
                        mMessages.remove(message.GetReplyID());
                        ssmCallBack<ssmMessage> mcb = mMessageCallbacks.get(message.GetReplyID());
                        mMessageCallbacks.remove(message.GetReplyID());

                        mcb.CallBack(message);

                        toRemove.add(i);
                    }
                }

                for (int i : toRemove)
                    ((LinkedList<ssmMessage>) mReceivedQueue).remove(i);

                List<byte[]> remove = new ArrayList<byte[]>();

                for (byte[] id : mMessageCallbacks.keySet())
                    if (mMessageCallbacks.get(id).TimedOut())
                        remove.add(id);
                    else
                    {
                        ssmMessage message = mMessages.get(id);

                        if (!message.ShouldSend())
                            remove.add(id);
                        else if (message.ShouldSend() && message.CanSend())
                        {
                            message.Send();

                            if (message.GetPeer() == null)
                                SendMessage(message);
                            else
                                SendMessage(message, message.GetPeer());
                        }
                    }

                for (byte[] tRemove : remove)
                {
                    mMessageCallbacks.remove(tRemove);
                    mMessages.remove(tRemove);
                }
            } finally
            {
                mLock.unlock();
            }

            try
            {
                Thread.sleep(25);
            } catch (Exception e)
            {
            }
        }
    }
}

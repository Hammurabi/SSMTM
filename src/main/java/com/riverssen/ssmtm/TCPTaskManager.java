package com.riverssen.ssmtm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TCPTaskManager implements TaskManager
{
    private       Set<Peer>                             mPeers;
    private       Map<byte[], SSMCallBack<Message>>     mMessageCallbacks;
    private       Map<byte[], Message>                  mMessages;
    private       Map<String, TCPPeer>                  mConnections;
    private       List<Message>                         mReceivedQueue;
//    private Lock                                        mLock;
    private AtomicBoolean                               mKeepRunning;
    private int                                         mConnectionLimit;
    private List<Command>                               mCommands;
    private short                                       mPort;
    private SSMCallBack<Peer>                           mDisconnectionCallback;
    private RPCRuntime                                  mRPCEnvironment;
    private       Set<Peer>                             mBlockedPeers;
    private List<ConnectionTicket>                      mForceConnections;
    private List<Peer>                                  mForceDisconnections;
//    private List<Integer>                               mForceAbort;

    private class ConnectionTicket
    {
        final Peer  toPerr;
        final int   toPort;

        ConnectionTicket(final Peer to, int port)
        {
            this.toPerr = to;
            this.toPort = port;
        }
    }

    public TCPTaskManager()
    {
    }

    private void PollMessages()
    {
        for (Peer peer : mPeers)
        {
            TCPPeer pServer = mConnections.get(peer.toString());

            pServer.Poll(mReceivedQueue);
        }
    }

    @Override
    public void Setup(int connectionlimit, int port, SSMCallBack<Peer> disconnectionCallback, RPCRuntime rpcEnvironment)
    {
        this.mConnectionLimit   = connectionlimit;
        this.mPort              = (short) port;
        this.mDisconnectionCallback = disconnectionCallback;
        this.mRPCEnvironment    = rpcEnvironment;


        this.mPeers             = Collections.synchronizedSet(new LinkedHashSet<>());
        this.mMessageCallbacks  = Collections.synchronizedMap(new HashMap<>());
        this.mMessages          = Collections.synchronizedMap(new HashMap<>());
        this.mConnections       = Collections.synchronizedMap(new HashMap<>());
        this.mReceivedQueue     = Collections.synchronizedList(new LinkedList<>());
        this.mCommands          = Collections.synchronizedList(new ArrayList<>());
        this.mBlockedPeers      = Collections.synchronizedSet(new LinkedHashSet<>());
        this.mForceConnections  = Collections.synchronizedList(new LinkedList<>());
        this.mForceDisconnections = Collections.synchronizedList(new LinkedList<>());
        this.mKeepRunning       = new AtomicBoolean(false);
    }

    public void SendMessage(final Message message)
    {
        synchronized (mPeers)
        {
            for (Peer peer : mPeers)
            {
                TCPPeer pServer = mConnections.get(peer.toString());

                pServer.Send(message);
            }
        }

        if (message.GetShouldReply())
        {
            mMessageCallbacks.put(message.GetID(), message.GetCallBack());
            mMessages.put(message.GetID(), message);
            message.GetCallBack().Log();
            message.Send();
        }
    }

    public void SendMessage(final Message message, final Peer peer)
    {
        if (mConnections.containsKey(peer.toString()))
        {
            mConnections.get(peer.toString()).Send(message);
        }

        if (message.GetShouldReply())
        {
            mMessageCallbacks.put(message.GetID(), message.GetCallBack());
            mMessages.put(message.GetID(), message);
            message.GetCallBack().Log();
        }
    }

    public void SendMessageToAllExcept(final Message message, final Peer nosend)
    {
        synchronized (mPeers)
        {
            for (Peer peer : mPeers)
            {
                if (peer == nosend)
                    continue;

                TCPPeer pServer = mConnections.get(peer.toString());

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
    }

    public boolean ForceConnect(final Peer peer, final int port)
    {
        mForceConnections.add(new ConnectionTicket(peer, port));

        return true;
    }

    private void AddConnection(final Peer peer, Socket socket)
    {
//        mLock.lock();

        try
        {
            if (!mBlockedPeers.contains(peer) && !mConnections.containsKey(peer.toString()) && mConnectionLimit > mConnections.size())
            {
                try
                {
                    TCPPeer TCPpeer = new TCPPeer(peer, socket);

                    ExecutorService service = Executors.newSingleThreadExecutor();
                    service.execute(TCPpeer);

                    mConnections.put(peer.toString(), TCPpeer);
                    mPeers.add(peer);

                    System.out.println("connected to: " + peer);
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
//            mLock.unlock();
        }
    }

    public boolean ForceDisconnect(final Peer peer)
    {
        mForceDisconnections.add(peer);

        return true;
    }

    @Override
    public boolean IsConnected(Peer peer)
    {
        AtomicBoolean connected = new AtomicBoolean(false);

        synchronized (mPeers)
        {
            for (Peer p : mPeers)
                if (p.equals(peer))
                {
                    connected.set(true);
                    break;
                }
        }

        return connected.get();
    }

    @Override
    public void BlockPeer(final Peer peer)
    {
        mBlockedPeers.add(peer);
        mForceDisconnections.add(peer);
    }

    public Set<Peer> GetConnected()
    {
        return mPeers;
    }

    public List<Message> GetMessages()
    {
        return mReceivedQueue;
    }

    @Override
    public void RegisterCommand(final int command, final CommandExecutor executorRunnable)
    {
        this.mCommands.add(new Command(command, executorRunnable));
    }

    @Override
    public void AbortOperations()
    {
        mKeepRunning.set(false);
    }

    public void run()
    {
//        mLock = new ReentrantLock();
        mKeepRunning.set(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ServerSocket mServerSocket = null;

            try
            {
                mServerSocket = new ServerSocket(mPort);
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            if (mServerSocket == null)
                mKeepRunning.set(false);

            while (mKeepRunning.get())
            {
                try
                {
                    Socket socket = mServerSocket.accept();
                    Peer peer = new Peer(socket.getInetAddress());

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

        while (mKeepRunning.get())
        {
//            mLock.lock();
            try
            {
                PollMessages();

                synchronized (mForceConnections)
                {
                    for (ConnectionTicket ticket : mForceConnections)
                    {
                        if (!mConnections.containsKey(ticket.toPerr.toString()))
                        {
                            try {
                                Socket socket = new Socket(ticket.toPerr.GetAddress(), ticket.toPort);
                                TCPPeer TCPpeer = new TCPPeer(ticket.toPerr, socket);

                                ExecutorService service = Executors.newSingleThreadExecutor();
                                service.execute(TCPpeer);

                                mConnections.put(ticket.toPerr.toString(), TCPpeer);
                                mPeers.add(ticket.toPerr);

                                System.out.println("connected to: " + ticket.toPerr);

                            } catch (Exception e)
                            {
                                if (mConnections.containsKey(ticket.toPerr.toString()))
                                    mConnections.remove(ticket.toPerr.toString());
                                if (mPeers.contains(ticket.toPerr))
                                    mPeers.remove(ticket.toPerr);
                            }
                        }
                    }
                }

                synchronized (mForceDisconnections)
                {
                    for (Peer peer : mForceDisconnections)
                    {
                        if (mConnections.containsKey(peer.toString()))
                        {
                            mConnections.get(peer.toString()).Abort();
                            mPeers.remove(peer);
                        }
                    }
                }

//                for (int i : mForceAbort)
//                {
//                    mKeepRunning = false;
//                }

                mForceConnections.clear();
                mForceDisconnections.clear();

//                List<Integer> toRemove = new ArrayList<Integer>();

                synchronized (mReceivedQueue)
                {
                    for (int i = 0; i < mReceivedQueue.size(); i ++)
                    {
                        Message message = ((LinkedList<Message>) mReceivedQueue).get(i);

                        if (message.GetType() == MessageType.DISCONNECT)
                        {
                            ForceDisconnect(message.GetPeer());
                            mDisconnectionCallback.CallBack(message.GetPeer());
                        } else if (message.GetType() == MessageType.MESSAGE_RECEIVED_SUCCESSFULLY)
                        {
                            mMessageCallbacks.remove(message.GetReplyID());
                            mMessages.remove(message.GetReplyID());
                        } else if (message.GetType() == MessageType.MESSAGE_RECEIVED_CORRUPTED)
                        {
                            if (mMessageCallbacks.containsKey(message.GetReplyID()))
                                SendMessage(mMessages.get(message.GetData()), message.GetPeer());
                        } else if (message.GetType() == MessageType.RPC_COMMAND)
                            mRPCEnvironment.Execute(message, this);

                        for (Command command : mCommands)
                            if (command.GetCommand() == message.GetType())
                                command.Execute(message);

                        if (mMessageCallbacks.containsKey(message.GetReplyID()))
                        {
                            mMessages.remove(message.GetReplyID());
                            SSMCallBack<Message> mcb = mMessageCallbacks.get(message.GetReplyID());
                            mMessageCallbacks.remove(message.GetReplyID());

                            mcb.CallBack(message);
                        }
                    }
                }

                mReceivedQueue.clear();

                List<byte[]> remove = new ArrayList<byte[]>();

                synchronized (mMessageCallbacks)
                {
                    for (byte[] id : mMessageCallbacks.keySet())
                        if (mMessageCallbacks.get(id).TimedOut())
                            remove.add(id);
                        else
                        {
                            Message message = mMessages.get(id);

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
                }


                for (byte[] tRemove : remove)
                {
                    mMessageCallbacks.remove(tRemove);
                    mMessages.remove(tRemove);
                }
            } finally
            {
//                mLock.unlock();
            }

//            try
//            {
//                Thread.sleep(25);
//            } catch (Exception e)
//            {
//            }
        }

        synchronized (mConnections)
        {
            for (TCPPeer peer : mConnections.values())
                peer.Abort();
        }
    }
}

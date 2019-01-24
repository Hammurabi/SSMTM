package com.riverssen.ssmtm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ssmTCPPeer implements Runnable
{
    private final Queue<ssmMessage>     mMessageSendQueue;
    private final Queue<ssmMessage>     mMessageReceiveQueue;
    private final ssmPeer               mPeer;
    private Lock                        mLock;
    private boolean                     mKeepRunning;
    private Socket                      mSocket;

    private DataOutputStream            mDataOutputStream;
    private DataInputStream             mDataInputStream;

    private AtomicLong                  mNumCorrupted;

    public ssmTCPPeer(ssmPeer peer, Socket socket) throws IOException
    {
        this.mPeer                  = peer;
        this.mMessageSendQueue      = new LinkedList<ssmMessage>();
        this.mMessageReceiveQueue   = new LinkedList<ssmMessage>();
        this.mSocket                = socket;

        this.mDataOutputStream      = new DataOutputStream(socket.getOutputStream());
        this.mDataInputStream       = new DataInputStream(socket.getInputStream());
        this.mNumCorrupted          = new AtomicLong(0);
    }

    public void Poll(final Queue<ssmMessage> receiverQueue)
    {
        mLock.lock();
        try{
            receiverQueue.addAll(mMessageReceiveQueue);
            mMessageReceiveQueue.clear();
        } finally
        {
            mLock.unlock();
        }
    }

    public void Send(final ssmMessage message)
    {
        mLock.lock();
        try{
            mMessageSendQueue.add(message);
        } finally
        {
            mLock.unlock();
        }
    }

    public void Abort()
    {
        mLock.lock();
        try{
            mKeepRunning = false;
        } finally
        {
            mLock.unlock();
        }
    }

    public AtomicLong GetCorruptedMessages()
    {
        return mNumCorrupted;
    }

    public boolean GetShouldBlock()
    {
        return mNumCorrupted.get() > 20;
    }

    private void Send(final int type, final boolean shouldReply, final byte digest[], final byte[] message)
    {
        try {
            /**
             * We send the digest first (message->id(sha256(sha256(message->data)))
             */
            mDataOutputStream.write(digest);

            /**
             * We then write out the type of the message.
             */
            mDataOutputStream.writeInt(type);

            /**
             * Then we send information about this messages reply policy.
             */
            mDataOutputStream.writeBoolean(shouldReply);

            /**
             * Then we supply the message raw data size.
             */
            mDataOutputStream.writeInt(message.length);
            /**
             * Then we send the message data.
             */
            mDataOutputStream.write(message);

            /**
             * Finally, we flush the outputstream to get the message out there.
             */
            mDataOutputStream.flush();
        } catch (IOException e)
        {
        }
    }

    private void Receive()
    {
        byte digest[]   = new byte[32];

        try {
            mDataInputStream.readFully(digest);

            try {
                int  type       = mDataInputStream.readByte();

                boolean reply   = mDataInputStream.readBoolean();

                int length      = mDataInputStream.readInt();
                byte message[]  = new byte[length];

                mDataInputStream.readFully(message);

                mMessageReceiveQueue.add(new ssmMessage(type, reply, message, mPeer));
            } catch (Exception e)
            {
                mNumCorrupted.set(mNumCorrupted.get() + 1);
                ssmMessage message = new MessageCorrupted(digest);

                Send(message.GetType(), true, message.GetID(), message.GetData());
            }
        } catch (Exception e)
        {
            mNumCorrupted.set(mNumCorrupted.get() + 1);
        }
    }

    public void run()
    {
        mLock = new ReentrantLock();
        mKeepRunning = true;

        while (mKeepRunning)
        {
            mLock.lock();
            try{
                for (ssmMessage message : mMessageSendQueue)
                    Send(message.GetType(), message.GetShouldReply(), message.GetID(), message.GetData());

                mMessageSendQueue.clear();

                while (mDataInputStream.available() > 0)
                    Receive();
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                mLock.unlock();
            }

            try {
                Thread.sleep(100);
            } catch (Exception e)
            {
            }
        }

        try {
            mDataOutputStream.close();
            mDataInputStream.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

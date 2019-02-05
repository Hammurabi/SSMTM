package com.riverssen.ssmtm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TCPPeer implements Runnable
{
    private final List<Message>         mMessageSendQueue;
    private final List<Message>         mMessageReceiveQueue;
    private final Peer                  mPeer;
    private boolean                     mKeepRunning;
    private Socket                      mSocket;

    private DataOutputStream            mDataOutputStream;
    private DataInputStream             mDataInputStream;

    private AtomicLong                  mNumCorrupted;

    public TCPPeer(Peer peer, Socket socket) throws IOException
    {
        this.mPeer                  = peer;
        this.mMessageSendQueue      = Collections.synchronizedList(new LinkedList<Message>());
        this.mMessageReceiveQueue   = Collections.synchronizedList(new LinkedList<Message>());
        this.mSocket                = socket;

        this.mDataOutputStream      = new DataOutputStream(socket.getOutputStream());
        this.mDataInputStream       = new DataInputStream(socket.getInputStream());
        this.mNumCorrupted          = new AtomicLong(0);
    }

    public void Poll(final List<Message> receiverQueue)
    {
        synchronized (mMessageReceiveQueue)
        {
            receiverQueue.addAll(mMessageReceiveQueue);
            mMessageReceiveQueue.clear();
        }
    }

    public void Send(final Message message)
    {
        mMessageSendQueue.add(message);
    }

    public void Abort()
    {
        mKeepRunning = false;
    }

    public AtomicLong GetCorruptedMessages()
    {
        return mNumCorrupted;
    }

    public boolean GetShouldBlock()
    {
        return mNumCorrupted.get() > 20;
    }

    private void Send(final int type, final boolean shouldReply, final byte digest[], final byte replyID[], final byte[] message)
    {
        try {
            /**
             * We send the digest first (message->id(sha256(sha256(message->data)))
             */
            mDataOutputStream.write(digest);

            /**
             * We send the reply-id (previous message digest) (message->replyid)
             */
            mDataOutputStream.writeByte(replyID.length);
            if (replyID.length > 0)
                mDataOutputStream.write(replyID);

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
                byte replyID[]  = new byte[mDataInputStream.read()];
                if (replyID.length > 0)
                    mDataInputStream.readFully(replyID);

                int  type       = mDataInputStream.readByte();

                boolean reply   = mDataInputStream.readBoolean();

                int length      = mDataInputStream.readInt();
                byte message[]  = new byte[length];

                mDataInputStream.readFully(message);

                mMessageReceiveQueue.add(new Message(type, reply, replyID, message, mPeer));
            } catch (Exception e)
            {
                mNumCorrupted.set(mNumCorrupted.get() + 1);
                Message message = new MessageCorrupted(digest);

                Send(message.GetType(), true, message.GetID(), message.GetReplyID(), message.GetData());
            }
        } catch (Exception e)
        {
            mNumCorrupted.set(mNumCorrupted.get() + 1);
        }
    }

    @Override
    public void run()
    {
        mKeepRunning = true;

        while (mKeepRunning)
        {
            try{
                synchronized (mMessageSendQueue)
                {
                    for (Message message : mMessageSendQueue)
                        Send(message.GetType(), message.GetShouldReply(), message.GetID(), message.GetReplyID(), message.GetData());
                }

                mMessageSendQueue.clear();

                while (mDataInputStream.available() > 0)
                    Receive();
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
            }
        }

        try {
            mDataOutputStream.close();
            mDataInputStream.close();

            mSocket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

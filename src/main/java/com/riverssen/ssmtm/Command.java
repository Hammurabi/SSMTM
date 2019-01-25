package com.riverssen.ssmtm;

public class Command
{
    private final int               mCommand;
    private final CommandExecutor   mRunnable;

    public Command(final int cmd, final CommandExecutor runnable)
    {
        this.mCommand   = cmd;
        this.mRunnable  = runnable;
    }

    public final int GetCommand()
    {
        return mCommand;
    }

    public final CommandExecutor GetExecutor()
    {
        return mRunnable;
    }

    public void Execute(final Message message)
    {
        mRunnable.Execute(message);
    }
}

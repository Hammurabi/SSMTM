package com.riverssen.ssmtm;

public interface RPCRuntime
{
    void Execute(final Message message, final TaskManager taskManager);
}
package com.momosoftworks.coldsweat.util.math;

import java.util.Collection;
import java.util.function.BiConsumer;

public class InterruptibleIterator<T>
{
    private boolean stopped = false;
    Collection<T> stream;

    public InterruptibleIterator(Collection<T> stream)
    {
        this.stream = stream;
    }

    public void stop()
    {
        stopped = true;
    }

    public void run(BiConsumer<T, InterruptibleIterator<T>> consumer)
    {
        for (T t : stream)
        {
            if (stopped)
            {
                break;
            }
            consumer.accept(t, this);
        }
    }
}

package com.momosoftworks.coldsweat.util.serialization;

import java.io.*;
import java.util.Base64;
import java.util.function.Predicate;

public class SerializablePredicate<T> implements Predicate<T>, Serializable
{
    private final Predicate<T> predicate;

    public SerializablePredicate(Predicate<T> predicate)
    {   this.predicate = predicate;
    }

    @Override
    public boolean test(T t)
    {   return predicate.test(t);
    }

    // Utility methods for serialization/deserialization to string
    public static <T> String serialize(SerializablePredicate<T> predicate)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(predicate);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
        catch (Exception e)
        {   throw new RuntimeException(e);
        }
    }

    public static <T> SerializablePredicate<T> deserialize(String serialized)
    {
        try
        {
            byte[] data = Base64.getDecoder().decode(serialized);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            return (SerializablePredicate<T>) ois.readObject();
        }
        catch (Exception e)
        {   throw new RuntimeException(e);
        }
    }
}

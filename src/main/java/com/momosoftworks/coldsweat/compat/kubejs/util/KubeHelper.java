package com.momosoftworks.coldsweat.compat.kubejs.util;

import com.momosoftworks.coldsweat.ColdSweat;

public class KubeHelper
{
    public static <T> boolean expect(String parsedFrom, T obj, Class<T> objType)
    {
        if (obj == null)
        {   ColdSweat.LOGGER.error("Failed to parse KubeJS script: {} with ID \"{}\" does not exist", objType, parsedFrom);
            return false;
        }
        return true;
    }
}

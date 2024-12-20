package com.momosoftworks.coldsweat.api.util;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;

import java.util.function.Predicate;

public class Placement
{
    private final Mode mode;
    private final Order order;
    private final Predicate<TempModifier> predicate;

    public Placement(Mode mode, Order order, Predicate<TempModifier> predicate)
    {
        this.mode = mode;
        this.order = order;
        this.predicate = predicate;
    }
    public static final Placement AFTER_LAST = Placement.of(Mode.AFTER, Order.LAST, mod -> true);
    public static final Placement BEFORE_FIRST = Placement.of(Mode.BEFORE, Order.FIRST, mod -> true);

    public Mode mode()
    {   return mode;
    }
    public Order order()
    {   return order;
    }
    public Predicate<TempModifier> predicate()
    {   return predicate;
    }

    public static Placement of(Mode mode, Order order, Predicate<TempModifier> predicate)
    {   return new Placement(mode, order, predicate);
    }
    public static Placement of(Mode mode, Order order, Class<? extends TempModifier> clazz)
    {   return new Placement(mode, order, clazz::isInstance);
    }

    public enum Mode implements StringRepresentable
    {
        // Inserts the new modifier before the targeted modifier's position
        BEFORE("before"),
        // Inserts the new modifier after the targeted modifier's position
        AFTER("after"),
        // Replace the desired instance of the modifier (fails if no modifiers pass the predicate)
        REPLACE("replace"),
        // Replace the desired instance of the modifier if it exists, otherwise add it to the end
        REPLACE_OR_ADD("replace_or_add");

        private final String name;

        Mode(String name)
        {   this.name = name;
        }

        public boolean isReplacing()
        {   return this == REPLACE || this == REPLACE_OR_ADD;
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Mode byName(String name)
        {
            for (Mode mode : values())
            {
                if (mode.name.equals(name))
                {   return mode;
                }
            }
            return BEFORE;
        }
    }

    public enum Order implements StringRepresentable
    {
        // Targets the first modifier that passes the predicate
        FIRST("first"),
        // Targets the last modifier that passes the predicate
        LAST("last");

        private final String name;

        Order(String name)
        {   this.name = name;
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Order byName(String name)
        {
            for (Order order : values())
            {
                if (order.name.equals(name))
                {   return order;
                }
            }
            return FIRST;
        }
    }

    public enum Duplicates implements StringRepresentable
    {
        // Do not check for duplicate TempModifiers
        ALLOW("allow"),
        // Checks if the TempModifier has the same class
        BY_CLASS("by_class"),
        // Checks if the TempModifier has the same class and NBT data
        EXACT("exact");

        private final String name;

        Duplicates(String name)
        {   this.name = name;
        }

        public static boolean check(Duplicates policy, TempModifier modA, TempModifier modB)
        {
            switch (policy)
            {
                case ALLOW    : return false;
                case BY_CLASS : return modA.getClass().equals(modB.getClass());
                case EXACT    : return modA.equals(modB);
                default       : return false;
            }
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Duplicates byName(String name)
        {
            for (Duplicates policy : values())
            {
                if (policy.name.equals(name))
                {   return policy;
                }
            }
            return ALLOW;
        }
    }
}

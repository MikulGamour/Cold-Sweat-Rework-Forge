package com.momosoftworks.coldsweat.util.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.util.math.CSMath;

public interface StringRepresentable
{
    int PRE_BUILT_MAP_THRESHOLD = 16;

    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> pElementsSupplier) {
        return fromEnumWithMapping(pElementsSupplier, (p_275327_) -> {
            return p_275327_;
        });
    }

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(Supplier<E[]> pEnumValues, Function<String, String> pKeyFunction) {
        E[] ae = pEnumValues.get();
        if (ae.length > 16) {
            Map<String, E> map = Arrays.stream(ae).collect(Collectors.toMap((p_274905_) -> {
                return pKeyFunction.apply(p_274905_.getSerializedName());
            }, (p_274903_) -> {
                return p_274903_;
            }));
            return new StringRepresentable.EnumCodec<>(ae, (p_216438_) -> {
                return (E)(p_216438_ == null ? null : map.get(p_216438_));
            });
        } else {
            return new StringRepresentable.EnumCodec<>(ae, (p_274908_) -> {
                for(E e : ae) {
                    if (pKeyFunction.apply(e.getSerializedName()).equals(p_274908_)) {
                        return e;
                    }
                }

                return (E)null;
            });
        }
    }

    static Keyable keys(final StringRepresentable[] pSerializables) {
        return new Keyable() {
            public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
                return Arrays.stream(pSerializables).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
            }
        };
    }

    /** @deprecated */
    @Deprecated
    public static class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E>
    {
        private final Codec<E> codec;
        private final Function<String, E> resolver;

        public EnumCodec(E[] pValues, Function<String, E> pResolver) {
            this.codec = ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec((p_216461_) -> {
                return p_216461_.getSerializedName();
            }, pResolver), ExtraCodecs.idResolverCodec((p_216454_) -> {
                return p_216454_.ordinal();
            }, (p_216459_) -> {
                return (E)(p_216459_ >= 0 && p_216459_ < pValues.length ? pValues[p_216459_] : null);
            }, -1));
            this.resolver = pResolver;
        }

        public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> pOps, T pInput) {
            return this.codec.decode(pOps, pInput);
        }

        public <T> DataResult<T> encode(E pInput, DynamicOps<T> pOps, T pPrefix) {
            return this.codec.encode(pInput, pOps, pPrefix);
        }

        @Nullable
        public E byName(@Nullable String pName) {
            return this.resolver.apply(pName);
        }

        public E byName(@Nullable String pName, E pDefaultValue) {
            return CSMath.orElse(this.byName(pName), pDefaultValue);
        }
    }
}

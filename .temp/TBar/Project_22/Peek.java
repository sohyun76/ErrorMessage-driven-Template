package com.jnape.palatable.lambda.functions.builtin.fn2;

import com.jnape.palatable.lambda.functions.Effect;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.Fn2;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.io.IO;

/**
 * Given an {@link Effect}, "peek" at the value contained inside a {@link Functor} via {@link Functor#fmap(Fn1)},
 * applying the {@link Effect} to the contained value, if there is one.
 *
 * @param <A>  the functor parameter type
 * @param <FA> the functor type
 * @deprecated in favor of producing an {@link IO} from the given {@link Functor} and explicitly running it
 */
@Deprecated
public final class Peek<A, FA extends Functor<A, ?>> implements Fn2<Fn1<? super A, ? extends IO<?>>, FA, FA> {
    private static final Peek<?, ?> INSTANCE = new Peek<>();

    private Peek() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public FA checkedApply(Fn1<? super A, ? extends IO<?>> effect, FA fa) {
        return (FA) fa.fmap(a -> {
            effect.apply(a).unsafePerformIO();
            return a;
        }).coerce();
    }

    @SuppressWarnings("unchecked")
    public static <A, FA extends Functor<A, ?>> Peek<A, FA> peek() {
        return (Peek<A, FA>) INSTANCE;
    }

    public static <A, FA extends Functor<A, ?>> Fn1<FA, FA> peek(Fn1<? super A, ? extends IO<?>> effect) {
        return Peek.<A, FA>peek().apply(effect);
    }

    public static <A, FA extends Functor<A, ?>> FA peek(Fn1<? super A, ? extends IO<?>> effect, FA fa) {
        return Peek.<A, FA>peek(effect).apply(fa);
    }
}

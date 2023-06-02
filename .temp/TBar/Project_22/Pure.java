package com.jnape.palatable.lambda.functions.specialized;

import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.internal.Runtime;

import static com.jnape.palatable.lambda.functions.builtin.fn1.Downcast.downcast;

/**
 * Generalized, portable {@link Applicative#pure(Object)}, with a loosened {@link Functor} constraint.
 *
 * @param <F> the {@link Functor} to lift into
 */
@FunctionalInterface
public interface Pure<F extends Functor<?, ? extends F>> {

    <A> Functor<A, ? extends F> checkedApply(A a) throws Throwable;

    default <A, FA extends Functor<A, ? extends F>> FA apply(A a) {
        try {
            @SuppressWarnings("unchecked") FA fa = downcast(checkedApply(a));
            return fa;
        } catch (Throwable t) {
            throw Runtime.throwChecked(t);
        }
    }

    /**
     * Static method to aid inference.
     *
     * @param pure the {@link Pure}
     * @param <F>  the {@link Functor} witness
     * @return the {@link Pure}
     */
    static <F extends Functor<?, ? extends F>> Pure<F> pure(Pure<F> pure) {
        return pure;
    }

    /**
     * Extract an {@link Applicative Applicative's} {@link Applicative#pure(Object) pure} implementation to an instance
     * of {@link Pure}.
     *
     * @param app the {@link Applicative}
     * @param <F> the witness
     * @return the {@link Pure}
     */
    static <F extends Applicative<?, ? extends F>> Pure<F> of(Applicative<?, ? extends F> app) {
        return app::pure;
    }
}

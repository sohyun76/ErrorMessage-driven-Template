package com.jnape.palatable.lambda.functor;

import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.functions.builtin.fn1.Downcast.downcast;

/**
 * An interface for the generic covariant functorial operation <code>map</code> over some parameter <code>A</code>.
 * Functors are foundational to many of the classes provided by this library; generally, anything that can be thought of
 * as "mappable" is an instance of at least this interface.
 * <p>
 * For more information, read about <a href="https://en.wikipedia.org/wiki/Functor" target="_top">Functors</a>.
 *
 * @param <A> The type of the parameter
 * @param <F> The unification parameter
 * @see Bifunctor
 * @see Profunctor
 * @see Fn1
 * @see com.jnape.palatable.lambda.adt.hlist.Tuple2
 * @see com.jnape.palatable.lambda.adt.Either
 */
@FunctionalInterface
public interface Functor<A, F extends Functor<?, F>> {

    /**
     * Covariantly transmute this functor's parameter using the given mapping function. Generally this method is
     * specialized to return an instance of the class implementing Functor.
     *
     * @param <B> the new parameter type
     * @param fn  the mapping function
     * @return a functor over B (the new parameter type)
     */
    <B> Functor<B, F> fmap(Fn1<? super A, ? extends B> fn);

    /**
     * Convenience method for coercing this functor instance into another concrete type. Unsafe.
     *
     * @param <Concrete> the concrete functor instance to coerce this functor to
     * @return the coerced functor
     */
    default <Concrete extends Functor<A, F>> Concrete coerce() {
        return downcast(this);
    }
}

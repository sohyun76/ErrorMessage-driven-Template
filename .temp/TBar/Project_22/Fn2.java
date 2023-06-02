package com.jnape.palatable.lambda.functions;

import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.adt.product.Product2;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.internal.Runtime;

import java.util.function.BiFunction;

import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.functions.Fn3.fn3;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;

/**
 * A function taking two arguments.
 * <p>
 * Note that defining {@link Fn2} in terms of <code>Fn1</code> provides a reasonable approximation of currying in the
 * form of multiple {@link Fn2#apply} overloads that take different numbers of arguments.
 *
 * @param <A> The first argument type
 * @param <B> The second argument type
 * @param <C> The return type
 * @see Fn1
 */
@FunctionalInterface
public interface Fn2<A, B, C> extends Fn1<A, Fn1<B, C>> {

    C checkedApply(A a, B b) throws Throwable;

    /**
     * Invoke this function with the given arguments.
     *
     * @param a the first argument
     * @param b the second argument
     * @return the result of the function application
     */
    default C apply(A a, B b) {
        try {
            return checkedApply(a, b);
        } catch (Throwable t) {
            throw Runtime.throwChecked(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Fn1<B, C> checkedApply(A a) throws Throwable {
        return b -> checkedApply(a, b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <Z> Fn3<Z, A, B, C> widen() {
        return fn3(constantly(this));
    }

    /**
     * Partially apply this function by passing its first argument.
     *
     * @param a the first argument
     * @return an {@link Fn1}&lt;B, C&gt;
     */
    @Override
    default Fn1<B, C> apply(A a) {
        return (b) -> apply(a, b);
    }

    /**
     * Flip the order of the arguments.
     *
     * @return an {@link Fn2}&lt;B, A, C&gt;
     */
    default Fn2<B, A, C> flip() {
        return (b, a) -> apply(a, b);
    }

    /**
     * Returns an {@link Fn1} that takes the arguments as a <code>{@link Product2}&lt;A, B&gt;</code>.
     *
     * @return an {@link Fn1} taking a {@link Product2}
     */
    default Fn1<? super Product2<? extends A, ? extends B>, C> uncurry() {
        return (ab) -> apply(ab._1(), ab._2());
    }

    /**
     * View this {@link Fn2} as a {@link BiFunction}.
     *
     * @return the same logic as a {@link BiFunction}
     * @see BiFunction
     */
    default BiFunction<A, B, C> toBiFunction() {
        return this::apply;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <D> Fn2<A, B, C> discardR(Applicative<D, Fn1<A, ?>> appB) {
        return curried(Fn1.super.discardR(appB));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <Z> Fn2<Z, B, C> diMapL(Fn1<? super Z, ? extends A> fn) {
        return curried(Fn1.super.diMapL(fn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <Z> Fn2<Z, B, C> contraMap(Fn1<? super Z, ? extends A> fn) {
        return curried(Fn1.super.contraMap(fn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default <Y, Z> Fn3<Y, Z, B, C> compose(Fn2<? super Y, ? super Z, ? extends A> before) {
        return fn3(Fn1.super.compose(before));
    }

    /**
     * Static factory method for wrapping a {@link BiFunction} in an {@link Fn2}.
     *
     * @param biFunction the biFunction to adapt
     * @param <A>        the first input argument type
     * @param <B>        the second input argument type
     * @param <C>        the output type
     * @return the {@link Fn2}
     */
    static <A, B, C> Fn2<A, B, C> fromBiFunction(BiFunction<? super A, ? super B, ? extends C> biFunction) {
        return biFunction::apply;
    }

    /**
     * Static factory method for wrapping a curried {@link Fn1} in an {@link Fn2}.
     *
     * @param curriedFn1 the curried fn1 to adapt
     * @param <A>        the first input argument type
     * @param <B>        the second input argument type
     * @param <C>        the output type
     * @return the {@link Fn2}
     */
    static <A, B, C> Fn2<A, B, C> curried(Fn1<A, Fn1<B, C>> curriedFn1) {
        return (a, b) -> curriedFn1.apply(a).apply(b);
    }

    /**
     * Static factory method for wrapping an uncurried {@link Fn1} in an {@link Fn2}.
     *
     * @param uncurriedFn1 the uncurried {@link Fn1} to adapt
     * @param <A>          the first input argument type
     * @param <B>          the second input argument type
     * @param <C>          the output type
     * @return the {@link Fn2}
     */
    static <A, B, C> Fn2<A, B, C> curry(Fn1<? super Tuple2<A, B>, ? extends C> uncurriedFn1) {
        return (a, b) -> uncurriedFn1.apply(tuple(a, b));
    }

    /**
     * Static method to aid inference.
     *
     * @param fn2 the {@link Fn2}
     * @param <A> the first input type
     * @param <B> the second input type
     * @param <C> the output type
     * @return the {@link Fn2}
     */
    static <A, B, C> Fn2<A, B, C> fn2(Fn2<A, B, C> fn2) {
        return fn2;
    }
}

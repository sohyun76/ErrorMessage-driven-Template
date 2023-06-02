package com.jnape.palatable.lambda.functions.builtin.fn2;

import com.jnape.palatable.lambda.adt.coproduct.CoProduct2;
import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.Fn2;

import java.util.Collections;

import static com.jnape.palatable.lambda.functions.builtin.fn1.Flatten.flatten;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Map.map;
import static java.util.Collections.emptySet;

/**
 * Given an <code>Iterable&lt;A&gt;</code> <code>as</code> and a disjoint mapping function <code>a -&gt;
 * CoProduct2&lt;A, B&gt;</code>, return a {@link Tuple2} over the lazily unwrapped left <code>A</code> and right
 * <code>B</code> values in the first and second slots, respectively. Note that while the tuple must be constructed
 * eagerly, the left and right iterables contained therein are both lazy, so comprehension over infinite iterables is
 * supported.
 *
 * @param <A> A type contravariant to the input Iterable element type
 * @param <B> The output left Iterable element type, as well as the CoProduct2 A type
 * @param <C> The output right Iterable element type, as well as the CoProduct2 B type
 * @see CoProduct2
 */
public final class Partition<A, B, C> implements Fn2<Fn1<? super A, ? extends CoProduct2<B, C, ?>>, Iterable<A>, Tuple2<Iterable<B>, Iterable<C>>> {

    private static final Partition<?, ?, ?> INSTANCE = new Partition<>();

    private Partition() {
    }

    @Override
    public Tuple2<Iterable<B>, Iterable<C>> checkedApply(Fn1<? super A, ? extends CoProduct2<B, C, ?>> function,
                                                         Iterable<A> as) {
        return Tuple2.<Iterable<CoProduct2<B, C, ?>>>fill(map(function, as))
                .biMap(Map.<CoProduct2<B, C, ?>, Iterable<B>>map(cp -> cp.match(Collections::singleton, __ -> emptySet())),
                       Map.<CoProduct2<B, C, ?>, Iterable<C>>map(cp -> cp.match(__ -> emptySet(), Collections::singleton)))
                .biMap(flatten(), flatten());
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C> Partition<A, B, C> partition() {
        return (Partition<A, B, C>) INSTANCE;
    }

    public static <A, B, C> Fn1<Iterable<A>, Tuple2<Iterable<B>, Iterable<C>>> partition(
            Fn1<? super A, ? extends CoProduct2<B, C, ?>> function) {
        return Partition.<A, B, C>partition().apply(function);
    }

    public static <A, B, C> Tuple2<Iterable<B>, Iterable<C>> partition(
            Fn1<? super A, ? extends CoProduct2<B, C, ?>> function,
            Iterable<A> as) {
        return Partition.<A, B, C>partition(function).apply(as);
    }
}

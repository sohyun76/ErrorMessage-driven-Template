package com.jnape.palatable.lambda.adt.coproduct;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.choice.Choice2;
import com.jnape.palatable.lambda.adt.product.Product3;
import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.functions.Fn1.fn1;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;

/**
 * A generalization of the coproduct of three types.
 *
 * @param <A>   the first possible type
 * @param <B>   the second possible type
 * @param <C>   the third possible type
 * @param <CP3> the recursive type of this coproduct (used for embedding)
 * @see CoProduct2
 */
@FunctionalInterface
public interface CoProduct3<A, B, C, CP3 extends CoProduct3<A, B, C, ?>> {

    /**
     * Type-safe convergence requiring a match against all potential types.
     *
     * @param <R> result type
     * @param aFn morphism <code>A -&gt; R</code>
     * @param bFn morphism <code>B -&gt; R</code>
     * @param cFn morphism <code>C -&gt; R</code>
     * @return the result of applying the appropriate morphism to this coproduct's unwrapped value
     * @see CoProduct2#match(Fn1, Fn1)
     */
    <R> R match(Fn1<? super A, ? extends R> aFn, Fn1<? super B, ? extends R> bFn, Fn1<? super C, ? extends R> cFn);

    /**
     * Diverge this coproduct by introducing another possible type that it could represent.
     *
     * @param <D> the additional possible type of this coproduct
     * @return a {@link CoProduct4}&lt;A, B, C, D&gt;
     * @see CoProduct2#diverge()
     */
    default <D> CoProduct4<A, B, C, D, ? extends CoProduct4<A, B, C, D, ?>> diverge() {
        return new CoProduct4<A, B, C, D, CoProduct4<A, B, C, D, ?>>() {
            @Override
            public <R> R match(Fn1<? super A, ? extends R> aFn, Fn1<? super B, ? extends R> bFn,
                               Fn1<? super C, ? extends R> cFn, Fn1<? super D, ? extends R> dFn) {
                return CoProduct3.this.match(aFn, bFn, cFn);
            }
        };
    }

    /**
     * Converge this coproduct down to a lower order coproduct by mapping the last possible type into an earlier
     * possible type. This is the categorical dual of {@link CoProduct2#diverge}, which introduces the type
     * <code>C</code> and raises the order from 2 to 3.
     * <p>
     * The following laws hold for any two coproducts of single order difference:
     * <ul>
     * <li><em>Cancellation</em>: <code>coProductN.diverge().converge(CoProductN::a) == coProductN</code></li>
     * </ul>
     *
     * @param convergenceFn function from last possible type to earlier type
     * @return a {@link CoProduct2}&lt;A, B&gt;
     */
    default CoProduct2<A, B, ? extends CoProduct2<A, B, ?>> converge(
            Fn1<? super C, ? extends CoProduct2<A, B, ?>> convergenceFn) {
        return match(Choice2::a, Choice2::b, convergenceFn::apply);
    }

    /**
     * Project this coproduct onto a product.
     *
     * @return a product of the coproduct projection
     * @see CoProduct2#project()
     */
    default Product3<Maybe<A>, Maybe<B>, Maybe<C>> project() {
        return match(a -> tuple(just(a), nothing(), nothing()),
                     b -> tuple(nothing(), just(b), nothing()),
                     c -> tuple(nothing(), nothing(), just(c)));
    }

    /**
     * Convenience method for projecting this coproduct onto a product and then extracting the first slot value.
     *
     * @return an optional value representing the projection of the "a" type index
     */
    default Maybe<A> projectA() {
        return project()._1();
    }

    /**
     * Convenience method for projecting this coproduct onto a product and then extracting the second slot value.
     *
     * @return an optional value representing the projection of the "b" type index
     */
    default Maybe<B> projectB() {
        return project()._2();
    }

    /**
     * Convenience method for projecting this coproduct onto a product and then extracting the third slot value.
     *
     * @return an optional value representing the projection of the "c" type index
     */
    default Maybe<C> projectC() {
        return project()._3();
    }

    /**
     * Embed this coproduct inside another value; that is, given morphisms from this coproduct to <code>R</code>, apply
     * the appropriate morphism to this coproduct as a whole. Like {@link CoProduct3#match}, but without unwrapping the
     * value.
     *
     * @param <R> result type
     * @param aFn morphism <code>A v B v C -&gt; R</code>, applied in the <code>A</code> case
     * @param bFn morphism <code>A v B v C -&gt; R</code>, applied in the <code>B</code> case
     * @param cFn morphism <code>A v B v C -&gt; R</code>, applied in the <code>C</code> case
     * @return the result of applying the appropriate morphism to this coproduct
     */
    @SuppressWarnings("unchecked")
    default <R> R embed(Fn1<? super CP3, ? extends R> aFn, Fn1<? super CP3, ? extends R> bFn,
                        Fn1<? super CP3, ? extends R> cFn) {
        return this.<Fn1<CP3, R>>match(constantly(fn1(aFn)),
                                       constantly(fn1(bFn)),
                                       constantly(fn1(cFn)))
                .apply((CP3) this);
    }
}

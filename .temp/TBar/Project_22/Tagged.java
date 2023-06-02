package com.jnape.palatable.lambda.functor.builtin;

import com.jnape.palatable.lambda.adt.choice.Choice2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.builtin.fn1.Downcast;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;
import com.jnape.palatable.lambda.functions.specialized.Pure;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.Cocartesian;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.MonadRec;
import com.jnape.palatable.lambda.traversable.Traversable;

import java.util.Objects;

import static com.jnape.palatable.lambda.adt.choice.Choice2.b;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;

/**
 * Like {@link Const}, but the phantom parameter is in the contravariant position, and the value is in covariant
 * position.
 *
 * @param <S> the phantom type
 * @param <B> the value type
 */
public final class Tagged<S, B> implements
        MonadRec<B, Tagged<S, ?>>,
        Traversable<B, Tagged<S, ?>>, Cocartesian<S, B, Tagged<?, ?>> {

    private final B b;

    public Tagged(B b) {
        this.b = b;
    }

    /**
     * Extract the contained value.
     *
     * @return the value
     */
    public B unTagged() {
        return b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> flatMap(Fn1<? super B, ? extends Monad<C, Tagged<S, ?>>> f) {
        return f.apply(b).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> trampolineM(Fn1<? super B, ? extends MonadRec<RecursiveResult<B, C>, Tagged<S, ?>>> fn) {
        return new Tagged<>(trampoline(b -> fn.apply(b).<Tagged<S, RecursiveResult<B, C>>>coerce().unTagged(),
                                       unTagged()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> pure(C c) {
        return new Tagged<>(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> fmap(Fn1<? super B, ? extends C> fn) {
        return MonadRec.super.<C>fmap(fn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> zip(Applicative<Fn1<? super B, ? extends C>, Tagged<S, ?>> appFn) {
        return MonadRec.super.zip(appFn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> discardL(Applicative<C, Tagged<S, ?>> appB) {
        return MonadRec.super.discardL(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, B> discardR(Applicative<C, Tagged<S, ?>> appB) {
        return MonadRec.super.discardR(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C, App extends Applicative<?, App>, TravC extends Traversable<C, Tagged<S, ?>>,
            AppTrav extends Applicative<TravC, App>> AppTrav traverse(Fn1<? super B, ? extends Applicative<C, App>> fn,
                                                                      Fn1<? super TravC, ? extends AppTrav> pure) {
        return fn.apply(b)
                .<TravC>fmap(c -> Downcast.<TravC, Traversable<C, Tagged<S, ?>>>downcast(new Tagged<>(c)))
                .coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<Choice2<C, S>, Choice2<C, B>> cocartesian() {
        return new Tagged<>(b(b));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Z, C> Tagged<Z, C> diMap(Fn1<? super Z, ? extends S> lFn, Fn1<? super B, ? extends C> rFn) {
        return new Tagged<>(rFn.apply(b));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Z> Tagged<Z, B> diMapL(Fn1<? super Z, ? extends S> fn) {
        return (Tagged<Z, B>) Cocartesian.super.<Z>diMapL(fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Tagged<S, C> diMapR(Fn1<? super B, ? extends C> fn) {
        return (Tagged<S, C>) Cocartesian.super.<C>diMapR(fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Z> Tagged<Z, B> contraMap(Fn1<? super Z, ? extends S> fn) {
        return (Tagged<Z, B>) Cocartesian.super.<Z>contraMap(fn);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Tagged<?, ?> && Objects.equals(b, ((Tagged<?, ?>) other).b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(b);
    }

    @Override
    public String toString() {
        return "Tagged{b=" + b + '}';
    }

    /**
     * The canonical {@link Pure} instance for {@link Tagged}.
     *
     * @param <S> the phantom type
     * @return the {@link Pure} instance
     */
    public static <S> Pure<Tagged<S, ?>> pureTagged() {
        return Tagged::new;
    }
}

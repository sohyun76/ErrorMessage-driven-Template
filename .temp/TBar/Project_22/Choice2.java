package com.jnape.palatable.lambda.adt.choice;

import com.jnape.palatable.lambda.adt.Either;
import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.coproduct.CoProduct2;
import com.jnape.palatable.lambda.adt.hlist.HList;
import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;
import com.jnape.palatable.lambda.functions.specialized.Pure;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.Bifunctor;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.MonadRec;
import com.jnape.palatable.lambda.traversable.Traversable;

import java.util.Objects;

import static com.jnape.palatable.lambda.functions.builtin.fn2.Into.into;
import static com.jnape.palatable.lambda.functions.recursion.RecursiveResult.terminate;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;
import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;

/**
 * Canonical ADT representation of {@link CoProduct2}. Unlike {@link Either}, there is no concept of "success" or
 * "failure", so the domain of reasonable function semantics is more limited.
 *
 * @param <A> the first possible type
 * @param <B> the second possible type
 * @see Either
 * @see Choice3
 */
public abstract class Choice2<A, B> implements
        CoProduct2<A, B, Choice2<A, B>>,
        MonadRec<B, Choice2<A, ?>>,
        Bifunctor<A, B, Choice2<?, ?>>,
        Traversable<B, Choice2<A, ?>> {

    private Choice2() {
    }

    /**
     * Specialize this choice's projection to a {@link Tuple2}.
     *
     * @return a {@link Tuple2}
     */
    @Override
    public Tuple2<Maybe<A>, Maybe<B>> project() {
        return into(HList::tuple, CoProduct2.super.project());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <C> Choice3<A, B, C> diverge() {
        return match(Choice3::a, Choice3::b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Choice2<B, A> invert() {
        return match(Choice2::b, Choice2::a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <C> Choice2<A, C> fmap(Fn1<? super B, ? extends C> fn) {
        return MonadRec.super.<C>fmap(fn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <C> Choice2<C, B> biMapL(Fn1<? super A, ? extends C> fn) {
        return (Choice2<C, B>) Bifunctor.super.<C>biMapL(fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <C> Choice2<A, C> biMapR(Fn1<? super B, ? extends C> fn) {
        return (Choice2<A, C>) Bifunctor.super.<C>biMapR(fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <C, D> Choice2<C, D> biMap(Fn1<? super A, ? extends C> lFn,
                                            Fn1<? super B, ? extends D> rFn) {
        return match(a -> a(lFn.apply(a)), b -> b(rFn.apply(b)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Choice2<A, C> pure(C c) {
        return b(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Choice2<A, C> zip(Applicative<Fn1<? super B, ? extends C>, Choice2<A, ?>> appFn) {
        return MonadRec.super.zip(appFn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Lazy<Choice2<A, C>> lazyZip(
            Lazy<? extends Applicative<Fn1<? super B, ? extends C>, Choice2<A, ?>>> lazyAppFn) {
        return match(a -> lazy(a(a)),
                     b -> lazyAppFn.fmap(choiceF -> choiceF.<C>fmap(f -> f.apply(b)).coerce()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Choice2<A, C> discardL(Applicative<C, Choice2<A, ?>> appB) {
        return MonadRec.super.discardL(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Choice2<A, B> discardR(Applicative<C, Choice2<A, ?>> appB) {
        return MonadRec.super.discardR(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <C> Choice2<A, C> flatMap(Fn1<? super B, ? extends Monad<C, Choice2<A, ?>>> f) {
        return match(Choice2::a, b -> f.apply(b).coerce());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Choice2<A, C> trampolineM(Fn1<? super B, ? extends MonadRec<RecursiveResult<B, C>, Choice2<A, ?>>> fn) {
        return match(Choice2::a,
                     trampoline(b -> fn.apply(b).<Choice2<A, RecursiveResult<B, C>>>coerce()
                             .match(a -> terminate(a(a)),
                                    bOrC -> bOrC.fmap(Choice2::b))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C, App extends Applicative<?, App>, TravB extends Traversable<C, Choice2<A, ?>>,
            AppTrav extends Applicative<TravB, App>> AppTrav traverse(Fn1<? super B, ? extends Applicative<C, App>> fn,
                                                                      Fn1<? super TravB, ? extends AppTrav> pure) {
        return match(a -> pure.apply(Choice2.<A, C>a(a).<TravB>coerce()),
                     b -> fn.apply(b).<Choice2<A, C>>fmap(Choice2::b).<TravB>fmap(Functor::coerce).coerce());
    }

    /**
     * Static factory method for wrapping a value of type <code>A</code> in a {@link Choice2}.
     *
     * @param a   the value
     * @param <A> the first possible type
     * @param <B> the second possible type
     * @return the wrapped value as a {@link Choice2}&lt;A, B&gt;
     */
    public static <A, B> Choice2<A, B> a(A a) {
        return new _A<>(a);
    }

    /**
     * Static factory method for wrapping a value of type <code>B</code> in a {@link Choice2}.
     *
     * @param b   the value
     * @param <A> the first possible type
     * @param <B> the second possible type
     * @return the wrapped value as a {@link Choice2}&lt;A, B&gt;
     */
    public static <A, B> Choice2<A, B> b(B b) {
        return new _B<>(b);
    }

    /**
     * The canonical {@link Pure} instance for {@link Choice2}.
     *
     * @param <A> the first possible type
     * @return the {@link Pure} instance
     */
    public static <A> Pure<Choice2<A, ?>> pureChoice() {
        return Choice2::b;
    }

    private static final class _A<A, B> extends Choice2<A, B> {

        private final A a;

        private _A(A a) {
            this.a = a;
        }

        @Override
        public <R> R match(Fn1<? super A, ? extends R> aFn, Fn1<? super B, ? extends R> bFn) {
            return aFn.apply(a);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof _A
                    && Objects.equals(a, ((_A) other).a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a);
        }

        @Override
        public String toString() {
            return "Choice2{" +
                    "a=" + a +
                    '}';
        }
    }

    private static final class _B<A, B> extends Choice2<A, B> {

        private final B b;

        private _B(B b) {
            this.b = b;
        }

        @Override
        public <R> R match(Fn1<? super A, ? extends R> aFn, Fn1<? super B, ? extends R> bFn) {
            return bFn.apply(b);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof _B
                    && Objects.equals(b, ((_B) other).b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(b);
        }

        @Override
        public String toString() {
            return "Choice2{" +
                    "b=" + b +
                    '}';
        }
    }
}

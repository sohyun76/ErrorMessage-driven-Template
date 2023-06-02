package com.jnape.palatable.lambda.adt;

import com.jnape.palatable.lambda.adt.choice.Choice2;
import com.jnape.palatable.lambda.adt.choice.Choice3;
import com.jnape.palatable.lambda.adt.coproduct.CoProduct2;
import com.jnape.palatable.lambda.adt.hlist.HList;
import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn0;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;
import com.jnape.palatable.lambda.functions.specialized.Pure;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.io.IO;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.MonadError;
import com.jnape.palatable.lambda.monad.MonadRec;
import com.jnape.palatable.lambda.traversable.Traversable;

import java.util.Objects;
import java.util.Optional;

import static com.jnape.palatable.lambda.adt.Either.left;
import static com.jnape.palatable.lambda.adt.Unit.UNIT;
import static com.jnape.palatable.lambda.functions.Fn0.fn0;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Id.id;
import static com.jnape.palatable.lambda.functions.recursion.RecursiveResult.terminate;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;
import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;
import static com.jnape.palatable.lambda.io.IO.io;

/**
 * The optional type, representing a potentially absent value. This is lambda's analog of {@link Optional}, supporting
 * all the usual suspects like {@link Functor}, {@link Applicative}, {@link Traversable}, etc.
 *
 * @param <A> the optional parameter type
 * @see Optional
 */
public abstract class Maybe<A> implements
        CoProduct2<Unit, A, Maybe<A>>,
        MonadError<Unit, A, Maybe<?>>,
        MonadRec<A, Maybe<?>>,
        Traversable<A, Maybe<?>> {

    private Maybe() {
    }

    /**
     * If the value is present, return it; otherwise, return the value supplied by <code>otherSupplier</code>.
     *
     * @param otherFn0 the supplier for the other value
     * @return this value, or the supplied other value
     */
    public final A orElseGet(Fn0<A> otherFn0) {
        return match(__ -> otherFn0.apply(), id());
    }

    /**
     * If the value is present, return it; otherwise, return <code>other</code>.
     *
     * @param other the other value
     * @return this value, or the other value
     */
    public final A orElse(A other) {
        return orElseGet(() -> other);
    }

    /**
     * If the value is present, return it; otherwise, throw the {@link Throwable} supplied by
     * <code>throwableSupplier</code>.
     *
     * @param throwableSupplier the supplier of the potentially thrown {@link Throwable}
     * @param <E>               the Throwable type
     * @return the value, if present
     * @throws E the throwable, if the value is absent
     */
    public final <E extends Throwable> A orElseThrow(Fn0<? extends E> throwableSupplier) throws E {
        return orElseGet(fn0(() -> {
            throw throwableSupplier.apply();
        }));
    }

    /**
     * If this value is present and satisfies <code>predicate</code>, return <code>just</code> the value; otherwise,
     * return <code>nothing</code>.
     *
     * @param predicate the predicate to apply to the possibly absent value
     * @return maybe the present value that satisfied the predicate
     */
    public final Maybe<A> filter(Fn1<? super A, ? extends Boolean> predicate) {
        return flatMap(a -> predicate.apply(a) ? just(a) : nothing());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Maybe<A> throwError(Unit unit) {
        return nothing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Maybe<A> catchError(Fn1<? super Unit, ? extends Monad<A, Maybe<?>>> recoveryFn) {
        return match(recoveryFn, Maybe::just).coerce();
    }

    /**
     * If this value is absent, return the value supplied by <code>lSupplier</code> wrapped in <code>Either.left</code>.
     * Otherwise, wrap the value in <code>Either.right</code> and return it.
     *
     * @param <L>  the left parameter type
     * @param lFn0 the supplier for the left value
     * @return this value wrapped in an Either.right, or an Either.left around the result of lSupplier
     */
    public final <L> Either<L, A> toEither(Fn0<L> lFn0) {
        return fmap(Either::<L, A>right).orElseGet(() -> left(lFn0.apply()));
    }

    /**
     * Convert to {@link Optional}.
     *
     * @return the Optional
     */
    public final Optional<A> toOptional() {
        return fmap(Optional::of).orElseGet(Optional::empty);
    }

    /**
     * Lift the value into the {@link Maybe} monad
     *
     * @param b   the value
     * @param <B> the value type
     * @return Just b
     */
    @Override
    public final <B> Maybe<B> pure(B b) {
        return just(b);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the value is present, return {@link Maybe#just} <code>fn</code> applied to the value; otherwise, return
     * {@link Maybe#nothing}.
     */
    @Override
    public final <B> Maybe<B> fmap(Fn1<? super A, ? extends B> fn) {
        return MonadError.super.<B>fmap(fn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <B> Maybe<B> zip(Applicative<Fn1<? super A, ? extends B>, Maybe<?>> appFn) {
        return MonadError.super.zip(appFn).coerce();
    }

    /**
     * Terminate early if this is a {@link Nothing}; otherwise, continue the {@link Applicative#zip zip}.
     *
     * @param <B>       the result type
     * @param lazyAppFn the lazy other applicative instance
     * @return the zipped {@link Maybe}
     */
    @Override
    public <B> Lazy<Maybe<B>> lazyZip(Lazy<? extends Applicative<Fn1<? super A, ? extends B>, Maybe<?>>> lazyAppFn) {
        return match(constantly(lazy(nothing())),
                     a -> lazyAppFn.fmap(maybeF -> maybeF.<B>fmap(f -> f.apply(a)).coerce()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <B> Maybe<B> discardL(Applicative<B, Maybe<?>> appB) {
        return MonadError.super.discardL(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <B> Maybe<A> discardR(Applicative<B, Maybe<?>> appB) {
        return MonadError.super.discardR(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("RedundantTypeArguments")
    @Override
    public final <B> Maybe<B> flatMap(Fn1<? super A, ? extends Monad<B, Maybe<?>>> f) {
        return match(constantly(nothing()), f.fmap(Monad<B, Maybe<?>>::coerce));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> Maybe<B> trampolineM(Fn1<? super A, ? extends MonadRec<RecursiveResult<A, B>, Maybe<?>>> fn) {
        return match(constantly(nothing()), trampoline(a -> fn.apply(a).<Maybe<RecursiveResult<A, B>>>coerce()
                .match(constantly(terminate(nothing())),
                       aOrB -> aOrB.fmap(Maybe::just))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> Choice3<Unit, A, B> diverge() {
        return match(Choice3::a, Choice3::b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple2<Maybe<Unit>, Maybe<A>> project() {
        return CoProduct2.super.project().into(HList::tuple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Choice2<A, Unit> invert() {
        return match(Choice2::b, Choice2::a);
    }

    /**
     * If this value is present, accept it by <code>consumer</code>; otherwise, do nothing.
     *
     * @param effect the consumer
     * @return the same Maybe instance
     * @deprecated in favor of {@link Maybe#match(Fn1, Fn1) matching} into an {@link IO} and explicitly running it
     */
    @Deprecated
    public final Maybe<A> peek(Fn1<? super A, ? extends IO<?>> effect) {
        return match(constantly(io(this)), a -> effect.apply(a).fmap(constantly(this))).unsafePerformIO();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <B, App extends Applicative<?, App>, TravB extends Traversable<B, Maybe<?>>,
            AppTrav extends Applicative<TravB, App>> AppTrav traverse(Fn1<? super A, ? extends Applicative<B, App>> fn,
                                                                      Fn1<? super TravB, ? extends AppTrav> pure) {
        return match(__ -> pure.apply((TravB) Maybe.<B>nothing()), a -> (AppTrav) fn.apply(a).fmap(Maybe::just));
    }

    /**
     * Convenience static factory method for creating a {@link Maybe} from an {@link Either}. If <code>either</code> is
     * a right value, wrap the value in a <code>just</code> and return it; otherwise, return {@link #nothing()}.
     *
     * @param either the either instance
     * @param <A>    the potential right value
     * @return "Just" the right value, or nothing
     */
    public static <A> Maybe<A> fromEither(Either<?, A> either) {
        return either.toMaybe();
    }

    /**
     * Convenience static factory method for creating a {@link Maybe} from an {@link Optional}.
     *
     * @param optional the optional
     * @param <A>      the optional parameter type
     * @return the equivalent Maybe instance
     */
    public static <A> Maybe<A> fromOptional(Optional<? extends A> optional) {
        return optional.map(Maybe::<A>just).orElse(Maybe.nothing());
    }

    /**
     * Lift a potentially null value into {@link Maybe}. If <code>a</code> is not null, returns <code>just(a)</code>;
     * otherwise, returns {@link #nothing()}.
     *
     * @param a   the potentially null value
     * @param <A> the value parameter type
     * @return "Just" the value, or nothing
     */
    public static <A> Maybe<A> maybe(A a) {
        return a == null ? nothing() : just(a);
    }

    /**
     * Lift a non-null value into {@link Maybe}. This differs from {@link Maybe#maybe} in that the value *must* be
     * non-null; if it is null, a {@link NullPointerException} is thrown.
     *
     * @param a   the non-null value
     * @param <A> the value parameter type
     * @return "Just" the value
     * @throws NullPointerException if a is null
     */
    public static <A> Maybe<A> just(A a) {
        if (a == null)
            throw new NullPointerException();
        return new Just<>(a);
    }

    /**
     * Return nothing.
     *
     * @param <A> the type of the value, if there was one
     * @return nothing
     */
    @SuppressWarnings("unchecked")
    public static <A> Maybe<A> nothing() {
        return (Maybe<A>) Nothing.INSTANCE;
    }

    /**
     * The canonical {@link Pure} instance for {@link Maybe}.
     *
     * @return the {@link Pure} instance
     */
    public static Pure<Maybe<?>> pureMaybe() {
        return Maybe::just;
    }

    private static final class Nothing<A> extends Maybe<A> {
        private static final Nothing<?> INSTANCE = new Nothing<>();

        private Nothing() {
        }

        @Override
        public <R> R match(Fn1<? super Unit, ? extends R> aFn, Fn1<? super A, ? extends R> bFn) {
            return aFn.apply(UNIT);
        }

        @Override
        public String toString() {
            return "Nothing";
        }
    }

    private static final class Just<A> extends Maybe<A> {

        private final A a;

        private Just(A a) {
            this.a = a;
        }

        @Override
        public <R> R match(Fn1<? super Unit, ? extends R> aFn, Fn1<? super A, ? extends R> bFn) {
            return bFn.apply(a);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Just && Objects.equals(this.a, ((Just) other).a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a);
        }

        @Override
        public String toString() {
            return "Just " + a;
        }
    }
}

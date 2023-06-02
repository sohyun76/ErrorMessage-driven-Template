package com.jnape.palatable.lambda.functions.builtin.fn2;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.specialized.Predicate;
import com.jnape.palatable.traitor.annotations.TestTraits;
import com.jnape.palatable.traitor.runners.Traits;
import org.junit.Test;
import org.junit.runner.RunWith;
import testsupport.traits.EmptyIterableSupport;
import testsupport.traits.FiniteIteration;
import testsupport.traits.ImmutableIteration;
import testsupport.traits.Laziness;

import java.util.ArrayList;
import java.util.List;

import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn2.TakeWhile.takeWhile;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static testsupport.matchers.IterableMatcher.isEmpty;
import static testsupport.matchers.IterableMatcher.iterates;

@RunWith(Traits.class)
public class TakeWhileTest {

    @TestTraits({FiniteIteration.class, EmptyIterableSupport.class, ImmutableIteration.class, Laziness.class})
    public Fn1<Iterable<Object>, Iterable<Object>> createTestObject() {
        return takeWhile(constantly(true));
    }

    @Test
    public void takesElementsWhilePredicateIsTrue() {
        Predicate<Integer> lessThan3 = integer -> integer < 3;
        Iterable<Integer>  numbers   = takeWhile(lessThan3, asList(1, 2, 3, 4, 5));
        assertThat(numbers, iterates(1, 2));
    }

    @Test
    public void takesAllElementsIfPredicateNeverFails() {
        String[] requirements = {"fast", "good", "cheap"};
        assertThat(
                takeWhile(constantly(true), asList(requirements)),
                iterates(requirements)
        );
    }

    @Test
    public void takesNoElementsIfPredicateImmediatelyFails() {
        assertThat(takeWhile(constantly(false), asList(1, 2, 3)), isEmpty());
    }

    @Test
    public void deforestingExecutesPredicatesInOrder() {
        List<Integer> innerInvocations = new ArrayList<>();
        List<Integer> outerInvocations = new ArrayList<>();
        takeWhile(y -> {
            outerInvocations.add(y);
            return true;
        }, takeWhile(x -> {
            innerInvocations.add(x);
            return x < 3;
        }, asList(1, 2, 3))).forEach(__ -> {});
        assertThat(innerInvocations, iterates(1, 2, 3));
        assertThat(outerInvocations, iterates(1, 2));
    }
}

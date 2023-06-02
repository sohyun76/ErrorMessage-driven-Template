package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor._shared.TestRaptorTransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AccessStopArrivalTest {

    private static final int ALIGHT_STOP = 100;
    private static final int DEPARTURE_TIME = 8 * 60 * 60;
    private static final int LEG_DURATION = 10 * 60;
    private static final int ALIGHT_TIME = DEPARTURE_TIME + LEG_DURATION;
    private static final int COST = 500;

    private final AccessStopArrival<RaptorTripSchedule> subject = new AccessStopArrival<>(
        DEPARTURE_TIME,
        COST,
        new TestRaptorTransfer(ALIGHT_STOP, LEG_DURATION)
    );

    @Test
    public void arrivedByAccessLeg() {
        assertTrue(subject.arrivedByAccessLeg());
        assertFalse(subject.arrivedByTransit());
        assertFalse(subject.arrivedByTransfer());
    }

    @Test
    public void stop() {
        assertEquals(ALIGHT_STOP, subject.stop());
    }

    @Test
    public void arrivalTime() {
        assertEquals(ALIGHT_TIME, subject.arrivalTime());
    }

    @Test
    public void cost() {
        assertEquals(COST, subject.cost());
    }

    @Test
    public void round() {
        assertEquals(0, subject.round());
    }

    @Test
    public void travelDuration() {
        assertEquals(LEG_DURATION, subject.travelDuration());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void equalsThrowsExceptionByDesign() {
        subject.equals(null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void hashCodeThrowsExceptionByDesign() {
        subject.hashCode();
    }

    @Test
    public void testToString() {
        assertEquals(
                "Access { stop: 100, duration: 10m, arrival-time: 8:10, cost: 500 }",
                subject.toString()
        );
    }

    @Test
    public void timeShiftDefaultBehaviour() {
        final int dTime = 60;
        AbstractStopArrival<RaptorTripSchedule> result = subject.timeShiftNewArrivalTime(ALIGHT_TIME + dTime);

        assertEquals(result.arrivalTime(), ALIGHT_TIME + dTime);
        assertEquals(subject.cost(), result.cost());
        assertEquals(subject.travelDuration(), result.travelDuration());
        assertEquals(subject.round(), result.round());
        assertEquals(subject.stop(), result.stop());
        assertSame(subject.accessLeg().access(), result.accessLeg().access());
        assertEquals(subject.arrivedByAccessLeg(), result.arrivedByAccessLeg());
    }

    @Test
    public void timeShiftNotAllowed() {
        AbstractStopArrival<RaptorTripSchedule> original, result;
        TestRaptorTransfer access = new TestRaptorTransfer(ALIGHT_STOP, LEG_DURATION) {
            @Override public int latestArrivalTime(int time) { return -1; }
        };
        original = new AccessStopArrival<>(DEPARTURE_TIME, COST, access);

        final int dTime = 60;
        result = original.timeShiftNewArrivalTime(ALIGHT_TIME + dTime);

        assertSame(original, result);
    }

    @Test
    public void timeShiftPartiallyAllowed() {
        final int dTime = 60;
        AbstractStopArrival<RaptorTripSchedule> original, result;

        // Allow time-shift, but only by dTime
        TestRaptorTransfer access = new TestRaptorTransfer(ALIGHT_STOP, LEG_DURATION) {
            @Override public int latestArrivalTime(int time) { return ALIGHT_TIME + dTime; }
        };
        original = new AccessStopArrival<>(DEPARTURE_TIME, COST, access);

        result = original.timeShiftNewArrivalTime(ALIGHT_TIME + 7200);

        assertEquals(ALIGHT_TIME + dTime, result.arrivalTime());
    }
}
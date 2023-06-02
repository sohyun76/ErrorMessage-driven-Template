package org.opentripplanner.transit.raptor.speed_test;


import org.opentripplanner.transit.raptor.api.debug.DebugEvent;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.debug.DebugTopic;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch;
import org.opentripplanner.transit.raptor.util.IntUtils;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;
import org.opentripplanner.transit.raptor.util.TimeUtils;
import org.opentripplanner.util.TableFormatter;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrCompact;
import static org.opentripplanner.util.TableFormatter.Align.Center;
import static org.opentripplanner.util.TableFormatter.Align.Left;
import static org.opentripplanner.util.TableFormatter.Align.Right;

class SpeedTestDebugLogger<T extends RaptorTripSchedule> implements DebugLogger {
    private static final int NOT_SET = Integer.MIN_VALUE;

    private final boolean enableDebugLogging;
    private final NumberFormat numFormat = NumberFormat.getInstance(Locale.FRANCE);

    private int lastIterationTime = NOT_SET;
    private int lastRound = NOT_SET;
    private boolean pathHeader = true;

    private final TableFormatter arrivalTableFormatter = new TableFormatter(
        List.of(Center, Center, Right, Right, Right, Right, Left, Left),
        List.of("ARRIVAL", "LEG", "RND", "STOP", "ARRIVE", "COST", "TRIP", "DETAILS"),
        9, 7, 3, 5, 8, 9, 24, 0
    );

    private final TableFormatter pathTableFormatter = new TableFormatter(
        List.of(Center, Center, Right, Right, Right, Right, Right, Right, Left),
        List.of(">>> PATH", "TR", "FROM", "TO", "START", "END", "DURATION", "COST", "DETAILS"),
        9, 2, 5, 5, 8, 8, 8, 6, 0
    );

    SpeedTestDebugLogger(boolean enableDebugLogging) {
        this.enableDebugLogging = enableDebugLogging;
    }

    void stopArrivalLister(DebugEvent<ArrivalView<T>> e) {

        printIterationHeader(e.iterationStartTime());
        printRoundHeader(e.element().round());
        print(e.element(), e.action().toString(), e.reason());

        ArrivalView<?> byElement = e.rejectedDroppedByElement();
        if (e.action() == DebugEvent.Action.DROP && byElement != null) {
            print(byElement, "->by", "");
        }
    }

    void pathFilteringListener(DebugEvent<Path<T>> e) {
        if (pathHeader) {
            System.err.println();
            System.err.println(pathTableFormatter.printHeader());
            pathHeader = false;
        }

        Path<?> p = e.element();
        System.err.println(
            pathTableFormatter.printRow(
                e.action().toString(),
                p.numberOfTransfers(),
                p.accessLeg().toStop(),
                p.egressLeg().fromStop(),
                TimeUtils.timeToStrLong(p.accessLeg().fromTime()),
                TimeUtils.timeToStrLong(p.egressLeg().toTime()),
                TimeUtils.durationToStr(p.travelDurationInSeconds()),
                numFormat.format(p.cost()),
                details(e.action().toString(), e.reason(), e.element().toString())
            )
        );
    }

    @Override
    public boolean isEnabled(DebugTopic topic) {
        return enableDebugLogging;
    }

    @Override
    public void debug(DebugTopic topic, String message) {
        if(enableDebugLogging) {
            // We log to info - since debugging is controlled by the application
            if(message.contains("\n")) {
                System.err.printf("%s\n%s", topic, message);
            }
            else {
                System.err.printf("%-16s | %s%n", topic, message);
            }
        }
    }


    /* private methods */

    private void printIterationHeader(int iterationTime) {
        if (iterationTime == lastIterationTime) return;
        lastIterationTime = iterationTime;
        lastRound = NOT_SET;
        pathHeader = true;
        System.err.println("\n**  RUN RAPTOR FOR MINUTE: " + timeToStrCompact(iterationTime) + "  **");
    }

    private void print(ArrivalView<?> a, String action, String optReason) {
        String pattern = a.arrivedByTransit() ? a.transitLeg().trip().pattern().debugInfo() : "";
        System.err.println(
            arrivalTableFormatter.printRow(
                action,
                legType(a),
                a.round(),
                IntUtils.intToString(a.stop(), NOT_SET),
                TimeUtils.timeToStrLong(a.arrivalTime()),
                numFormat.format(a.cost()),
                pattern,
                details(action, optReason, path(a))
            )
        );
    }

    private static String details(String action, String optReason, String element) {
        return concat(optReason,  action + "ed element: " + element);
    }

    private static String path(ArrivalView<?> a) {
        return path(a, new PathStringBuilder()).toString()  + " (cost: " + a.cost() + ")";
    }

    private static PathStringBuilder path(ArrivalView<?> a, PathStringBuilder buf) {
        if (a.arrivedByAccessLeg()) {
            return buf.walk(legDuration(a)).sep().stop(a.stop());
        }
        // Recursively call this method to insert arrival in front of this arrival
        path(a.previous(), buf);

        buf.sep();

        if (a.arrivedByTransit()) {
            if(a.previous().arrivalTime() > a.arrivalTime()) {
                throw new IllegalStateException("TODO: Add support for REVERSE search!");
            }
            TripTimesSearch.BoarAlightTimes b = TripTimesSearch.findTripForwardSearch(a);
            buf.transit(a.transitLeg().trip().pattern().debugInfo(), b.boardTime, a.arrivalTime());
        } else {
            buf.walk(legDuration(a));
        }
        return buf.sep().stop(a.stop());
    }

    /**
     * The absolute time duration in seconds of a trip.
     */
    private static int legDuration(ArrivalView<?> a) {
        if(a.arrivedByAccessLeg()) {
            return a.accessLeg().access().durationInSeconds();
        }
        if(a.arrivedByTransfer()) {
            return a.transferLeg().durationInSeconds();
        }
        if(a.arrivedAtDestination()) {
            return a.egressLeg().egress().durationInSeconds();
        }
        throw new IllegalStateException("Unsuported type: " + a.getClass());
    }

    private void printRoundHeader(int round) {
        if (round == lastRound) return;
        lastRound = round;

        System.err.println();
        System.err.println(arrivalTableFormatter.printHeader());
    }

    private static String concat(String s, String t) {
        if(s == null || s.isEmpty()) {
            return t == null ? "" : t;
        }
        return s + ", " + (t == null ? "" : t);
    }

    private String legType(ArrivalView<?> a) {
        if (a.arrivedByAccessLeg()) { return "Access"; }
        if (a.arrivedByTransit()) { return "Transit"; }
        // We use Walk instead of Transfer so it is easier to distinguish from Transit
        if (a.arrivedByTransfer()) { return "Walk"; }
        if (a.arrivedAtDestination()) { return "Egress"; }
        throw new IllegalStateException("Unknown mode for: " + this);
    }
}

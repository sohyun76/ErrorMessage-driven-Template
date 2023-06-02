package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch;


/**
 * Build a path from a destination arrival - this maps between the domain of routing
 * to the domain of result paths. All values not needed for routing is computed as part of this mapping.
 */
public final class ForwardPathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {
    private int iterationDepartureTime = -1;


    public ForwardPathMapper(WorkerLifeCycle lifeCycle) {
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
        PathLeg<T> lastLeg;
        AccessPathLeg<T> accessLeg;

        lastLeg = createEgressPathLeg(destinationArrival);

        for (ArrivalView<T> arrival = destinationArrival.previous(); true; arrival = arrival.previous()) {
            if (arrival.arrivedByTransit()) {
                lastLeg = createTransitLeg(arrival, lastLeg);
            } else if (arrival.arrivedByTransfer()) {
                lastLeg = createTransferLeg(arrival, lastLeg);
            } else if (arrival.arrivedByAccessLeg()) {
                accessLeg = createAccessPathLeg(arrival, lastLeg);
                break;
            } else {
                throw new RuntimeException("Unknown arrival type");
            }
        }

        return new Path<>(iterationDepartureTime, accessLeg, RaptorCostConverter.toOtpDomainCost(destinationArrival.cost()));
    }

    private EgressPathLeg<T> createEgressPathLeg(DestinationArrival<T> destinationArrival) {
        RaptorTransfer egress = destinationArrival.egressLeg().egress();
        int departureTime = destinationArrival.arrivalTime() - egress.durationInSeconds();

        return new EgressPathLeg<>(
            egress,
            destinationArrival.previous().stop(),
            departureTime,
            destinationArrival.arrivalTime(),
            domainCost(destinationArrival)
        );
    }

    private TransitPathLeg<T> createTransitLeg(ArrivalView<T> arrival, PathLeg<T> lastLeg) {
        TripTimesSearch.BoarAlightTimes r = TripTimesSearch.findTripForwardSearch(arrival);

        return new TransitPathLeg<>(
                arrival.previous().stop(),
                r.boardTime,
                arrival.stop(),
                r.alightTime,
                domainCost(arrival),
                arrival.transitLeg().trip(),
                lastLeg
        );
    }

    private TransferPathLeg<T> createTransferLeg(ArrivalView<T> arrival, PathLeg<T> nextLeg) {
        int departureTime = arrival.arrivalTime() - arrival.transferLeg().durationInSeconds();

        return new TransferPathLeg<>(
                arrival.previous().stop(),
                departureTime,
                arrival.stop(),
                arrival.arrivalTime(),
                domainCost(arrival),
                nextLeg
        );
    }

    private AccessPathLeg<T> createAccessPathLeg(ArrivalView<T> from, PathLeg<T> nextLeg) {
        RaptorTransfer access = from.accessLeg().access();
        int departureTime = from.arrivalTime() - access.durationInSeconds();

        return new AccessPathLeg<>(
            access,
            from.stop(),
            departureTime,
            from.arrivalTime(),
            RaptorCostConverter.toOtpDomainCost(from.cost()),
            nextLeg
        );
    }

    private static int domainCost(ArrivalView<?> to) {
        return RaptorCostConverter.toOtpDomainCost(to.cost() - to.previous().cost());
    }
}

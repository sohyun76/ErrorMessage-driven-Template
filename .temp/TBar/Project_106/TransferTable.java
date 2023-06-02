package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.common.model.P2;

// TODO OTP2 reimplement all special kinds of transfers

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 */
public class TransferTable implements Serializable {

    /**
     * Table which contains transfers between two stops
     */
    protected Multimap<P2<Stop>, Transfer> table = ArrayListMultimap.create();

    private Transfer getTransfer(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip) {
        Collection<Transfer> transfers = table.get(new P2<>(fromStop, toStop));
        for (Transfer transfer : transfers) {
            if (transfer.getFromTrip() == fromTrip && transfer.getToTrip() == toTrip) {
                return transfer;
            }
        }
        return null;
    }

    public Collection<Transfer> getTransfers() {
        return table.values();
    }

    public void addTransfer(Transfer transfer) {
        table.put(new P2<>(transfer.getFromStop(), transfer.getToStop()), transfer);
    }
}

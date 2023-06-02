package org.opentripplanner.graph_builder.module.linking;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import gnu.trove.set.TIntSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.opentripplanner.common.geometry.SphericalDistanceLibrary.fastDistance;
import static org.opentripplanner.graph_builder.module.FakeGraph.*;

public class LinkingTest {

    /**
     * Ensure that splitting edges yields edges that are identical in length for forward and back edges.
     * StreetEdges have lengths expressed internally in mm, and we want to be sure that not only do they
     * sum to the same values but also that they 
     */
    @Test
    public void testSplitting () {
        GeometryFactory gf= GeometryUtils.getGeometryFactory();
        double x = -122.123;
        double y = 37.363; 
        for (double delta = 0; delta <= 2; delta += 0.005) {

            StreetVertex v0 = new IntersectionVertex(null, "zero", x, y);
            StreetVertex v1 = new IntersectionVertex(null, "one", x + delta, y + delta);
            LineString geom = gf.createLineString(new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() });
            double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
            StreetEdge s0 = new StreetEdge(v0, v1, geom, "test", dist, StreetTraversalPermission.ALL, false);
            StreetEdge s1 = new StreetEdge(v1, v0, (LineString) geom.reverse(), "back", dist, StreetTraversalPermission.ALL, true);

            // split it but not too close to the end
            double splitVal = Math.random() * 0.95 + 0.025;

            SplitterVertex sv0 = new SplitterVertex(null, "split", x + delta * splitVal, y + delta * splitVal, s0);
            SplitterVertex sv1 = new SplitterVertex(null, "split", x + delta * splitVal, y + delta * splitVal, s1);

            P2<StreetEdge> sp0 = s0.split(sv0, true);
            P2<StreetEdge> sp1 = s1.split(sv1, true);

            // distances expressed internally in mm so this epsilon is plenty good enough to ensure that they
            // have the same values
            assertEquals(sp0.first.getDistanceMeters(), sp1.second.getDistanceMeters(), 0.0000001);
            assertEquals(sp0.second.getDistanceMeters(), sp1.first.getDistanceMeters(), 0.0000001);
            assertFalse(sp0.first.isBack());
            assertFalse(sp0.second.isBack());
            assertTrue(sp1.first.isBack());
            assertTrue(sp1.second.isBack());
        }
    }

    /**
     * Test that all the stops are linked identically
     * to the street network on two builds of similar graphs
     * with additional stops in one.
     * 
     * We do this by building the graphs and then comparing the stop tree caches.
     */
    @Test
    public void testStopsLinkedIdentically () throws UnsupportedEncodingException {
        // build the graph without the added stops
        Graph g1 = buildGraphNoTransit();
        addRegularStopGrid(g1);
        link(g1);

        Graph g2 = buildGraphNoTransit();
        addExtraStops(g2);
        addRegularStopGrid(g2);
        link(g2);

        // compare the linkages
        for (TransitStopVertex ts : Iterables.filter(g1.getVertices(), TransitStopVertex.class)) {
            List<StreetTransitLink> stls1 = outgoingStls(ts);
            assertTrue(stls1.size() >= 1);

            TransitStopVertex other = (TransitStopVertex) g2.getVertex(ts.getLabel());
            List<StreetTransitLink> stls2 = outgoingStls(other);

            assertEquals("Unequal number of links from stop " + ts, stls1.size(), stls2.size());

            for (int i = 0; i < stls1.size(); i++) {
                Vertex v1 = stls1.get(i).getToVertex();
                Vertex v2 = stls2.get(i).getToVertex();
                assertEquals(v1.getLat(), v2.getLat(), 1e-10);
                assertEquals(v1.getLon(), v2.getLon(), 1e-10);
            }

        }
    }

    private static List<StreetTransitLink> outgoingStls (final TransitStopVertex tsv) {
        List<StreetTransitLink> stls = tsv.getOutgoing().stream()
                .filter(StreetTransitLink.class::isInstance)
                .map(StreetTransitLink.class::cast)
                .collect(Collectors.toList());
        stls.sort(Comparator.comparing(e -> e.getGeometry().getLength()));
        return stls;
    }

}

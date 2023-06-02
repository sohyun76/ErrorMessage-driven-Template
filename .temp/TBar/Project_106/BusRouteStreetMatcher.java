package org.opentripplanner.graph_builder.module.map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.model.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Uses the shapes from GTFS to determine which streets buses drive on. This is used to improve the quality of
 * the shapes shown for the user.
 *
 * GTFS provides a mapping from trips→shapes. This module provides a mapping from stops→trips and shapes→edges.
 * Then transitively we get a mapping from stop→edges.
 */
public class BusRouteStreetMatcher implements GraphBuilderModule {
    private static final Logger log = LoggerFactory.getLogger(BusRouteStreetMatcher.class);

    public List<String> provides() {
        return Arrays.asList("edge matching");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "transit");
    }

    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {

        // Mapbuilder needs transit index
        graph.index();

        StreetMatcher matcher = new StreetMatcher(graph);
        log.info("Finding corresponding street edges for trip patterns...");
        // Why do we need to iterate over the routes? Why not just patterns?
        for (Route route : graph.index.getAllRoutes()) {
            for (TripPattern pattern : graph.index.getPatternsForRoute().get(route)) {
                if (pattern.getMode() == TransitMode.BUS) {
                    /* we can only match geometry to streets on bus routes */
                    log.debug("Matching {}", pattern);
                    //If there are no shapes in GTFS pattern geometry is generated
                    //generated geometry is useless for street matching
                    //that is why pattern.geometry is null in that case
                    if (pattern.getGeometry() == null) {
                        continue;
                    }

                    for (int i = 0; i < pattern.numHopGeometries(); i++) {
                        LineString hopGeometry = pattern.getHopGeometry(i);

                        List<Edge> edges = matcher.match(hopGeometry);
                        if (edges == null || edges.isEmpty()) {
                            log.warn("Could not match to street network: {}", pattern);
                            continue;
                        }
                        List<Coordinate> coordinates = new ArrayList<>();
                        for (Edge e : edges) {
                            coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
                        }
                        Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
                        LineString ls = GeometryUtils.getGeometryFactory().createLineString(coordinates.toArray(coordinateArray));
                        // Replace the hop's geometry from GTFS with that of the equivalent OSM edges.
                        pattern.setHopGeometry(i, ls);
                    }
                }
            }
        }
    }

    @Override
    public void checkInputs() {
        //no file inputs
    }
}

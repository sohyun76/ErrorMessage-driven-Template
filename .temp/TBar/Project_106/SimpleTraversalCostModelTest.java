package org.opentripplanner.routing.core;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for SimpleIntersectionTraversalCostModel.
 * 
 * TODO(flamholz): add tests for traversal with traffic lights and without a car.
 * 
 * @author avi
 */
public class SimpleTraversalCostModelTest {
    
    private Graph graph;
    
    private RoutingRequest options;

    public SimpleIntersectionTraversalCostModel costModel;
    
    @Before
    public void before() {
        graph = new Graph();
        costModel = new SimpleIntersectionTraversalCostModel();
        
        // Initialize the routing request.
        options = new RoutingRequest();
        options.carSpeed = 1.0;
        options.walkSpeed = 1.0;
        options.carDecelerationSpeed = (2.0);
        options.carAccelerationSpeed  = (2.0);
        options.setStreetSubRequestModes(TraverseModeSet.allModes());
    }
    
    @Test
    public void testCalculateTurnAngle() {
        // Graph for a fictional grid city with turn restrictions
        IntersectionVertex v1 = vertex("maple_1st", new Coordinate(2.0, 2.0), false);
        IntersectionVertex v2 = vertex("maple_2nd", new Coordinate(2.0, 1.0), false);
        
        StreetEdge e1 = edge(v1, v2, 1.0, false);

        // Edge has same first and last angle.
        assertEquals(90, e1.getInAngle());
        assertEquals(90, e1.getOutAngle());
        
        // 2 new ones
        IntersectionVertex v3 = vertex("test2", new Coordinate(1.0, 1.0), false);
        
        // Second edge
        StreetEdge e2 = edge(v2, v3, 1.0, false);

        assertEquals(0, e2.getInAngle());
        assertEquals(0, e2.getOutAngle());
        
        // Difference should be about 90.
        int diff = (e1.getOutAngle() - e2.getInAngle());
        assertEquals(90, diff);
        
        int turnAngle = costModel.calculateTurnAngle(e1, e2, options);
        assertEquals(270, turnAngle);
    }
    
    @Test
    public void testTurnDirectionChecking() {
        // 3 points on a roughly on line
        Coordinate a = new Coordinate(-73.990989, 40.750167);
        Coordinate b = new Coordinate(-73.988049, 40.749094);
        Coordinate c = new Coordinate(-73.984981, 40.747761);
        
        // A vertex for each. No light.
        IntersectionVertex u = vertex("from_v", a, false);
        IntersectionVertex v = vertex("intersection", b, false);
        IntersectionVertex w = vertex("to_v", c, false);
        
        // Two edges.
        StreetEdge fromEdge = edge(u, v, 1.0, false);
        StreetEdge toEdge = edge(v, w, 1.0, false);
        
        int turnAngle = costModel.calculateTurnAngle(fromEdge, toEdge, options);
        assertFalse(costModel.isRightTurn(turnAngle));
        assertFalse(costModel.isLeftTurn(turnAngle));
        
        // AKA is a straight ahead.
    }
    
    @Test
    public void testFreeFlowing() {
        // 3 points on a roughly on line
        Coordinate a = new Coordinate(-73.990989, 40.750167);
        Coordinate b = new Coordinate(-73.988049, 40.749094);
        Coordinate c = new Coordinate(-73.984981, 40.747761);
        
        // A vertex for each. No light.
        IntersectionVertex u = vertex("from_v", a, false);
        IntersectionVertex v = vertex("intersection", b, false);
        IntersectionVertex w = vertex("to_v", c, false);
        v.freeFlowing = true;
        
        // Two edges.
        StreetEdge fromEdge = edge(u, v, 1.0, false);
        StreetEdge toEdge = edge(v, w, 1.0, false);
        
        float fromSpeed = 1.0f;
        float toSpeed = 1.0f;
        TraverseMode mode = TraverseMode.CAR;
        
        double traversalCost = costModel.computeTraversalCost(v, fromEdge, toEdge, mode, options, fromSpeed, toSpeed);
        
        // Vertex is free-flowing so cost should be 0.0.
        assertEquals(0.0, traversalCost, 0.0);
    }
    
    @Test
    public void testInferredFreeFlowing() {
        // 3 points on a roughly on line
        Coordinate a = new Coordinate(-73.990989, 40.750167);
        Coordinate b = new Coordinate(-73.988049, 40.749094);
        Coordinate c = new Coordinate(-73.984981, 40.747761);
        
        // A vertex for each. No light.
        IntersectionVertex u = vertex("from_v", a, false);
        IntersectionVertex v = vertex("intersection", b, false);
        IntersectionVertex w = vertex("to_v", c, false);
        
        // Two edges - will infer that the vertex is free-flowing since there is no light.
        StreetEdge fromEdge = edge(u, v, 1.0, false);
        StreetEdge toEdge = edge(v, w, 1.0, false);
        
        float fromSpeed = 1.0f;
        float toSpeed = 1.0f;
        TraverseMode mode = TraverseMode.CAR;
        
        double traversalCost = costModel.computeTraversalCost(v, fromEdge, toEdge, mode, options, fromSpeed, toSpeed);
        
        // Vertex is free-flowing so cost should be 0.0.
        assertEquals(0.0, traversalCost, 0.0);
    }
    
    @Test
    public void testStraightNoLightInCar() {
        // 3 points on a roughly on line
        Coordinate a = new Coordinate(-73.990989, 40.750167);
        Coordinate b = new Coordinate(-73.988049, 40.749094);
        Coordinate c = new Coordinate(-73.984981, 40.747761);
                
        // A vertex for each. No light.
        IntersectionVertex u = vertex("from_v", a, false);
        IntersectionVertex v = vertex("intersection", b, false);
        IntersectionVertex w = vertex("to_v", c, false);
    
        // Two edges.
        StreetEdge fromEdge = edge(u, v, 1.0, false);
        StreetEdge toEdge = edge(v, w, 1.0, false);
        
        // 3rd edge prevents inferral of free-flowingness
        StreetEdge extraEdge = edge(v, u, 1.0, false);
                
        float fromSpeed = 1.0f;
        float toSpeed = 1.0f;
        TraverseMode mode = TraverseMode.CAR;
        
        double traversalCost = costModel.computeTraversalCost(v, fromEdge, toEdge, mode, options, fromSpeed, toSpeed);
        
        // Cost with default values = 5.0
        assertEquals(5, traversalCost, 0.0);
    }
    
    @Test
    public void testRightNoLightInCar() {
        // 3 points that form a right turn on the map
        Coordinate a = new Coordinate(40.750167, -73.990989);
        Coordinate b = new Coordinate(40.749094, -73.988049);
        Coordinate c = new Coordinate(40.748509, -73.988693);
        
        // A vertex for each. No light.
        IntersectionVertex u = vertex("from_v", a, false);
        IntersectionVertex v = vertex("intersection", b, false);
        IntersectionVertex w = vertex("to_v", c, false);
    
        // Two edges.
        StreetEdge fromEdge = edge(u, v, 1.0, false);
        StreetEdge toEdge = edge(v, w, 1.0, false);
        
        // 3rd edge prevents inferral of free-flowingness
        StreetEdge extraEdge = edge(v, u, 1.0, false);
                
        int turnAngle = costModel.calculateTurnAngle(fromEdge, toEdge, options);
        assertTrue(costModel.isRightTurn(turnAngle));
        assertFalse(costModel.isLeftTurn(turnAngle));
        
        float fromSpeed = 1.0f;
        float toSpeed = 1.0f;
        TraverseMode mode = TraverseMode.CAR;
        
        double traversalCost = costModel.computeTraversalCost(v, fromEdge, toEdge, mode, options, fromSpeed, toSpeed);
        
        // Cost with default values = 8.0
        assertEquals(8.0, traversalCost, 0.0);
    }
    
    @Test
    public void testLeftNoLightInCar() {
        // 3 points that form a right turn on the map
        Coordinate a = new Coordinate(40.750167, -73.990989);
        Coordinate b = new Coordinate(40.749094, -73.988049);
        Coordinate c = new Coordinate(40.749760 , -73.987749);
        
        // A vertex for each. No light.
        IntersectionVertex u = vertex("from_v", a, false);
        IntersectionVertex v = vertex("intersection", b, false);
        IntersectionVertex w = vertex("to_v", c, false);
    
        // Two edges.
        StreetEdge fromEdge = edge(u, v, 1.0, false);
        StreetEdge toEdge = edge(v, w, 1.0, false);
        
        // 3rd edge prevents inferral of free-flowingness
        StreetEdge extraEdge = edge(v, u, 1.0, false);
                
        int turnAngle = costModel.calculateTurnAngle(fromEdge, toEdge, options);
        assertFalse(costModel.isRightTurn(turnAngle));
        assertTrue(costModel.isLeftTurn(turnAngle));
        
        float fromSpeed = 1.0f;
        float toSpeed = 1.0f;
        TraverseMode mode = TraverseMode.CAR;
        
        double traversalCost = costModel.computeTraversalCost(v, fromEdge, toEdge, mode, options, fromSpeed, toSpeed);
        
        // Cost with default values = 8.0
        assertEquals(8.0, traversalCost, 0.0);
    }

    /****
     * Private Methods
     ****/

    private IntersectionVertex vertex(String label, Coordinate coord, boolean hasLight) {
        IntersectionVertex v = new IntersectionVertex(graph, label, coord.y, coord.x);
        v.trafficLight = hasLight;
        return v;
    }

    /**
     * Create an edge. If twoWay, create two edges (back and forth).
     * 
     * @param vA
     * @param vB
     * @param length
     * @param back true if this is a reverse edge
     */
    private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        StreetTraversalPermission perm = StreetTraversalPermission.ALL;
        return new StreetEdge(vA, vB, geom, name, length, perm, back);
    }
}

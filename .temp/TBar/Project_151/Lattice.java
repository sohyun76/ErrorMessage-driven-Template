/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.lattice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.segment_file.Token;
import org.apache.joshua.util.ChartSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lattice representation of a directed graph.
 *
 * @author Lane Schwartz
 * @author Matt Post post@cs.jhu.edu
 * @since 2008-07-08
 *
 */
public class Lattice<Value> implements Iterable<Node<Value>> {

  private static final Logger LOG = LoggerFactory.getLogger(Lattice.class);

  /**
   * True if there is more than one path through the lattice.
   */
  private boolean latticeHasAmbiguity;

  /**
   * Costs of the best path between each pair of nodes in the lattice.
   */
  private ChartSpan<Integer> distances = null;

  /**
   * List of all nodes in the lattice. Nodes are assumed to be in topological order.
   */
  private final List<Node<Value>> nodes;


  JoshuaConfiguration config = null;

  /**
   * Constructs a new lattice from an existing list of (connected) nodes.
   * <p>
   * The list of nodes must already be in topological order. If the list is not in topological
   * order, the behavior of the lattice is not defined.
   *
   * @param nodes A list of nodes which must be in topological order.
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public Lattice(List<Node<Value>> nodes, JoshuaConfiguration config) {
    this.nodes = nodes;
    //    this.distances = calculateAllPairsShortestPath();
    this.latticeHasAmbiguity = true;
  }

  public Lattice(List<Node<Value>> nodes, boolean isAmbiguous, JoshuaConfiguration config) {
    // Node<Value> sink = new Node<Value>(nodes.size());
    // nodes.add(sink);
    this.nodes = nodes;
    //    this.distances = calculateAllPairsShortestPath();
    this.latticeHasAmbiguity = isAmbiguous;
  }

  /**
   * Instantiates a lattice from a linear chain of values, i.e., a sentence.
   *
   * @param linearChain a sequence of Value objects
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public Lattice(Value[] linearChain, JoshuaConfiguration config) {
    this.latticeHasAmbiguity = false;
    this.nodes = new ArrayList<>();

    Node<Value> previous = new Node<>(0);
    nodes.add(previous);

    int i = 1;

    for (Value value : linearChain) {

      Node<Value> current = new Node<>(i);
      float cost = 0.0f;
      // if (i > 4) cost = (float)i/1.53432f;
      previous.addArc(current, cost, value);

      nodes.add(current);

      previous = current;
      i++;
    }

    //    this.distances = calculateAllPairsShortestPath();
  }

  public final boolean hasMoreThanOnePath() {
    return latticeHasAmbiguity;
  }

  /**
   * Computes the shortest distance between two nodes, which is used (perhaps among other places) in
   * computing which rules can apply over which spans of the input
   *
   * @param arc an {@link org.apache.joshua.lattice.Arc} of values
   * @return the shortest distance between two nodes
   */
  public int distance(Arc<Value> arc) {
    return this.getShortestPath(arc.getTail().getNumber(), arc.getHead().getNumber());
  }

  public int distance(int i, int j) {
    return this.getShortestPath(i, j);
  }

  /**
   * Convenience method to get a lattice from a linear sequence of {@link Token} objects.
   *
   * @param source input string from which to create a {@link org.apache.joshua.lattice.Lattice}
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   * @return Lattice representation of the linear chain.
   */
  public static Lattice<Token> createTokenLatticeFromString(String source, JoshuaConfiguration config) {
    String[] tokens = source.split("\\s+");
    Token[] integerSentence = new Token[tokens.length];
    for (int i = 0; i < tokens.length; i++) {
      integerSentence[i] = new Token(tokens[i], config);
    }

    return new Lattice<>(integerSentence, config);
  }

  public static Lattice<Token> createTokenLatticeFromPLF(String data, JoshuaConfiguration config) {
    ArrayList<Node<Token>> nodes = new ArrayList<>();

    // This matches a sequence of tuples, which describe arcs leaving this node
    Pattern nodePattern = Pattern.compile("(.+?)\\(\\s*(\\(.+?\\),\\s*)\\s*\\)(.*)");

    /*
     * This matches a comma-delimited, parenthesized tuple of a (a) single-quoted word (b) a number,
     * optionally in scientific notation (c) an offset (how many states to jump ahead)
     */
    Pattern arcPattern = Pattern
        .compile("\\s*\\('(.+?)',\\s*(-?\\d+\\.?\\d*?(?:[eE]-?\\d+)?),\\s*(\\d+)\\),\\s*(.*)");

    Matcher nodeMatcher = nodePattern.matcher(data);

    boolean latticeIsAmbiguous = false;

    int nodeID = 0;
    Node<Token> startNode = new Node<>(nodeID);
    nodes.add(startNode);

    while (nodeMatcher.matches()) {

      String nodeData = nodeMatcher.group(2);
      String remainingData = nodeMatcher.group(3);

      nodeID++;

      Node<Token> currentNode;
      if (nodeID < nodes.size() && nodes.get(nodeID) != null) {
        currentNode = nodes.get(nodeID);
      } else {
        currentNode = new Node<>(nodeID);
        while (nodeID > nodes.size())
          nodes.add(new Node<>(nodes.size()));
        nodes.add(currentNode);
      }

      Matcher arcMatcher = arcPattern.matcher(nodeData);
      int numArcs = 0;
      if (!arcMatcher.matches()) {
        throw new RuntimeException("Parse error!");
      }
      while (arcMatcher.matches()) {
        numArcs++;
        String arcLabel = arcMatcher.group(1);
        float arcWeight = Float.parseFloat(arcMatcher.group(2));
        int destinationNodeID = nodeID + Integer.parseInt(arcMatcher.group(3));

        Node<Token> destinationNode;
        if (destinationNodeID < nodes.size() && nodes.get(destinationNodeID) != null) {
          destinationNode = nodes.get(destinationNodeID);
        } else {
          destinationNode = new Node<>(destinationNodeID);
          while (destinationNodeID > nodes.size())
            nodes.add(new Node<>(nodes.size()));
          nodes.add(destinationNode);
        }

        String remainingArcs = arcMatcher.group(4);

        Token arcToken = new Token(arcLabel, config);
        currentNode.addArc(destinationNode, arcWeight, arcToken);

        arcMatcher = arcPattern.matcher(remainingArcs);
      }
      if (numArcs > 1)
        latticeIsAmbiguous = true;

      nodeMatcher = nodePattern.matcher(remainingData);
    }

    /* Add <s> to the start of the lattice. */
    if (nodes.size() > 1 && nodes.get(1) != null) {
      Node<Token> firstNode = nodes.get(1);
      startNode.addArc(firstNode, 0.0f, new Token(Vocabulary.START_SYM, config));
    }

    /* Add </s> as a final state, connect it to the previous end-state */
    nodeID = nodes.get(nodes.size()-1).getNumber() + 1;
    Node<Token> endNode = new Node<>(nodeID);
    nodes.get(nodes.size()-1).addArc(endNode, 0.0f, new Token(Vocabulary.STOP_SYM, config));
    nodes.add(endNode);

    return new Lattice<>(nodes, latticeIsAmbiguous, config);
  }

  /**
   * Constructs a lattice from a given string representation.
   *
   * @param data String representation of a lattice.
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   * @return A lattice that corresponds to the given string.
   */
  public static Lattice<String> createStringLatticeFromString(String data, JoshuaConfiguration config) {

    Map<Integer, Node<String>> nodes = new HashMap<>();

    Pattern nodePattern = Pattern.compile("(.+?)\\((\\(.+?\\),)\\)(.*)");
    Pattern arcPattern = Pattern.compile("\\('(.+?)',(\\d+.\\d+),(\\d+)\\),(.*)");

    Matcher nodeMatcher = nodePattern.matcher(data);

    int nodeID = -1;

    while (nodeMatcher.matches()) {

      String nodeData = nodeMatcher.group(2);
      String remainingData = nodeMatcher.group(3);

      nodeID++;

      Node<String> currentNode;
      if (nodes.containsKey(nodeID)) {
        currentNode = nodes.get(nodeID);
      } else {
        currentNode = new Node<>(nodeID);
        nodes.put(nodeID, currentNode);
      }

      LOG.debug("Node : {}", nodeID);

      Matcher arcMatcher = arcPattern.matcher(nodeData);

      while (arcMatcher.matches()) {
        String arcLabel = arcMatcher.group(1);
        float arcWeight = Float.valueOf(arcMatcher.group(2));
        int destinationNodeID = nodeID + Integer.parseInt(arcMatcher.group(3));

        Node<String> destinationNode;
        if (nodes.containsKey(destinationNodeID)) {
          destinationNode = nodes.get(destinationNodeID);
        } else {
          destinationNode = new Node<>(destinationNodeID);
          nodes.put(destinationNodeID, destinationNode);
        }

        String remainingArcs = arcMatcher.group(4);

        LOG.debug("\t{} {} {}", arcLabel, arcWeight, destinationNodeID);

        currentNode.addArc(destinationNode, arcWeight, arcLabel);

        arcMatcher = arcPattern.matcher(remainingArcs);
      }

      nodeMatcher = nodePattern.matcher(remainingData);
    }

    List<Node<String>> nodeList = new ArrayList<>(nodes.values());
    Collections.sort(nodeList, new NodeIdentifierComparator());

    LOG.debug("Nodelist={}", nodeList);

    return new Lattice<>(nodeList, config);
  }

  /**
   * Gets the cost of the shortest path between two nodes.
   *
   * @param from ID of the starting node.
   * @param to ID of the ending node.
   * @return The cost of the shortest path between the two nodes.
   */
  public int getShortestPath(int from, int to) {
    // System.err.println(String.format("DISTANCE(%d,%d) = %f", from, to, costs[from][to]));
    if (distances == null)
      this.distances = calculateAllPairsShortestPath();

    return distances.get(from, to);
  }

  /**
   * Gets the shortest distance through the lattice.
   * @return int representing the shortest distance through the lattice
   */
  public int getShortestDistance() {
    if (distances == null)
      distances = calculateAllPairsShortestPath();
    return distances.get(0, nodes.size()-1);
  }

  /**
   * Gets the node with a specified integer identifier. If the identifier is negative, we count
   * backwards from the end of the array, Perl-style (-1 is the last element, -2 the penultimate,
   * etc).
   *
   * @param index Integer identifier for a node.
   * @return The node with the specified integer identifier
   */
  public Node<Value> getNode(int index) {
    if (index >= 0)
      return nodes.get(index);
    else
      return nodes.get(size() + index);
  }

  public List<Node<Value>> getNodes() {
    return nodes;
  }

  /**
   * Returns an iterator over the nodes in this lattice.
   *
   * @return An iterator over the nodes in this lattice.
   */
  @Override
  public Iterator<Node<Value>> iterator() {
    return nodes.iterator();
  }

  /**
   * Returns the number of nodes in this lattice.
   *
   * @return The number of nodes in this lattice.
   */
  public int size() {
    return nodes.size();
  }

  /**
   * Calculate the all-pairs shortest path for all pairs of nodes.
   * <p>
   * Note: This method assumes no backward arcs. If there are backward arcs, the returned shortest
   * path costs for that node may not be accurate.
   *
   * @return The all-pairs shortest path for all pairs of nodes.
   */
  private ChartSpan<Integer> calculateAllPairsShortestPath() {

    ChartSpan<Integer> distance = new ChartSpan<>(nodes.size() - 1, Integer.MAX_VALUE);
    distance.setDiagonal(0);

    /* Mark reachability between immediate neighbors */
    for (Node<Value> tail : nodes) {
      for (Arc<Value> arc : tail.getOutgoingArcs()) {
        Node<Value> head = arc.getHead();
        distance.set(tail.id(), head.id(), 1);
      }
    }

    int size = nodes.size();

    for (int width = 2; width <= size; width++) {
      for (int i = 0; i < size - width; i++) {
        int j = i + width;
        for (int k = i + 1; k < j; k++) {
          distance.set(i, j, Math.min(distance.get(i, j), distance.get(i, k) + distance.get(k, j)));
        }
      }
    }

    return distance;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    for (Node<Value> start : this) {
      for (Arc<Value> arc : start.getOutgoingArcs()) {
        s.append(arc.toString());
        s.append('\n');
      }
    }

    return s.toString();
  }

  public static void main(String[] args) {

    List<Node<String>> nodes = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      nodes.add(new Node<>(i));
    }

    nodes.get(0).addArc(nodes.get(1), 1.0f, "x");
    nodes.get(1).addArc(nodes.get(2), 1.0f, "y");
    nodes.get(0).addArc(nodes.get(2), 1.5f, "a");
    nodes.get(2).addArc(nodes.get(3), 3.0f, "b");
    nodes.get(2).addArc(nodes.get(3), 5.0f, "c");

    Lattice<String> graph = new Lattice<>(nodes, null);

    System.out.println("Shortest path from 0 to 3: " + graph.getShortestPath(0, 3));
  }

  /**
   * Replaced the arc from node i to j with the supplied lattice. This is used to do OOV
   * segmentation of words in a lattice.
   *
   * @param i start node of arc
   * @param j end node of arc
   * @param newNodes new nodes used within the replacement operation
   */
  public void insert(int i, int j, List<Node<Value>> newNodes) {

    nodes.get(i).setOutgoingArcs(newNodes.get(0).getOutgoingArcs());

    newNodes.remove(0);
    nodes.remove(j);
    Collections.reverse(newNodes);

    for (Node<Value> node: newNodes)
      nodes.add(j, node);

    this.latticeHasAmbiguity = false;
    for (int x = 0; x < nodes.size(); x++) {
      nodes.get(x).setID(x);
      this.latticeHasAmbiguity |= (nodes.get(x).getOutgoingArcs().size() > 1);
    }

    this.distances = null;
  }

  /**
   * Constructs a lattice from a given string representation.
   *
   * @param data String representation of a lattice.
   * @return A lattice that corresponds to the given string.
   */
  public static Lattice<String> createFromString(String data) {

    Map<Integer,Node<String>> nodes = new HashMap<>();

    Pattern nodePattern = Pattern.compile("(.+?)\\((\\(.+?\\),)\\)(.*)");
    Pattern arcPattern = Pattern.compile("\\('(.+?)',(\\d+.\\d+),(\\d+)\\),(.*)");

    Matcher nodeMatcher = nodePattern.matcher(data);

    int nodeID = -1;

    while (nodeMatcher.matches()) {

      String nodeData = nodeMatcher.group(2);
      String remainingData = nodeMatcher.group(3);

      nodeID++;

      Node<String> currentNode;
      if (nodes.containsKey(nodeID)) {
        currentNode = nodes.get(nodeID);
      } else {
        currentNode = new Node<>(nodeID);
        nodes.put(nodeID, currentNode);
      }

      LOG.debug("Node : {}", nodeID);

      Matcher arcMatcher = arcPattern.matcher(nodeData);

      while (arcMatcher.matches()) {
        String arcLabel = arcMatcher.group(1);
        double arcWeight = Double.valueOf(arcMatcher.group(2));
        int destinationNodeID = nodeID + Integer.valueOf(arcMatcher.group(3));

        Node<String> destinationNode;
        if (nodes.containsKey(destinationNodeID)) {
          destinationNode = nodes.get(destinationNodeID);
        } else {
          destinationNode = new Node<>(destinationNodeID);
          nodes.put(destinationNodeID, destinationNode);
        }

        String remainingArcs = arcMatcher.group(4);

        LOG.debug("\t {} {} {}", arcLabel,  arcWeight, destinationNodeID);

        currentNode.addArc(destinationNode, (float) arcWeight, arcLabel);

        arcMatcher = arcPattern.matcher(remainingArcs);
      }

      nodeMatcher = nodePattern.matcher(remainingData);
    }

    List<Node<String>> nodeList = new ArrayList<>(nodes.values());
    Collections.sort(nodeList, new NodeIdentifierComparator());

    LOG.debug("Nodelist={}", nodeList);

    return new Lattice<>(nodeList, new JoshuaConfiguration());
  }
}

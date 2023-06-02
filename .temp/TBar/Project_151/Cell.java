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
package org.apache.joshua.decoder.chart_parser;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.hypergraph.HyperEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class implement functions: (1) combine small itesm into larger ones using rules, and create
 * items and hyper-edges to construct a hyper-graph, (2) evaluate model score for items, (3)
 * cube-pruning Note: Bin creates Items, but not all Items will be used in the hyper-graph
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
class Cell {

  // ===============================================================
  // Static fields
  // ===============================================================
  private static final Logger LOG = LoggerFactory.getLogger(Cell.class);


  // The chart this cell belongs to
  private Chart chart = null;

  // The top-level (goal) symbol
  private final int goalSymbol;

  // to maintain uniqueness of nodes
  private final HashMap<HGNode.Signature, HGNode> nodesSigTbl = new LinkedHashMap<>();

  // signature by lhs
  private final Map<Integer, SuperNode> superNodesTbl = new HashMap<>();

  /**
   * sort values in nodesSigTbl, we need this list when necessary
   */
  private List<HGNode> sortedNodes = null;


  // ===============================================================
  // Constructor
  // ===============================================================

  public Cell(Chart chart, int goalSymID) {
    this.chart = chart;
    this.goalSymbol = goalSymID;
  }

  public Cell(Chart chart, int goal_sym_id, int constraint_symbol_id) {
    this(chart, goal_sym_id);
  }

  // ===============================================================
  // Package-protected methods
  // ===============================================================
  
  public Set<Integer> getKeySet() {
    return superNodesTbl.keySet();
  }
  
  public SuperNode getSuperNode(int lhs) {
    return superNodesTbl.get(lhs);
  }

  /**
   * This function loops over all items in the top-level bin (covering the input sentence from
   * <s> ... </s>), looking for items with the goal LHS. For each of these, 
   * add all the items with GOAL_SYM state into the goal bin the goal bin has only one Item, which
   * itself has many hyperedges only "goal bin" should call this function
   */
  // note that the input bin is bin[0][n], not the goal bin
  boolean transitToGoal(Cell bin, List<FeatureFunction> featureFunctions, int sentenceLength) {
    this.sortedNodes = new ArrayList<>();
    HGNode goalItem = null;

    for (HGNode antNode : bin.getSortedNodes()) {
      if (antNode.lhs == this.goalSymbol) {
        float logP = antNode.bestHyperedge.getBestDerivationScore();
        List<HGNode> antNodes = new ArrayList<>();
        antNodes.add(antNode);

        float finalTransitionLogP = ComputeNodeResult.computeFinalCost(featureFunctions, antNodes,
            0, sentenceLength, null, this.chart.getSentence());

        List<HGNode> previousItems = new ArrayList<>();
        previousItems.add(antNode);

        HyperEdge dt = new HyperEdge(null, logP + finalTransitionLogP, finalTransitionLogP,
            previousItems, null);

        if (null == goalItem) {
          goalItem = new HGNode(0, sentenceLength + 1, this.goalSymbol, null, dt, logP
              + finalTransitionLogP);
          this.sortedNodes.add(goalItem);
        } else {
          goalItem.addHyperedgeInNode(dt);
        }
      } // End if item.lhs == this.goalSymID
    } // End foreach Item in bin.get_sorted_items()

    int itemsInGoalBin = getSortedNodes().size();
    if (1 != itemsInGoalBin) {
      LOG.error("the goal_bin does not have exactly one item");
      return false;
    }

    return true;
  }

  /**
   * a note about pruning: when a hyperedge gets created, it first needs to pass through
   * shouldPruneEdge filter. Then, if it does not trigger a new node (i.e. will be merged to an old
   * node), then does not trigger pruningNodes. If it does trigger a new node (either because its
   * signature is new or because its logP is better than the old node's logP), then it will trigger
   * pruningNodes, which might causes *other* nodes got pruned as well
   * */

  /**
   * Creates a new hyperedge and adds it to the chart, subject to pruning. The logic of this
   * function is as follows: if the pruner permits the edge to be added, we build the new edge,
   * which ends in an HGNode. If this is the first time we've built an HGNode for this point in the
   * graph, it gets added automatically. Otherwise, we add the hyperedge to the existing HGNode,
   * possibly updating the HGNode's cache of the best incoming hyperedge.
   * 
   * @return the new hypernode, or null if the cell was pruned.
   */
  HGNode addHyperEdgeInCell(ComputeNodeResult result, Rule rule, int i, int j, List<HGNode> ants,
      SourcePath srcPath, boolean noPrune) {

//    System.err.println(String.format("ADD_EDGE(%d-%d): %s", i, j, rule.getRuleString()));
//    if (ants != null) {
//      for (int xi = 0; xi < ants.size(); xi++) {
//        System.err.println(String.format("  -> TAIL %s", ants.get(xi)));
//      }
//    }

    List<DPState> dpStates = result.getDPStates();
    float pruningEstimate = result.getPruningEstimate();
    float transitionLogP = result.getTransitionCost();
    float finalizedTotalLogP = result.getViterbiCost();

    /**
     * Here, the edge has passed pre-pruning. The edge will be added to the chart in one of three
     * ways:
     * 
     * 1. If there is no existing node, a new one gets created and the edge is its only incoming
     * hyperedge.
     * 
     * 2. If there is an existing node, the edge will be added to its list of incoming hyperedges,
     * possibly taking place as the best incoming hyperedge for that node.
     */

    HyperEdge hyperEdge = new HyperEdge(rule, finalizedTotalLogP, transitionLogP, ants, srcPath);
    HGNode newNode = new HGNode(i, j, rule.getLHS(), dpStates, hyperEdge, pruningEstimate);

    /**
     * each node has a list of hyperedges, need to check whether the node is already exist, if
     * yes, just add the hyperedges, this may change the best logP of the node
     * */
    HGNode oldNode = this.nodesSigTbl.get(newNode.signature());
    if (null != oldNode) { // have an item with same states, combine items
      this.chart.nMerged++;

      /**
       * the position of oldItem in this.heapItems may change, basically, we should remove the
       * oldItem, and re-insert it (linear time), this is too expense)
       **/
      if (newNode.getScore() > oldNode.getScore()) { // merge old to new: semiring plus

        newNode.addHyperedgesInNode(oldNode.hyperedges);
        // This will update the HashMap, so that the oldNode is destroyed.
        addNewNode(newNode);
      } else {// merge new to old, does not trigger pruningItems
        oldNode.addHyperedgesInNode(newNode.hyperedges);
      }

    } else { // first time item
      this.chart.nAdded++; // however, this item may not be used in the future due to pruning in
      // the hyper-graph
      addNewNode(newNode);
    }

    return newNode;
  }

  List<HGNode> getSortedNodes() {
    ensureSorted();
    return this.sortedNodes;
  }
  
  Map<Integer, SuperNode> getSortedSuperItems() {
    ensureSorted();
    return this.superNodesTbl;
  }
  
  // ===============================================================
  // Private Methods
  // ===============================================================

  /**
   * two cases this function gets called (1) a new hyperedge leads to a non-existing node signature
   * (2) a new hyperedge's signature matches an old node's signature, but the best-logp of old node
   * is worse than the new hyperedge's logP
   * */
  private void addNewNode(HGNode node) {
    this.nodesSigTbl.put(node.signature(), node); // add/replace the item
    this.sortedNodes = null; // reset the list
    
//    System.err.println(String.format("** NEW NODE %s %d %d", Vocabulary.word(node.lhs), node.i, node.j));

    // since this.sortedItems == null, this is not necessary because we will always call
    // ensure_sorted to reconstruct the this.tableSuperItems
    // add a super-items if necessary
    SuperNode si = this.superNodesTbl.get(node.lhs);
    if (null == si) {
      si = new SuperNode(node.lhs);
      this.superNodesTbl.put(node.lhs, si);
    }
    si.nodes.add(node);// TODO what about the dead items?
  }

  /**
   * get a sorted list of Nodes in the cell, and also make sure the list of node in any SuperItem is
   * sorted, this will be called only necessary, which means that the list is not always sorted,
   * mainly needed for goal_bin and cube-pruning
   */
  private void ensureSorted() {
    if (null == this.sortedNodes) {
      
      // get sortedNodes.
      this.sortedNodes = new ArrayList<>(this.nodesSigTbl.size());
      for (HGNode node : this.nodesSigTbl.values()) {
        this.sortedNodes.add(node);
      }

      // sort the node in an decreasing-LogP order 
      this.sortedNodes.sort(HGNode.inverseLogPComparator);

      // TODO: we cannot create new SuperItem here because the DotItem link to them.
      // Thus, we clear nodes from existing SuperNodes
      for (SuperNode superNode : this.superNodesTbl.values()) {
        superNode.nodes.clear();
      }

      for (HGNode node : this.sortedNodes) {
        SuperNode superNode = this.superNodesTbl.get(node.lhs);
        checkNotNull(superNode, "Does not have super Item, have to exist");
        superNode.nodes.add(node);
      }

      // Remove SuperNodes who may not contain any nodes anymore due to pruning
      for (Iterator<Entry<Integer, SuperNode>> it = this.superNodesTbl.entrySet().iterator(); it.hasNext(); ) {
        Entry<Integer, SuperNode> entry = it.next();
        if (entry.getValue().nodes.isEmpty()) {
          it.remove();
        }
      }
    }
  }
}

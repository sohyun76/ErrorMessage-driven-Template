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
package org.apache.joshua.decoder.hypergraph;

import java.util.HashMap;

import org.apache.joshua.corpus.Vocabulary;

/**
 * during the pruning process, many Item/Deductions may not be explored at all due to the early-stop
 * in pruning_deduction
 * 
 * @author Zhifei Li, zhifei.work@gmail.com
 * @version $LastChangedDate$
 */
public class HyperGraphPruning extends TrivialInsideOutside {

  final HashMap<HGNode, Boolean> processedNodesTbl = new HashMap<>();
  double bestLogProb;// viterbi unnormalized log prob in the hypergraph

  boolean ViterbiPruning = false;// Viterbi or Posterior pruning

  boolean fixThresholdPruning = true;
  double THRESHOLD_GENERAL = 10;// if the merit is worse than the best_log_prob by this number, then
                                // prune
  double THRESHOLD_GLUE = 10;// if the merit is worse than the best_log_prob by this number, then
                             // prune

  int numSurvivedEdges = 0;
  int numSurvivedNodes = 0;

  int glueGrammarOwner = 0;// TODO


  public HyperGraphPruning(boolean fixThreshold, double thresholdGeneral, double thresholdGlue) {
    fixThresholdPruning = fixThreshold;
    THRESHOLD_GENERAL = thresholdGeneral;
    THRESHOLD_GLUE = thresholdGlue;
    glueGrammarOwner = Vocabulary.id("glue");// TODO
  }

  public void clearState() {
    processedNodesTbl.clear();
    super.clearState();
  }


  // ######################### pruning here ##############
  public void pruningHG(HyperGraph hg) {

    runInsideOutside(hg, 2, 1, 1.0);// viterbi-max, log-semiring

    if (fixThresholdPruning) {
      pruningHGHelper(hg);
      super.clearState();
    } else {
      throw new RuntimeException("wrong call");
    }
  }

  private void pruningHGHelper(HyperGraph hg) {

    this.bestLogProb = getLogNormalizationConstant();// set the best_log_prob

    numSurvivedEdges = 0;
    numSurvivedNodes = 0;
    processedNodesTbl.clear();
    pruningNode(hg.goalNode);

    // clear up
    processedNodesTbl.clear();

    System.out.println("Item suvived ratio: " + numSurvivedNodes * 1.0 / hg.numNodes + " =  "
        + numSurvivedNodes + "/" + hg.numNodes);
    System.out.println("Deduct suvived ratio: " + numSurvivedEdges * 1.0 / hg.numEdges + " =  "
        + numSurvivedEdges + "/" + hg.numEdges);
  }


  private void pruningNode(HGNode it) {

    if (processedNodesTbl.containsKey(it)) return;

    processedNodesTbl.put(it, true);
    boolean shouldSurvive = false;

    // ### recursive call on each deduction
    for (int i = 0; i < it.hyperedges.size(); i++) {
      HyperEdge dt = it.hyperedges.get(i);
      boolean survived = pruningEdge(dt, it);// deduction-specifc operation
      if (survived) {
        shouldSurvive = true; // at least one deduction survive
      } else {
        it.hyperedges.remove(i);
        i--;
      }
    }
    // TODO: now we simply remove the pruned deductions, but in general, we may want to update the
    // variables mainted in the item (e.g., best_deduction); this depends on the pruning method used

    /*
     * by defintion: "should_surive==false" should be impossible, since if I got called, then my
     * upper-deduction must survive, then i will survive because there must be one way to reach me
     * from lower part in order for my upper-deduction survive
     */
    if (!shouldSurvive) {
      throw new RuntimeException("item explored but does not survive");
      // TODO: since we always keep the best_deduction, this should never be true
    } else {
      numSurvivedNodes++;
    }
  }


  // if survive, return true
  // best-deduction is always kept
  private boolean pruningEdge(HyperEdge dt, HGNode parent) {

    /**
     * TODO: theoretically, if an item is get called, then its best deduction should always be kept
     * even just by the threshold-checling. In reality, due to precision of Double, the
     * threshold-checking may not be perfect
     */
    if (dt != parent.bestHyperedge) { // best deduction should always survive if the Item is get
                                      // called
      // ### prune?
      if (shouldPruneHyperedge(dt, parent)) {
        return false; // early stop
      }
    }

    // ### still survive, recursive call all my ant-items
    if (null != dt.getTailNodes()) {
      // recursive call on each ant item, note: the ant_it will not be pruned
      // as I need it
      dt.getTailNodes().forEach(this::pruningNode);
    }

    // ### if get to here, then survive; remember: if I survive, then my upper-item must survive
    numSurvivedEdges++;
    return true; // survive
  }

  private boolean shouldPruneHyperedge(HyperEdge dt, HGNode parent) {

    // ### get merit
    double postLogProb = getEdgeUnormalizedPosteriorLogProb(dt, parent);


    if (dt.getRule() != null && dt.getRule().getOwner().equals(glueGrammarOwner)
        && dt.getRule().getArity() == 2) { // specicial rule: S->S X
      // TODO
      return (postLogProb - this.bestLogProb < THRESHOLD_GLUE);
    } else {
      return (postLogProb - this.bestLogProb < THRESHOLD_GENERAL);
    }
  }

}

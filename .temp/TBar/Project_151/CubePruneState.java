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

import java.util.Arrays;
import java.util.List;

import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.chart_parser.DotChart.DotNode;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;

// ===============================================================
// CubePruneState class
// ===============================================================
public class CubePruneState implements Comparable<CubePruneState> {
  final int[] ranks;
  final ComputeNodeResult computeNodeResult;
  final List<HGNode> antNodes;
  final List<Rule> rules;
  private DotNode dotNode;

  public CubePruneState(ComputeNodeResult score, int[] ranks, List<Rule> rules, List<HGNode> antecedents, DotNode dotNode) {
    this.computeNodeResult = score;
    this.ranks = ranks;
    this.rules = rules;
    this.antNodes = antecedents;
    this.dotNode = dotNode;
  }

  /**
   * This returns the list of DP states associated with the result.
   * 
   * @return
   */
  List<DPState> getDPStates() {
    return this.computeNodeResult.getDPStates();
  }
  
  Rule getRule() {
    return this.rules.get(this.ranks[0]-1);
  }

  public String toString() {
    String sb = "STATE ||| rule=" + getRule() + " inside cost = " +
        computeNodeResult.getViterbiCost() + " estimate = " +
        computeNodeResult.getPruningEstimate();
    return sb;
  }

  public void setDotNode(DotNode node) {
    this.dotNode = node;
  }

  public DotNode getDotNode() {
    return this.dotNode;
  }

  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (!this.getClass().equals(obj.getClass()))
      return false;
    CubePruneState state = (CubePruneState) obj;
    if (state.ranks.length != ranks.length)
      return false;
    for (int i = 0; i < ranks.length; i++)
      if (state.ranks[i] != ranks[i])
        return false;
    return getDotNode() == state.getDotNode();

  }

  public int hashCode() {
    int hash = (dotNode != null) ? dotNode.hashCode() : 0;
    hash += Arrays.hashCode(ranks);

    return hash;
  }

  /**
   * Compares states by ExpectedTotalLogP, allowing states to be sorted according to their inverse
   * order (high-prob first).
   */
  public int compareTo(CubePruneState another) {
    if (this.computeNodeResult.getPruningEstimate() < another.computeNodeResult
        .getPruningEstimate()) {
      return 1;
    } else if (this.computeNodeResult.getPruningEstimate() == another.computeNodeResult
        .getPruningEstimate()) {
      return 0;
    } else {
      return -1;
    }
  }
}
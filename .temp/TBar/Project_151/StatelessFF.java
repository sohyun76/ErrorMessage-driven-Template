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
package org.apache.joshua.decoder.ff;

import java.util.List;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

/**
 * Stateless feature functions do not contribute any state. You need not implement this class to
 * create a stateless feature function, but it provides a few convenience functions.
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Juri Ganitkevich juri@cs.jhu.edu
 */

public abstract class StatelessFF extends FeatureFunction {

  public StatelessFF(FeatureVector weights, String name, String[] args, JoshuaConfiguration config) {
    super(weights, name, args, config);
  }

  public final boolean isStateful() {
    return false;
  }

  /**
   * The estimated cost of applying this feature, given only the rule. This is used in sorting the
   * rules for cube pruning. For most features, this will be 0.0.
   */
  public float estimateCost(Rule rule) {
    return 0.0f;
  }

  /**
   * Implementations of this should return null, since no state is contributed.
   */
  @Override
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, Sentence sentence, Accumulator acc);

  /**
   * Implementations of this should return null, since no state is contributed.
   */
  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, Sentence sentence,
      Accumulator acc) {
    return null;
  }

  /**
   * Stateless functions do not have an estimate of the future cost because they do not have access
   * to the state.
   */
  public final float estimateFutureCost(Rule rule, DPState state, Sentence sentence) {
    return 0.0f;
  }
}

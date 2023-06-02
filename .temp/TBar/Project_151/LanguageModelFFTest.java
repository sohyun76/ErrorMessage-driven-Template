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

package org.apache.joshua.decoder.ff.lm;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertEquals;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.state_maintenance.NgramDPState;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LanguageModelFFTest {

  private static final float WEIGHT = 0.5f;

  private LanguageModelFF ff;

  @BeforeMethod
  public void setUp() {
    Decoder.resetGlobalState();

    FeatureVector weights = new FeatureVector();
    weights.set("lm_0", WEIGHT);
    String[] args = {"-lm_type", "berkeleylm", "-lm_order", "2", "-lm_file", "./src/test/resources/lm/berkeley/lm"};

    JoshuaConfiguration config = new JoshuaConfiguration();
    ff = new LanguageModelFF(weights, args, config);
  }

  @AfterMethod
  public void tearDown() {
    Decoder.resetGlobalState();
  }

  @Test
  public void givenNonStartSymbol_whenEstimateFutureCost_thenMultipleWeightAndLogProbabilty() {
    int[] left = {3};
    NgramDPState currentState = new NgramDPState(left, new int[left.length]);

    float score =  ff.getLM().sentenceLogProbability(left, 2, 1);
    assertEquals(-99.0f, score, 0.0f);

    float cost = ff.estimateFutureCost(null, currentState, null);
    assertEquals(score * WEIGHT, cost, 0.0f);
  }

  @Test
  public void givenOnlyStartSymbol_whenEstimateFutureCost_thenZeroResult() {
    int startSymbolId = Vocabulary.id(Vocabulary.START_SYM);
    int[] left = {startSymbolId};
    NgramDPState currentState = new NgramDPState(left, new int[left.length]);

    float score = ff.getLM().sentenceLogProbability(left, 2, 2);
    assertEquals(0.0f, score, 0.0f);

    float cost = ff.estimateFutureCost(null, currentState, null);
    assertEquals(cost, score * WEIGHT, 0.0f);
  }

  @Test
  public void givenStartAndOneMoreSymbol_whenEstimateFutureCost_thenMultipleWeightAndLogProbabilty() {
    int startSymbolId = Vocabulary.id(Vocabulary.START_SYM);
    assertThat(startSymbolId, not(equalTo(3)));
    int[] left = {startSymbolId, 3};
    NgramDPState currentState = new NgramDPState(left, new int[left.length]);

    float score = ff.getLM().sentenceLogProbability(left, 2, 2);
    assertEquals(score, -100.752754f, 0.0f);

    float cost = ff.estimateFutureCost(null, currentState, null);
    assertEquals(cost, score * WEIGHT, 0.0f);
  }
}

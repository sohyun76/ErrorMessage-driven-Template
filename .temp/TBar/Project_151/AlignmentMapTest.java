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
 package org.apache.joshua.system;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;


public class AlignmentMapTest {
  
  private Rule rule1 = null;
  private Rule rule2 = null;
  private static Map<Integer, List<Integer>> expectedAlignmentMap = null;
  private static final int[] expectedNonTerminalPositions = {2,5};

  @BeforeMethod
  public void setUp() throws Exception {
    Vocabulary.clear();
    int[] sourceRhs = {Vocabulary.id("A1"),Vocabulary.id("A2"),-1,Vocabulary.id("B"),Vocabulary.id("C"),-2};
    int[] targetRhs = {Vocabulary.id("c"),Vocabulary.id("b1"),-1,Vocabulary.id("b2"),-4,Vocabulary.id("a")};
    int arity = 2; // 2 non terminals
    String alignment = "0-5 1-5 3-1 3-3 4-0";
    expectedAlignmentMap = new HashMap<Integer, List<Integer>>();
    expectedAlignmentMap.put(0, Arrays.asList(4));
    expectedAlignmentMap.put(5, Arrays.asList(0,1));
    expectedAlignmentMap.put(1, Arrays.asList(3));
    expectedAlignmentMap.put(3, Arrays.asList(3));
    rule1 = new Rule(-1, sourceRhs, targetRhs, "", arity, alignment);
    rule2 = new Rule(-1, sourceRhs, targetRhs, new FeatureVector(), arity, null); // rule with no alignment
  }

  @Test
  public void test() {
    // test regular rule with arity 2
    Map<Integer, List<Integer>> alignmentMap1 = rule1.getAlignmentMap();
    assertEquals(expectedAlignmentMap, alignmentMap1);
    int[] nonTerminalPositions1 = rule1.getNonTerminalSourcePositions();
    assertArrayEquals(expectedNonTerminalPositions, nonTerminalPositions1);
    
    // test rule with no alignment
    Map<Integer, List<Integer>> alignmentMap2 = rule2.getAlignmentMap();
    assertTrue(alignmentMap2.isEmpty());
    int[] nonTerminalPositions2 = rule2.getNonTerminalSourcePositions();
    assertArrayEquals(expectedNonTerminalPositions, nonTerminalPositions2);
  }

}

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
 package org.apache.joshua.util;

import org.testng.annotations.Test;

import static org.apache.joshua.util.FormatUtils.cleanNonTerminal;
import static org.apache.joshua.util.FormatUtils.escapeSpecialSymbols;
import static org.apache.joshua.util.FormatUtils.isNonterminal;
import static org.apache.joshua.util.FormatUtils.ensureNonTerminalBrackets;
import static org.apache.joshua.util.FormatUtils.stripNonTerminalIndex;
import static org.apache.joshua.util.FormatUtils.unescapeSpecialSymbols;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

public class FormatUtilsTest {
  
  @Test
  public void givenTokens_whenIsNonTerminal_thenTokensCorrectlyClassified() {
    assertTrue(isNonterminal("[X]"));
    assertTrue(isNonterminal("[X,1]"));
    assertFalse(isNonterminal("[]"));
    assertFalse(isNonterminal("[X)"));
  }
  
  @Test
  public void givenTokens_whenCleanNonTerminal_thenCorrectlyCleaned() {
    assertEquals(cleanNonTerminal("[GOAL]"), "GOAL");
    assertEquals(cleanNonTerminal("[X]"), "X");
    assertEquals(cleanNonTerminal("[X,1]"), "X");
    assertEquals(cleanNonTerminal("bla"), "bla");
    assertEquals(cleanNonTerminal("[bla"), "[bla");
  }
  
  @Test
  public void givenTokens_whenStripNonTerminalIndex_thenCorrectlyStripped() {
    assertEquals(stripNonTerminalIndex("[X,1]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X,114]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X,]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X"), "[[X]");
  }
  
  @Test
  public void givenTokens_whenMarkup_thenCorrectMarkup() {
    assertEquals(ensureNonTerminalBrackets("X"), "[X]");
  }
  
  @Test
  public void givenSpecialSymbols_whenEscapeSpecialSymbols_thenCorrectlyEscaped() {
    assertEquals(escapeSpecialSymbols("[ ] | ["), "-lsb- -rsb- -pipe- -lsb-");
  }
  
  @Test
  public void givenEscapedSpecialSymbols_whenUnEscapeSpecialSymbols_thenCorrectlyUnEscaped() {
    assertEquals(unescapeSpecialSymbols("-lsb- -rsb- -pipe- -lsb-"), "[ ] | [");
  }

}

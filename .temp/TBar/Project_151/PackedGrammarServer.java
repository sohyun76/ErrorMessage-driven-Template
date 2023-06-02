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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.Trie;
import org.apache.joshua.decoder.ff.tm.packed.PackedGrammar;
import org.apache.joshua.util.io.LineReader;

public class PackedGrammarServer {

  private PackedGrammar grammar;

  public PackedGrammarServer(String packed_directory,JoshuaConfiguration joshuaConfiguration) throws
      IOException {
    grammar = new PackedGrammar(packed_directory, -1, "owner", "thrax", joshuaConfiguration);
  }

  public List<Rule> get(String source) {
    return get(source.trim().split("\\s+"));
  }
  
  public List<Rule> get(String[] source) {
    int[] src = Vocabulary.addAll(source);
    Trie walker = grammar.getTrieRoot();
    for (int s : src) {
      walker = walker.match(s);
      if (walker == null)
        return null;
    }
    return walker.getRuleCollection().getRules();
  }
  
  public Map<String, Float> scores(String source, String target) {
    return scores(source.trim().split("\\s+"), target.trim().split("\\s+"));
  }
  
  public Map<String, Float> scores(String[] source, String[] target) {
    List<Rule> rules = get(source);
    
    if (rules == null)
      return null;
    
    int[] tgt = Vocabulary.addAll(target);
    for (Rule r : rules)
      if (Arrays.equals(tgt, r.getEnglish()))
        return r.getFeatureVector().getMap();
    
    return null;
  }
  
  
  public static void main(String[] args) throws IOException {
    JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
    PackedGrammarServer pgs = new PackedGrammarServer(args[0], joshuaConfiguration);
    
    for (String line: new LineReader(System.in)) {
      List<Rule> rules = pgs.get(line);
      if (rules == null) continue;
      for (Rule r : rules)
        System.out.println(r.toString());
    }
  }
}

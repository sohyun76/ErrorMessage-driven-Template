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
package org.apache.joshua.decoder.ff.tm.hash_based;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.JoshuaConfiguration.OOVItem;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.tm.AbstractGrammar;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.GrammarReader;
import org.apache.joshua.decoder.ff.tm.Trie;
import org.apache.joshua.decoder.ff.tm.format.HieroFormatReader;
import org.apache.joshua.decoder.ff.tm.format.MosesFormatReader;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a memory-based bilingual BatchGrammar.
 * <p>
 * The rules are stored in a trie. Each trie node has: (1) RuleBin: a list of rules matching the
 * french sides so far (2) A HashMap of next-layer trie nodes, the next french word used as the key
 * in HashMap
 * 
 * @author Zhifei Li zhifei.work@gmail.com
 * @author Matt Post post@cs.jhu.edu
 */
public class MemoryBasedBatchGrammar extends AbstractGrammar {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryBasedBatchGrammar.class);

  /* The number of rules read. */
  private int qtyRulesRead = 0;

  /* The number of distinct source sides. */
  private int qtyRuleBins = 0;

  private int numDenseFeatures = 0;

  /* The trie root. */
  private final MemoryBasedTrie root = new MemoryBasedTrie();

  /* The file containing the grammar. */
  private String grammarFile;

  private GrammarReader<Rule> modelReader;

  /**
   * Constructor used by Decoder mostly.
   * @param owner the associated decoder-wide {@link org.apache.joshua.decoder.ff.tm.OwnerMap}
   * @param config a {@link org.apache.joshua.decoder.JoshuaConfiguration} object
   * @param spanLimit the maximum span of the input grammar rule(s) can be applied to.
   */
  public MemoryBasedBatchGrammar(String owner, JoshuaConfiguration config, int spanLimit) {
    this(null, owner, config, spanLimit);
  }
  
  /**
   * Constructor used by Decoder for creating custom grammars.
   * 
   * @param file the file to load the grammar from
   * @param owner the associated decoder-wide {@link org.apache.joshua.decoder.ff.tm.OwnerMap}
   * @param config a {@link org.apache.joshua.decoder.JoshuaConfiguration} object
   * @param spanLimit the maximum span of the input grammar rule(s) can be applied to.
   */
  public MemoryBasedBatchGrammar(String file, String owner, JoshuaConfiguration config, int spanLimit) {
    super(owner, config, spanLimit);
  }
  
  /**
   * Constructor to initialize a GrammarReader (unowned)
   * @param reader the GrammarReader used for storing ASCII line-based grammars on disk.
   * @param config a {@link org.apache.joshua.decoder.JoshuaConfiguration} object
   * @param spanLimit the maximum span of the input grammar rule(s) can be applied to.
   */
  public MemoryBasedBatchGrammar(
      final GrammarReader<Rule> reader, final JoshuaConfiguration config, final int spanLimit) {
    super(OwnerMap.UNKNOWN_OWNER, config, spanLimit);
    modelReader = reader;
  }

  public MemoryBasedBatchGrammar(String formatKeyword, String grammarFile, String owner,
      String defaultLHSSymbol, int spanLimit, JoshuaConfiguration joshuaConfiguration)
      throws IOException {

    super(owner, joshuaConfiguration, spanLimit);
    Vocabulary.id(defaultLHSSymbol);
    this.grammarFile = grammarFile;

    // ==== loading grammar
    try {
      this.modelReader = createReader(formatKeyword, grammarFile);
    } catch (IOException e) {
      LOG.warn("Couldn't load a '{}' type grammar from file '{}'", formatKeyword, grammarFile);
    }
    if (modelReader != null) {
      for (Rule rule : modelReader)
        if (rule != null) {
          addRule(rule);
        }
    } else {
      LOG.info("Couldn't create a GrammarReader for file {} with format {}",
          grammarFile, formatKeyword);
    }
    
    LOG.info("MemoryBasedBatchGrammar: Read {} rules with {} distinct source sides from '{}'",
        this.qtyRulesRead, this.qtyRuleBins, grammarFile);
  }

  protected GrammarReader<Rule> createReader(String format, String grammarFile) throws IOException {

    if (grammarFile != null) {
      if ("hiero".equals(format) || "thrax".equals(format) || "phrase".equals(format)) {
        return new HieroFormatReader(grammarFile);
      } else if ("moses".equals(format)) {
        return new MosesFormatReader(grammarFile);
      } else {
        throw new RuntimeException(String.format("* FATAL: unknown grammar format '%s'", format));
      }
    }
    return null;
  }

  // ===============================================================
  // Methods
  // ===============================================================

  @Override
  public int getNumRules() {
    return this.qtyRulesRead;
  }

  /**
   * if the span covered by the chart bin is greater than the limit, then return false
   */
  public boolean hasRuleForSpan(int i, int j, int pathLength) {
    if (this.spanLimit == -1) { // mono-glue grammar
      return (i == 0);
    } else {
      // System.err.println(String.format("%s HASRULEFORSPAN(%d,%d,%d)/%d = %s",
      // Vocabulary.word(this.owner), i, j, pathLength, spanLimit, pathLength <= this.spanLimit));
      return (pathLength <= this.spanLimit);
    }
  }

  public Trie getTrieRoot() {
    return this.root;
  }

  /**
   * Adds a rule to the grammar.
   */
  public void addRule(Rule rule) {

    this.qtyRulesRead++;

    rule.setOwner(owner);

    if (numDenseFeatures == 0)
      numDenseFeatures = rule.getFeatureVector().getDenseFeatures().size();

    // === identify the position, and insert the trie nodes as necessary
    MemoryBasedTrie pos = root;
    int[] french = rule.getFrench();

    maxSourcePhraseLength = Math.max(maxSourcePhraseLength, french.length);

    for (int curSymID : french) {
      /*
       * Note that the nonTerminal symbol in the french is not cleaned (i.e., will be sth like
       * [X,1]), but the symbol in the Trie has to be cleaned, so that the match does not care about
       * the markup (i.e., [X,1] or [X,2] means the same thing, that is X) if
       * (Vocabulary.nt(french[k])) { curSymID = modelReader.cleanNonTerminal(french[k]); if
       * (logger.isLoggable(Level.FINEST)) logger.finest("Amended to: " + curSymID); }
       */

      MemoryBasedTrie nextLayer = (MemoryBasedTrie) pos.match(curSymID);
      if (null == nextLayer) {
        nextLayer = new MemoryBasedTrie();
        if (pos.hasExtensions() == false) {
          pos.childrenTbl = new HashMap<>();
        }
        pos.childrenTbl.put(curSymID, nextLayer);
      }
      pos = nextLayer;
    }

    // === add the rule into the trie node
    if (!pos.hasRules()) {
      pos.ruleBin = new MemoryBasedRuleBin(rule.getArity(), rule.getFrench());
      this.qtyRuleBins++;
    }
    pos.ruleBin.addRule(rule);
  }

  /***
   * Takes an input word and creates an OOV rule in the current grammar for that word.
   * 
   * @param sourceWord integer representation of word
   * @param featureFunctions {@link java.util.List} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   */
  @Override
  public void addOOVRules(int sourceWord, List<FeatureFunction> featureFunctions) {

    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now
    // almost
    // certainly is)
    final int targetWord = this.joshuaConfiguration.mark_oovs ? Vocabulary.id(Vocabulary
        .word(sourceWord) + "_OOV") : sourceWord;

    int[] sourceWords = { sourceWord };
    int[] targetWords = { targetWord };
    final String oovAlignment = "0-0";

    if (this.joshuaConfiguration.oovList != null && this.joshuaConfiguration.oovList.size() != 0) {
      for (OOVItem item : this.joshuaConfiguration.oovList) {
        Rule oovRule = new Rule(Vocabulary.id(item.label), sourceWords, targetWords, "", 0,
            oovAlignment);
        addRule(oovRule);
        oovRule.estimateRuleCost(featureFunctions);
      }
    } else {
      int nt_i = Vocabulary.id(this.joshuaConfiguration.default_non_terminal);
      Rule oovRule = new Rule(nt_i, sourceWords, targetWords, "", 0, oovAlignment);
      addRule(oovRule);
      oovRule.estimateRuleCost(featureFunctions);
    }
  }

  /**
   * Saves the grammar to the specified location.
   */
  @Override
  public void save() {
    
    LOG.info("Saving custom grammar to file '{}'", grammarFile);
    
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(grammarFile), "UTF-8"));

      ArrayList<Trie> nodes = new ArrayList<Trie>();
      nodes.add(root);
      while (nodes.size() > 0) {
        Trie trie = nodes.remove(0);
        // find all rules at the current node, print them
        if (trie.hasRules()) {
          for (Rule rule: trie.getRuleCollection().getRules()) {
            try {
              LOG.info("  rule: {}", rule.textFormat());
              out.write(rule.textFormat() + "\n");
            } catch (IOException e) {
              e.printStackTrace();
              return;
            }
          }
        }

        // graph is acyclical so we shouldn't have to check for having visited
        if (trie.hasExtensions())
          nodes.addAll(trie.getExtensions());
      }
      
      out.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }
  }

  /**
   * Adds a default set of glue rules.
   * 
   * @param featureFunctions an {@link java.util.ArrayList} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   */
  public void addGlueRules(ArrayList<FeatureFunction> featureFunctions) {
    HieroFormatReader reader = new HieroFormatReader();

    String goalNT = FormatUtils.cleanNonTerminal(joshuaConfiguration.goal_symbol);
    String defaultNT = FormatUtils.cleanNonTerminal(joshuaConfiguration.default_non_terminal);

    String[] ruleStrings = new String[] {
        String.format("[%s] ||| %s ||| %s ||| 0", goalNT, Vocabulary.START_SYM,
            Vocabulary.START_SYM),
        String.format("[%s] ||| [%s,1] [%s,2] ||| [%s,1] [%s,2] ||| -1", goalNT, goalNT, defaultNT,
            goalNT, defaultNT),
        String.format("[%s] ||| [%s,1] %s ||| [%s,1] %s ||| 0", goalNT, goalNT,
            Vocabulary.STOP_SYM, goalNT, Vocabulary.STOP_SYM) };

    for (String ruleString : ruleStrings) {
      Rule rule = reader.parseLine(ruleString);
      addRule(rule);
      rule.estimateRuleCost(featureFunctions);
    }
  }

  @Override
  public int getNumDenseFeatures() {
    return numDenseFeatures;
  }
}

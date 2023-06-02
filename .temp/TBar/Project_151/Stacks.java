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
package org.apache.joshua.decoder.phrase;

/***
 * Entry point for phrase-based decoding, analogous to {@link Chart} for the CKY algorithm. This
 * class organizes all the stacks used for decoding, and is responsible for building them. Stack
 * construction is stack-centric: that is, we loop over the number of source words in increasing sizes;
 * at each step of this iteration, we break the search between smaller stack sizes and source-side
 * phrase sizes.
 * 
 * The end result of decoding is a {@link Hypergraph} with the same format as hierarchical decoding.
 * Phrases are treating as left-branching rules, and the span information (i,j) is overloaded so
 * that i means nothing and j represents the index of the last-translated source word in each
 * hypothesis. This means that most hypergraph code can work without modification. The algorithm 
 * ensures that the coverage vector is consistent but the resulting hypergraph may not be projective,
 * which is different from the CKY algorithm, which does produce projective derivations. 
 * 
 * TODO Lattice decoding is not yet supported.
 */

import static org.apache.joshua.decoder.ff.tm.OwnerMap.UNKNOWN_OWNER;

import java.util.ArrayList;
import java.util.List;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.ComputeNodeResult;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.tm.AbstractGrammar;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.hypergraph.HyperEdge;
import org.apache.joshua.decoder.hypergraph.HyperGraph;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stacks {

  private static final Logger LOG = LoggerFactory.getLogger(Stacks.class);

  // The list of stacks, grouped according to number of source words covered
  private List<Stack> stacks;

  // The end state
  private Hypothesis end;
  
  final List<FeatureFunction> featureFunctions;

  private final Sentence sentence;

  private final JoshuaConfiguration config;

  /* Contains all the phrase tables */
  private final PhraseChart chart;
  
  /**
   * Entry point. Initialize everything. Create pass-through (OOV) phrase table and glue phrase
   * table (with start-of-sentence and end-of-sentence rules).
   * 
   * @param sentence input to {@link org.apache.joshua.lattice.Lattice}
   * @param featureFunctions {@link java.util.List} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   * @param grammars an array of {@link org.apache.joshua.decoder.ff.tm.Grammar}'s
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public Stacks(Sentence sentence, List<FeatureFunction> featureFunctions, Grammar[] grammars, 
      JoshuaConfiguration config) {

    this.sentence = sentence;
    this.featureFunctions = featureFunctions;
    this.config = config;
    
    int num_phrase_tables = 0;
    for (Grammar grammar : grammars)
      if (grammar instanceof PhraseTable)
        ++num_phrase_tables;
    
    PhraseTable[] phraseTables = new PhraseTable[num_phrase_tables + 2];
    for (int i = 0, j = 0; i < grammars.length; i++)
      if (grammars[i] instanceof PhraseTable)
        phraseTables[j++] = (PhraseTable) grammars[i];
    
    phraseTables[phraseTables.length - 2] = new PhraseTable(UNKNOWN_OWNER, config);
    phraseTables[phraseTables.length - 2].addRule(Hypothesis.END_RULE);
    
    phraseTables[phraseTables.length - 1] = new PhraseTable("oov", config);
    AbstractGrammar.addOOVRules(phraseTables[phraseTables.length - 1], sentence.getLattice(), featureFunctions, config.true_oovs_only);
    
    this.chart = new PhraseChart(phraseTables, featureFunctions, sentence, config.num_translation_options);
  }
  
  
  /**
   * The main algorithm. Returns a hypergraph representing the search space.
   * 
   * @return a {@link org.apache.joshua.decoder.hypergraph.HyperGraph} representing the search space
   */
  public HyperGraph search() {
    
    long startTime = System.currentTimeMillis();
    
    Future future = new Future(chart);
    stacks = new ArrayList<>();
    
    // <s> counts as the first word. Pushing null lets us count from one.
    stacks.add(null);

    // Initialize root hypothesis with <s> context and future cost for everything.
    ComputeNodeResult result = new ComputeNodeResult(this.featureFunctions, Hypothesis.BEGIN_RULE,
        null, -1, 1, null, this.sentence);
    Stack firstStack = new Stack(sentence, config);
    firstStack.add(new Hypothesis(result.getDPStates(), future.Full()));
    stacks.add(firstStack);
    
    // Decode with increasing numbers of source words. 
    for (int source_words = 2; source_words <= sentence.length(); ++source_words) {
      Stack targetStack = new Stack(sentence, config);
      stacks.add(targetStack);

      // Iterate over stacks to continue from.
      for (int phrase_length = 1; phrase_length <= Math.min(source_words - 1, chart.MaxSourcePhraseLength());
          phrase_length++) {
        int from_stack = source_words - phrase_length;
        Stack tailStack = stacks.get(from_stack);

        LOG.debug("WORDS {} MAX {} (STACK {} phrase_length {})", source_words,
            chart.MaxSourcePhraseLength(), from_stack, phrase_length);
        
        /* Each from stack groups together lots of different coverage vectors that all cover the
         * same number of words. We have the number of covered words from from_stack, and the length
         * of the phrases we are going to add from (source_words - from_stack). We now iterate over
         * all coverage vectors, finding the set of phrases that can extend each of them, given
         * the two constraints: the phrase length, and the current coverage vector. These will all
         * be grouped under the same target stack.
         */
        for (Coverage coverage: tailStack.getCoverages()) {
          ArrayList<Hypothesis> hypotheses = tailStack.get(coverage); 
          
          // the index of the starting point of the first possible phrase
          int begin = coverage.firstZero();
          
          // the absolute position of the ending spot of the last possible phrase
          int last_end = Math.min(coverage.firstZero() + config.reordering_limit, chart.SentenceLength());
          int last_begin = (last_end > phrase_length) ? (last_end - phrase_length) : 0;

          for (begin = coverage.firstZero(); begin <= last_begin; begin++) {
            if (!coverage.compatible(begin, begin + phrase_length) ||
                ! permissible(coverage, begin, begin + phrase_length)) {
              continue;
            }

            // Don't append </s> until the end
            if (begin == sentence.length() - 1 && source_words != sentence.length()) 
              continue;            

            /* We have found a permissible phrase start point and length for the current coverage
             * vector. Find all the phrases over that span.
             */
            PhraseNodes phrases = chart.getRange(begin, begin + phrase_length);
            if (phrases == null)
              continue;

            LOG.debug("Applying {} target phrases over [{}, {}]",
                phrases.size(), begin, begin + phrase_length);
            
            // TODO: could also compute some number of features here (e.g., non-LM ones)
            // float score_delta = context.GetScorer().transition(ant, phrases, begin, begin + phrase_length);
            
            // Future costs: remove span to be filled.
            float future_delta = future.Change(coverage, begin, begin + phrase_length);
            
            /* This associates with each span a set of hypotheses that can be extended by
             * phrases from that span. The hypotheses are wrapped in HypoState objects, which
             * augment the hypothesis score with a future cost.
             */
            Candidate cand = new Candidate(featureFunctions, sentence, hypotheses, phrases, future_delta, new int[] {0, 0});
            targetStack.addCandidate(cand);
          }
        }
      }

      /* At this point, every vertex contains a list of all existing hypotheses that the target
       * phrases in that vertex could extend. Now we need to create the search object, which
       * implements cube pruning. There are up to O(n^2) cubes, n the size of the current stack,
       * one cube each over each span of the input. Each "cube" has two dimensions: one representing
       * the target phrases over the span, and one representing all of these incoming hypotheses.
       * We seed the chart with the best item in each cube, and then repeatedly pop and extend.
       */
      
//      System.err.println(String.format("\nBuilding cube-pruning chart for %d words", source_words));

      targetStack.search();
    }
    
    LOG.info("Input {}: Search took {} seconds", sentence.id(),
        (System.currentTimeMillis() - startTime) / 1000.0f);
    
    return createGoalNode();
  }
    
  /**
   * Enforces reordering constraints. Our version of Moses' ReorderingConstraint::Check() and
   * SearchCubePruning::CheckDistortion(). 
   * 
   * @param coverage
   * @param begin
   * @param i
   * @return
   */
  private boolean permissible(Coverage coverage, int begin, int end) {
    int firstZero = coverage.firstZero();

    if (config.reordering_limit < 0)
      return true;
    
    /* We can always start with the first zero since it doesn't create a reordering gap
     */
    if (begin == firstZero)
      return true;

    /* If a gap is created by applying this phrase, make sure that you can reach the first
     * zero later on without violating the distortion constraint.
     */
    return end - firstZero <= config.reordering_limit;

  }


  /**
   * Searches through the goal stack, calling the final transition function on each node, and then returning
   * the best item. Usually the final transition code doesn't add anything, because all features
   * have already computed everything they need to. The standard exception is language models that
   * have not yet computed their prefix probabilities (which is not the case with KenLM, the default).
   * 
   * @return
   */
  private HyperGraph createGoalNode() {
    Stack lastStack = stacks.get(sentence.length());
    
    for (Hypothesis hyp: lastStack) {
      float score = hyp.getScore();
      List<HGNode> tailNodes = new ArrayList<>();
      tailNodes.add(hyp);
      
      float finalTransitionScore = ComputeNodeResult.computeFinalCost(featureFunctions, tailNodes, 0, sentence.length(), null, sentence);

//      System.err.println(String.format("createGoalNode: final score: %f -> %f", score, finalTransitionScore));
      
      if (null == this.end)
        this.end = new Hypothesis(null, score + finalTransitionScore, hyp, sentence.length(), null);

      HyperEdge edge = new HyperEdge(null, score + finalTransitionScore, finalTransitionScore, tailNodes, null);
      end.addHyperedgeInNode(edge);
    }
    
    return new HyperGraph(end, -1, -1, this.sentence);
  }
}

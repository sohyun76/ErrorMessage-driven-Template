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

import static org.apache.joshua.util.FormatUtils.isNonterminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Support;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.StatefulFF;
import org.apache.joshua.decoder.ff.lm.berkeley_lm.LMGrammarBerkeley;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.state_maintenance.NgramDPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;

/**
 * This class performs the following:
 * <ol>
 * <li>Gets the additional LM score due to combinations of small items into larger ones by using
 * rules</li>
 * <li>Gets the LM state</li>
 * <li>Gets the left-side LM state estimation score</li>
 * </ol>
 *
 * @author Matt Post post@cs.jhu.edu
 * @author Juri Ganitkevitch juri@cs.jhu.edu
 * @author Zhifei Li, zhifei.work@gmail.com
 */
public class LanguageModelFF extends StatefulFF {

  static final Logger LOG = LoggerFactory.getLogger(LanguageModelFF.class);

  public static int LM_INDEX = 0;
  private int startSymbolId;

  /**
   * N-gram language model. We assume the language model is in ARPA format for equivalent state:
   *
   * <ol>
   * <li>We assume it is a backoff lm, and high-order ngram implies low-order ngram; absense of
   * low-order ngram implies high-order ngram</li>
   * <li>For a ngram, existence of backoffweight =&gt; existence a probability Two ways of dealing with
   * low counts:
   * <ul>
   * <li>SRILM: don't multiply zeros in for unknown words</li>
   * <li>Pharaoh: cap at a minimum score exp(-10), including unknown words</li>
   * </ul>
   * </li>
   * </ol>
   */
  protected NGramLanguageModel languageModel;
  
  protected final static String NAME_PREFIX = "lm_";
  protected final static String OOV_SUFFIX = "_oov";
  protected final String oovFeatureName;

  /**
   * We always use this order of ngram, though the LMGrammar may provide higher order probability.
   */
  protected final int ngramOrder;

  /**
   * We cache the weight of the feature since there is only one.
   */
  protected final float weight;
  protected final float oovWeight;
  protected String type;
  protected final String path;

  /** Whether this is a class-based LM */
  protected boolean isClassLM;
  private ClassMap classMap;
  
  /** Whether this feature function fires LM oov indicators */ 
  protected boolean withOovFeature;
  protected int oovDenseFeatureIndex = -1;

  public LanguageModelFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, NAME_PREFIX + LM_INDEX, args, config);
    this.oovFeatureName = NAME_PREFIX + LM_INDEX + OOV_SUFFIX;
    LM_INDEX++;

    this.type = parsedArgs.get("lm_type");
    this.ngramOrder = Integer.parseInt(parsedArgs.get("lm_order"));
    this.path = config.getFilePath(parsedArgs.get("lm_file"));

    if (parsedArgs.containsKey("class_map")) {
      this.isClassLM = true;
      this.classMap = new ClassMap(parsedArgs.get("class_map"));
    }
    
    if (parsedArgs.containsKey("oov_feature")) {
      this.withOovFeature = true;
    }

    // The dense feature initialization hasn't happened yet, so we have to retrieve this as sparse
    this.weight = weights.getSparse(name);
    this.oovWeight = weights.getSparse(oovFeatureName);

    initializeLM();
  }

  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    oovDenseFeatureIndex = denseFeatureIndex + 1;

    final ArrayList<String> names = new ArrayList<>(2);
    names.add(name);
    if (withOovFeature) {
      names.add(oovFeatureName);
    }
    return names;
  }

  /**
   * Initializes the underlying language model.
   */
  protected void initializeLM() {
    switch (type) {
    case "kenlm":
      this.languageModel = new KenLM(ngramOrder, path);

      break;
    case "berkeleylm":
      this.languageModel = new LMGrammarBerkeley(ngramOrder, path);

      break;
    default:
      String msg = String.format("* FATAL: Invalid backend lm_type '%s' for LanguageModel", type)
          + "*        Permissible values for 'lm_type' are 'kenlm' and 'berkeleylm'";
      throw new RuntimeException(msg);
    }

    Vocabulary.registerLanguageModel(this.languageModel);
    Vocabulary.id(config.default_non_terminal);

    startSymbolId = Vocabulary.id(Vocabulary.START_SYM);
  }

  public NGramLanguageModel getLM() {
    return this.languageModel;
  }

  public boolean isClassLM() {
  	return this.isClassLM;
  }

  public String logString() {
    return String.format("%s, order %d (weight %.3f), classLm=%s", name, languageModel.getOrder(), weight, isClassLM);
  }

  /**
   * Computes the features incurred along this edge. Note that these features
   * are unweighted costs of the feature; they are the feature cost, not the
   * model cost, or the inner product of them.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
    SourcePath sourcePath, Sentence sentence, Accumulator acc) {

    if (rule == null) {
      return null;
    }

    int[] words;
    if (config.source_annotations) {
      // get source side annotations and project them to the target side
      words = getTags(rule, i, j, sentence);
    } else {
      words = getRuleIds(rule);
    }
    
    if (withOovFeature) {
      acc.add(oovDenseFeatureIndex, getOovs(words));
    }

    return computeTransition(words, tailNodes, acc);

	}

  /**
   * Retrieve ids from rule. These are either simply the rule ids on the target
   * side, their corresponding class map ids, or the configured source-side
   * annotation tags.
   * @param rule an input from from which to retrieve ids
   * @return an array if int's representing the id's from the input Rule
   */
  @VisibleForTesting
  public int[] getRuleIds(final Rule rule) {
    if (this.isClassLM) {
      // map words to class ids
      return getClasses(rule);
    }
    // Regular LM: use rule word ids
    return rule.getEnglish();
  }
  
  /**
   * Returns the number of LM oovs on the rule's target side.
   * Skips nonterminals.
   * @param words an input int array representing words we wish to obtain OOVs for
   * @return the number of OOVs for thr given int array
   */
  @VisibleForTesting
  public int getOovs(final int[] words) {
    int result = 0;
    for (int id : words) {
      if (!isNonterminal(id) && languageModel.isOov(id)) {
        result++;
      }
    }
    return result;
  }

  /**
   * Input sentences can be tagged with information specific to the language model. This looks for
   * such annotations by following a word's alignments back to the source words, checking for
   * annotations, and replacing the surface word if such annotations are found.
   * @param rule the {@link org.apache.joshua.decoder.ff.tm.Rule} to use
   * @param begin todo
   * @param end todo
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return todo
   */
  protected int[] getTags(Rule rule, int begin, int end, Sentence sentence) {
    /* Very important to make a copy here, so the original rule is not modified */
    int[] tokens = Arrays.copyOf(rule.getEnglish(), rule.getEnglish().length);
    byte[] alignments = rule.getAlignment();

    //    System.err.println(String.format("getTags() %s", rule.getRuleString()));

    /* For each target-side token, project it to each of its source-language alignments. If any of those
     * are annotated, take the first annotation and quit.
     */
    if (alignments != null) {
      for (int i = 0; i < tokens.length; i++) {
        if (tokens[i] > 0) { // skip nonterminals
          for (int j = 0; j < alignments.length; j += 2) {
            if (alignments[j] == i) {
              int index = tokens.length == alignments.length ? i : j;
              String annotation = sentence.getAnnotation((int) alignments[index] + begin, "class");
              if (annotation != null) {
                //                System.err.println(String.format("  word %d source %d abs %d annotation %d/%s",
                //                    i, alignments[i], alignments[i] + begin, annotation, Vocabulary.word(annotation)));
                tokens[i] = Vocabulary.id(annotation);
                break;
              }
            }
          }
        }
      }
    }

    return tokens;
  }

  /**
   * Sets the class map if this is a class LM
   * @param fileName a string path to a file
   * @throws IOException if there is an error reading the input file
   */
  public void setClassMap(String fileName) throws IOException {
    this.classMap = new ClassMap(fileName);
  }

  /**
   * Replace each word in a rule with the target side classes.
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to use when obtaining tokens
   * @return int[] of tokens
   */
  protected int[] getClasses(Rule rule) {
    if (this.classMap == null) {
      throw new RuntimeException("The class map is not set. Cannot use the class LM ");
    }
    /* Very important to make a copy here, so the original rule is not modified */
    int[] tokens = Arrays.copyOf(rule.getEnglish(), rule.getEnglish().length);
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i] > 0 ) { // skip non-terminals
        tokens[i] = this.classMap.getClassID(tokens[i]);
      }
    }
    return tokens;
  }

  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, Sentence sentence,
      Accumulator acc) {
    return computeFinalTransition((NgramDPState) tailNode.getDPState(stateIndex), acc);
  }

  /**
   * This function computes all the complete n-grams found in the rule, as well as the incomplete
   * n-grams on the left-hand side.
   */
  @Override
  public float estimateCost(Rule rule) {

    float lmEstimate = 0.0f;
    boolean considerIncompleteNgrams = true;

    int[] enWords = getRuleIds(rule);

    List<Integer> words = new ArrayList<>();
    boolean skipStart = (enWords[0] == startSymbolId); 

    /*
     * Move through the words, accumulating language model costs each time we have an n-gram (n >=
     * 2), and resetting the series of words when we hit a nonterminal.
     */
    for (int currentWord : enWords) {
      if (FormatUtils.isNonterminal(currentWord)) {
        lmEstimate += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
        words.clear();
        skipStart = false;
      } else {
        words.add(currentWord);
      }
    }
    lmEstimate += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
    
    final float oovEstimate = (withOovFeature) ? getOovs(enWords) : 0f;

    return weight * lmEstimate + oovWeight * oovEstimate;
  }

  /**
   * Estimates the future cost of a rule. For the language model feature, this is the sum of the
   * costs of the leftmost k-grams, k = [1..n-1].
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState currentState, Sentence sentence) {
    NgramDPState state = (NgramDPState) currentState;

    float estimate = 0.0f;
    int[] leftContext = state.getLeftLMStateWords();

    if (null != leftContext) {
      boolean skipStart = true;
      if (leftContext[0] != startSymbolId) {
        skipStart = false;
      }
      estimate += scoreChunkLogP(leftContext, true, skipStart);
    }
    // NOTE: no future cost for oov weight
    return weight * estimate;
  }

  /**
   * Compute the cost of a rule application. The cost of applying a rule is computed by determining
   * the n-gram costs for all n-grams created by this rule application, and summing them. N-grams
   * are created when (a) terminal words in the rule string are followed by a nonterminal (b)
   * terminal words in the rule string are preceded by a nonterminal (c) we encounter adjacent
   * nonterminals. In all of these situations, the corresponding boundary words of the node in the
   * hypergraph represented by the nonterminal must be retrieved.
   *
   * IMPORTANT: only complete n-grams are scored. This means that hypotheses with fewer words
   * than the complete n-gram state remain *unscored*. This fact adds a lot of complication to the
   * code, including the use of the computeFinal* family of functions, which correct this fact for
   * sentences that are too short on the final transition.
   */
  private NgramDPState computeTransition(int[] enWords, List<HGNode> tailNodes, Accumulator acc) {

    int[] current = new int[this.ngramOrder];
    int[] shadow = new int[this.ngramOrder];
    int ccount = 0;
    float transitionLogP = 0.0f;
    int[] left_context = null;

    for (int curID : enWords) {
      if (FormatUtils.isNonterminal(curID)) {
        int index = -(curID + 1);

        NgramDPState state = (NgramDPState) tailNodes.get(index).getDPState(stateIndex);
        int[] left = state.getLeftLMStateWords();
        int[] right = state.getRightLMStateWords();

        // Left context.
        for (int aLeft : left) {
          current[ccount++] = aLeft;

          if (left_context == null && ccount == this.ngramOrder - 1)
            left_context = Arrays.copyOf(current, ccount);

          if (ccount == this.ngramOrder) {
            // Compute the current word probability, and remove it.
            float prob = this.languageModel.ngramLogProbability(current, this.ngramOrder);
            //            System.err.println(String.format("-> prob(%s) = %f", Vocabulary.getWords(current), prob));
            transitionLogP += prob;
            System.arraycopy(current, 1, shadow, 0, this.ngramOrder - 1);
            int[] tmp = current;
            current = shadow;
            shadow = tmp;
            --ccount;
          }
        }
        System.arraycopy(right, 0, current, ccount - right.length, right.length);
      } else { // terminal words
        current[ccount++] = curID;

        if (left_context == null && ccount == this.ngramOrder - 1)
          left_context = Arrays.copyOf(current, ccount);

        if (ccount == this.ngramOrder) {
          // Compute the current word probability, and remove it.s
          float prob = this.languageModel.ngramLogProbability(current, this.ngramOrder);
          //          System.err.println(String.format("-> prob(%s) = %f", Vocabulary.getWords(current), prob));
          transitionLogP += prob;
          System.arraycopy(current, 1, shadow, 0, this.ngramOrder - 1);
          int[] tmp = current;
          current = shadow;
          shadow = tmp;
          --ccount;
        }
      }
    }
    //    acc.add(name, transitionLogP);
    acc.add(denseFeatureIndex, transitionLogP);

    if (left_context != null) {
      return new NgramDPState(left_context, Arrays.copyOfRange(current, ccount - this.ngramOrder
          + 1, ccount));
    } else {
      int[] context = Arrays.copyOf(current, ccount);
      return new NgramDPState(context, context);
    }
  }

  /**
   * This function differs from regular transitions because we incorporate the cost of incomplete
   * left-hand ngrams, as well as including the start- and end-of-sentence markers (if they were
   * requested when the object was created).
   *
   * @param state the dynamic programming state
   * @return the final transition probability (including incomplete n-grams)
   */
  private NgramDPState computeFinalTransition(NgramDPState state, Accumulator acc) {

    //    System.err.println(String.format("LanguageModel::computeFinalTransition()"));

    float res = 0.0f;
    LinkedList<Integer> currentNgram = new LinkedList<>();
    int[] leftContext = state.getLeftLMStateWords();
    int[] rightContext = state.getRightLMStateWords();

    for (int t : leftContext) {
      currentNgram.add(t);

      if (currentNgram.size() >= 2) { // start from bigram
        float prob = this.languageModel
            .ngramLogProbability(Support.toArray(currentNgram), currentNgram.size());
        res += prob;
      }
      if (currentNgram.size() == this.ngramOrder)
        currentNgram.removeFirst();
    }

    // Tell the accumulator
    //    acc.add(name, res);
    acc.add(denseFeatureIndex, res);

    // State is the same
    return new NgramDPState(leftContext, rightContext);
  }


  /**
   * Compatibility method for {@link #scoreChunkLogP(int[], boolean, boolean)}
   */
  private float scoreChunkLogP(List<Integer> words, boolean considerIncompleteNgrams,
      boolean skipStart) {
    return scoreChunkLogP(Ints.toArray(words), considerIncompleteNgrams, skipStart);
  }

  /**
   * This function is basically a wrapper for NGramLanguageModel::sentenceLogProbability(). It
   * computes the probability of a phrase ("chunk"), using lower-order n-grams for the first n-1
   * words.
   *
   * @param words
   * @param considerIncompleteNgrams
   * @param skipStart
   * @return the phrase log probability
   */
  private float scoreChunkLogP(int[] words, boolean considerIncompleteNgrams,
      boolean skipStart) {

    float score = 0.0f;
    if (words.length > 0) {
      int startIndex;
      if (!considerIncompleteNgrams) {
        startIndex = this.ngramOrder;
      } else if (skipStart) {
        startIndex = 2;
      } else {
        startIndex = 1;
      }
      score = this.languageModel.sentenceLogProbability(words, this.ngramOrder, startIndex);
    }

    return score;
  }

  /**
   * Public method to set LM_INDEX back to 0.
   * Required if multiple instances of the JoshuaDecoder live in the same JVM.
   */
  public static void resetLmIndex() {
    LM_INDEX = 0;
  }
}

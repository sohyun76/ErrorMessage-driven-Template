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
package org.apache.joshua.decoder.ff.tm;

import static org.apache.joshua.decoder.ff.tm.OwnerMap.UNKNOWN_OWNER_ID;

import java.util.ArrayList;
import java.util.Arrays;  
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class define the interface for Rule. 
 * 
 * All feature scores are interpreted as negative log probabilities, and are therefore negated.
 * Note that not all features need to be negative log probs, but you should be aware that they
 * will be negated, so if you want a positive count, it should come in as negative.
 * 
 * Normally, the feature score in the rule should be *cost* (i.e., -LogP), so that the feature
 * weight should be positive
 * 
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author Matt Post post@cs.jhu.edu
 */
public class Rule implements Comparator<Rule>, Comparable<Rule> {

  private static final Logger LOG = LoggerFactory.getLogger(Rule.class);
  private int lhs; // tag of this rule
  private int[] source; // pointer to the RuleCollection, as all the rules under it share the same
                         // Source side
  protected int arity;

  // And a string containing the sparse ones
  //protected final String sparseFeatureString;
  protected final Supplier<String> sparseFeatureStringSupplier;
  private final Supplier<FeatureVector> featuresSupplier;

  /*
   * a feature function will be fired for this rule only if the owner of the rule matches the owner
   * of the feature function
   */
  private OwnerId owner = UNKNOWN_OWNER_ID;

  /**
   * This is the cost computed only from the features present with the grammar rule. This cost is
   * needed to sort the rules in the grammar for cube pruning, but isn't the full cost of applying
   * the rule (which will include contextual features that can't be computed until the rule is
   * applied).
   */
  private float estimatedCost = Float.NEGATIVE_INFINITY;

  private float precomputableCost = Float.NEGATIVE_INFINITY;

  private int[] target;

  // The alignment string, e.g., 0-0 0-1 1-1 2-1
  private String alignmentString;
  private final Supplier<byte[]> alignmentSupplier;

  /**
   * Constructs a new rule using the provided parameters. Rule id for this rule is
   * undefined. Note that some of the sparse features may be unlabeled, but they cannot be mapped to
   * their default names ("tm_OWNER_INDEX") until later, when we know the owner of the rule. This is
   * not known until the rule is actually added to a grammar in Grammar::addRule().
   * 
   * Constructor used by other constructors below;
   * 
   * @param lhs Left-hand side of the rule.
   * @param source Source language right-hand side of the rule.
   * @param target Target language right-hand side of the rule.
   * @param sparseFeatures Feature value scores for the rule.
   * @param arity Number of nonterminals in the source language right-hand side.
   * @param owner todo
   */
  public Rule(int lhs, int[] source, int[] target, String sparseFeatures, int arity, OwnerId owner) {
    this.lhs = lhs;
    this.source = source;
    this.arity = arity;
    this.owner = owner;
    this.target = target;
    this.sparseFeatureStringSupplier = Suppliers.memoize(() -> sparseFeatures);
    this.featuresSupplier = initializeFeatureSupplierFromString();
    this.alignmentSupplier = initializeAlignmentSupplier();
  }
  
  /**
   * Constructor used by PackedGrammar's sortRules()
   * @param lhs todo
   * @param sourceRhs todo
   * @param targetRhs todo
   * @param features todo
   * @param arity todo
   * @param owner todo
   */
  public Rule(int lhs, int[] sourceRhs, int[] targetRhs, FeatureVector features, int arity, OwnerId owner) {
    this.lhs = lhs;
    this.source = sourceRhs;
    this.arity = arity;
    this.owner = owner;
    this.target = targetRhs;
    this.featuresSupplier = Suppliers.memoize(() -> features);
    this.sparseFeatureStringSupplier = initializeSparseFeaturesStringSupplier();
    this.alignmentSupplier = initializeAlignmentSupplier();
  }

  /**
   * Constructor used for SamtFormatReader and GrammarBuilderWalkerFunction's getRuleWithSpans()
   * Rule is unowned.
   * @param lhs todo
   * @param sourceRhs todo
   * @param targetRhs todo
   * @param sparseFeatures todo
   * @param arity todo
   */
  public Rule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity) {
    this(lhs, sourceRhs, targetRhs, sparseFeatures, arity, OwnerMap.UNKNOWN_OWNER_ID);
  }

  /**
   * Constructor used for addOOVRules(), HieroFormatReader and PhraseRule.
   * @param lhs todo
   * @param sourceRhs todo
   * @param targetRhs todo
   * @param sparseFeatures todo
   * @param arity todo
   * @param alignment todo
   */
  public Rule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity, String alignment) {
    this(lhs, sourceRhs, targetRhs, sparseFeatures, arity);
    this.alignmentString = alignment;
  }
  
  /**
   * Constructor (implicitly) used by PackedRule
   */
  public Rule() {
    this.lhs = -1;
    this.sparseFeatureStringSupplier = initializeSparseFeaturesStringSupplier();
    this.featuresSupplier = initializeFeatureSupplierFromString();
    this.alignmentSupplier = initializeAlignmentSupplier();
  }

  // ==========================================================================
  // Lazy loading Suppliers for alignments, feature vector, and feature strings
  // ==========================================================================
  
  private Supplier<byte[]> initializeAlignmentSupplier(){
    return Suppliers.memoize(() ->{
      byte[] alignment = null;
      String alignmentString = getAlignmentString();
      if (alignmentString != null) {
        String[] tokens = alignmentString.split("[-\\s]+");
        alignment = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++)
          alignment[i] = (byte) Short.parseShort(tokens[i]);
      }
      return alignment;
    });
  }
  
  /**
   * If Rule was constructed with sparseFeatures String, we lazily populate the
   * FeatureSupplier.
   */
  private Supplier<FeatureVector> initializeFeatureSupplierFromString(){
    return Suppliers.memoize(() ->{
      if (!owner.equals(UNKNOWN_OWNER_ID)) {
        return new FeatureVector(getFeatureString(), "tm_" + OwnerMap.getOwner(owner) + "_");
      } else {
        return new FeatureVector();
      }
    });
  }
  
  /**
   * If Rule was constructed with a FeatureVector, we lazily populate the sparseFeaturesStringSupplier.
   */
  private Supplier<String> initializeSparseFeaturesStringSupplier() {
    return Suppliers.memoize(() -> getFeatureVector().toString());
  }

  // ===============================================================
  // Attributes
  // ===============================================================

  public void setEnglish(int[] eng) {
    this.target = eng;
  }

  public int[] getEnglish() {
    return this.target;
  }

  /**
   * Two Rules are equal of they have the same LHS, the same source RHS and the same target
   * RHS.
   * 
   * @param o the object to check for equality
   * @return true if o is the same Rule as this rule, false otherwise
   */
  public boolean equals(Object o) {
    if (!(o instanceof Rule)) {
      return false;
    }
    Rule other = (Rule) o;
    if (getLHS() != other.getLHS()) {
      return false;
    }
    if (!Arrays.equals(getFrench(), other.getFrench())) {
      return false;
    }
    return Arrays.equals(target, other.getEnglish());
  }

  public int hashCode() {
    // I just made this up. If two rules are equal they'll have the
    // same hashcode. Maybe someone else can do a better job though?
    int frHash = Arrays.hashCode(getFrench());
    int enHash = Arrays.hashCode(target);
    return frHash ^ enHash ^ getLHS();
  }

  // ===============================================================
  // Attributes
  // ===============================================================

  public void setArity(int arity) {
    this.arity = arity;
  }

  public int getArity() {
    return this.arity;
  }

  public void setOwner(final OwnerId owner) {
    this.owner = owner;
  }

  public OwnerId getOwner() {
    return this.owner;
  }

  public void setLHS(int lhs) {
    this.lhs = lhs;
  }

  public int getLHS() {
    return this.lhs;
  }

  public void setFrench(int[] french) {
    this.source = french;
  }

  public int[] getFrench() {
    return this.source;
  }

  /**
   * This function does the work of turning the string version of the sparse features (passed in
   * when the rule was created) into an actual set of features. This is a bit complicated because we
   * support intermingled labeled and unlabeled features, where the unlabeled features are mapped to
   * a default name template of the form "tm_OWNER_INDEX".
   * 
   * This function returns the dense (phrasal) features discovered when the rule was loaded. Dense
   * features are the list of unlabeled features that preceded labeled ones. They can also be
   * specified as labeled features of the form "tm_OWNER_INDEX", but the former format is preferred.
   * 
   * @return the {@link org.apache.joshua.decoder.ff.FeatureVector} for this rule
   */
  public FeatureVector getFeatureVector() {
    return featuresSupplier.get();
  }

  /**
   * This function returns the estimated cost of a rule, which should have been computed when the
   * grammar was first sorted via a call to Rule::estimateRuleCost(). This function is a getter
   * only; it will not compute the value if it has not already been set. It is necessary in addition
   * to estimateRuleCost(models) because sometimes the value needs to be retrieved from contexts
   * that do not have access to the feature functions.
   * 
   * This function is called by the rule comparator when sorting the grammar. As such it may be
   * called many times and any implementation of it should be a cached implementation.
   * 
   * @return the estimated cost of the rule (a lower bound on the true cost)
   */
  public float getEstimatedCost() {
    return estimatedCost;
  }

  /**
   * Precomputable costs is the inner product of the weights found on each grammar rule and the
   * weight vector. This is slightly different from the estimated rule cost, which can include other
   * features (such as a language model estimate). This getter and setter should also be cached, and
   * is basically provided to allow the PhraseModel feature to cache its (expensive) computation for
   * each rule.
   *
   * The weights are passed in as dense weights and sparse weights. This allows the dense weight
   * precomputation to be even faster (since we don't have to query a hash map. 
   *
   * @param dense_weights the dense weights from the model
   * @param weights the sparse weights from the model
   */
  public void setPrecomputableCost(float[] dense_weights, FeatureVector weights) {
    float cost = 0.0f;
    FeatureVector features = getFeatureVector();
    for (int i = 0; i < features.getDenseFeatures().size() && i < dense_weights.length; i++) {
      cost += dense_weights[i] * features.getDense(i);
    }

    for (String key: features.getSparseFeatures().keySet()) {
      cost += weights.getSparse(key) * features.getSparse(key);
    }
    
    this.precomputableCost = cost;
  }
  
  /**
   * @return the precomputed model cost of each rule
   */
  public float getPrecomputableCost() {
    return precomputableCost;
  }
  
  public float getDenseFeature(int k) {
    return getFeatureVector().getDense(k);
  }
  
  /**
   * This function estimates the cost of a rule, which is used for sorting the rules for cube
   * pruning. The estimated cost is basically the set of precomputable features (features listed
   * along with the rule in the grammar file) along with any other estimates that other features
   * would like to contribute (e.g., a language model estimate). This cost will be a lower bound on
   * the rule's actual cost.
   * 
   * The value of this function is used only for sorting the rules. When the rule is later applied
   * in context to particular hypernodes, the rule's actual cost is computed.
   * 
   * @param models the list of models available to the decoder
   * @return estimated cost of the rule
   */
  public float estimateRuleCost(List<FeatureFunction> models) {
    if (null == models)
      return 0.0f;

    if (this.estimatedCost <= Float.NEGATIVE_INFINITY) {
      this.estimatedCost = 0.0f; // weights.innerProduct(computeFeatures());

//      LOG.debug("estimateCost({} ;; {})", getFrenchWords(), getEnglishWords());
      for (FeatureFunction ff : models) {
        float val = ff.estimateCost(this);
//        LOG.debug("  FEATURE {} -> {}", ff.getName(), val);
        this.estimatedCost += val; 
      }
    }
    
    return estimatedCost;
  }

  // ===============================================================
  // Methods
  // ===============================================================

  public String toString() {
    String sb = Vocabulary.word(this.getLHS()) +
        " ||| " +
        getFrenchWords() +
        " ||| " +
        getEnglishWords() +
        " |||" +
        " " + getFeatureVector() +
        String.format(" ||| est=%.3f", getEstimatedCost()) +
        String.format(" pre=%.3f", getPrecomputableCost());
    return sb;
  }
  
  /**
   * Returns a version of the rule suitable for reading in from a text file.
   * 
   * @return string version of the rule
   */
  public String textFormat() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.getLHS()));
    sb.append(" |||");
    
    int nt = 1;
    for (int i = 0; i < getFrench().length; i++) {
      if (getFrench()[i] < 0)
        sb.append(" ").append(
            Vocabulary.word(getFrench()[i]).replaceFirst("\\]", String.format(",%d]", nt++)));
      else
        sb.append(" ").append(Vocabulary.word(getFrench()[i]));
    }
    sb.append(" |||");
    nt = 1;
    for (int i = 0; i < getEnglish().length; i++) {
      if (getEnglish()[i] < 0)
        sb.append(" ").append(
            Vocabulary.word(getEnglish()[i]).replaceFirst("\\]", String.format(",%d]", nt++)));
      else
        sb.append(" ").append(Vocabulary.word(getEnglish()[i]));
    }
    sb.append(" |||");
    sb.append(" ").append(getFeatureString());
    if (getAlignmentString() != null)
      sb.append(" ||| ").append(getAlignmentString());
    return sb.toString();
  }

  public String getFeatureString() {
    return sparseFeatureStringSupplier.get();
  }

  /**
   * Returns an alignment as a sequence of integers. The integers at positions i and i+1 are paired,
   * with position i indexing the source and i+1 the target.
   * 
   * @return a byte[] from the {@link com.google.common.base.Supplier}
   */
  public byte[] getAlignment() {
    return this.alignmentSupplier.get();
  }
  
  public String getAlignmentString() {
    return this.alignmentString;
  }

  /**
   * The nonterminals on the English side are pointers to the source side nonterminals (-1 and -2),
   * rather than being directly encoded. These number indicate the correspondence between the
   * nonterminals on each side, introducing a level of indirection however when we want to resolve
   * them. So to get the ID, we need to look up the corresponding source side ID.
   * 
   * @return The string of English words
   */
  public String getEnglishWords() {
    int[] foreignNTs = getForeignNonTerminals();
  
    StringBuilder sb = new StringBuilder();
    for (Integer index : getEnglish()) {
      if (index >= 0)
        sb.append(Vocabulary.word(index)).append(" ");
      else
        sb.append(Vocabulary.word(foreignNTs[-index - 1]).replace("]",
            String.format(",%d] ", Math.abs(index))));
    }
  
    return sb.toString().trim();
  }

  public boolean isTerminal() {
    for (int i = 0; i < getEnglish().length; i++)
      if (getEnglish()[i] < 0)
        return false;
  
    return true;
  }

  /**
   * Return the French (source) nonterminals as list of Strings
   * 
   * @return a list of strings
   */
  public int[] getForeignNonTerminals() {
    int[] nts = new int[getArity()];
    int index = 0;
    for (int id : getFrench())
      if (id < 0)
        nts[index++] = -id;
    return nts;
  }
  
  /**
   * Returns an array of size getArity() containing the source indeces of non terminals.
   * 
   * @return an array of size getArity() containing the source indeces of non terminals
   */
  public int[] getNonTerminalSourcePositions() {
    int[] nonTerminalPositions = new int[getArity()];
    int ntPos = 0;
    for (int sourceIdx = 0; sourceIdx < getFrench().length; sourceIdx++) {
      if (getFrench()[sourceIdx] < 0)
        nonTerminalPositions[ntPos++] = sourceIdx;
    }
    return nonTerminalPositions;
  }
  
  /**
   * Parses the Alignment byte[] into a Map from target to (possibly a list of) source positions.
   * Used by the WordAlignmentExtractor.
   * 
   * @return a {@link java.util.Map} of alignments
   */
  public Map<Integer, List<Integer>> getAlignmentMap() {
    byte[] alignmentArray = getAlignment();
    Map<Integer, List<Integer>> alignmentMap = new HashMap<>();
    if (alignmentArray != null) {
      for (int alignmentIdx = 0; alignmentIdx < alignmentArray.length; alignmentIdx += 2 ) {
        int s = alignmentArray[alignmentIdx];
        int t = alignmentArray[alignmentIdx + 1];
        List<Integer> values = alignmentMap.get(t);
        if (values == null)
          alignmentMap.put(t, values = new ArrayList<>());
        values.add(s);
      }
    }
    return alignmentMap;
  }

  /**
   * Return the English (target) nonterminals as list of Strings
   * 
   * @return list of strings
   */
  public int[] getEnglishNonTerminals() {
    int[] nts = new int[getArity()];
    int[] foreignNTs = getForeignNonTerminals();
    int index = 0;
  
    for (int i : getEnglish()) {
      if (i < 0)
        nts[index++] = foreignNTs[Math.abs(getEnglish()[i]) - 1];
    }
  
    return nts;
  }

  private int[] getNormalizedEnglishNonterminalIndices() {
    int[] result = new int[getArity()];
  
    int ntIndex = 0;
    for (Integer index : getEnglish()) {
      if (index < 0)
        result[ntIndex++] = -index - 1;
    }
  
    return result;
  }

  public boolean isInverting() {
    int[] normalizedEnglishNonTerminalIndices = getNormalizedEnglishNonterminalIndices();
    if (normalizedEnglishNonTerminalIndices.length == 2) {
      if (normalizedEnglishNonTerminalIndices[0] == 1) {
        return true;
      }
    }
    return false;
  }

  public String getFrenchWords() {
    return Vocabulary.getWords(getFrench());
  }

  public static final String NT_REGEX = "\\[[^\\]]+?\\]";

  private Pattern getPattern() {
    String source = getFrenchWords();
    String pattern = Pattern.quote(source);
    pattern = pattern.replaceAll(NT_REGEX, "\\\\E.+\\\\Q");
    pattern = pattern.replaceAll("\\\\Q\\\\E", "");
    pattern = "(?:^|\\s)" + pattern + "(?:$|\\s)";
    return Pattern.compile(pattern);
  }

  /**
   * Matches the string representation of the rule's source side against a sentence
   * 
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return true if there is a match
   */
  public boolean matches(Sentence sentence) {
    // System.err.println(String.format("match(%s,%s) = %s", Pattern.quote(getFrenchWords()),
    // sentence.annotatedSource(), match));
    return getPattern().matcher(sentence.fullSource()).find();
  }

  /**
   * This comparator is used for sorting the rules during cube pruning. An estimate of the cost
   * of each rule is computed and used to sort. 
   */
  public static final Comparator<Rule> EstimatedCostComparator = (rule1, rule2) -> {
    float cost1 = rule1.getEstimatedCost();
    float cost2 = rule2.getEstimatedCost();
    return Float.compare(cost2,  cost1);
  };
  
  public int compare(Rule rule1, Rule rule2) {
    return EstimatedCostComparator.compare(rule1, rule2);
  }

  public int compareTo(Rule other) {
    return EstimatedCostComparator.compare(this, other);
  }

  public String getRuleString() {
    return String.format("%s -> %s ||| %s", Vocabulary.word(getLHS()), getFrenchWords(), getEnglishWords());
  }
}

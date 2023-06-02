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
package org.apache.joshua.decoder.hypergraph;

import static org.apache.joshua.util.FormatUtils.unescapeSpecialSymbols;
import static org.apache.joshua.util.FormatUtils.removeSentenceMarkers;
import static java.util.Collections.emptyList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.BLEU;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.fragmentlm.Tree;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.io.DeNormalize;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.decoder.segment_file.Token;
import org.apache.joshua.util.FormatUtils;
import org.apache.joshua.decoder.StructuredTranslation;
import org.apache.joshua.decoder.StructuredTranslationFactory;

/**
 * <p>This class implements lazy k-best extraction on a hyper-graph.</p>
 * 
 * <p>K-best extraction over hypergraphs is a little hairy, but is best understood in the following
 * manner. Imagine a hypergraph, which is composed of nodes connected by hyperedges. A hyperedge has
 * exactly one parent node and 1 or more tail nodes, corresponding to the rank of the rule that gave
 * rise to the hyperedge. Each node has 1 or more incoming hyperedges.</p>
 * 
 * <p>K-best extraction works in the following manner. A derivation is a set of nodes and hyperedges
 * that leads from the root node down and exactly covers the source-side sentence. To define a
 * derivation, we start at the root node, choose one of its incoming hyperedges, and then recurse to
 * the tail (or antecedent) nodes of that hyperedge, where we continually make the same decision.</p>
 * 
 * <p>Each hypernode has its hyperedges sorted according to their model score. To get the best
 * (Viterbi) derivation, we simply recursively follow the best hyperedge coming in to each
 * hypernode.</p>
 * 
 * <p>How do we get the second-best derivation? It is defined by changing exactly one of the decisions
 * about which hyperedge to follow in the recursion. Somewhere, we take the second-best. Similarly,
 * the third-best derivation makes a single change from the second-best: either making another
 * (differnt) second-best choice somewhere along the 1-best derivation, or taking the third-best
 * choice at the same spot where the second-best derivation took the second-best choice. And so on.</p>
 * 
 * <p>This class uses two classes that encode the necessary meta-information. The first is the
 * DerivationState class. It roughly corresponds to a hyperedge, and records, for each of that
 * hyperedge's tail nodes, which-best to take. So for a hyperedge with three tail nodes, the 1-best
 * derivation will be (1,1,1), the second-best will be one of (2,1,1), (1,2,1), or (1,1,2), the
 * third best will be one of</p>
 * 
 * <code>(3,1,1), (2,2,1), (1,1,3)</code>
 * 
 * <p>and so on.</p>
 * 
 * <p>The configuration parameter `output-format` controls what exactly is extracted from the forest.
 * See documentation for that below. Note that Joshua does not store individual feature values while 
 * decoding, but only the cost of each edge (in the form of a float). Therefore, if you request
 * the features values (`%f` in `output-format`), the feature functions must be replayed, which
 * is expensive.</p>
 * 
 * <p>The configuration parameter `top-n` controls how many items are returned. If this is set to 0,
 * k-best extraction should be turned off entirely.</p>
 * 
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author Matt Post post@cs.jhu.edu
 */
public class KBestExtractor {
  private final JoshuaConfiguration joshuaConfiguration;
  private final String outputFormat;
  private final HashMap<HGNode, VirtualNode> virtualNodesTable = new HashMap<>();

  // static final String rootSym = JoshuaConfiguration.goal_symbol;
  static final String rootSym = "ROOT";
  static final int rootID = Vocabulary.id(rootSym);

  private enum Side {
    SOURCE, TARGET
  }

  /* Whether to extract only unique strings */
  private final boolean extractUniqueNbest;

  /* Which side to output (source or target) */
  private final Side defaultSide;

  /* The input sentence */
  private final Sentence sentence;

  /* The weights being used to score the forest */
  private final FeatureVector weights;

  /* The feature functions */
  private final List<FeatureFunction> featureFunctions;

  /* BLEU statistics of the references */
  private BLEU.References references = null;

  public KBestExtractor(
      Sentence sentence,
      List<FeatureFunction> featureFunctions,
      FeatureVector weights,
      boolean isMonolingual,
      JoshuaConfiguration joshuaConfiguration) {

    this.featureFunctions = featureFunctions;

    this.joshuaConfiguration = joshuaConfiguration;
    this.outputFormat = this.joshuaConfiguration.outputFormat;
    this.extractUniqueNbest = joshuaConfiguration.use_unique_nbest;

    this.weights = weights;
    this.defaultSide = (isMonolingual ? Side.SOURCE : Side.TARGET);
    this.sentence = sentence;

    if (joshuaConfiguration.rescoreForest) {
      references = new BLEU.References(sentence.references());
    }
  }

  /**
   * Returns the kth derivation.
   * 
   * You may need to reset_state() before you call this function for the first time.
   * 
   * @param node the node to start at
   * @param k the kth best derivation (indexed from 1)
   * @return the derivation object
   */
  public DerivationState getKthDerivation(HGNode node, int k) {
    final VirtualNode virtualNode = getVirtualNode(node);
    return virtualNode.lazyKBestExtractOnNode(this, k);
  }
  
  /**
   * Returns the k-th Structured Translation.
   * 
   * @param node The node to extract from
   * @param k The (1-indexed) index of the item wanted
   * @return a StructuredTranslation object
   */
  public StructuredTranslation getKthStructuredTranslation(HGNode node, int k) {
    StructuredTranslation result = null;
    final DerivationState derivationState = getKthDerivation(node, k);
    if (derivationState != null) {
      result = StructuredTranslationFactory.fromKBestDerivation(sentence, derivationState);
    }
    return result;
  }
  
  /**
   * This is an entry point for extracting k-best hypotheses as StructuredTranslation objects.
   * It computes all of them and returning a list of StructuredTranslation objects.
   * These objects hold all translation information (string, tokens, features, alignments, score).
   * 
   * @param hg the hypergraph to extract from
   * @param topN how many to extract
   * @return list of StructuredTranslation objects, empty if there is no HyperGraph goal node.
   */
  public List<StructuredTranslation> KbestExtractOnHG(HyperGraph hg, int topN) {
    resetState();
    if (hg == null || hg.goalNode == null) {
      return emptyList();
    }
    final List<StructuredTranslation> kbest = new ArrayList<>(topN);
    for (int k = 1; k <= topN; k++) {
      StructuredTranslation translation = getKthStructuredTranslation(hg.goalNode, k);
      if (translation == null) {
        break;
      }
      kbest.add(translation);
    }
    return kbest;
  }
  
  /**
   * Compute the string that is output from the decoder, using the "output-format" config file
   * parameter as a template.
   * 
   * You may need to {@link org.apache.joshua.decoder.hypergraph.KBestExtractor#resetState()} 
   * before you call this function for the first time.
   * 
   * @param node todo
   * @param k todo
   * @return todo
   */
  public String getKthHyp(HGNode node, int k) {

    String outputString = null;
    DerivationState derivationState = getKthDerivation(node, k);
    if (derivationState != null) {
      // ==== read the kbest from each hgnode and convert to output format
      String hypothesis = maybeProjectCase(
                            unescapeSpecialSymbols(
                              removeSentenceMarkers(
                                derivationState.getHypothesis())), derivationState);
      
      
      /*
       * To save space, the decoder only stores the model cost,
       * no the individual feature values.
       * If you want to output them, you have to replay them.
       */

      FeatureVector features = new FeatureVector();
      if (outputFormat.contains("%f") || outputFormat.contains("%d"))
        features = derivationState.getFeatures();

      outputString = outputFormat
          .replace("%k", Integer.toString(k))
          .replace("%s", hypothesis)
          .replace("%S", DeNormalize.processSingleLine(hypothesis))
          // TODO (kellens): Fix the recapitalization here
          .replace("%i", Integer.toString(sentence.id()))
          .replace("%f", joshuaConfiguration.moses ? features.mosesString() : features.toString())
          .replace("%c", String.format("%.3f", derivationState.cost));

      if (outputFormat.contains("%t")) {
        outputString = outputString.replace("%t", derivationState.getTree());
      }

      if (outputFormat.contains("%e")) {
        outputString = outputString.replace("%e", removeSentenceMarkers(derivationState.getHypothesis(Side.SOURCE)));
      }

      /* %d causes a derivation with rules one per line to be output */
      if (outputFormat.contains("%d")) {
        outputString = outputString.replace("%d", derivationState.getDerivation());
      }
      
      /* %a causes output of word level alignments between input and output hypothesis */
      if (outputFormat.contains("%a")) {
        outputString = outputString.replace("%a",  derivationState.getWordAlignment());
      }
      
    }

    return outputString;
  }

  // =========================== end kbestHypergraph

  /**
   * If requested, projects source-side lettercase to target, and appends the alignment from
   * to the source-side sentence in ||s.
   * 
   * @param hypothesis todo
   * @param state todo
   * @return source-side lettercase to target, and appends the alignment from to the source-side sentence in ||s
   */
  private String maybeProjectCase(String hypothesis, DerivationState state) {
    String output = hypothesis;

    if (joshuaConfiguration.project_case) {
      String[] tokens = hypothesis.split("\\s+");
      List<List<Integer>> points = state.getWordAlignmentList();
      for (int i = 0; i < points.size(); i++) {
        List<Integer> target = points.get(i);
        for (int source: target) {
          Token token = sentence.getTokens().get(source + 1); // skip <s>
          String annotation = "";
          if (token != null && token.getAnnotation("lettercase") != null)
            annotation = token.getAnnotation("lettercase");
          if (source != 0 && annotation.equals("upper"))
            tokens[i] = FormatUtils.capitalize(tokens[i]);
          else if (annotation.equals("all-upper"))
            tokens[i] = tokens[i].toUpperCase();
        }
      }

      output = String.join(" ",  tokens);
    }

    return output;
  }

  /**
   * Convenience function for k-best extraction that prints to STDOUT.
   * @param hg the {@link org.apache.joshua.decoder.hypergraph.HyperGraph} from which to extract
   * @param topN the number of k
   * @throws IOException if there is an error writing the extraction
   */
  public void lazyKBestExtractOnHG(HyperGraph hg, int topN) throws IOException {
    lazyKBestExtractOnHG(hg, topN, new BufferedWriter(new OutputStreamWriter(System.out)));
  }

  /**
   * This is the entry point for extracting k-best hypotheses. It computes all of them, writing
   * the results to the BufferedWriter passed in. If you want intermediate access to the k-best
   * derivations, you'll want to call getKthHyp() or getKthDerivation() directly.
   * 
   * The number of derivations that are looked for is controlled by the `top-n` parameter.
   * Note that when `top-n` is set to 0, k-best extraction is disabled entirely, and only things 
   * like the viterbi string and the model score are available to the decoder. Since k-best
   * extraction involves the recomputation of features to get the component values, turning off
   * that extraction saves a lot of time when only the 1-best string is desired.
   * 
   * @param hg the hypergraph to extract from
   * @param topN how many to extract
   * @param out object to write to
   * @throws IOException if there is an error writing the extraction
   */
  public void lazyKBestExtractOnHG(HyperGraph hg, int topN, BufferedWriter out) throws IOException {

    resetState();

    if (null == hg.goalNode)
      return;

    for (int k = 1; k <= topN; k++) {
      String hypStr = getKthHyp(hg.goalNode, k);
      if (null == hypStr)
        break;

      out.write(hypStr);
      out.write("\n");
      out.flush();
    }
  }

  /**
   * This clears the virtualNodesTable, which maintains a list of virtual nodes. This should be
   * called in between forest rescorings.
   */
  public void resetState() {
    virtualNodesTable.clear();
  }

  /**
   * Returns the {@link org.apache.joshua.decoder.hypergraph.KBestExtractor.VirtualNode} 
   * corresponding to an {@link org.apache.joshua.decoder.hypergraph.HGNode}. 
   * If no such VirtualNode exists, it is created.
   * 
   * @param hgnode from which we wish to create a 
   *    {@link org.apache.joshua.decoder.hypergraph.KBestExtractor.VirtualNode}
   * @return the corresponding {@link org.apache.joshua.decoder.hypergraph.KBestExtractor.VirtualNode}
   */
  private VirtualNode getVirtualNode(HGNode hgnode) {
    VirtualNode virtualNode = virtualNodesTable.get(hgnode);
    if (null == virtualNode) {
      virtualNode = new VirtualNode(hgnode);
      virtualNodesTable.put(hgnode, virtualNode);
    }
    return virtualNode;
  }

  /**
   * This class is essentially a wrapper around an HGNode, annotating it with information needed to
   * record which hypotheses have been explored from this point. There is one virtual node for
   * each HGNode in the underlying hypergraph. This VirtualNode maintains information about the
   * k-best derivations from that point on, retaining the derivations computed so far and a priority 
   * queue of candidates.
   */
  private class VirtualNode {

    // The node being annotated.
    HGNode node = null;

    // sorted ArrayList of DerivationState, in the paper is: D(^) [v]
    public final List<DerivationState> nbests = new ArrayList<>();

    // remember frontier states, best-first; in the paper, it is called cand[v]
    private PriorityQueue<DerivationState> candHeap = null;

    // Remember which DerivationState has been explored (positions in the hypercube). This allows
    // us to avoid duplicated states that are reached from different places of expansion, e.g.,
    // position (2,2) can be reached be extending (1,2) and (2,1).
    private HashSet<DerivationState> derivationTable = null;

    // This records unique *strings* at each item, used for unique-nbest-string extraction.
    private HashSet<String> uniqueStringsTable = null;

    public VirtualNode(HGNode it) {
      this.node = it;
    }

    /**
     * This returns a DerivationState corresponding to the kth-best derivation rooted at this node.
     * 
     * @param kbestExtractor todo
     * @param k (indexed from one)
     * @return the k-th best (1-indexed) hypothesis, or null if there are no more.
     */
    // return: the k-th hyp or null; k is started from one
    private DerivationState lazyKBestExtractOnNode(KBestExtractor kbestExtractor, int k) {
      if (nbests.size() >= k) { // no need to continue
        return nbests.get(k - 1);
      }

      // ### we need to fill in the l_nest in order to get k-th hyp
      DerivationState derivationState = null;

      /*
       * The first time this is called, the heap of candidates (the frontier of the cube) is
       * uninitialized. This recursive call will seed the candidates at each node.
       */
      if (null == candHeap) {
        getCandidates(kbestExtractor);
      }

      /*
       * Now build the kbest list by repeatedly popping the best candidate and then placing all
       * extensions of that hypothesis back on the candidates list.
       */
      int tAdded = 0; // sanity check
      while (nbests.size() < k) {
        if (candHeap.size() > 0) {
          derivationState = candHeap.poll();
          // derivation_tbl.remove(res.get_signature());//TODO: should remove? note that two state
          // may be tied because the cost is the same
          if (extractUniqueNbest) {
            // We pass false for extract_nbest_tree because we want; to check that the hypothesis
            // *strings* are unique, not the trees.
            final String res_str = derivationState.getHypothesis();
            
            if (!uniqueStringsTable.contains(res_str)) {
              nbests.add(derivationState);
              uniqueStringsTable.add(res_str);
            }
          } else {
            nbests.add(derivationState);
          }

          // Add all extensions of this hypothesis to the candidates list.
          lazyNext(kbestExtractor, derivationState);

          // debug: sanity check
          tAdded++;
          // this is possible only when extracting unique nbest
          if (!extractUniqueNbest && tAdded > 1) {
            throw new RuntimeException("In lazyKBestExtractOnNode, add more than one time, k is "
                + k);
          }
        } else {
          break;
        }
      }
      if (nbests.size() < k) {
        derivationState = null;// in case we do not get to the depth of k
      }
      // debug: sanity check
      // if (l_nbest.size() >= k && l_nbest.get(k-1) != res) {
      // throw new RuntimeException("In lazy_k_best_extract, ranking is not correct ");
      // }

      return derivationState;
    }

    /**
     * This function extends the current hypothesis, adding each extended item to the list of
     * candidates (assuming they have not been added before). It does this by, in turn, extending
     * each of the tail node items.
     * 
     * @param kbestExtractor
     * @param previousState
     */
    private void lazyNext(KBestExtractor kbestExtractor, DerivationState previousState) {
      /* If there are no tail nodes, there is nothing to do. */
      if (null == previousState.edge.getTailNodes())
        return;

      /* For each tail node, create a new state candidate by "sliding" that item one position. */
      for (int i = 0; i < previousState.edge.getTailNodes().size(); i++) {
        /* Create a new virtual node that is a copy of the current node */
        HGNode tailNode = previousState.edge.getTailNodes().get(i);
        VirtualNode virtualTailNode = kbestExtractor.getVirtualNode(tailNode);
        // Copy over the ranks.
        int[] newRanks = new int[previousState.ranks.length];
        System.arraycopy(previousState.ranks, 0, newRanks, 0, newRanks.length);
        // Now increment/slide the current tail node by one
        newRanks[i] = previousState.ranks[i] + 1;

        // Create a new state so we can see if it's new. The cost will be set below if it is.
        DerivationState nextState = new DerivationState(previousState.parentNode,
            previousState.edge, newRanks, 0.0f, previousState.edgePos);

        // Don't add the state to the list of candidates if it's already been added.
        if (!derivationTable.contains(nextState)) {
          // Make sure that next candidate exists
          virtualTailNode.lazyKBestExtractOnNode(kbestExtractor, newRanks[i]);
          // System.err.println(String.format("  newRanks[%d] = %d and tail size %d", i,
          // newRanks[i], virtualTailNode.nbests.size()));
          if (newRanks[i] <= virtualTailNode.nbests.size()) {
            // System.err.println("NODE: " + this.node);
            // System.err.println("  tail is " + virtualTailNode.node);
            float cost = previousState.getModelCost()
                - virtualTailNode.nbests.get(previousState.ranks[i] - 1).getModelCost()
                + virtualTailNode.nbests.get(newRanks[i] - 1).getModelCost();
            nextState.setCost(cost);

            if (joshuaConfiguration.rescoreForest)
              nextState.bleu = nextState.computeBLEU();

            candHeap.add(nextState);
            derivationTable.add(nextState);

            // System.err.println(String.format("  LAZYNEXT(%s", nextState));
          }
        }
      }
    }

    /**
     * this is the seeding function, for example, it will get down to the leaf, and sort the
     * terminals get a 1best from each hyperedge, and add them into the heap_cands
     * 
     * @param kbestExtractor
     */
    private void getCandidates(KBestExtractor kbestExtractor) {
      /* The list of candidates extending from this (virtual) node. */
      candHeap = new PriorityQueue<>(11, new DerivationStateComparator());

      /*
       * When exploring the cube frontier, there are multiple paths to each candidate. For example,
       * going down 1 from grid position (2,1) is the same as going right 1 from grid position
       * (1,2). To avoid adding states more than once, we keep a list of derivation states we have
       * already added to the candidates heap.
       * 
       * TODO: these should really be keyed on the states themselves instead of a string
       * representation of them.
       */
      derivationTable = new HashSet<>();

      /*
       * A Joshua configuration option allows the decoder to output only unique strings. In that
       * case, we keep an list of the frontiers of derivation states extending from this node.
       */
      if (extractUniqueNbest) {
        uniqueStringsTable = new HashSet<>();
      }

      /*
       * Get the single-best derivation along each of the incoming hyperedges, and add the lot of
       * them to the priority queue of candidates in the form of DerivationState objects.
       * 
       * Note that since the hyperedges are not sorted according to score, the first derivation
       * computed here may not be the best. But since the loop over all hyperedges seeds the entire
       * candidates list with the one-best along each of them, when the candidate heap is polled
       * afterwards, we are guaranteed to have the best one.
       */
      int pos = 0;
      for (HyperEdge edge : node.hyperedges) {
        DerivationState bestState = getBestDerivation(kbestExtractor, node, edge, pos);
        // why duplicate, e.g., 1 2 + 1 0 == 2 1 + 0 1 , but here we should not get duplicate
        if (!derivationTable.contains(bestState)) {
          candHeap.add(bestState);
          derivationTable.add(bestState);
        } else { // sanity check
          throw new RuntimeException(
              "get duplicate derivation in get_candidates, this should not happen"
                  + "\nsignature is " + bestState + "\nl_hyperedge size is "
                  + node.hyperedges.size());
        }
        pos++;
      }

      // TODO: if tem.size is too large, this may cause unnecessary computation, we comment the
      // segment to accommodate the unique nbest extraction
      /*
       * if(tem.size()>global_n){ heap_cands=new PriorityQueue<DerivationState>(new DerivationStateComparator()); for(int i=1;
       * i<=global_n; i++) heap_cands.add(tem.poll()); }else heap_cands=tem;
       */
    }

    // get my best derivation, and recursively add 1best for all my children, used by get_candidates
    // only
    /**
     * This computes the best derivation along a particular hyperedge. It is only called by
     * getCandidates() to initialize the candidates priority queue at each (virtual) node.
     * 
     * @param kbestExtractor
     * @param parentNode
     * @param hyperEdge
     * @param edgePos
     * @return an object representing the best derivation from this node
     */
    private DerivationState getBestDerivation(KBestExtractor kbestExtractor, HGNode parentNode,
        HyperEdge hyperEdge, int edgePos) {
      int[] ranks;
      float cost = 0.0f;

      /*
       * There are two cases: (1) leaf nodes and (2) internal nodes. A leaf node is represented by a
       * hyperedge with no tail nodes.
       */
      if (hyperEdge.getTailNodes() == null) {
        ranks = null;

      } else {
        // "ranks" records which derivation to take at each of the tail nodes. Ranks are 1-indexed.
        ranks = new int[hyperEdge.getTailNodes().size()];

        /* Initialize the one-best at each tail node. */
        for (int i = 0; i < hyperEdge.getTailNodes().size(); i++) { // children is ready
          ranks[i] = 1;
          VirtualNode childVirtualNode = kbestExtractor.getVirtualNode(hyperEdge.getTailNodes()
              .get(i));
          // recurse
          childVirtualNode.lazyKBestExtractOnNode(kbestExtractor, ranks[i]);
        }
      }
      cost = hyperEdge.getBestDerivationScore();

      DerivationState state = new DerivationState(parentNode, hyperEdge, ranks, cost, edgePos);
      if (joshuaConfiguration.rescoreForest)
        state.bleu = state.computeBLEU();

      return state;
    }
  }

  /**
   * A DerivationState describes which path to follow through the hypergraph. For example, it
   * might say to use the 1-best from the first tail node, the 9th-best from the second tail node,
   * and so on. This information is represented recursively through a chain of DerivationState
   * objects. This function follows that chain, extracting the information according to a number
   * of parameters, and returning results to a string, and also (optionally) accumulating the
   * feature values into the passed-in FeatureVector.
   */

  // each DerivationState roughly corresponds to a hypothesis
  public class DerivationState {
    /* The edge ("e" in the paper) */
    public final HyperEdge edge;

    /* The edge's parent node */
    public final HGNode parentNode;

    /*
     * This state's position in its parent node's list of incoming hyperedges (used in signature
     * calculation)
     */
    public final int edgePos;

    /*
     * The rank item to select from each of the incoming tail nodes ("j" in the paper, an ArrayList
     * of size |e|)
     */
    public final int[] ranks;

    /*
     * The cost of the hypothesis, including a weighted BLEU score, if any.
     */
    private float cost;

    private float bleu = 0.0f;

    /*
     * The BLEU sufficient statistics associated with the edge's derivation. Note that this is a
     * function of the complete derivation headed by the edge, i.e., all the particular
     * subderivations of edges beneath it. That is why it must be contained in DerivationState
     * instead of in the HyperEdge itself.
     */
    BLEU.Stats stats = null;

    public DerivationState(HGNode pa, HyperEdge e, int[] r, float c, int pos) {
      parentNode = pa;
      edge = e;
      ranks = r;
      cost = c;
      edgePos = pos;
      bleu = 0.0f;
    }

    /**
     * Computes a scaled approximate BLEU from the accumulated statistics. We know the number of
     * words; to compute the effective reference length, we take the real reference length statistic
     * and scale it by the percentage of the input sentence that is consumed, based on the
     * assumption that the total number of words in the hypothesis scales linearly with the input
     * sentence span.
     * 
     * @return float representing {@link org.apache.joshua.decoder.BLEU} score
     */
    public float computeBLEU() {
      if (stats == null) {
        float percentage = 1.0f * (parentNode.j - parentNode.i) / (sentence.length());
        // System.err.println(String.format("computeBLEU: (%d - %d) / %d = %f", parentNode.j,
        // parentNode.i, sentence.length(), percentage));
        stats = BLEU.compute(edge, percentage, references);

        if (edge.getTailNodes() != null) {
          for (int id = 0; id < edge.getTailNodes().size(); id++) {
            stats.add(getChildDerivationState(edge, id).stats);
          }
        }
      }

      return BLEU.score(stats);
    }

    public void setCost(float cost2) {
      this.cost = cost2;
    }

    /**
     * Returns the model cost. This is obtained by subtracting off the incorporated BLEU score (if
     * used).
     * 
     * @return float representing model cost
     */
    public float getModelCost() {
      return this.cost;
    }

    /**
     * Returns the model cost plus the BLEU score.
     * 
     * @return float representing model cost plus the BLEU score
     */
    public float getCost() {
      return cost - weights.getSparse("BLEU") * bleu;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder(String.format("DS[[ %s (%d,%d)/%d ||| ",
          Vocabulary.word(parentNode.lhs), parentNode.i, parentNode.j, edgePos));
      sb.append("ranks=[ ");
      if (ranks != null)
        for (int rank : ranks)
          sb.append(rank + " ");
      sb.append("] ||| ").append(String.format("%.5f ]]", cost));
      return sb.toString();
    }

    public boolean equals(Object other) {
      if (other instanceof DerivationState) {
        DerivationState that = (DerivationState) other;
        if (edgePos == that.edgePos) {
          if (ranks != null && that.ranks != null) {
            if (ranks.length == that.ranks.length) {
              for (int i = 0; i < ranks.length; i++)
                if (ranks[i] != that.ranks[i])
                  return false;
              return true;
            }
          }
        }
      }

      return false;
    }

    /**
     * DerivationState objects are unique to each VirtualNode, so the unique identifying information
     * only need contain the edge position and the ranks.
     * @return hashof the edge position and ranks
     */
    public int hashCode() {
      int hash = edgePos;
      if (ranks != null) {
        for (int i = 0; i < ranks.length; i++)
          hash = hash * 53 + i;
      }

      return hash;
    }

    /**
     * Visits every state in the derivation in a depth-first order.
     * @param visitor todo
     * @return todo
     */
    private DerivationVisitor visit(DerivationVisitor visitor) {
      return visit(visitor, 0, 0);
    }

    private DerivationVisitor visit(DerivationVisitor visitor, int indent, int tailNodeIndex) {

      visitor.before(this, indent, tailNodeIndex);

      final Rule rule = edge.getRule();
      final List<HGNode> tailNodes = edge.getTailNodes();

      if (rule == null) {
        getChildDerivationState(edge, 0).visit(visitor, indent + 1, 0);
      } else {
        if (tailNodes != null) {
          for (int index = 0; index < tailNodes.size(); index++) {
            getChildDerivationState(edge, index).visit(visitor, indent + 1, index);
          }
        }
      }

      visitor.after(this, indent, tailNodeIndex);

      return visitor;
    }
    
    public String getWordAlignment() {
      return visit(new WordAlignmentExtractor()).toString();
    }
    
    public List<List<Integer>> getWordAlignmentList() {
      final WordAlignmentExtractor visitor = new WordAlignmentExtractor();
      visit(visitor);
      return visitor.getFinalWordAlignments();
    }

    public String getTree() {
      return visit(new TreeExtractor()).toString();
    }
    
    public String getHypothesis() {
      return getHypothesis(defaultSide);
    }

    private String getHypothesis(final Side side) {
      return visit(new OutputStringExtractor(side.equals(Side.SOURCE))).toString();
    }

    public FeatureVector getFeatures() {
      final FeatureVectorExtractor extractor = new FeatureVectorExtractor(featureFunctions, sentence);
      visit(extractor);
      return extractor.getFeatures();
    }

    public String getDerivation() {
      return visit(new DerivationExtractor()).toString();
    }

    /**
     * Helper function for navigating the hierarchical list of DerivationState objects. This
     * function looks up the VirtualNode corresponding to the HGNode pointed to by the edge's
     * {tailNodeIndex}th tail node.
     * 
     * @param edge todo
     * @param tailNodeIndex todo
     * @return todo
     */
    public DerivationState getChildDerivationState(HyperEdge edge, int tailNodeIndex) {
      HGNode child = edge.getTailNodes().get(tailNodeIndex);
      VirtualNode virtualChild = getVirtualNode(child);
      return virtualChild.nbests.get(ranks[tailNodeIndex] - 1);
    }

  } // end of Class DerivationState

  public static class DerivationStateComparator implements Comparator<DerivationState> {
    // natural order by cost
    public int compare(DerivationState one, DerivationState another) {
      if (one.getCost() > another.getCost()) {
        return -1;
      } else if (one.getCost() == another.getCost()) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  /**
   * This interface provides a generic way to do things at each stage of a derivation. The
   * DerivationState::visit() function visits every node in a derivation and calls the
   * DerivationVisitor functions both before and after it visits each node. This provides a common
   * way to do different things to the tree (e.g., extract its words, assemble a derivation, and so
   * on) without having to rewrite the node-visiting code.
   * 
   * @author Matt Post post@cs.jhu.edu
   */
  public interface DerivationVisitor {
    /**
     * Called before each node's children are visited.
     *
     * @param state the derivation state
     * @param level the tree depth
     * @param tailNodeIndex the tailNodeIndex corresponding to state
     */
    void before(DerivationState state, int level, int tailNodeIndex);

    /**
     * Called after a node's children have been visited.
     * 
     * @param state the derivation state
     * @param level the tree depth
     * @param tailNodeIndex the tailNodeIndex corresponding to state
     */
    void after(DerivationState state, int level, int tailNodeIndex);
  }
  
  /**
   * Assembles a Penn treebank format tree for a given derivation.
   */
  public class TreeExtractor implements DerivationVisitor {

    /* The tree being built. */
    private Tree tree;

    public TreeExtractor() {
      tree = null;
    }

    /**
     * Before visiting the children, find the fragment representation for the current rule,
     * and merge it into the tree we're building.
     */
    @Override
    public void before(DerivationState state, int indent, int tailNodeIndex) {
      HyperEdge edge = state.edge;
      Rule rule = edge.getRule();

      // Skip the special top-level rule
      if (rule == null) {
        return;
      }

      String lhs = Vocabulary.word(rule.getLHS());
      String unbracketedLHS = lhs.substring(1, lhs.length() - 1);

      /* Find the fragment corresponding to this flattened rule in the fragment map; if it's not
       * there, just pretend it's a depth-one rule.
       */
      Tree fragment = Tree.getFragmentFromYield(rule.getEnglishWords());
      if (fragment == null) {
        String subtree = String.format("(%s{%d-%d} %s)", unbracketedLHS, 
            state.parentNode.i, state.parentNode.j, 
            quoteTerminals(rule.getEnglishWords()));
        fragment = Tree.fromString(subtree);
      }
      
      merge(fragment);
    }

    /**
     * Quotes just the terminals in the yield of a tree, represented as a string. This is to force
     * compliance with the Tree class, which interprets all non-quoted strings as nonterminals. 
     * 
     * @param words a string of words representing a rule's yield
     * @return
     */
    private String quoteTerminals(String words) {
      StringBuilder quotedWords = new StringBuilder();
      for (String word: words.split("\\s+"))
        if (word.startsWith("[") && word.endsWith("]"))
          quotedWords.append(String.format("%s ", word));
        else
        quotedWords.append(String.format("\"%s\" ", word));

      return quotedWords.substring(0, quotedWords.length() - 1);
    }

    @Override
    public void after(DerivationState state, int indent, int tailNodeIndex) {
      // do nothing
    }

    public String toString() {
      return tree.unquotedString();
    }

    /**
     * Either set the root of the tree or merge this tree by grafting it onto the first nonterminal
     * in the yield of the parent tree.
     * 
     * @param fragment
     */
    private void merge(Tree fragment) {
      if (tree == null) {
        tree = fragment;
      } else {
        Tree parent = tree.getNonterminalYield().get(0);
        parent.setLabel(Vocabulary.word(fragment.getLabel()));
        parent.setChildren(fragment.getChildren());
      }
    }
  }

  /**
   * Assembles an informative version of the derivation. Each rule is printed as it is encountered.
   * Don't try to parse this output; make something that writes out JSON or something, instead.
   * 
   * @author Matt Post post@cs.jhu.edu
   */
  public class DerivationExtractor implements DerivationVisitor {

    final StringBuffer sb;

    public DerivationExtractor() {
      sb = new StringBuffer();
    }

    @Override
    public void before(DerivationState state, int indent, int tailNodeIndex) {

      HyperEdge edge = state.edge;
      Rule rule = edge.getRule();

      if (rule != null) {

        for (int i = 0; i < indent * 2; i++)
          sb.append(" ");

        final FeatureVectorExtractor extractor = new FeatureVectorExtractor(featureFunctions, sentence);
        extractor.before(state, indent, tailNodeIndex);
        final FeatureVector transitionFeatures = extractor.getFeatures();

        // sb.append(rule).append(" ||| " + features + " ||| " +
        // KBestExtractor.this.weights.innerProduct(features));
        sb.append(String.format("%d-%d", state.parentNode.i, state.parentNode.j));
        sb.append(" ||| ").append(Vocabulary.word(rule.getLHS())).append(" -> ")
            .append(Vocabulary.getWords(rule.getFrench())).append(" /// ")
            .append(rule.getEnglishWords());
        sb.append(" |||");
        for (DPState dpState : state.parentNode.getDPStates()) {
          sb.append(" ").append(dpState);
        }
        sb.append(" ||| ").append(transitionFeatures);
        sb.append(" ||| ").append(weights.innerProduct(transitionFeatures));
        if (rule.getAlignment() != null)
          sb.append(" ||| ").append(Arrays.toString(rule.getAlignment()));
        sb.append("\n");
      }
    }

    public String toString() {
      return sb.toString();
    }

    @Override
    public void after(DerivationState state, int level, int tailNodeIndex) {}
  }
  

}

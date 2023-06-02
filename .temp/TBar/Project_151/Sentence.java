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
package org.apache.joshua.decoder.segment_file;

import static org.apache.joshua.util.FormatUtils.addSentenceMarkers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.LanguageModelStateManager;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.lattice.Arc;
import org.apache.joshua.lattice.Lattice;
import org.apache.joshua.lattice.Node;
import org.apache.joshua.util.ChartSpan;
import org.apache.joshua.util.Regex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents lattice input. The lattice is contained on a single line and is represented
 * in PLF (Python Lattice Format), e.g.,
 * 
 * <pre>
 * ((('ein',0.1,1),('dieses',0.2,1),('haus',0.4,2),),(('haus',0.8,1),),)
 * </pre>
 * 
 * @author Matt Post post@cs.jhu.edu
 */

public class Sentence {

  private static final Logger LOG = LoggerFactory.getLogger(Sentence.class);

  /* The sentence number. */
  public int id = -1;

  /*
   * The source and target sides of the input sentence. Target sides are present when doing
   * alignment or forced decoding.
   */
  protected String source = null;
  protected String fullSource = null;
  
  protected String target = null;
  protected String fullTarget = null;
  protected String[] references = null;

  /* Lattice representation of the source sentence. */
  protected Lattice<Token> sourceLattice = null;

  /* List of constraints */
  private final List<ConstraintSpan> constraints;
  
  public JoshuaConfiguration config = null;

  private LanguageModelStateManager stateManager = new LanguageModelStateManager();

  /**
   * Constructor. Receives a string representing the input sentence. This string may be a
   * string-encoded lattice or a plain text string for decoding.
   * 
   * @param inputString representing the input sentence
   * @param id ID to associate with the input string
   * @param joshuaConfiguration a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public Sentence(String inputString, int id, JoshuaConfiguration joshuaConfiguration) {
  
    inputString = Regex.spaces.replaceAll(inputString, " ").trim();
    
    config = joshuaConfiguration;
    
    this.constraints = new LinkedList<>();

    // Check if the sentence has SGML markings denoting the
    // sentence ID; if so, override the id passed in to the
    // constructor
    Matcher start = SEG_START.matcher(inputString);
    if (start.find()) {
      source = SEG_END.matcher(start.replaceFirst("")).replaceFirst("");
      String idstr = start.group(1);
      this.id = Integer.parseInt(idstr);

    } else {
      if (inputString.contains(" ||| ")) {
        /* Target-side given; used for parsing and forced decoding */
        String[] pieces = inputString.split("\\s?\\|{3}\\s?");
        source = pieces[0];
        target = pieces[1];
        if (target.equals(""))
          target = null;
        if (pieces.length > 2) {
          references = new String[pieces.length - 2];
          System.arraycopy(pieces, 2, references, 0, pieces.length - 2);
        }
        this.id = id;

      } else {
        /* Regular ol' input sentence */
        source = inputString;
        this.id = id;
      }
    }
    
    // Only trim strings
    if (! (joshuaConfiguration.lattice_decoding && source.startsWith("(((")))
      adjustForLength(joshuaConfiguration.maxlen);
  }

  /**
   * Indicates whether the underlying lattice is a linear chain, i.e., a sentence.
   * 
   * @return true if this is a linear chain, false otherwise
   */
  public boolean isLinearChain() {
    return ! this.getLattice().hasMoreThanOnePath();
  }

  // Matches the opening and closing <seg> tags, e.g.,
  // <seg id="72">this is a test input sentence</seg>.
  protected static final Pattern SEG_START = Pattern
      .compile("^\\s*<seg\\s+id=\"?(\\d+)\"?[^>]*>\\s*");
  protected static final Pattern SEG_END = Pattern.compile("\\s*</seg\\s*>\\s*$");

  /**
   * Returns the length of the sentence. For lattices, the length is the shortest path through the
   * lattice. The length includes the &lt;s&gt; and &lt;/s&gt; sentence markers. 
   * 
   * @return number of input tokens + 2 (for start and end of sentence markers)
   */
  public int length() {
    return this.getLattice().getShortestDistance();
  }

  /**
   * Returns the annotations for a specific word (specified by an index) in the 
   * sentence
   * 
   * @param index The location of the word in the sentence
   * @param key The annotation identity
   * @return The annotations associated with this word
   */
  public String getAnnotation(int index, String key) {
    return getTokens().get(index).getAnnotation(key);
  }

  /**
   * This function computes the intersection of \sigma^+ (where \sigma is the terminal vocabulary)
   * with all character-level segmentations of each OOV in the input sentence.
   * 
   * The idea is to break apart noun compounds in languages like German (such as the word "golfloch"
   * = "golf" (golf) + "loch" (hole)), allowing them to be translated.
   * 
   * @param grammars a list of grammars to consult to find in- and out-of-vocabulary items
   */
  public void segmentOOVs(Grammar[] grammars) {
    Lattice<Token> oldLattice = this.getLattice();

    /* Build a list of terminals across all grammars */
    HashSet<Integer> vocabulary = new HashSet<>();
    for (Grammar grammar : grammars) {
      Iterator<Integer> iterator = grammar.getTrieRoot().getTerminalExtensionIterator();
      while (iterator.hasNext())
        vocabulary.add(iterator.next());
    }

    List<Node<Token>> oldNodes = oldLattice.getNodes();

    /* Find all the subwords that appear in the vocabulary, and create the lattice */
    for (int nodeid = oldNodes.size() - 3; nodeid >= 1; nodeid -= 1) {
      if (oldNodes.get(nodeid).getOutgoingArcs().size() == 1) {
        Arc<Token> arc = oldNodes.get(nodeid).getOutgoingArcs().get(0);
        String word = Vocabulary.word(arc.getLabel().getWord());
        if (!vocabulary.contains(arc.getLabel())) {
          // System.err.println(String.format("REPL: '%s'", word));
          List<Arc<Token>> savedArcs = oldNodes.get(nodeid).getOutgoingArcs();

          char[] chars = word.toCharArray();
          ChartSpan<Boolean> wordChart = new ChartSpan<>(chars.length + 1, false);
          ArrayList<Node<Token>> nodes = new ArrayList<>(chars.length + 1);
          nodes.add(oldNodes.get(nodeid));
          for (int i = 1; i < chars.length; i++)
            nodes.add(new Node<>(i));
          nodes.add(oldNodes.get(nodeid + 1));
          for (int width = 1; width <= chars.length; width++) {
            for (int i = 0; i <= chars.length - width; i++) {
              int j = i + width;
              if (width != chars.length) {
                Token token = new Token(word.substring(i, j), config);
                if (vocabulary.contains(id)) {
                  nodes.get(i).addArc(nodes.get(j), 0.0f, token);
                  wordChart.set(i, j, true);
                  //                    System.err.println(String.format("  FOUND '%s' at (%d,%d)", word.substring(i, j),
                  //                        i, j));
                }
              }

              for (int k = i + 1; k < j; k++) {
                if (wordChart.get(i, k) && wordChart.get(k, j)) {
                  wordChart.set(i, j, true);
                  //                    System.err.println(String.format("    PATH FROM %d-%d-%d", i, k, j));
                }
              }
            }
          }

          /* If there's a path from beginning to end */
          if (wordChart.get(0, chars.length)) {
            // Remove nodes not part of a complete path
            HashSet<Node<Token>> deletedNodes = new HashSet<>();
            for (int k = 1; k < nodes.size() - 1; k++)
              if (!(wordChart.get(0, k) && wordChart.get(k, chars.length)))
                nodes.set(k, null);

            int delIndex = 1;
            while (delIndex < nodes.size())
              if (nodes.get(delIndex) == null) {
                deletedNodes.add(nodes.get(delIndex));
                nodes.remove(delIndex);
              } else
                delIndex++;

            for (Node<Token> node : nodes) {
              int arcno = 0;
              while (arcno != node.getOutgoingArcs().size()) {
                Arc<Token> delArc = node.getOutgoingArcs().get(arcno);
                if (deletedNodes.contains(delArc.getHead()))
                  node.getOutgoingArcs().remove(arcno);
                else {
                  arcno++;
                  //                    System.err.println("           ARC: " + Vocabulary.word(delArc.getLabel()));
                }
              }
            }

            // Insert into the main lattice
            this.getLattice().insert(nodeid, nodeid + 1, nodes);
          } else {
            nodes.get(0).setOutgoingArcs(savedArcs);
          }
        }
      }
    }
  }

  /**
   * If the input sentence is too long (not counting the &lt;s&gt; and &lt;/s&gt; tokens), it is truncated to
   * the maximum length, specified with the "maxlen" parameter.
   * 
   * Note that this code assumes the underlying representation is a sentence, and not a lattice. Its
   * behavior is undefined for lattices.
   * 
   * @param length int representing the length to truncate the sentence to
   */
  protected void adjustForLength(int length) {
    int size = this.getLattice().size() - 2; // subtract off the start- and end-of-sentence tokens

    if (size > length) {
      LOG.warn("sentence {} too long {}, truncating to length {}", id(), size, length);

      // Replace the input sentence (and target) -- use the raw string, not source()
      String[] tokens = source.split("\\s+");
      source = tokens[0];
      for (int i = 1; i < length; i++)
        source += " " + tokens[i];
      sourceLattice = null;
      if (target != null) {
        target = "";
      }
    }
  }

  public boolean isEmpty() {
    return source.matches("^\\s*$");
  }

  public int id() {
    return id;
  }

  /**
   * Returns the raw source-side input string.
   * @return the raw source-side input string
   */
  public String rawSource() {
    return source;
  }
  
  /**
   * Returns the source-side string with annotations --- if any --- stripped off.
   * 
   * @return  the source-side string with annotations --- if any --- stripped off
   */
  public String source() {
    StringBuilder str = new StringBuilder();
    int[] ids = getWordIDs();
    for (int i = 1; i < ids.length - 1; i++) {
      str.append(Vocabulary.word(ids[i])).append(" ");
    }
    return str.toString().trim();
  }

  /**
   * Returns a sentence with the start and stop symbols added to the 
   * beginning and the end of the sentence respectively
   * 
   * @return String The input sentence with start and stop symbols
   */
  public String fullSource() {
    if (fullSource == null) {
      fullSource = addSentenceMarkers(source());
    }
    return fullSource;  
  }

  /**
   * If a target side was supplied with the sentence, this will be non-null. This is used when doing
   * synchronous parsing or constrained decoding. The input format is:
   * 
   * Bill quiere ir a casa ||| Bill wants to go home
   * 
   * If the parameter parse=true is set, parsing will be triggered, otherwise constrained decoding.
   * 
   * @return target side of sentence translation
   */
  public String target() {
    return target;
  }

  public String fullTarget() {
    if (fullTarget == null) {
      fullTarget = addSentenceMarkers(target());
    }
    return fullTarget; 
  }

  public String source(int i, int j) {
    StringTokenizer st = new StringTokenizer(fullSource());
    int index = 0;
    StringBuilder substring = new StringBuilder();
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (index >= j)
        break;
      if (index >= i)
        substring.append(token).append(" ");
      index++;
    }
    return substring.toString().trim();
  }

  public String[] references() {
    return references;
  }

  /**
   * Returns the sequence of tokens comprising the sentence. This assumes you've done the checking
   * to makes sure the input string (the source side) isn't a PLF waiting to be parsed.
   * 
   * @return a {@link java.util.List} of {@link org.apache.joshua.decoder.segment_file.Token}'s comprising the sentence
   */
  public List<Token> getTokens() {
    assert isLinearChain();
    List<Token> tokens = new ArrayList<>();
    for (Node<Token> node: getLattice().getNodes())
      if (node != null && node.getOutgoingArcs().size() > 0) 
        tokens.add(node.getOutgoingArcs().get(0).getLabel());
    return tokens;
  }
  
  /**
   * Returns the sequence of word IDs comprising the input sentence. Assumes this is not a general
   * lattice, but a linear chain.
   * @return an int[] comprising all word ID's
   */
  public int[] getWordIDs() {
    List<Token> tokens = getTokens();
    int[] ids = new int[tokens.size()];
    for (int i = 0; i < tokens.size(); i++)
      ids[i] = tokens.get(i).getWord();
    return ids;
  }
  
  /**
   * Returns the sequence of word ids comprising the sentence. Assumes this is a sentence and
   * not a lattice.
   *  
   * @return the sequence of word ids comprising the sentence
   */
  public Lattice<String> stringLattice() {
    assert isLinearChain();
    return Lattice.createStringLatticeFromString(source(), config);
  }

  public List<ConstraintSpan> constraints() {
    return constraints;
  }

  public Lattice<Token> getLattice() {
    if (this.sourceLattice == null) {
      if (config.lattice_decoding && rawSource().startsWith("(((")) {
        if (config.search_algorithm.equals("stack")) {
          throw new RuntimeException("* FATAL: lattice decoding currently not supported for stack-based search algorithm.");
        }
        this.sourceLattice = Lattice.createTokenLatticeFromPLF(rawSource(), config);
      } else
        this.sourceLattice = Lattice.createTokenLatticeFromString(String.format("%s %s %s", Vocabulary.START_SYM,
            rawSource(), Vocabulary.STOP_SYM), config);
    }
    return this.sourceLattice;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(source());
    if (target() != null) {
      sb.append(" ||| ").append(target());
    }
    return sb.toString();
  }

  public boolean hasPath(int begin, int end) {
    return getLattice().distance(begin, end) != -1;
  }

  public Node<Token> getNode(int i) {
    return getLattice().getNode(i);
  }

  public LanguageModelStateManager getStateManager() {
    return stateManager;
  }
}

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
package org.apache.joshua.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSetFilter {

  private static final Logger LOG = LoggerFactory.getLogger(TestSetFilter.class);

  private Filter filter = null;

  // for caching of accepted rules
  private String lastSourceSide;
  private boolean acceptedLastSourceSide;

  public int cached = 0;
  public int RULE_LENGTH = 12;
  public boolean verbose = false;
  public boolean parallel = false;

  private static final String DELIMITER = "|||";
  private static final String DELIMITER_REGEX = " \\|\\|\\| ";
  public static final String DELIM = String.format(" %s ", DELIMITER);
  public static final Pattern P_DELIM = Pattern.compile(DELIMITER_REGEX);
  private final String NT_REGEX = "\\[[^\\]]+?\\]";

  public TestSetFilter() {
    acceptedLastSourceSide = false;
    lastSourceSide = null;
  }

  public String getFilterName() {
    if (filter != null)
      if (filter instanceof FastFilter)
        return "fast";
      else if (filter instanceof LooseFilter)
        return "loose";
      else
        return "exact";
    return "null";
  }

  public void setVerbose(boolean value) {
    verbose = value;
  }

  public void setParallel(boolean value) {
    parallel = value;
  }

  public void setFilter(String type) {
    switch (type) {
    case "fast":
      filter = new FastFilter();
      break;
    case "exact":
      filter = new ExactFilter();
      break;
    case "loose":
      filter = new LooseFilter();
      break;
    default:
      throw new RuntimeException(String.format("Invalid filter type '%s'", type));
    }
  }

  public void setRuleLength(int value) {
    RULE_LENGTH = value;
  }

  private void loadTestSentences(String filename) throws IOException {
    int count = 0;

    try {
      for (String line: new LineReader(filename)) {
        filter.addSentence(line);
        count++;
      }
    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage(), e);
    }

    if (verbose)
      System.err.println(String.format("Added %d sentences.\n", count));
  }

  /**
   * Top-level filter, responsible for calling the fast or exact version. Takes the source side
   * of a rule and determines whether there is any sentence in the test set that can match it.
   * @param sourceSide an input source sentence
   * @return true if is any sentence in the test set can match the source input
   */
  public boolean inTestSet(String sourceSide) {
    if (!sourceSide.equals(lastSourceSide)) {
      lastSourceSide = sourceSide;
      acceptedLastSourceSide = filter.permits(sourceSide);
    } else {
      cached++;
    }

    return acceptedLastSourceSide;
  }

  /**
   * Determines whether a rule is an abstract rule. An abstract rule is one that has no terminals on
   * its source side.
   *
   * If the rule is abstract, the rule's arity is returned. Otherwise, 0 is returned.
   */
  private boolean isAbstract(String source) {
    int nonterminalCount = 0;
    for (String t : source.split("\\s+")) {
      if (!t.matches(NT_REGEX))
        return false;
      nonterminalCount++;
    }
    return nonterminalCount != 0;
  }

  private interface Filter {
    /* Tell the filter about a sentence in the test set being filtered to */
    void addSentence(String sentence);

    /* Returns true if the filter permits the specified source side */
    boolean permits(String sourceSide);
  }

  private class FastFilter implements Filter {
    private Set<String> ngrams = null;

    public FastFilter() {
      ngrams = new HashSet<>();
    }

    @Override
    public boolean permits(String source) {
      for (String chunk : source.split(NT_REGEX)) {
        chunk = chunk.trim();
        /* Important: you need to make sure the string isn't empty. */
        if (!chunk.equals("") && !ngrams.contains(chunk))
          return false;
      }
      return true;
    }

    @Override
    public void addSentence(String sentence) {
      String[] tokens = sentence.trim().split("\\s+");
      int maxOrder = RULE_LENGTH < tokens.length ? RULE_LENGTH : tokens.length;
      for (int order = 1; order <= maxOrder; order++) {
        for (int start = 0; start < tokens.length - order + 1; start++)
          ngrams.add(createNGram(tokens, start, order));
      }
    }

    private String createNGram(String[] tokens, int start, int order) {
      if (order < 1 || start + order > tokens.length) {
        return "";
      }
      String result = tokens[start];
      for (int i = 1; i < order; i++)
        result += " " + tokens[start + i];
      return result;
    }
  }

  private class LooseFilter implements Filter {
    List<String> testSentences = null;

    public LooseFilter() {
      testSentences = new ArrayList<>();
    }

    @Override
    public void addSentence(String source) {
      testSentences.add(source);
    }

    @Override
    public boolean permits(String source) {
      Pattern pattern = getPattern(source);
      for (String testSentence : testSentences) {
        if (pattern.matcher(testSentence).find()) {
          return true;
        }
      }
      return isAbstract(source);
    }

    protected Pattern getPattern(String source) {
      String pattern = source;
      pattern = pattern.replaceAll(String.format("\\s*%s\\s*", NT_REGEX), ".+");
      pattern = pattern.replaceAll("\\s+", ".*");
//      System.err.println(String.format("PATTERN(%s) = %s", source, pattern));
      return Pattern.compile(pattern);
    }
  }

  /**
   * This class is the same as LooseFilter except with a tighter regex for matching rules.
   */
  private class ExactFilter implements Filter {
    private FastFilter fastFilter = null;
    private final Map<String, Set<Integer>> sentencesByWord;
    List<String> testSentences = null;

    public ExactFilter() {
      fastFilter = new FastFilter();
      sentencesByWord = new HashMap<>();
      testSentences = new ArrayList<>();
    }

    @Override
    public void addSentence(String source) {
      fastFilter.addSentence(source);
      addSentenceToWordHash(source, testSentences.size());
      testSentences.add(source);
    }

    /**
     * Always permit abstract rules. Otherwise, query the fast filter, and if that passes, apply
     *
     */
    @Override
    public boolean permits(String sourceSide) {
      if (isAbstract(sourceSide))
        return true;

      if (fastFilter.permits(sourceSide)) {
        Pattern pattern = getPattern(sourceSide);
        for (int i : getSentencesForRule(sourceSide)) {
          if (pattern.matcher(testSentences.get(i)).find()) {
            return true;
          }
        }
      }
      return false;
    }

    protected Pattern getPattern(String source) {
      String pattern = Pattern.quote(source);
      pattern = pattern.replaceAll(NT_REGEX, "\\\\E.+\\\\Q");
      pattern = pattern.replaceAll("\\\\Q\\\\E", "");
      pattern = "(?:^|\\s)" + pattern + "(?:$|\\s)";
      return Pattern.compile(pattern);
    }

    /*
     * Map words to all the sentences they appear in.
     */
    private void addSentenceToWordHash(String sentence, int index) {
      String[] tokens = sentence.split("\\s+");
      for (String t : tokens) {
        if (! sentencesByWord.containsKey(t))
          sentencesByWord.put(t, new HashSet<>());
        sentencesByWord.get(t).add(index);
      }
    }

    private Set<Integer> getSentencesForRule(String source) {
      Set<Integer> sentences = null;
      for (String token : source.split("\\s+")) {
        if (!token.matches(NT_REGEX)) {
          if (sentencesByWord.containsKey(token)) {
            if (sentences == null)
              sentences = new HashSet<>(sentencesByWord.get(token));
            else
              sentences.retainAll(sentencesByWord.get(token));
          }
        }
      }

      return sentences;
    }
  }

  public static void main(String[] argv) throws IOException {
    // do some setup
    if (argv.length < 1) {
      System.err.println("usage: TestSetFilter [-v|-p|-f|-e|-l|-n N|-g grammar] test_set1 [test_set2 ...]");
      System.err.println("    -g    grammar file (can also be on STDIN)");
      System.err.println("    -v    verbose output");
      System.err.println("    -p    parallel compatibility");
      System.err.println("    -f    fast mode (default)");
      System.err.println("    -e    exact mode (slower)");
      System.err.println("    -l    loose mode");
      System.err.println("    -n    max n-gram to compare to (default 12)");
      return;
    }

    String grammarFile = null;

    TestSetFilter filter = new TestSetFilter();

    for (int i = 0; i < argv.length; i++) {
      switch (argv[i]) {
      case "-v":
        filter.setVerbose(true);
        continue;
      case "-p":
        filter.setParallel(true);
        continue;
      case "-g":
        grammarFile = argv[++i];
        continue;
      case "-f":
        filter.setFilter("fast");
        continue;
      case "-e":
        filter.setFilter("exact");
        continue;
      case "-l":
        filter.setFilter("loose");
        continue;
      case "-n":
        filter.setRuleLength(Integer.parseInt(argv[i + 1]));
        i++;
        continue;
      }

      filter.loadTestSentences(argv[i]);
    }

    int rulesIn = 0;
    int rulesOut = 0;
    if (filter.verbose) {
      System.err.println(String.format("Filtering rules with the %s filter...", filter.getFilterName()));
//      System.err.println("Using at max " + filter.RULE_LENGTH + " n-grams...");
    }
    try(LineReader reader = (grammarFile != null)
        ? new LineReader(grammarFile, filter.verbose)
        : new LineReader(System.in)) {
      for (String rule: reader) {
        rulesIn++;

        String[] parts = P_DELIM.split(rule);
        if (parts.length >= 4) {
          // the source is the second field for thrax grammars, first field for phrasal ones
          String source = rule.startsWith("[") ? parts[1].trim() : parts[0].trim();
          if (filter.inTestSet(source)) {
            System.out.println(rule);
            if (filter.parallel)
              System.out.flush();
            rulesOut++;
          } else if (filter.parallel) {
            System.out.println("");
            System.out.flush();
          }
        }
      }
      if (filter.verbose) {
        System.err.println("[INFO] Total rules read: " + rulesIn);
        System.err.println("[INFO] Rules kept: " + rulesOut);
        System.err.println("[INFO] Rules dropped: " + (rulesIn - rulesOut));
        System.err.println("[INFO] cached queries: " + filter.cached);
      }

    }
  }
}

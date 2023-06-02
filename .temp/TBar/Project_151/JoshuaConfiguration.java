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
package org.apache.joshua.decoder;

import static org.apache.joshua.util.FormatUtils.cleanNonTerminal;
import static org.apache.joshua.util.FormatUtils.ensureNonTerminalBrackets;

import java.io.File;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.joshua.decoder.ff.StatefulFF;
import org.apache.joshua.decoder.ff.fragmentlm.Tree;
import org.apache.joshua.util.FormatUtils;
import org.apache.joshua.util.Regex;
import org.apache.joshua.util.io.LineReader;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration file for Joshua decoder.
 *
 * When adding new features to Joshua, any new configurable parameters should be added to this
 * class.
 *
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author Matt Post post@cs.jhu.edu
 */
public class JoshuaConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(JoshuaConfiguration.class);

  // This records the directory from which relative file references are normalized.
  // If a config file is loaded, that will override the current directory.
  public String modelRootPath = Paths.get(".").toAbsolutePath().normalize().toString();

  // whether to construct a StructuredTranslation object for each request instead of
  // printing to stdout. Used when the Decoder is used from Java directly.
  public Boolean use_structured_output = false;

  // If set to true, Joshua will lowercase the input, creating an annotation that marks the
  // original case
  public boolean lowercase = false;

  // If set to true, Joshua will recapitalize the output by projecting the case from aligned
  // source-side words
  public boolean project_case = false;

  // List of grammar files to read
  public ArrayList<String> tms = new ArrayList<>();

  // A rule cache for commonly used tries to avoid excess object allocations
  // Testing shows there's up to ~95% hit rate when cache size is 5000 Trie nodes.
  public Integer cachedRuleSize = 5000;

  /*
   * The file to read the weights from (part of the sparse features implementation). Weights can
   * also just be listed in the main config file.
   */
  public String weights_file = "";
  // Default symbols. The symbol here should be enclosed in square brackets.
  public String default_non_terminal = FormatUtils.ensureNonTerminalBrackets("X");
  public String goal_symbol = FormatUtils.ensureNonTerminalBrackets("GOAL");

  /*
   * A list of OOV symbols in the form
   *
   * [X1] weight [X2] weight [X3] weight ...
   *
   * where the [X] symbols are nonterminals and the weights are weights. For each OOV word w in the
   * input sentence, Joshua will create rules of the form
   *
   * X1 -> w (weight)
   *
   * If this is empty, an unweighted default_non_terminal is used.
   */
  public class OOVItem implements Comparable<OOVItem> {
    public final String label;

    public final float weight;

    OOVItem(String l, float w) {
      label = l;
      weight = w;
    }
    @Override
    public int compareTo(OOVItem other) {
      if (weight > other.weight)
        return -1;
      else if (weight < other.weight)
        return 1;
      return 0;
    }
  }

  public ArrayList<OOVItem> oovList = null;

  /*
   * Whether to segment OOVs into a lattice
   */
  public boolean segment_oovs = false;

  /*
   * Enable lattice decoding.
   */
  public boolean lattice_decoding = false;

  /*
   * If false, sorting of the complete grammar is done at load time. If true, grammar tries are not
   * sorted till they are first accessed. Amortized sorting means you get your first translation
   * much, much quicker (good for debugging), but that per-sentence decoding is a bit slower.
   */
  public boolean amortized_sorting = true;
  // syntax-constrained decoding
  public boolean constrain_parse = false;

  public boolean use_pos_labels = false;

  // oov-specific
  public boolean true_oovs_only = false;

  /* Dynamic sentence-level filtering. */
  public boolean filter_grammar = false;

  /* The cube pruning pop limit. Set to 0 for exhaustive pruning. */
  public int pop_limit = 100;

  /* Maximum sentence length. Sentences longer than this are truncated. */
  public int maxlen = 200;

  /*
   * N-best configuration.
   */
  // Make sure output strings in the n-best list are unique.
  public boolean use_unique_nbest = true;

  /* Include the phrasal alignments in the output (not word-level alignmetns at the moment). */
  public boolean include_align_index = false;

  /* The number of hypotheses to output by default. */
  public int topN = 1;

  /**
   * This string describes the format of each line of output from the decoder (i.e., the
   * translations). The string can include arbitrary text and also variables. The following
   * variables are available:
   *
   * <pre>
   * - %i the 0-indexed sentence number
   * - %e the source string %s the translated sentence
   * - %S the translated sentence with some basic capitalization and denormalization
   * - %t the synchronous derivation
   * - %f the list of feature values (as name=value pairs)
   * - %c the model cost
   * - %w the weight vector
   * - %a the alignments between source and target words (currently unimplemented)
   * - %d a verbose, many-line version of the derivation
   * </pre>
   */
  public String outputFormat = "%i ||| %s ||| %f ||| %c";

  /* The number of decoding threads to use (-threads). */
  public int num_parallel_decoders = 1;

  /*
   * When true, _OOV is appended to all words that are passed through (useful for something like
   * transliteration on the target side
   */
  public boolean mark_oovs = false;

  /* Enables synchronous parsing. */
  public boolean parse = false; // perform synchronous parsing


  /* A list of the feature functions. */
  public ArrayList<String> features = new ArrayList<>();

  /* A list of weights found in the main config file (instead of in a separate weights file) */
  public ArrayList<String> weights = new ArrayList<>();

  /* Determines whether to expect JSON input or plain lines */
  public enum INPUT_TYPE { plain, json }

  public INPUT_TYPE input_type = INPUT_TYPE.plain;

  /* Type of server. Not sure we need to keep the regular TCP one around. */
  public enum SERVER_TYPE { none, TCP, HTTP }

  public SERVER_TYPE server_type = SERVER_TYPE.TCP;

  /* If set, Joshua will start a (multi-threaded, per "threads") TCP/IP server on this port. */
  public int server_port = 0;

  /*
   * Whether to do forest rescoring. If set to true, the references are expected on STDIN along with
   * the input sentences in the following format:
   * 
   * input sentence ||| ||| reference1 ||| reference2 ...
   * 
   * (The second field is reserved for the output sentence for alignment and forced decoding).
   */

  public boolean rescoreForest = false;
  public float rescoreForestWeight = 10.0f;

  /*
   * Location of fragment mapping file, which maps flattened SCFG rules to their internal
   * representation.
   */
  public String fragmentMapFile = null;

  /*
   * Whether to use soft syntactic constraint decoding /fuzzy matching, which allows that any
   * nonterminal may be substituted for any other nonterminal (except for OOV and GOAL)
   */
  public boolean fuzzy_matching = false;

  public static final String SOFT_SYNTACTIC_CONSTRAINT_DECODING_PROPERTY_NAME = "fuzzy_matching";

  /***
   * Phrase-based decoding parameters.
   */
  
  /* The search algorithm: currently either "cky" or "stack" */
  public String search_algorithm = "cky";

  /* The distortion limit */
  public int reordering_limit = 8;

  /* The number of target sides considered for each source side (after sorting by model weight) */
  public int num_translation_options = 20;

  /* If true, decode using a dot chart (standard CKY+); if false, use the much more efficient
   * version of Sennrich (SSST 2014)
   */
  public boolean use_dot_chart = true;

  /* Moses compatibility */
  public boolean moses = false;

  /* If true, just print out the weights found in the config file, and exit. */
  public boolean show_weights_and_quit = false;

  /* Read input from a file (Moses compatible flag) */
  public String input_file = null;

  /* Write n-best output to this file */
  public String n_best_file = null;

  /* Whether to look at source side for special annotations */
  public boolean source_annotations = false;

  /* Weights overridden from the command line */
  public String weight_overwrite = "";

  /* Timeout in seconds for threads */
  public long translation_thread_timeout = 30_000;

  /**
   * This method resets the state of JoshuaConfiguration back to the state after initialization.
   * This is useful when for example making different calls to the decoder within the same java
   * program, which otherwise leads to potential errors due to inconsistent state as a result of
   * loading the configuration multiple times without resetting etc.
   *
   * This leads to the insight that in fact it may be an even better idea to refactor the code and
   * make JoshuaConfiguration an object that is is created and passed as an argument, rather than a
   * shared static object. This is just a suggestion for the next step.
   *
   */
  public void reset() {
    LOG.info("Resetting the JoshuaConfiguration to its defaults ...");
    LOG.info("\n\tResetting the StatefullFF global state index ...");
    LOG.info("\n\t...done");
    StatefulFF.resetGlobalStateIndex();
    tms = new ArrayList<>();
    weights_file = "";
    default_non_terminal = "[X]";
    oovList = new ArrayList<>();
    oovList.add(new OOVItem(default_non_terminal, 1.0f));
    goal_symbol = "[GOAL]";
    amortized_sorting = true;
    constrain_parse = false;
    use_pos_labels = false;
    true_oovs_only = false;
    filter_grammar = false;
    pop_limit = 100;
    maxlen = 200;
    use_unique_nbest = false;
    include_align_index = false;
    topN = 1;
    outputFormat = "%i ||| %s ||| %f ||| %c";
    num_parallel_decoders = 1;
    mark_oovs = false;
    // oracleFile = null;
    parse = false; // perform synchronous parsing
    features = new ArrayList<>();
    weights = new ArrayList<>();
    server_port = 0;

    reordering_limit = 8;
    num_translation_options = 20;
    LOG.info("...done");
  }

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * To process command-line options, we write them to a file that looks like the config file, and
   * then call readConfigFile() on it. It would be more general to define a class that sits on a
   * stream and knows how to chop it up, but this was quicker to implement.
   * 
   * @param options string array of command line options
   */
  public void processCommandLineOptions(String[] options) {
    try {
      File tmpFile = File.createTempFile("options", null, null);
      PrintWriter out = new PrintWriter(new FileWriter(tmpFile));

      for (int i = 0; i < options.length; i++) {
        String key = options[i].substring(1);
        if (i + 1 == options.length || options[i + 1].startsWith("-")) {
          // if this is the last item, or if the next item
          // is another flag, then this is a boolean flag
          out.println(key + " = true");

        } else {
          out.print(key + " =");
          while (i + 1 < options.length && ! options[i + 1].startsWith("-")) {
            out.print(String.format(" %s", options[i + 1]));
            i++;
          }
          out.println();
        }
      }
      out.close();
      
//      LOG.info("Parameters overridden from the command line:");
      this.readConfigFile(tmpFile.getCanonicalPath());

      tmpFile.delete();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This records the path of the config file passed as a command-line argument
   * (-c or -config). It is used to produce absolute path names when relative ones are found
   * in the config file.
   * 
   * @param path the root path for the joshua models
   */
  public void setConfigFilePath(String path) {
    this.modelRootPath = path;
  }

  /**
   * Returns the absolute file path of the argument. Files that are already absolute path names
   * are returned unmodified, but relative file names have the `modelRootPath` prepended (if
   * it was set).
   * 
   * @param filename the path of the file, absolute or relative
   * @return the absolute file path of a file
   */
  public String getFilePath(String filename) {
    if (filename.startsWith("/"))
      return filename;
    else if (this.modelRootPath != null)
      return this.modelRootPath + "/" + filename;
    else {
      LOG.warn("File '{}' is a relative path but no config file path was set", filename);
      return filename;
    }
  }

  public void readConfigFile(String configFile) throws IOException {

    LineReader configReader = new LineReader(configFile, false);
    try {
      for (String line : configReader) {
        line = line.trim(); // .toLowerCase();

        if (Regex.commentOrEmptyLine.matches(line))
          continue;

        /*
         * There are two kinds of substantive (non-comment, non-blank) lines: parameters and feature
         * values. Parameters match the pattern "key = value"; all other substantive lines are
         * interpreted as features.
         */

        if (line.contains("=")) { // parameters; (not feature function)
          String[] fds = Regex.equalsWithSpaces.split(line, 2);
          if (fds.length < 2) {
            LOG.warn("skipping config file line '{}'", line);
            continue;
          }

          String parameter = normalize_key(fds[0]);

          if (parameter.equals(normalize_key("lm"))) {
            /* This is deprecated. This support old LM lines of the form
             * 
             *   lm = berkeleylm 5 false false 100 lm.gz
             * 
             * LMs are now loaded as general feature functions, so we transform that to either
             * 
             *   LanguageModel -lm_order 5 -lm_type berkeleylm -lm_file lm.gz
             * 
             * If the line were state minimizing:
             * 
             *   lm = kenlm 5 true false 100 lm.gz
             *              
             * StateMinimizingLanguageModel -lm_order 5 -lm_file lm.gz
             */

            String[] tokens = fds[1].split("\\s+");
            if (tokens[2].equals("true"))
              features.add(String.format("StateMinimizingLanguageModel -lm_type kenlm -lm_order %s -lm_file %s",
                  tokens[1], tokens[5]));
            else
              features.add(String.format("LanguageModel -lm_type %s -lm_order %s -lm_file %s",
                  tokens[0], tokens[1], tokens[5]));

          } else if (parameter.equals(normalize_key("tm"))) {
            /* If found, convert old format:
             *   tm = TYPE OWNER MAXSPAN PATH
             * to new format
             *   tm = TYPE -owner OWNER -maxspan MAXSPAN -path PATH    
             */
            String tmLine = fds[1];

            String[] tokens = fds[1].split("\\s+");
            if (! tokens[1].startsWith("-")) { // old format
              tmLine = String.format("%s -owner %s -maxspan %s -path %s", tokens[0], tokens[1], tokens[2], tokens[3]);
              LOG.warn("Converting deprecated TM line from '{}' -> '{}'", fds[1], tmLine);
            }
            tms.add(tmLine);

          } else if (parameter.equals("v")) {

            // This is already handled in ArgsParser, skip it here, easier than removing it there

          } else if (parameter.equals(normalize_key("parse"))) {
            parse = Boolean.parseBoolean(fds[1]);
            LOG.debug("parse: {}", parse);

          } else if (parameter.equals(normalize_key("oov-list"))) {
            if (new File(fds[1]).exists()) {
              oovList = new ArrayList<>();
              try {
                File file = new File(fds[1]);
                BufferedReader br = new BufferedReader(new FileReader(file));
                try {
                  String str = br.readLine();
                  while (str != null) {
                    String[] tokens = str.trim().split("\\s+");

                    oovList.add(new OOVItem(FormatUtils.ensureNonTerminalBrackets(tokens[0]),
                            (float) Math.log(Float.parseFloat(tokens[1]))));

                    str = br.readLine();
                  }
                  br.close();
                } catch(IOException e){
                  System.out.println(e);
                }
              } catch(IOException e){
                System.out.println(e);
              }
              Collections.sort(oovList);

            } else {
              String[] tokens = fds[1].trim().split("\\s+");
              if (tokens.length % 2 != 0) {
                throw new RuntimeException(String.format("* FATAL: invalid format for '%s'", fds[0]));
              }
              oovList = new ArrayList<>();

              for (int i = 0; i < tokens.length; i += 2)
                oovList.add(new OOVItem(FormatUtils.ensureNonTerminalBrackets(tokens[i]),
                    (float) Math.log(Float.parseFloat(tokens[i + 1]))));

              Collections.sort(oovList);
            }

          } else if (parameter.equals(normalize_key("lattice-decoding"))) {
            lattice_decoding = true;

          } else if (parameter.equals(normalize_key("segment-oovs"))) {
            segment_oovs = true;
            lattice_decoding = true;

          } else if (parameter.equals(normalize_key("default-non-terminal"))) {
            default_non_terminal = ensureNonTerminalBrackets(cleanNonTerminal(fds[1].trim()));
            LOG.debug("default_non_terminal: {}", default_non_terminal);

          } else if (parameter.equals(normalize_key("goal-symbol"))) {
            goal_symbol = ensureNonTerminalBrackets(cleanNonTerminal(fds[1].trim()));
            LOG.debug("goalSymbol: {}", goal_symbol);

          } else if (parameter.equals(normalize_key("weights-file"))) {
            weights_file = fds[1];

          } else if (parameter.equals(normalize_key("constrain_parse"))) {
            constrain_parse = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("true_oovs_only"))) {
            true_oovs_only = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("filter-grammar"))) {
            filter_grammar = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("amortize"))) {
            amortized_sorting = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("use_pos_labels"))) {
            use_pos_labels = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("use_unique_nbest"))) {
            use_unique_nbest = Boolean.valueOf(fds[1]);
            LOG.debug("use_unique_nbest: {}", use_unique_nbest);

          } else if (parameter.equals(normalize_key("output-format"))) {
            outputFormat = fds[1];
            LOG.debug("output-format: {}", outputFormat);

          } else if (parameter.equals(normalize_key("include_align_index"))) {
            include_align_index = Boolean.valueOf(fds[1]);
            LOG.debug("include_align_index: {}", include_align_index);

          } else if (parameter.equals(normalize_key("top_n"))) {
            topN = Integer.parseInt(fds[1]);
            LOG.debug("topN: {}", topN);

          } else if (parameter.equals(normalize_key("num_parallel_decoders"))
              || parameter.equals(normalize_key("threads"))) {
            num_parallel_decoders = Integer.parseInt(fds[1]);
            if (num_parallel_decoders <= 0) {
              throw new IllegalArgumentException(
                  "Must specify a positive number for num_parallel_decoders");
            }
            LOG.debug("num_parallel_decoders: {}", num_parallel_decoders);

          } else if (parameter.equals(normalize_key("mark_oovs"))) {
            mark_oovs = Boolean.valueOf(fds[1]);
            LOG.debug("mark_oovs: {}", mark_oovs);

          } else if (parameter.equals(normalize_key("pop-limit"))) {
            pop_limit = Integer.parseInt(fds[1]);
            LOG.info("pop-limit: {}", pop_limit);

          } else if (parameter.equals(normalize_key("input-type"))) {
            switch (fds[1]) {
            case "json":
              input_type = INPUT_TYPE.json;
              break;
            case "plain":
              input_type = INPUT_TYPE.plain;
              break;
            default:
              throw new RuntimeException(
                  String.format("* FATAL: invalid server type '%s'", fds[1]));
            }
            LOG.info("    input-type: {}", input_type);

          } else if (parameter.equals(normalize_key("server-type"))) {
            if (fds[1].toLowerCase().equals("tcp"))
              server_type = SERVER_TYPE.TCP;
            else if (fds[1].toLowerCase().equals("http"))
              server_type = SERVER_TYPE.HTTP;

            LOG.info("    server-type: {}", server_type);

          } else if (parameter.equals(normalize_key("server-port"))) {
            server_port = Integer.parseInt(fds[1]);
            LOG.info("    server-port: {}", server_port);

          } else if (parameter.equals(normalize_key("rescore-forest"))) {
            rescoreForest = true;
            LOG.info("    rescore-forest: {}", rescoreForest);

          } else if (parameter.equals(normalize_key("rescore-forest-weight"))) {
            rescoreForestWeight = Float.parseFloat(fds[1]);
            LOG.info("    rescore-forest-weight: {}", rescoreForestWeight);

          } else if (parameter.equals(normalize_key("maxlen"))) {
            // reset the maximum length
            maxlen = Integer.parseInt(fds[1]);

          } else if (parameter.equals("c") || parameter.equals("config")) {
            // this was used to send in the config file, just ignore it

          } else if (parameter.equals(normalize_key("feature-function"))) {
            // add the feature to the list of features for later processing
            features.add(fds[1]);

          } else if (parameter.equals(normalize_key("maxlen"))) {
            // add the feature to the list of features for later processing
            maxlen = Integer.parseInt(fds[1]);

          } else if (parameter
              .equals(normalize_key(SOFT_SYNTACTIC_CONSTRAINT_DECODING_PROPERTY_NAME))) {
            fuzzy_matching = Boolean.parseBoolean(fds[1]);
            LOG.debug("fuzzy_matching: {}", fuzzy_matching);

          } else if (parameter.equals(normalize_key("fragment-map"))) {
            fragmentMapFile = fds[1];
            Tree.readMapping(fragmentMapFile);

            /** PHRASE-BASED PARAMETERS **/
          } else if (parameter.equals(normalize_key("search"))) {
            search_algorithm = fds[1];

            if (!search_algorithm.equals("cky") && !search_algorithm.equals("stack")) {
              throw new RuntimeException(
                  "-search must be one of 'stack' (for phrase-based decoding) " +
                      "or 'cky' (for hierarchical / syntactic decoding)");
            }

            if (search_algorithm.equals("cky") && include_align_index) {
              throw new RuntimeException(
                  "include_align_index is currently not supported with cky search");
            }

          } else if (parameter.equals(normalize_key("reordering-limit"))) {
            reordering_limit = Integer.parseInt(fds[1]);

          } else if (parameter.equals(normalize_key("num-translation-options"))) {
            num_translation_options = Integer.parseInt(fds[1]);

          } else if (parameter.equals(normalize_key("no-dot-chart"))) {
            use_dot_chart = false;

          } else if (parameter.equals(normalize_key("moses"))) {
            moses = true; // triggers some Moses-specific compatibility options

          } else if (parameter.equals(normalize_key("show-weights"))) {
            show_weights_and_quit = true;

          } else if (parameter.equals(normalize_key("n-best-list"))) {
            // for Moses compatibility
            String[] tokens = fds[1].split("\\s+");
            n_best_file = tokens[0];
            if (tokens.length > 1)
              topN = Integer.parseInt(tokens[1]);

          } else if (parameter.equals(normalize_key("input-file"))) {
            // for Moses compatibility
            input_file = fds[1];

          } else if (parameter.equals(normalize_key("weight-file"))) {
            // for Moses, ignore

          } else if (parameter.equals(normalize_key("weight-overwrite"))) {
            weight_overwrite = fds[1];

          } else if (parameter.equals(normalize_key("source-annotations"))) {
            // Check source sentence
            source_annotations = true;

          } else if (parameter.equals(normalize_key("cached-rules-size"))) {
            // Check source sentence
            cachedRuleSize = Integer.parseInt(fds[1]);
          } else if (parameter.equals(normalize_key("lowercase"))) {
            lowercase = true;

          } else if (parameter.equals(normalize_key("project-case"))) {
            project_case = true;

          } else {

            if (parameter.equals(normalize_key("use-sent-specific-tm"))
                || parameter.equals(normalize_key("add-combined-cost"))
                || parameter.equals(normalize_key("use-tree-nbest"))
                || parameter.equals(normalize_key("use-kenlm"))
                || parameter.equals(normalize_key("useCubePrune"))
                || parameter.equals(normalize_key("useBeamAndThresholdPrune"))
                || parameter.equals(normalize_key("regexp-grammar"))) {
              LOG.warn("ignoring deprecated parameter '{}'", fds[0]);

            } else {
              throw new RuntimeException("FATAL: unknown configuration parameter '" + fds[0] + "'");
            }
          }

          LOG.info("    {} = '{}'", normalize_key(fds[0]), fds[1]);

        } else {
          /*
           * Lines that don't have an equals sign and are not blank lines, empty lines, or comments,
           * are feature values, which can be present in this file
           */

          weights.add(line);
        }
      }
    } finally {
      configReader.close();
    }
  }

  /**
   * Checks for invalid variable configurations
   */
  public void sanityCheck() {
  }
  
  /**
   * Sets the verbosity level to v (0: OFF; 1: INFO; 2: DEBUG).
   * 
   * @param v the verbosity level (0, 1, or 2)
   */
  public void setVerbosity(int v) {
    Decoder.VERBOSE = v;
    switch (Decoder.VERBOSE) {
    case 0:
      LogManager.getRootLogger().setLevel(Level.OFF);
      break;
    case 1:
      LogManager.getRootLogger().setLevel(Level.INFO);
      break;
    case 2:
      LogManager.getRootLogger().setLevel(Level.DEBUG);
      break;
    }
  }

  /**
   * Normalizes parameter names by removing underscores and hyphens and lowercasing. This defines
   * equivalence classes on external use of parameter names, permitting arbitrary_under_scores and
   * camelCasing in paramter names without forcing the user to memorize them all. Here are some
   * examples of equivalent ways to refer to parameter names:
   * <pre>
   * {pop-limit, poplimit, PopLimit, popLimit, pop_lim_it} {lmfile, lm-file, LM-FILE, lm_file}
   * </pre>
   * 
   * @param text the string to be normalized
   * @return normalized key
   * 
   */
  public static String normalize_key(String text) {
    return text.replaceAll("[-_]", "").toLowerCase();
  }
}

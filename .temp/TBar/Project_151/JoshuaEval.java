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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.TreeSet;

import org.apache.joshua.metrics.EvaluationMetric;

public class JoshuaEval {
    final static DecimalFormat f4 = new DecimalFormat("###0.0000");

    // if true, evaluation is performed for each candidate translation as
    // well as on the entire candidate set
    static boolean verbose;

    // number of candidate translations
    static int numSentences;

    // number of reference translations per sentence
    static int refsPerSen;

    // 0: no normalization, 1: "NIST-style" tokenization, and also rejoin 'm, 're, *'s, 've, 'll, 'd,
    // and n't,
    // 2: apply 1 and also rejoin dashes between letters, 3: apply 1 and also drop non-ASCII
    // characters
    // 4: apply 1+2+3
    static private int textNormMethod;

    // refSentences[i][r] is the rth reference translation of the ith sentence
    static String[][] refSentences;

    // name of evaluation metric
    static String metricName;

    // options for the evaluation metric (e.g. for BLEU, maxGramLength and effLengthMethod)
    static String[] metricOptions;

    // the scorer
    static EvaluationMetric evalMetric;

    // if true, the reference set(s) is (are) evaluated
    static boolean evaluateRefs;

    // file names for input files. When refsPerSen > 1, refFileName can be
    // the name of a single file, or a file name prefix.
    static String refFileName;
    static String candFileName;

    // format of the candidate file: "plain" if one candidate per sentence, and "nbest" if a decoder
    // output
    static String candFileFormat;

    // if format is nbest, evaluate the r'th candidate of each sentence
    static int candRank;

    private static void evaluateCandsPlain(String inFileName) {
        String[] topCandidates = getTopCandidates(inFileName, "plain", 1, 1);
        evaluate(topCandidates, evalMetric);
        String[] refs = new String[refSentences.length];
        for (int i = 0; i < refSentences.length; i++) {
            refs[i] = refSentences[i][0];
        }
        double lengthRatio = lengthRatio(topCandidates, refs);
        System.out.printf("\nCandidate/Reference Length Ratio = %.4f\n", lengthRatio);
    }


    private static void evaluateCandsNbest(String inFileName, int testIndex) {
        String[] topCandidates = getTopCandidates(inFileName, "nbest", -1, testIndex);
        evaluate(topCandidates, evalMetric);
    }


    private static void evaluateRefSet(int r) {
        String [] topCandidates = getTopCandidates(refFileName, "plain", refsPerSen, r);
        evaluate(topCandidates, evalMetric);
    }


    private static void evaluate(String[] topCand_str, EvaluationMetric evalMetric) {
        int[] IA = new int[numSentences];
        for (int i = 0; i < numSentences; ++i) {
            IA[i] = i;
        }
        int[][] SS = evalMetric.suffStats(topCand_str, IA);

        int suffStatsCount = evalMetric.get_suffStatsCount();

        int[] totStats = new int[suffStatsCount];
        for (int s = 0; s < suffStatsCount; ++s) {
            totStats[s] = 0;
            for (int i = 0; i < numSentences; ++i) {
                totStats[s] += SS[i][s];
            }
        }
        evalMetric.printDetailedScore_fromStats(totStats, false);

        if (verbose) {
            println("");
            println("Printing detailed scores for individual sentences...");
            for (int i = 0; i < numSentences; ++i) {
                print("Sentence #" + i + ": ");
                int[] stats = new int[suffStatsCount];
                System.arraycopy(SS[i], 0, stats, 0, suffStatsCount);
                evalMetric.printDetailedScore_fromStats(stats, true);
                // already prints a \n
            }
        }

    } // void evaluate(...)

    /**
     * Reads Candidate Strings
     * @param inFileName File name
     * @param inFileFormat : choices : {'plain', 'nbest'}
     * @param candPerSen how many candidates are provided per sentence?
     *                   (if inFileFormat is nbest, then candPerSen is ignored, since it is variable)
     * @param testIndex which of the candidates (for each sentence) should be tested?
     *                  e.g. testIndex=1 means first candidate should be evaluated
     *                  testIndex=candPerSen means last candidate should be evaluated
     * @return Array of Candidate strings
     */
    private static String[] getTopCandidates(String inFileName, String inFileFormat, int candPerSen, int testIndex) {
        if (inFileFormat.equals("plain") && candPerSen < 1) {
            throw new RuntimeException("candPerSen must be positive for a file in plain format.");
        }

        if (inFileFormat.equals("plain") && (testIndex < 1 || testIndex > candPerSen)) {
            throw new RuntimeException("For the plain format, testIndex must be in [1,candPerSen]");
        }
        String[] topCand_str = new String[numSentences];
        // BUG: all of this needs to be replaced with the SegmentFileParser and related interfaces.
        try (InputStream inStream = new FileInputStream(new File(inFileName));
             BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "utf8"))) {

            // read the candidates
            String line, candidate_str;
            if (inFileFormat.equals("plain")) {

                for (int i = 0; i < numSentences; ++i) {

                    // skip candidates 1 through testIndex-1
                    for (int n = 1; n < testIndex; ++n) {
                        line = inFile.readLine();
                    }

                    // read testIndex'th candidate
                    candidate_str = inFile.readLine();
                    topCand_str[i] = normalize(candidate_str, textNormMethod);

                    for (int n = testIndex + 1; n <= candPerSen; ++n) {
                        // skip candidates testIndex+1 through candPerSen-1
                        // (this probably only applies when evaluating a combined reference file)
                        line = inFile.readLine();
                    }

                } // for (i)

            } else { // nbest format

                int i = 0;
                int n = 1;
                line = inFile.readLine();

                while (line != null && i < numSentences) {

                  /*
                   * line format:
                   *
                   * .* ||| words of candidate translation . ||| feat-1_val feat-2_val ...
                   * feat-numParams_val .*
                   */
                    while (n < candRank) {
                        line = inFile.readLine();
                        ++n;
                    }

                    // at the moment, line stores the candRank'th candidate (1-indexed) of the i'th sentence
                    // (0-indexed)
                    if (line == null) {
                        println("Not enough candidates in " + inFileName + " to extract the " + candRank
                                + "'th candidate for each sentence.");
                        throw new RuntimeException("(Failed to extract one for the " + i + "'th sentence (0-indexed).)");
                    }

                    int read_i = Integer.parseInt(line.substring(0, line.indexOf(" |||")).trim());
                    if (read_i == i) {
                        line = line.substring(line.indexOf("||| ") + 4); // get rid of initial text
                        candidate_str = line.substring(0, line.indexOf(" |||"));
                        topCand_str[i] = normalize(candidate_str, textNormMethod);
                        if (i < numSentences - 1) {
                            while (read_i == i) {
                                line = inFile.readLine();
                                read_i = Integer.parseInt(line.substring(0, line.indexOf(" |||")).trim());
                            }
                        }
                        n = 1;
                        i += 1;
                    } else {
                        String msg = "Not enough candidates in " + inFileName + " to extract the " + candRank
                                + "'th candidate for each sentence. (Failed to extract one for the "
                                + i + "'th sentence (0-indexed).)";
                        throw new RuntimeException(msg);
                    }

                } // while (line != null)

                if (i != numSentences) {
                    throw new RuntimeException("Not enough candidates were found (i = " + i + "; was expecting " + numSentences
                            + ")");
                }

            } // nbest format
            inFile.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return topCand_str;
    }


    private static void printUsage(int argsLen) {
        println("Oops, you provided " + argsLen + " args!");
        println("");
        println("Usage:");
        println(" JoshuaEval [-cand candFile] [-format candFileformat] [-rank r]\n            [-ref refFile] [-rps refsPerSen] [-m metricName metric options]\n            [-evr evalRefs] [-v verbose]");
        println("");
        println(" (*) -cand candFile: candidate translations\n       [[default: candidates.txt]]");
        println(" (*) -format candFileFormat: is the candidate file a plain file (one candidate\n       per sentence) or does it contain multiple candidates per sentence as\n       a decoder's output)?  For the first, use \"plain\".  For the second,\n       use \"nbest\".\n       [[default: plain]]");
        println(" (*) -rank r: if format=nbest, evaluate the set of r'th candidates.\n       [[default: 1]]");
        println(" (*) -ref refFile: reference translations (or file name prefix)\n       [[default: references.txt]]");
        println(" (*) -rps refsPerSen: number of reference translations per sentence\n       [[default: 1]]");
        println(" (*) -txtNrm textNormMethod: how should text be normalized?\n          (0) don't normalize text,\n       or (1) \"NIST-style\", and also rejoin 're, *'s, n't, etc,\n       or (2) apply 1 and also rejoin dashes between letters,\n       or (3) apply 1 and also drop non-ASCII characters,\n       or (4) apply 1+2+3\n       [[default: 1]]");
        println(" (*) -m metricName metric options: name of evaluation metric and its options\n       [[default: BLEU 4 closest]]");
        println(" (*) -evr evalRefs: evaluate references (1) or not (0) (sanity check)\n       [[default: 0]]");
        println(" (*) -v verbose: evaluate individual sentences (1) or not (0)\n       [[default: 0]]");
        println("");
        println("Ex.: java JoshuaEval -cand nbest.out -ref ref.all -rps 4 -m BLEU 4 shortest");
    }


    private static void processArgsAndInitialize(String[] args) {
        EvaluationMetric.set_knownMetrics();

        // set default values
        candFileName = "candidates.txt";
        candFileFormat = "plain";
        candRank = 1;
        refFileName = "references.txt";
        refsPerSen = 1;
        textNormMethod = 1;
        metricName = "BLEU";
        metricOptions = new String[2];
        metricOptions[0] = "4";
        metricOptions[1] = "closest";
        evaluateRefs = false;
        verbose = false;

        int argno = 0;

        while (argno < args.length) {
            String option = args[argno];
            switch (option) {
                case "-cand":
                    candFileName = args[argno + 1];
                    break;
                case "-format":
                    candFileFormat = args[argno + 1];
                    if (!candFileFormat.equals("plain") && !candFileFormat.equals("nbest")) {
                        throw new RuntimeException("candFileFormat must be either plain or nbest.");
                    }
                    break;
                case "-rank":
                    candRank = Integer.parseInt(args[argno + 1]);
                    if (refsPerSen < 1) {
                        throw new RuntimeException("Argument for -rank must be positive.");
                    }
                    break;
                case "-ref":
                    refFileName = args[argno + 1];
                    break;
                case "-rps":
                    refsPerSen = Integer.parseInt(args[argno + 1]);
                    if (refsPerSen < 1) {
                        throw new RuntimeException("refsPerSen must be positive.");
                    }
                    break;
                case "-txtNrm":
                    textNormMethod = Integer.parseInt(args[argno + 1]);
                    if (textNormMethod < 0 || textNormMethod > 4) {
                        throw new RuntimeException("textNormMethod should be between 0 and 4");
                    }
                    break;
                case "-m":
                    metricName = args[argno + 1];
                    if (EvaluationMetric.knownMetricName(metricName)) {
                        int optionCount = EvaluationMetric.metricOptionCount(metricName);
                        metricOptions = new String[optionCount];
                        for (int opt = 0; opt < optionCount; ++opt) {
                            metricOptions[opt] = args[argno + opt + 2];
                        }
                        argno += optionCount;
                    } else {
                        throw new RuntimeException("Unknown metric name " + metricName + ".");
                    }
                    break;
                case "-evr":
                    int evr = Integer.parseInt(args[argno + 1]);
                    if (evr == 1) {
                        evaluateRefs = true;
                    } else if (evr == 0) {
                        evaluateRefs = false;
                    } else {
                        throw new RuntimeException("evalRefs must be either 0 or 1.");
                    }
                    break;
                case "-v":
                    int v = Integer.parseInt(args[argno + 1]);
                    if (v == 1) {
                        verbose = true;
                    } else if (v == 0) {
                        verbose = false;
                    } else {
                        throw new RuntimeException("verbose must be either 0 or 1.");
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown option " + option);
            }

            argno += 2;

        } // while (argno)

        if (refsPerSen > 1) {
            String refFile = refFileName + "0";
            if (!new File(refFile).exists())
                refFile = refFileName + ".0";
            if (!new File(refFile).exists()) {
                throw new RuntimeException(String.format("* FATAL: can't find first reference file '%s{0,.0}'", refFileName));
            }

            numSentences = countLines(refFile);
        } else {
            numSentences = countLines(refFileName);
        }

        // read in reference sentences
        refSentences = new String[numSentences][refsPerSen];

        try {

            // read in reference sentences
            BufferedReader reference_readers[] = new BufferedReader[refsPerSen];
            if (refsPerSen == 1) {
                reference_readers[0] = new BufferedReader(new InputStreamReader(new FileInputStream(new File(refFileName)), "utf8"));
            } else {
                for (int i = 0; i < refsPerSen; i++) {
                    String refFile = refFileName + i;
                    if (!new File(refFile).exists())
                        refFile = refFileName + "." + i;
                    if (!new File(refFile).exists()) {
                        throw new RuntimeException(String.format("* FATAL: can't find reference file '%s'", refFile));
                    }

                    reference_readers[i] = new BufferedReader(new InputStreamReader(new FileInputStream(new File(refFile)), "utf8"));
                }
            }

            for (int i = 0; i < numSentences; ++i) {
                for (int r = 0; r < refsPerSen; ++r) {
                    // read the rth reference translation for the ith sentence
                    refSentences[i][r] = normalize(reference_readers[r].readLine(), textNormMethod);
                }
            }

            // close all the reference files
            for (int i = 0; i < refsPerSen; i++)
                reference_readers[i].close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // set static data members for the EvaluationMetric class
        EvaluationMetric.set_numSentences(numSentences);
        EvaluationMetric.set_refsPerSen(refsPerSen);
        EvaluationMetric.set_refSentences(refSentences);

        // do necessary initialization for the evaluation metric
        evalMetric = EvaluationMetric.getMetric(metricName, metricOptions);
        println("Processing " + numSentences + " sentences...");

    } // processArgsAndInitialize(String[] args)

    private static String normalize(String str, int normMethod) {
        if (normMethod == 0) return str;

        // replace HTML/SGML
        str = str.replaceAll("&quot;", "\"");
        str = str.replaceAll("&amp;", "&");
        str = str.replaceAll("&lt;", "<");
        str = str.replaceAll("&gt;", ">");
        str = str.replaceAll("&apos;", "'");


        // split on these characters:
        // ! " # $ % & ( ) * + / : ; < = > ? @ [ \ ] ^ _ ` { | } ~
        // i.e. ASCII 33-126, except alphanumeric, and except "," "-" "." "'"

        // ! "# $%& ( ) * +/:;<=> ?@ [ \ ] ^_` { | }~
        String split_on = "!\"#\\$%&\\(\\)\\*\\+/:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~";

        // println("split_on: " + split_on);

        for (int k = 0; k < split_on.length(); ++k) {
            // for each split character, reprocess the string
            String regex = "" + split_on.charAt(k);
            if (regex.equals("\\")) {
                ++k;
                regex += split_on.charAt(k);
            }
            str = str.replaceAll(regex, " " + regex + " ");
        }


        // split on "." and "," and "-", conditioned on proper context

        str = " " + str + " ";
        str = str.replaceAll("\\s+", " ");

        TreeSet<Integer> splitIndices = new TreeSet<>();

        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (ch == '.' || ch == ',') {
                // split if either of the previous or next characters is a non-digit
                char prev_ch = str.charAt(i - 1);
                char next_ch = str.charAt(i + 1);
                if (prev_ch < '0' || prev_ch > '9' || next_ch < '0' || next_ch > '9') {
                    splitIndices.add(i);
                }
            } else if (ch == '-') {
                // split if preceded by a digit
                char prev_ch = str.charAt(i - 1);
                if (prev_ch >= '0' && prev_ch <= '9') {
                    splitIndices.add(i);
                }
            }
        }

        String str0 = str;
        str = "";

        for (int i = 0; i < str0.length(); ++i) {
            if (splitIndices.contains(i)) {
                str += " " + str0.charAt(i) + " ";
            } else {
                str += str0.charAt(i);
            }
        }


        // rejoin i'm, we're, *'s, won't, don't, etc

        str = " " + str + " ";
        str = str.replaceAll("\\s+", " ");

        str = str.replaceAll(" i 'm ", " i'm ");
        str = str.replaceAll(" we 're ", " we're ");
        str = str.replaceAll(" 's ", "'s ");
        str = str.replaceAll(" 've ", "'ve ");
        str = str.replaceAll(" 'll ", "'ll ");
        str = str.replaceAll(" 'd ", "'d ");
        str = str.replaceAll(" n't ", "n't ");


        // remove spaces around dashes
        if (normMethod == 2 || normMethod == 4) {

            TreeSet<Integer> skipIndices = new TreeSet<>();
            str = " " + str + " ";

            for (int i = 0; i < str.length(); ++i) {
                char ch = str.charAt(i);
                if (ch == '-') {
                    // rejoin if surrounded by spaces, and then letters
                    if (str.charAt(i - 1) == ' ' && str.charAt(i + 1) == ' ') {
                        if (Character.isLetter(str.charAt(i - 2)) && Character.isLetter(str.charAt(i + 2))) {
                            skipIndices.add(i - 1);
                            skipIndices.add(i + 1);
                        }
                    }
                }
            }

            str0 = str;
            str = "";

            for (int i = 0; i < str0.length(); ++i) {
                if (!skipIndices.contains(i)) {
                    str += str0.charAt(i);
                }
            }
        }

        // drop non-ASCII characters
        if (normMethod == 3 || normMethod == 4) {

            str0 = str;
            str = "";

            for (int i = 0; i < str0.length(); ++i) {
                char ch = str0.charAt(i);
                if (ch <= 127) { // i.e. if ASCII
                    str += ch;
                }
            }
        }

        str = str.replaceAll("\\s+", " ");

        str = str.trim();

        return str;
    }

    // TODO: we should handle errors properly for the three use sites of this function, and should
    // remove the function.
    // OK, but we don't want it to use LineReader, so it can function within the standalone release of
    // Z-MERT. -- O.Z.
    private static int countLines(String fileName) {
        int count = 0;

        try {
            BufferedReader inFile = new BufferedReader(new FileReader(fileName));
            String line;
            do {
                line = inFile.readLine();
                if (line != null) ++count;
            } while (line != null);

            inFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return count;
    }


    /**
     * Computes length ratios between two string arrays.
     * @param arr1
     * @param arr2
     * @return ratio of lengths
     */
    private static double lengthRatio(String[] arr1, String[] arr2){

        assert arr1.length == arr2.length;
        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < arr1.length; i++){
            count1 += arr1[i] == null ? 0 : arr1[i].split(" ").length;
            count2 += arr2[i] == null ? 0 : arr2[i].split(" ").length;
        }
        return 1.0 * count1 / count2;

    }

    private static void println(Object obj) {
        System.out.println(obj);
    }

    private static void print(Object obj) {
        System.out.print(obj);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage(args.length);
            System.exit(0);
        } else {
            processArgsAndInitialize(args);
        }
        // non-specified args will be set to default values in processArgsAndInitialize

        if (candFileFormat.equals("plain")) {
            println("Evaluating candidate translations in plain file " + candFileName + "...");
            evaluateCandsPlain(candFileName);
        } else if (candFileFormat.equals("nbest")) {
            println("Evaluating set of " + candRank + "'th candidate translations from " + candFileName
                    + "...");
            evaluateCandsNbest(candFileName, candRank);
        }
        println("");

        if (evaluateRefs) {
            // evaluate the references themselves; useful if developing a new evaluation metric
            println("");
            println("PERFORMING SANITY CHECK:");
            println("------------------------");
            println("");
            println("This metric's scores range from " + evalMetric.worstPossibleScore() + " (worst) to "
                    + evalMetric.bestPossibleScore() + " (best).");

            for (int r = 1; r <= refsPerSen; ++r) {
                println("");
                println("(*) Evaluating reference set " + r + ":");
                println("");
                evaluateRefSet(r);
                println("");
            }
        }
    }
}

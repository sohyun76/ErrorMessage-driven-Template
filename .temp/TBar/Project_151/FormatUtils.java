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

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.apache.joshua.corpus.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for format issues.
 * 
 * @author Juri Ganitkevitch
 * @author Lane Schwartz
 */
public class FormatUtils {

  private static final Logger LOG = LoggerFactory.getLogger(FormatUtils.class);

  private static final String INDEX_SEPARATOR = ",";

  /**
   * Determines whether the string is a nonterminal by checking that the first character is [
   * and the last character is ].
   * 
   * @param token input string
   * @return true if it's a nonterminal symbol, false otherwise
   */
  public static boolean isNonterminal(String token) {
    return (token.length() >=3 && token.charAt(0) == '[') && (token.charAt(token.length() - 1) == ']');
  }
  
  /**
   * Determines whether the ID represents a nonterminal. This is a trivial check, since nonterminal
   * IDs are simply negative ones.
   * 
   * @param id the vocabulary ID
   * @return true if a nonterminal ID, false otherwise
   */
  public static boolean isNonterminal(int id) {
    return id < 0;
  }

  /**
   * Nonterminals are stored in the vocabulary in square brackets. This removes them when you 
   * just want the raw nonterminal word.
   * Supports indexed and non-indexed nonTerminals:
   * [GOAL] -&gt; GOAL
   * [X,1] -&gt; [X]
   * 
   * @param nt the nonterminal, e.g., "[GOAL]"
   * @return the cleaned nonterminal, e.g., "GOAL"
   */
  public static String cleanNonTerminal(String nt) {
    if (isNonterminal(nt)) {
      if (isIndexedNonTerminal(nt)) {
        // strip ",.*]"
        return nt.substring(1, nt.indexOf(INDEX_SEPARATOR));
      }
      // strip "]"
      return nt.substring(1, nt.length() - 1);
    }
    return nt;
  }
  
  private static boolean isIndexedNonTerminal(String nt) {
    return nt.contains(INDEX_SEPARATOR);
  }

  /**
   * Removes the index from a nonTerminal: [X,1] -&gt; [X].
   * @param nt an input non-terminal string
   * @return the stripped non terminal string
   */
  public static String stripNonTerminalIndex(String nt) {
    return ensureNonTerminalBrackets(cleanNonTerminal(nt));
  }

  /**
   * Nonterminals on source and target sides are represented as [X,1], where 1 is an integer
   * that links the two sides. This function extracts the index, e.g.,
   * 
   * getNonterminalIndex("[X,7]") produces 7
   * 
   * @param nt the nonterminal string
   * @return the index
   */
  public static int getNonterminalIndex(String nt) {
    return Integer.parseInt(nt.substring(nt.indexOf(INDEX_SEPARATOR) + 1, nt.length() - 1));
  }

  /**
   * Ensures that a string looks like what the system considers a nonterminal to be.
   * 
   * @param nt the nonterminal string
   * @return the nonterminal string surrounded in square brackets (if not already)
   */
  public static String ensureNonTerminalBrackets(String nt) {
    if (isNonterminal(nt)) 
      return nt;
    else 
      return "[" + nt + "]";
  }
  
  public static String escapeSpecialSymbols(String s) {
    return s.replaceAll("\\[",  "-lsb-")
            .replaceAll("\\]",  "-rsb-")
            .replaceAll("\\|",  "-pipe-");
  }
  
  public static String unescapeSpecialSymbols(String s) {
    return s.replace("-lsb-", "[")
            .replace("-rsb-", "]")
            .replace("-pipe-", "|");
  }
  
  /**
   * wrap sentence with sentence start/stop markers 
   * as defined by Vocabulary; separated by a single whitespace.
   * @param s an input sentence
   * @return the wrapped sentence
   */
  public static String addSentenceMarkers(String s) {
    return Vocabulary.START_SYM + " " + s + " " + Vocabulary.STOP_SYM;
  }
  
  /**
   * strip sentence markers (and whitespaces) from string
   * @param s the sentence to strip of markers (and whitespaces)
   * @return the stripped string
   */
  public static String removeSentenceMarkers(String s) {
    return s.replaceAll("<s> ", "").replace(" </s>", "");
  }

  /**
   * Returns true if the String parameter represents a valid number.
   * <p>
   * The body of this method is taken from the Javadoc documentation for the Java Double class.
   * 
   * @param string an input string
   * @see java.lang.Double
   * @return <code>true</code> if the string represents a valid number, <code>false</code> otherwise
   */
  public static boolean isNumber(String string) {
    final String Digits = "(\\p{Digit}+)";
    final String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    final String Exp = "[eE][+-]?" + Digits;
    final String fpRegex = ("[\\x00-\\x20]*" + // Optional leading "whitespace"
        "[+-]?(" + // Optional sign character
        "NaN|" + // "NaN" string
        "Infinity|" + // "Infinity" string

        // A decimal floating-point string representing a finite positive
        // number without a leading sign has at most five basic pieces:
        // Digits . Digits ExponentPart FloatTypeSuffix
        //
        // Since this method allows integer-only strings as input
        // in addition to strings of floating-point literals, the
        // two sub-patterns below are simplifications of the grammar
        // productions from the Java Language Specification, 2nd
        // edition, section 3.10.2.

        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
        "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

    // . Digits ExponentPart_opt FloatTypeSuffix_opt
        "(\\.(" + Digits + ")(" + Exp + ")?)|" +

        // Hexadecimal strings
        "((" +
        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
        "(0[xX]" + HexDigits + "(\\.)?)|" +

        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

        ")[pP][+-]?" + Digits + "))" + "[fFdD]?))" + "[\\x00-\\x20]*");// Optional
                                                                       // trailing
                                                                       // "whitespace"

    return Pattern.matches(fpRegex, string);
  }

  /**
   * Set System.out and System.err to use the UTF8 character encoding.
   * 
   * @return <code>true</code> if both System.out and System.err were successfully set to use UTF8,
   *         <code>false</code> otherwise.
   */
  public static boolean useUTF8() {

    try {
      System.setOut(new PrintStream(System.out, true, "UTF8"));
      System.setErr(new PrintStream(System.err, true, "UTF8"));
      return true;
    } catch (UnsupportedEncodingException e1) {
      LOG.warn("UTF8 is not a valid encoding; using system default encoding for System.out and System.err.");
      return false;
    } catch (SecurityException e2) {
      LOG.warn("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
      return false;
    }
  }
  
  /**
   * Determines if a string contains ALL CAPS
   * 
   * @param token an input token
   * @return true if the string is all in uppercase, false otherwise
   */
  public static boolean ISALLUPPERCASE(String token) {
    for (int i = 0; i < token.length(); i++)
      if (! Character.isUpperCase(token.charAt(i)))
        return false;
    return true;
  }

  public static String capitalize(String word) {
    if (word == null || word.length() == 0)
      return word;
    return word.substring(0, 1).toUpperCase() + word.substring(1);
  }
}

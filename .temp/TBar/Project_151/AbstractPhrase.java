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
package org.apache.joshua.corpus;

/**
 * This class provides a skeletal implementation of the base methods likely to be common to most or
 * all implementations of the <code>Phrase</code> interface.
 * 
 * @author Lane Schwartz
 * @author Chris Callison-Burch
 */
public abstract class AbstractPhrase implements Phrase {

  // ===============================================================
  // Constants
  // ===============================================================

  /** seed used in hash code generation */
  public static final int HASH_SEED = 17;

  /** offset used in has code generation */
  public static final int HASH_OFFSET = 37;

  /**
   * Splits a sentence (on white space), then looks up the integer representations of each word
   * using the supplied symbol table.
   * 
   * @param sentence White-space separated String of words.
   * 
   * @return Array of integers corresponding to the words in the sentence.
   */
  protected int[] splitSentence(String sentence) {
    String[] w = sentence.split("\\s+");
    int[] words = new int[w.length];
    for (int i = 0; i < w.length; i++)
      words[i] = Vocabulary.id(w[i]);
    return words;
  }

  /**
   * Uses the standard java approach of calculating hashCode. Start with a seed, add in every value
   * multiplying the exsiting hash times an offset.
   * 
   * @return int hashCode for the list
   */
  public int hashCode() {
    int result = HASH_SEED;
    for (int i = 0; i < size(); i++) {
      result = HASH_OFFSET * result + getWordID(i);
    }
    return result;
  }


  /**
   * Two phrases are their word IDs are the same. Note that this could give a false positive if
   * their Vocabularies were different but their IDs were somehow the same.
   */
  public boolean equals(Object o) {

    if (o instanceof Phrase) {
      Phrase other = (Phrase) o;

      if (this.size() != other.size()) return false;
      for (int i = 0; i < size(); i++) {
        if (this.getWordID(i) != other.getWordID(i)) return false;
      }
      return true;
    } else {
      return false;
    }

  }


  /**
   * Compares the two strings based on the lexicographic order of words defined in the Vocabulary.
   * 
   * @param other the object to compare to
   * @return -1 if this object is less than the parameter, 0 if equals, 1 if greater
   * @exception ClassCastException if the passed object is not of type Phrase
   */
  public int compareTo(Phrase other) {
    int length = size();
    int otherLength = other.size();
    for (int i = 0; i < length; i++) {
      if (i < otherLength) {
        int difference = getWordID(i) - other.getWordID(i);
        if (difference != 0) return difference;
      } else {
        // same but other is shorter, so we are after
        return 1;
      }
    }
    if (length < otherLength) {
      return -1;
    } else {
      return 0;
    }
  }

  /**
   * Returns a string representation of the phrase.
   * 
   * @return a space-delimited string of the words in the phrase.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < size(); i++) {
      String word = Vocabulary.word(getWordID(i));
      if (i != 0) buf.append(' ');
      buf.append(word);
    }
    return buf.toString();
  }

}

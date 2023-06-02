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

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Class that represents a one to (possibly) many alignment from target to
 * source. Extends from a LinkedList. Instances of this class are updated by the
 * WordAlignmentExtractor.substitute() method. 
 * The {@link org.apache.joshua.decoder.hypergraph.AlignedSourceTokens#shiftBy(int, int)} 
 * method shifts the
 * elements in the list by a scalar to reflect substitutions of non terminals in
 * the rule. if indexes are final, i.e. the point instance has been substituted
 * into a parent WordAlignmentState once, 
 * {@link org.apache.joshua.decoder.hypergraph.AlignedSourceTokens#isFinal} is set to true. 
 * This is
 * necessary since the final source index of a point is known once we have
 * substituted in a complete WordAlignmentState into its parent. If the index in
 * the list is a non terminal, {@link org.apache.joshua.decoder.hypergraph.AlignedSourceTokens#isNonTerminal} = true
 */
class AlignedSourceTokens extends LinkedList<Integer> {

  private static final long serialVersionUID = 1L;
  /** whether this Point refers to a non terminal in source&target */
  private boolean isNonTerminal = false;
  /** whether this instance does not need to be updated anymore */
  private boolean isFinal = false;
  /** whether the word this Point corresponds to has no alignment in source */
  private boolean isNull = false;

  AlignedSourceTokens() {}

  void setFinal() {
    isFinal = true;
  }

  void setNonTerminal() {
    isNonTerminal = true;
  }

  void setNull() {
    isNull = true;
  }

  @Override
  /**
   * returns true if element was added.
   */
  public boolean add(Integer x) {
    return !isNull && super.add(x);
  }

  public boolean isNonTerminal() {
    return isNonTerminal;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isNull() {
    return isNull;
  }

  /**
   * shifts each item in the LinkedList by <shift>.
   * Only applies to items larger than <start>
   */
  void shiftBy(int start, int shift) {
    if (!isFinal && !isNull) {
      final ListIterator<Integer> it = this.listIterator();
      while (it.hasNext()) {
        final int x = it.next();
        if (x > start) {
          it.set(x + shift);
        }
      }
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (isFinal)
      sb.append("f");
    if (isNull) {
      sb.append("[NULL]");
    } else {
      sb.append(super.toString());
    }
    if (isNonTerminal)
      sb.append("^");
    return sb.toString();
  }
}
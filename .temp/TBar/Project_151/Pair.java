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

/**
 * Represents a pair of elements.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 * 
 * @param <First> Type of the first element in the pair.
 * @param <Second> Type of the second element in the pair.
 */
public class Pair<First, Second> {

  /** The first element of the pair. */
  public First first;

  /** The second element of the pair. */
  public Second second;

  private Integer hashCode = null;

  /**
   * Constructs a pair of elements.
   * 
   * @param first the first element in the pair
   * @param second the second element in the pair
   */
  public Pair(First first, Second second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Gets the second element in the pair
   * 
   * @return the first element in the pair
   */
  public First getFirst() {
    return first;
  }

  /**
   * Sets the first element in the pair.
   * 
   * @param first the new value for the first element in the pair
   */
  public void setFirst(First first) {
    this.first = first;
  }

  /**
   * Gets the second element in the pair.
   * 
   * @return the second element in the pair
   */
  public Second getSecond() {
    return second;
  }

  /**
   * Sets the second element in the pair.
   * 
   * @param second the new value for the second element in the pair
   */
  public void setSecond(Second second) {
    this.second = second;
  }


  public int hashCode() {

    if (hashCode == null) {
      if (first == null) {
        if (second == null) {
          hashCode = 0;
        } else {
          hashCode = second.hashCode();
        }
      } else if (second == null) {
        hashCode = first.hashCode();
      } else {
        hashCode = first.hashCode() + 37 * second.hashCode();
      }
    }

    return hashCode;
  }

  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if (o instanceof Pair<?, ?>) {

      Pair<First, Second> other = (Pair<First, Second>) o;

      if (first == null) {
        if (second == null) {
          return other.first == null && other.second == null;
        } else {
          return other.first == null && second.equals(other.second);
        }
      } else if (second == null) {
        return first.equals(other.first) && other.second == null;
      } else {
        return first.equals(other.first) && second.equals(other.second);
      }

    } else {
      return false;
    }
  }

}

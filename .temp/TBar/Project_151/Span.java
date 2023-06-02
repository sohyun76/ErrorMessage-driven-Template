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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Represents a span with an inclusive starting index and an exclusive ending index.
 * 
 * @author Lane Schwartz
 */
public class Span implements Iterable<Integer>, Comparable<Span> {

  /** Inclusive starting index of this span. */
  public final int start;

  /** Exclusive ending index of this span. */
  public final int end;


  /**
   * Constructs a new span with the given inclusive starting and exclusive ending indices.
   * 
   * @param start Inclusive starting index of this span.
   * @param end Exclusive ending index of this span.
   */
  public Span(int start, int end) {
    this.start = start;
    this.end = end;
  }


  /**
   * Returns the length of the span.
   * 
   * @return the length of the span; this is equivalent to <code>span.end - span.start</code>.
   */
  public int size() {
    return end - start;
  }

  /**
   * Returns all subspans of the given Span.
   * 
   * @return a list of all subspans.
   */
  public List<Span> getSubSpans() {
    return getSubSpans(size());
  }

  /**
   * Returns all subspans of the given Span, up to a specified Span size.
   * 
   * @param max the maximum Span size to return
   * @return a list all subspans up to the given size
   */
  public List<Span> getSubSpans(int max) {
    int spanSize = size();
    ArrayList<Span> result = new ArrayList<>(max * spanSize);
    for (int len = max; len > 0; len--) {
      for (int i = start; i < end - len + 1; i++) {
        result.add(new Span(i, i + len));
      }
    }
    return result;
  }

  public boolean strictlyContainedIn(Span o) {
    return (start >= o.start) && (end <= o.end) && !(start == o.start && end == o.end);
  }

  /**
   * Returns true if the other span does not intersect with this one.
   * @param o new {@link org.apache.joshua.corpus.Span} to check for intersection
   * @return true if the other span does not intersect with this one
   */
  public boolean disjointFrom(Span o) {
    if (start < o.start) {
      return end <= o.start;
    }
    if (end > o.end) {
      return start >= o.end;
    }
    return false;
  }

  public String toString() {
    return "[" + start + "-" + end + ")";
  }


  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {

      int next = start;

      public boolean hasNext() {
        return next < end;
      }

      public Integer next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return next++;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }


  public int compareTo(Span o) {

    if (o == null) {
      throw new NullPointerException();
    } else {

      if (start < o.start) {
        return -1;
      } else if (start > o.start) {
        return 1;
      } else {
        if (end < o.end) {
          return -1;
        } else if (end > o.end) {
          return 1;
        } else {
          return 0;
        }
      }
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof Span) {
      Span other = (Span) o;
      return (start == other.start && end == other.end);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return start * 31 + end * 773;
  }
}

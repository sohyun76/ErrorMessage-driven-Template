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

// Imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache is a class that implements a least recently used cache. It is a straightforward extension
 * of java.util.LinkedHashMap with its removeEldestEntry method overridden, so that stale entries
 * are deleted once we reach the specified capacity of the Cache.
 * <p>
 * This class is quite useful for storing the results of computations that we would do many times
 * over in the FeatureFunctions.
 * 
 * @author Chris Callison-Burch
 * @since 14 April 2005
 * 
 */
public class Cache<K, V> extends LinkedHashMap<K, V> {

  private static final long serialVersionUID = 6073387072740892061L;

  /** Logger for this class. */
  private static final Logger LOG = LoggerFactory.getLogger(Cache.class);
  // ===============================================================
  // Constants
  // ===============================================================

  /**
   * A constant is used as the default the cache size if none is specified.
   */
  public static final int DEFAULT_CAPACITY = 100000000;

  /** Default initial capacity of the cache. */
  public static final int INITIAL_CAPACITY = 1000000;

  /** Default load factor of the cache. */
  public static final float LOAD_FACTOR = 0.75f;

  /**
   * By default, ordering mode of the cache is access order (true).
   */
  public static final boolean ACCESS_ORDER = true;


  // ===============================================================
  // Member variables
  // ===============================================================

  /** Maximum number of items that the cache can contain. */
  final int maxCapacity;

  // ===============================================================
  // Constructor(s)
  // ===============================================================

  /**
   * Creates a Cache with a set capacity.
   * 
   * @param maxCapacity the maximum capacity of the cache.
   */
  public Cache(int maxCapacity) {
    super((maxCapacity < INITIAL_CAPACITY) ? maxCapacity : INITIAL_CAPACITY, LOAD_FACTOR,
        ACCESS_ORDER);
    this.maxCapacity = maxCapacity;
  }


  /**
   * Creates a Cache with the DEFAULT_CAPACITY.
   */
  public Cache() {
    this(DEFAULT_CAPACITY);
  }

  // ===============================================================
  // Public
  // ===============================================================

  // ===========================================================
  // Accessor methods (set/get)
  // ===========================================================

  @Override
  public V get(Object key) {
    LOG.debug("Cache get   key: {}", key);
    return super.get(key);
  }


  @Override
  public V put(K key, V value) {
    LOG.debug("Cache put   key: {}", key);
    return super.put(key, value);
  }

  // ===========================================================
  // Methods
  // ===========================================================

  @Override
  public boolean containsKey(Object key) {
    boolean contains = super.containsKey(key);
    if (contains){
      LOG.debug("Cache has key: {}", key);
    } else {
      LOG.debug("Cache lacks key: {}", key);
    }
    return contains;
  }


  // ===============================================================
  // Protected
  // ===============================================================

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * This method is invoked by put and putAll after inserting a new entry into the map. Once we
   * reach the capacity of the cache, we remove the oldest entry each time a new entry is added.
   * This reduces memory consumption by deleting stale entries.
   * 
   * @param eldest the eldest entry
   * @return true if the capacity is greater than the maximum capacity
   */
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    boolean removing = size() > maxCapacity;
    if (removing ) {
      LOG.debug("Cache loses key: {}",  eldest.getKey());
    }
    return removing;
  }

  // ===============================================================
  // Private
  // ===============================================================

  // ===============================================================
  // Methods
  // ===============================================================


  // ===============================================================
  // Static
  // ===============================================================


  // ===============================================================
  // Main
  // ===============================================================

}

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.apache.joshua.decoder.ff.lm.NGramLanguageModel;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static singular vocabulary class.
 * Supports (de-)serialization into a vocabulary file.
 *
 * @author Juri Ganitkevitch
 */

public class Vocabulary implements Externalizable {

  private static final Logger LOG = LoggerFactory.getLogger(Vocabulary.class);
  private final static ArrayList<NGramLanguageModel> LMs = new ArrayList<>();

  private static List<String> idToString;
  private static Map<String, Integer> stringToId;
  private static final StampedLock lock = new StampedLock();

  static final int UNKNOWN_ID = 0;
  static final String UNKNOWN_WORD = "<unk>";

  public static final String START_SYM = "<s>";
  public static final String STOP_SYM = "</s>";

  static {
    clear();
  }

  public static boolean registerLanguageModel(NGramLanguageModel lm) {
    long lock_stamp = lock.writeLock();
    try {
      // Store the language model.
      LMs.add(lm);
      // Notify it of all the existing words.
      boolean collision = false;
      for (int i = idToString.size() - 1; i > 0; i--)
        collision = collision || lm.registerWord(idToString.get(i), i);
      return collision;
    } finally {
      lock.unlockWrite(lock_stamp);
    }
  }

  /**
   * Reads a vocabulary from file. This deletes any additions to the vocabulary made prior to
   * reading the file.
   *
   * @param vocab_file path to a vocabulary file
   * @return Returns true if vocabulary was read without mismatches or collisions.
   * @throws IOException of the file cannot be found or read properly
   */
  public static boolean read(final File vocab_file) throws IOException {
    DataInputStream vocab_stream =
        new DataInputStream(new BufferedInputStream(new FileInputStream(vocab_file)));
    int size = vocab_stream.readInt();
    LOG.info("Read {} entries from the vocabulary", size);
    clear();
    for (int i = 0; i < size; i++) {
      int id = vocab_stream.readInt();
      String token = vocab_stream.readUTF();
      if (id != Math.abs(id(token))) {
        vocab_stream.close();
        return false;
      }
    }
    vocab_stream.close();
    return (size + 1 == idToString.size());
  }

  public static void write(String file_name) throws IOException {
    long lock_stamp =lock.readLock();
    try {
      File vocab_file = new File(file_name);
      DataOutputStream vocab_stream =
          new DataOutputStream(new BufferedOutputStream(new FileOutputStream(vocab_file)));
      vocab_stream.writeInt(idToString.size() - 1);
      LOG.info("Writing vocabulary: {} tokens", idToString.size() - 1);
      for (int i = 1; i < idToString.size(); i++) {
        vocab_stream.writeInt(i);
        vocab_stream.writeUTF(idToString.get(i));
      }
      vocab_stream.close();
    }
    finally{
      lock.unlockRead(lock_stamp);
    }
  }

  /**
   * Get the id of the token if it already exists, new id is created otherwise.
   *
   * TODO: currently locks for every call. Separate constant (frozen) ids from
   * changing (e.g. OOV) ids. Constant ids could be immutable -&gt; no locking.
   * Alternatively: could we use ConcurrentHashMap to not have to lock if
   * actually contains it and only lock for modifications?
   * 
   * @param token a token to obtain an id for
   * @return the token id
   */
  public static int id(String token) {
    // First attempt an optimistic read
    long attempt_read_lock = lock.tryOptimisticRead();
    if (stringToId.containsKey(token)) {
      int resultId = stringToId.get(token);
      if (lock.validate(attempt_read_lock)) {
        return resultId;
      }
    }

    // The optimistic read failed, try a read with a stamped read lock
    long read_lock_stamp = lock.readLock();
    try {
      if (stringToId.containsKey(token)) {
        return stringToId.get(token);
      }
    } finally {
      lock.unlockRead(read_lock_stamp);
    }

    // Looks like the id we want is not there, let's get a write lock and add it
    long write_lock_stamp = lock.writeLock();
    try {
      if (stringToId.containsKey(token)) {
        return stringToId.get(token);
      }
      int id = idToString.size() * (FormatUtils.isNonterminal(token) ? -1 : 1);

      // register this (token,id) mapping with each language
      // model, so that they can map it to their own private
      // vocabularies
      for (NGramLanguageModel lm : LMs)
        lm.registerWord(token, Math.abs(id));

      idToString.add(token);
      stringToId.put(token, id);
      return id;
    } finally {
      lock.unlockWrite(write_lock_stamp);
    }
  }

  public static boolean hasId(int id) {
    long lock_stamp = lock.readLock();
    try {
      id = Math.abs(id);
      return (id < idToString.size());
    }
    finally{
      lock.unlockRead(lock_stamp);
    }
  }

  public static int[] addAll(String sentence) {
    return addAll(sentence.split("\\s+"));
  }

  public static int[] addAll(String[] tokens) {
    int[] ids = new int[tokens.length];
    for (int i = 0; i < tokens.length; i++)
      ids[i] = id(tokens[i]);
    return ids;
  }

  public static String word(int id) {
    long lock_stamp = lock.readLock();
    try {
      id = Math.abs(id);
      return idToString.get(id);
    }
    finally{
      lock.unlockRead(lock_stamp);
    }
  }

  public static String getWords(int[] ids) {
    return getWords(ids, " ");
  }
  
  public static String getWords(int[] ids, final String separator) {
    if (ids.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ids.length - 1; i++) {
      sb.append(word(ids[i])).append(separator);
    }
    return sb.append(word(ids[ids.length - 1])).toString();
  }

  public static String getWords(final Iterable<Integer> ids) {
    StringBuilder sb = new StringBuilder();
    for (int id : ids)
      sb.append(word(id)).append(" ");
    return sb.deleteCharAt(sb.length() - 1).toString();
  }

  public static int getUnknownId() {
    return UNKNOWN_ID;
  }

  public static String getUnknownWord() {
    return UNKNOWN_WORD;
  }

  public static int size() {
    long lock_stamp = lock.readLock();
    try {
      return idToString.size();
    } finally {
      lock.unlockRead(lock_stamp);
    }
  }

  public static synchronized int getTargetNonterminalIndex(int id) {
    return FormatUtils.getNonterminalIndex(word(id));
  }

  /**
   * Clears the vocabulary and initializes it with an unknown word. Registered
   * language models are left unchanged.
   */
  public static void clear() {
    long lock_stamp = lock.writeLock();
    try {
      idToString = new ArrayList<>();
      stringToId = new HashMap<>();

      idToString.add(UNKNOWN_ID, UNKNOWN_WORD);
      stringToId.put(UNKNOWN_WORD, UNKNOWN_ID);
    } finally {
      lock.unlockWrite(lock_stamp);
    }
  }

  public static void unregisterLanguageModels() {
    LMs.clear();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void readExternal(ObjectInput in)
      throws IOException, ClassNotFoundException {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean equals(Object o) {
    return getClass() == o.getClass();
  }

}

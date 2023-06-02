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

import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.base.Throwables;
import org.apache.joshua.decoder.io.TranslationRequestStream;

/**
 * This class represents a streaming sequence of translations. It is returned by the main entry
 * point to the Decoder object, the call to decodeAll. The translations here are parallel to the
 * input sentences in the corresponding TranslationRequest object. Because of parallelization, the
 * translated sentences might be computed out of order. Each Translation is sent to this
 * TranslationResponseStream object by a DecoderThreadRunner via the record() function, which places the
 * Translation in the right place. When the next translation in a sequence is available, next() is
 * notified.
 * 
 * @author Matt Post post@cs.jhu.edu
 */
public class TranslationResponseStream implements Iterator<Translation>, Iterable<Translation> {

  /* The source sentences to be translated. */
  private TranslationRequestStream request = null;

  /*
   * This records the index of the sentence at the head of the underlying list. The iterator's
   * next() blocks when the value at this position in the translations LinkedList is null.
   */
  private int currentID = 0;

  /* The set of translated sentences. */
  private LinkedList<Translation> translations = null;

  private boolean spent = false;

  private Translation nextTranslation;
  private Throwable fatalException;

  public TranslationResponseStream(TranslationRequestStream request) {
    this.request = request;
    this.translations = new LinkedList<>();
  }

  /**
   * This is called when null is received from the TranslationRequest, indicating that there are no
   * more input sentences to translated. That in turn means that the request size will no longer
   * grow. We then notify any waiting thread if the last ID we've processed is the last one, period.
   */
  public void finish() {
    synchronized (this) {
      spent = true;
      if (currentID == request.size()) {
        this.notifyAll();
      }
    }
  }

  /**
   * This is called whenever a translation is completed by one of the decoder threads. There may be
   * a current output thread waiting for the current translation, which is determined by checking if
   * the ID of the translation is the same as the one being waited for (currentID). If so, the
   * thread waiting for it is notified.
   * 
   * @param translation a translated input object
   */
  public void record(Translation translation) {
    synchronized (this) {

      /* Pad the set of translations with nulls to accommodate the new translation. */
      int offset = translation.id() - currentID;
      while (offset >= translations.size())
        translations.add(null);
      translations.set(offset, translation);

      /*
       * If the id of the current translation is at the head of the list (first element), then we
       * have the next Translation to be return, and we should notify anyone waiting on next(),
       * which will then remove the item and increment the currentID.
       */
      if (translation.id() == currentID) {
        this.notify();
      }
    }
  }

  /**
   * Returns the next Translation, blocking if necessary until it's available, since the next
   * Translation might not have been produced yet.
   * 
   * @return first element from the list of {@link org.apache.joshua.decoder.Translation}'s
   */
  @Override
  public Translation next() {
    synchronized(this) {
      if (this.hasNext()) {
        Translation t = this.nextTranslation;
        this.nextTranslation = null;
        return t;
      }
      
      return null;
    }
  }
   
  @Override
  public boolean hasNext() {
    synchronized (this) {

      if (nextTranslation != null)
        return true;
      
      /*
       * If there are no more input sentences, and we've already distributed what we then know is
       * the last one, we're done.
       */
      if (spent && currentID == request.size())
        return false;

      /*
       * Otherwise, there is another sentence. If it's not available already, we need to wait for
       * it.
       */
      if (translations.size() == 0 || translations.peek() == null) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      fatalErrorCheck();

      /* We now have the sentence and can return it. */
      currentID++;
      this.nextTranslation = translations.poll();
      return this.nextTranslation != null;
    }
  }

  @Override
  public Iterator<Translation> iterator() {
    return this;
  }

  public void propagate(Throwable ex) {
    synchronized (this) {
      fatalException = ex;
      notify();
    }
  }

  private void fatalErrorCheck() {
    if (fatalException != null) {
      Throwables.propagate(fatalException);
    }
  }
}
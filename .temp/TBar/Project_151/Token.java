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
package org.apache.joshua.decoder.segment_file;

import static org.apache.joshua.util.FormatUtils.escapeSpecialSymbols;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the identity of a word and its annotations in a sentence.

 * @author "Gaurav Kumar"
 * @author Matt Post
 */
public class Token {

  private static final Logger LOG = LoggerFactory.getLogger(Token.class);

  // The token without the annotations
  private String token; 
  private final int tokenID;

  private HashMap<String,String> annotations = null;

  /**
   * <p>Constructor : Creates a Token object from a raw word
   * Extracts and assigns an annotation when available.
   * Any word can be marked with annotations, which are arbitrary semicolon-delimited
   * key[=value] pairs (the value is optional) listed in brackets after a word, e.g.,</p>
   * <pre>
   *    Je[ref=Samuel;PRO] voudrais[FUT;COND]
   * </pre>
   * 
   * <p>This will create a dictionary annotation on the word of the following form for "Je"</p>
   * 
   * <pre>
   *   ref -&gt; Samuel
   *   PRO -&gt; PRO
   * </pre>
   * 
   * <p>and the following for "voudrais":</p>
   * 
   * <pre>
   *   FUT  -&gt; FUT
   *   COND -&gt; COND
   * </pre>
   * 
   * @param rawWord A word with annotation information (possibly)
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   *  
   */
  public Token(String rawWord, JoshuaConfiguration config) {

    JoshuaConfiguration joshuaConfiguration = config;
    
    annotations = new HashMap<>();
    
    // Matches a word with an annotation
    // Check guidelines in constructor description
    Pattern pattern = Pattern.compile("(\\S+)\\[(\\S+)\\]");
    Matcher tag = pattern.matcher(rawWord);
    if (tag.find()) {
      // Annotation match found
      token = tag.group(1);
      String tagStr = tag.group(2);

      for (String annotation: tagStr.split(";")) {
        int where = annotation.indexOf("=");
        if (where != -1) {
          annotations.put(annotation.substring(0, where), annotation.substring(where + 1));
        } else {
          annotations.put(annotation, annotation);
        }
      }
    } else {
      // No match found, which implies that this token does not have any annotations 
      token = rawWord;
    }

    // Mask strings that cause problems for the decoder. This has to be done *after* parsing for
    // annotations.
    token = escapeSpecialSymbols(token);

    if (joshuaConfiguration != null && joshuaConfiguration.lowercase) {
      if (FormatUtils.ISALLUPPERCASE(token))
        annotations.put("lettercase", "all-upper");
      else if (Character.isUpperCase(token.charAt(0)))
        annotations.put("lettercase",  "upper");
      else
        annotations.put("lettercase",  "lower");
      
      LOG.debug("TOKEN: {} -> {} ({})", token, token.toLowerCase(), annotations.get("lettercase"));
      token = token.toLowerCase(); 
    }
    
    tokenID = Vocabulary.id(token);
  }
  
  /**
   * Returns the word ID (vocab ID) for this token
   * 
   * @return int A word ID
   */
  public int getWord() {
    return tokenID;
  }

  /**
   * Returns the string associated with this token
   * @return String A word
   */
  public String getWordIdentity() {
    return token;
  }
  
  public String toString() {
    return token;
  }

  /**
   * Returns the annotationID (vocab ID)
   * associated with this token
   * @param key A type ID
   * @return the annotationID (vocab ID)
   */
  public String getAnnotation(String key) {
    if (annotations.containsKey(key)) {
      return annotations.get(key);
    }
    
    return null;
  }
}
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
package org.apache.joshua.util.encoding;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTypeAnalyzer {

  private static final Logger LOG = LoggerFactory.getLogger(FeatureTypeAnalyzer.class);

  private final ArrayList<FeatureType> types;

  private final Map<Integer, Integer> featureToType;

  private final Map<Integer, Integer> featureIdMap;

  // Is the feature setup labeled.
  private boolean labeled;

  // Is the encoder configuration open for new features (that are not assumed boolean)?
  private final boolean open;

  public FeatureTypeAnalyzer() {
    this(false);
  }

  public FeatureTypeAnalyzer(boolean open) {
    this.open = open;
    this.types = new ArrayList<>();
    this.featureToType = new HashMap<>();
    this.featureIdMap = new HashMap<>();
  }

  public void readConfig(String config_filename) throws IOException {
    try(LineReader reader = new LineReader(config_filename)) {
      while (reader.hasNext()) {
        // Clean up line, chop comments off and skip if the result is empty.
        String line = reader.next().trim();
        if (line.indexOf('#') != -1)
          line = line.substring(0, line.indexOf('#'));
        if (line.isEmpty())
          continue;
        String[] fields = line.split("[\\s]+");

        if ("encoder".equals(fields[0])) {
          // Adding an encoder to the mix.
          if (fields.length < 3) {
            throw new RuntimeException("Incomplete encoder line in config.");
          }
          String encoder_key = fields[1];
          List<Integer> feature_ids = new ArrayList<>();
          for (int i = 2; i < fields.length; i++)
            feature_ids.add(Vocabulary.id(fields[i]));
          addFeatures(encoder_key, feature_ids);
        }
      }
    }
  }

  public void addFeatures(String encoder_key, List<Integer> feature_ids) {
    int index = addType(encoder_key);
    for (int feature_id : feature_ids)
      featureToType.put(feature_id, index);
  }

  private int addType(String encoder_key) {
    FeatureType ft = new FeatureType(encoder_key);
    int index = types.indexOf(ft);
    if (index < 0) {
      types.add(ft);
      return types.size() - 1;
    }
    return index;
  }

  private int addType() {
    types.add(new FeatureType());
    return types.size() - 1;
  }

  public void observe(int feature_id, float value) {
    Integer type_id = featureToType.get(feature_id);
    if (type_id == null && open) {
      type_id = addType();
      featureToType.put(feature_id, type_id);
    }
    if (type_id != null)
      types.get(type_id).observe(value);
  }

  // Inspects the collected histograms, inferring actual type of feature. Then replaces the
  // analyzer, if present, with the most compact applicable type.
  public void inferTypes(boolean labeled) {
    types.forEach(FeatureType::inferUncompressedType);
    if (LOG.isInfoEnabled()) {
      for (int id : featureToType.keySet()) {
        LOG.info("Type inferred: {} is {}", (labeled ? Vocabulary.word(id) : "Feature " + id),
            types.get(featureToType.get(id)).encoder.getKey());
      }
    }
  }

  public void buildFeatureMap() {
    int[] known_features = new int[featureToType.keySet().size()];
    int i = 0;
    for (int f : featureToType.keySet())
      known_features[i++] = f;
    Arrays.sort(known_features);

    featureIdMap.clear();
    for (i = 0; i < known_features.length; ++i)
      featureIdMap.put(known_features[i], i);
  }

  public int getRank(int feature_id) {
    return featureIdMap.get(feature_id);
  }

  public IntEncoder getIdEncoder() {
    int num_features = featureIdMap.size();
    if (num_features <= Byte.MAX_VALUE)
      return PrimitiveIntEncoder.BYTE;
    else if (num_features <= Character.MAX_VALUE)
      return PrimitiveIntEncoder.CHAR;
    else
      return PrimitiveIntEncoder.INT;
  }

  public void write(String file_name) throws IOException {
    File out_file = new File(file_name);
    BufferedOutputStream buf_stream = new BufferedOutputStream(new FileOutputStream(out_file));
    DataOutputStream out_stream = new DataOutputStream(buf_stream);

    buildFeatureMap();

    getIdEncoder().writeState(out_stream);
    out_stream.writeBoolean(labeled);
    out_stream.writeInt(types.size());
    for (FeatureType type : types)
      type.encoder.writeState(out_stream);

    out_stream.writeInt(featureToType.size());
    for (int feature_id : featureToType.keySet()) {
      if (labeled)
        out_stream.writeUTF(Vocabulary.word(feature_id));
      else
        out_stream.writeInt(feature_id);
      out_stream.writeInt(featureIdMap.get(feature_id));
      out_stream.writeInt(featureToType.get(feature_id));
    }
    out_stream.close();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int feature_id : featureToType.keySet()) {
      sb.append(types.get(featureToType.get(feature_id)).analyzer.toString(Vocabulary.word(feature_id)));
    }
    return sb.toString();
  }

  public boolean isLabeled() {
    return labeled;
  }

  public void setLabeled(boolean labeled) {
    this.labeled = labeled;
  }

  static class FeatureType {
    FloatEncoder encoder;
    Analyzer analyzer;
    final int bits;

    FeatureType() {
      encoder = null;
      analyzer = new Analyzer();
      bits = -1;
    }

    FeatureType(String key) {
      // either throws or returns non-null
      FloatEncoder e = EncoderFactory.getFloatEncoder(key);
      encoder = e;
      analyzer = null;
      bits = -1;
    }

    void inferUncompressedType() {
      if (encoder != null)
        return;
      encoder = analyzer.inferUncompressedType();
      analyzer = null;
    }

    void inferType() {
      if (encoder != null)
        return;
      encoder = analyzer.inferType(bits);
      analyzer = null;
    }

    void observe(float value) {
      if (analyzer != null)
        analyzer.add(value);
    }

    @Override
    public boolean equals(Object t) {
      if (t != null && t instanceof FeatureType) {
        FeatureType that = (FeatureType) t;
        if (this.encoder != null) {
          return this.encoder.equals(that.encoder);
        } else {
          if (that.encoder != null)
            return false;
          if (this.analyzer != null)
            return this.analyzer.equals(that.analyzer);
        }
      }
      return false;
    }
  }
}

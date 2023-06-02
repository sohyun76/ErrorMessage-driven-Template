/**
 * Copyright 2019 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.singer.writer.partitioners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.kafka.common.PartitionInfo;

import com.pinterest.singer.writer.KafkaMessagePartitioner;

/**
 * This partition is used to group a batch of messages into one partition
 */
public class SinglePartitionPartitioner implements KafkaMessagePartitioner {

  private Random random = new Random(System.currentTimeMillis());

  private Map<Integer, Integer> partitionMap = new HashMap<>();

  public int partition(Object object, List<PartitionInfo> partitions) {
    int numOfPartitions = partitions.size();
    if (!partitionMap.containsKey(numOfPartitions)){
      int selectedPartition = Math.abs(random.nextInt() % numOfPartitions);
      partitionMap.put(numOfPartitions, selectedPartition);
    }
    return partitionMap.get(numOfPartitions);
  }
}

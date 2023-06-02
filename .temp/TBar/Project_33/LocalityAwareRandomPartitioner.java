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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.common.PartitionInfo;

/**
 * Locality aware partitioner random partitioner
 */
public class LocalityAwareRandomPartitioner extends LocalityAwarePartitioner {

  // refresh every 10 seconds
  public static final long REFRESH_INTERVAL_MS = 10_000;
  private ThreadLocalRandom random = ThreadLocalRandom.current();

  public LocalityAwareRandomPartitioner() {
    super(REFRESH_INTERVAL_MS);
  }

  protected LocalityAwareRandomPartitioner(String rack, long refreshIntervalMs) {
    super(rack, refreshIntervalMs);
  }

  @Override
  public int partition(Object messageKey, List<PartitionInfo> partitions) {
    if (localPartitions == null || isTimeToRefresh()) {
      checkAndAssignLocalPartitions(partitions);
      // set next refresh time
      updateNextRefreshTime();
    }
    // we supply on local partitions to this base partitioner
    return localPartitions.get(Math.abs(random.nextInt() % localPartitions.size())).partition();
  }

}
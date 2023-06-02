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
 * Utility class for bit twiddling.
 * 
 * @author Lane Schwartz
 */
public class Bits {

  /**
   * Encodes two shorts in an int.
   * 
   * @param high input high short to encode
   * @param low input low short to encode
   * @return encoded int
   */
  public static int encodeAsInt(short high, short low) {

    // Store the first short value in the highest 16 bits of the int
    int key = high | 0x00000000;
    key <<= 16;

    // Store the second short value in the lowest 16 bits of the int
    int lowInt = low & 0x0000FFFF;
    key |= lowInt;

    return key;

  }

  /**
   * Decodes the high 16 bits of an integer as a short.
   * 
   * @param i Integer value to decode
   * @return Short representation of the high 16 bits of the integer
   */
  public static short decodeHighBits(int i) {

    long key = i & 0xFFFF0000L;

    key >>= 16;

    return (short) key;

  }


  /**
   * Decodes the low 16 bits of an integer as a short.
   * 
   * @param i Integer value to decode
   * @return Short representation of the high 16 bits of the integer
   */
  public static short decodeLowBits(int i) {

    return (short) i;

  }


  /**
   * Encodes two integers in a long.
   * 
   * @param high input high int to encode
   * @param low input low int to encode
   * @return encoded long
   */
  public static long encodeAsLong(int high, int low) {

    // Store the first int value in the highest 32 bits of the long
    long key = high | 0x0000000000000000L;
    key <<= 32;

    // Store the second int value in the lowest 32 bits of the long
    long lowLong = low & 0x00000000FFFFFFFFL;
    key |= lowLong;

    return key;

  }

  /**
   * Decodes the high 32 bits of a long as an integer.
   * 
   * @param l Long value to decode
   * @return Integer representation of the high 32 bits of the long
   */
  public static int decodeHighBits(long l) {

    long key = l & 0xFFFFFFFF00000000L;

    key >>= 32;

    return (int) key;

  }


  /**
   * Decodes the low 32 bits of a long as an integer.
   * 
   * @param l Long value to decode
   * @return Integer representation of the high 32 bits of the long
   */
  public static int decodeLowBits(long l) {

    return (int) l;

  }
}

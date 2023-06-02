/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.parser.rest;

import cc.redpen.parser.common.Line;
import cc.redpen.parser.common.LineParser;

public class MultiLineProcessUtils {
    static boolean processMultiLineMatch(Character prevChar, Character nextChar, LineParser.TargetLine target) {
        Line prevLine = target.previousLine;
        Line nextLine = target.nextLine;
        if (prevChar != null && nextChar != null) {
            if (processLineMatch(nextChar, nextLine) && processLineMatch(prevChar, prevLine)) {
                prevLine.erase();
                nextLine.erase();
                return true;
            }
        }
        if (prevChar == null && nextChar != null) {
            if (processLineMatch(nextChar, nextLine)) {
                nextLine.erase();
                return true;
            }
        }
        if (prevChar != null && nextChar == null) {
            if (processLineMatch(prevChar, prevLine)) {
                prevLine.erase();
                return true;
            }
        }
        return false;
    }

    static private boolean processLineMatch(char ch, Line line) {
        if (line == null) { return false; }
        if (line.isAllSameCharacter() && (line.length() >= 4) && line.charAt(0) == ch) {
            return true;
        }
        return false;
    }
}

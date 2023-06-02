/*
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
package cc.redpen.parser.review;

import cc.redpen.parser.common.Line;

public class ReVIEWLine extends Line {
    /**
     * Construct a line using the supplied string
     *
     * @param str    the text of the line
     * @param lineno the original line number
     */
    public ReVIEWLine(String str, int lineno) {
        super(str, lineno);
        this.inlineMarkupDelimiters = "";
        if (!str.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if ((i < str.length() - 1) && (ch == '\\')) {
                    i++;
                    ch = str.charAt(i);
                    escaped.add(true);
                }
                else {
                    escaped.add(false);
                }
                offsets.add(i);
                characters.add(ch);
                valid.add(true);
            }
        }

        // trim the end
        while (!characters.isEmpty() &&
                Character.isWhitespace(characters.get(characters.size() - 1))) {
            characters.remove(characters.size() - 1);
        }
    }
}

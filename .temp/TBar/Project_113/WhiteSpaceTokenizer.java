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
package cc.redpen.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Character.isWhitespace;

public class WhiteSpaceTokenizer implements RedPenTokenizer {

    private static final Pattern[] BLACKLIST_TOKEN_PATTERNS = new Pattern[]{
            Pattern.compile("^[-+#$€£¥]?\\d+(\\.\\d+)?[%€¥¢₽]?$") // a number [+-]n[.n]
    };

    private static final String DELIMITERS = " \u00A0\t\n\r?!,:;.()\u2014\"";
    private static final String WRAPPED_DELIMITERS = "\'"; // delimiters need to wrapped with white spaces


    public WhiteSpaceTokenizer() {
    }

    @Override
    public List<TokenElement> tokenize(String content) {
        List<TokenElement> tokens = new ArrayList<>();

        String surface = "";
        int offset = 0;
        List<String> tags = new ArrayList<>();

        for (int i = 0, l = content.length(); i < l; i++) {
            char ch = content.charAt(i);
            if ((DELIMITERS.indexOf(ch) != -1) || isWrappedDelimiters(content, i)) {
                if (isSuitableToken(surface)) {
                    tokens.add(new TokenElement(surface, tags, offset));
                }
                if (!isWhitespace(ch) && ch != '\u00A0') {
                    tokens.add(new TokenElement(String.valueOf(ch), tags, i));
                }
                surface = "";
                offset = -1;
            } else {
                if (offset < 0) {
                    offset = i;
                }
                surface += ch;
            }
        }

        if (isSuitableToken(surface)) {
            tokens.add(new TokenElement(surface, tags, offset));
        }

        return tokens;
    }

    private boolean isWrappedDelimiters(String content, int i) {
        Character ch = content.charAt(i);
        return WRAPPED_DELIMITERS.indexOf(ch) != -1 && (
                    (i > 0 && content.charAt(i - 1) == ' ') ||
                    (i < content.length() - 1 && content.charAt(i + 1) == ' ')
                );
    }

    private boolean isSuitableToken(String surface) {

        if (surface.isEmpty()) {
            return false;
        }

        for (Pattern pattern : BLACKLIST_TOKEN_PATTERNS) {
            if (pattern.matcher(surface).find()) {
                return false;
            }
        }
        return true;
    }
}

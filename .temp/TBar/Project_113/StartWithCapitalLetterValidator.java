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
package cc.redpen.validator.sentence;

import cc.redpen.model.Sentence;
import cc.redpen.tokenizer.TokenElement;
import cc.redpen.validator.DictionaryValidator;

import java.util.List;
import java.util.Locale;

import static java.util.Collections.singletonList;

/**
 * Check if the input sentence start with a capital letter.
 */
final public class StartWithCapitalLetterValidator extends DictionaryValidator {
    public StartWithCapitalLetterValidator() {
        super("capital-letter-exception-list/capital-case-exception-list");
    }

    @Override
    public List<String> getSupportedLanguages() {
        return singletonList(Locale.ENGLISH.getLanguage());
    }

    @Override
    public void validate(Sentence sentence) {
        String content = sentence.getContent();
        List<TokenElement> tokens = sentence.getTokens();
        String headWord = "";
        for (TokenElement token : tokens) {
            if (!token.getSurface().equals("")) { // skip white space
                headWord = token.getSurface();
                break;
            }
        }

        if (tokens.size() == 0 || inDictionary(headWord)) {
            return;
        }

        char headChar = '≡';
        for (char ch : content.toCharArray()) {
            if (ch != ' ') {
                headChar = ch;
                break;
            }
        }

        if (headChar == '≡') {
            return;
        }

        if (Character.isLowerCase(headChar)) {
            addLocalizedError(sentence, headChar);
        }
    }
}

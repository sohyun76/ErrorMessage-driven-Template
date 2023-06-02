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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpellingValidator extends SpellingDictionaryValidator {

    private Map<String, List<TokenElement>> words = new HashMap<>();

    @Override
    public void validate(Sentence sentence) {
        for (TokenElement token : sentence.getTokens()) {
            String surface = token.getSurface().toLowerCase();
            if (surface.length() == 0 || surface.matches("\\P{L}+")) continue;

            if (dictionaryExists() && !inDictionary(surface)) {
                addLocalizedErrorFromToken(sentence, token);
            }
        }
    }
}

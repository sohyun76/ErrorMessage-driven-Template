/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.validator.section;

import cc.redpen.model.Paragraph;
import cc.redpen.model.Section;
import cc.redpen.model.Sentence;
import cc.redpen.tokenizer.TokenElement;
import cc.redpen.validator.Validator;

import java.util.*;

/**
 * DuplicatedSectionValidator check if there are highly similar section pairs.
 */
final public class DuplicatedSectionValidator extends Validator {
    private List<SectionVector> sectionVectors = new ArrayList<>();

    class SectionVector {
        public final Sentence header;
        public final Map<String, Integer> sectionVector;

        public SectionVector(Sentence header, Map<String, Integer> vector) {
            this.header = header;
            this.sectionVector = vector;
        }

        @Override
        public String toString() {
            return "SectionVector{" +
                    "header=" + header +
                    ", sectionVector=" + sectionVector +
                    '}';
        }
    }

    public DuplicatedSectionValidator() {
        super("threshold", 0.9f); // Default threshold (Cosine similarity).
    }

    @Override
    public void preValidate(Section section) {
        Map<String, Integer> sectionVector = extractWordFrequency(section);
        this.sectionVectors.add(new SectionVector(section.getHeaderContent(0), sectionVector));
    }

    private Map<String, Integer> extractWordFrequency(Section section) {
        Map<String, Integer> sectionVector = new HashMap<>();
        for (Paragraph paragraph : section.getParagraphs()) {
            for (Sentence sentence : paragraph.getSentences()) {
                addWords(sectionVector, sentence);
            }
        }
        // apply to sentences in section header
        for (Sentence headerSentence : section.getHeaderContents()) {
            addWords(sectionVector, headerSentence);
        }
        return sectionVector;
    }

    @Override
    public void validate(Section section) {
        Map<String, Integer> targetVector = extractWordFrequency(section);
        for (SectionVector sectionVector : sectionVectors) {
            Map<String, Integer> candidateVector = sectionVector.sectionVector;
            // NOTE: not header.equals() since the we need check if the references are identical
            if (sectionVector.header != section.getHeaderContent(0) &&
                    calcCosine(targetVector, candidateVector) > getFloat("threshold")) {
                Optional<Sentence> header = Optional.ofNullable(section.getHeaderContent(0));
                //NOTE: without the following information, addLocaledError cannot create an error.
                //FIXME: ideally document.builder should take a responsibility not to have void paragraph and header.
                if (section.getNumberOfParagraphs() == 0 || sectionVector.header == null) {
                    continue;
                }
                addLocalizedError(header.orElse(section.getParagraph(0).getSentence(0)),
                        sectionVector.header.getLineNumber());
            }
        }
    }

    private double calcCosine(Map<String, Integer> targetVector,
            Map<String, Integer> argumentVector) {
        int innerProduct = 0;
        int targetVecLen = calcLength(targetVector);
        int argumentVecLen = calcLength(argumentVector);

        if (targetVecLen == 0 || argumentVecLen == 0) {
            return 0.0;
        }

        for (String word : targetVector.keySet()) {
            if (argumentVector.containsKey(word)) {
                innerProduct += targetVector.get(word) * argumentVector.get(word);
            }
        }
        return innerProduct / (Math.sqrt(targetVecLen) * Math.sqrt(argumentVecLen));
    }

    private int calcLength(Map<String, Integer> targetVector) {
        int length = 0;
        for (String word : targetVector.keySet()) {
            length += targetVector.get(word) * targetVector.get(word);
        }
        return length;
    }

    private void addWords(Map<String, Integer> sectionVector, Sentence sentence) {
        for (TokenElement token : sentence.getTokens()) {
            String surface = token.getSurface();
            if (!sectionVector.containsKey(surface)){
                sectionVector.put(surface, 0);
            }
            Integer currentNum = sectionVector.get(surface);
            sectionVector.put(surface, ++currentNum);
        }
    }
}

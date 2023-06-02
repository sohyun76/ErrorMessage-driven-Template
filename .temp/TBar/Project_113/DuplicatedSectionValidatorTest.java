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

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Document;
import cc.redpen.model.Sentence;
import cc.redpen.validator.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicatedSectionValidatorTest {

    @Test
    void testDetectDuplicatedSection() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("DuplicatedSection"))
                .build();

        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addSectionHeader("this is header")
                        .addParagraph()
                        .addSentence(new Sentence("he is a super man.", 1))
                        .addSection(1)
                        .addSectionHeader("this is header 2")
                        .addParagraph()
                        .addSentence(new Sentence("he is a super man.", 2))
                        .build());

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(2, errors.get(documents.get(0)).size());
    }

    @Test
    void testDetectNonDuplicatedSection() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("DuplicatedSection"))
                .build();

        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addSectionHeader("foobar")
                        .addParagraph()
                        .addSentence(new Sentence("baz baz baz", 1))
                        .addSection(1)
                        .addSectionHeader("aho")
                        .addParagraph()
                        .addSentence(new Sentence("zoo zoo zoo", 2))
                        .build());

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }

    @Test
    void testDetectDuplicatedSectionWithSameHeader() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("DuplicatedSection"))
                .build();

        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addSectionHeader("this is header.")
                        .addParagraph()
                        .addSentence(new Sentence("he is a super man.", 1))
                        .addSection(1)
                        .addSectionHeader("this is header.")
                        .addParagraph()
                        .addSentence(new Sentence("he is a super man.", 2))
                        .build());

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(2, errors.get(documents.get(0)).size());
    }

    @Test
    void testDetectNonDuplicatedSectionWithLowThreshold() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("DuplicatedSection").addProperty("threshold", "0.0"))
                .build();

        List<Document> documents = new ArrayList<>();
        // create two section which contains only one same word, "baz"
        documents.add(
        Document.builder()
                .addSection(1)
                .addSectionHeader("foobar")
                .addParagraph()
                .addSentence(new Sentence("baz foo foo", 1))
                .addSection(1)
                .addSectionHeader("aho")
                .addParagraph()
                .addSentence(new Sentence("baz zoo zoo", 2))
                .build());

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(2, errors.get(documents.get(0)).size());
    }

    @Test
    void testEmpty() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("DuplicatedSection"))
                .build();

        List<Document> documents = new ArrayList<>();
        // create two section which contains only one same word, "baz"
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addParagraph()
                        .addSentence(new Sentence("baz foo foo", 1))
                        .addSection(1)
                        .addSectionHeader("foobar")
                        .addParagraph().addSentence(new Sentence("baz foo foo", 1))
                        .build());

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(1, errors.get(documents.get(0)).size());
    }

    @Test
    void testEmpty2() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("DuplicatedSection").addProperty("threshold", "0.0"))
                .build();

        List<Document> documents = new ArrayList<>();
        // create two section which contains only one same word, "baz"
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addParagraph()
                        .addSentence("baz foo foo", 2)
                        .addSection(2)
                        .addSectionHeader("baz foo foo")
                        .addParagraph()
                        .build());

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(1, errors.get(documents.get(0)).size());
    }
}

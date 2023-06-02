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
package cc.redpen.validator.sentence;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Document;
import cc.redpen.model.Sentence;
import cc.redpen.parser.LineOffset;
import cc.redpen.tokenizer.NeologdJapaneseTokenizer;
import cc.redpen.validator.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cc.redpen.config.SymbolType.COMMA;
import static cc.redpen.config.SymbolType.EXCLAMATION_MARK;
import static cc.redpen.config.SymbolType.FULL_STOP;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidSymbolValidatorTest {
    @Test
    void testWithInvalidSymbol() throws RedPenException {
        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder(new NeologdJapaneseTokenizer())
                        .addSection(1)
                        .addParagraph()
                        .addSentence(new Sentence("わたしはカラオケが大好き！", 1))
                        .build());

        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .addSymbol(new Symbol(EXCLAMATION_MARK, '!', "！"))
                .build();

        RedPen redPen = new RedPen(conf);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(1, errors.get(documents.get(0)).size());
        assertEquals(Optional.of(new LineOffset(1, 12)), errors.get(documents.get(0)).get(0).getStartPosition());
        assertEquals(Optional.of(new LineOffset(1, 13)), errors.get(documents.get(0)).get(0).getEndPosition());
    }

    @Test
    void testWithoutInvalidSymbol() throws RedPenException {
        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addParagraph()
                        .addSentence(new Sentence("I like Karaoke", 1))
                        .build());

        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .addSymbol(new Symbol(EXCLAMATION_MARK, '!', "！"))
                .build();

        RedPen redPen = new RedPen(conf);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }

    @Test
    void testWithoutMultipleInvalidSymbol() throws RedPenException {

        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addParagraph()
                        .addSentence(new Sentence("わたしは、カラオケが大好き！", 1)) // NOTE: two invalid symbols
                        .build());

        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .addSymbol(new Symbol(EXCLAMATION_MARK, '!', "！"))
                .addSymbol(new Symbol(COMMA, ',', "、"))
                .build();

        RedPen redPen = new RedPen(conf);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(2, errors.get(documents.get(0)).size());
    }

    @Test
    void testFloatingNumber() throws RedPenException {

        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addParagraph()
                        .addSentence(new Sentence("Ubuntu v1.6 はいいね。", 1))
                        .build());

        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .addSymbol(new Symbol(FULL_STOP, '。', "."))
                .build();

        RedPen redPen = new RedPen(conf);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }

    @Test
    void testFloatingNumberInTheEndOfSentence() throws RedPenException {

        List<Document> documents = new ArrayList<>();
        documents.add(
                Document.builder()
                        .addSection(1)
                        .addParagraph()
                        .addSentence(new Sentence("私が好きな OS は Ubuntu v1.6。", 1))
                        .build());

        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .addSymbol(new Symbol(FULL_STOP, '。', "."))
                .build();

        RedPen redPen = new RedPen(conf);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }

    @Test
    void testDefaultJapaneseSetting() throws Exception {
        List<Document> documents = new ArrayList<>();
        documents.add(Document.builder(new NeologdJapaneseTokenizer())
                              .addSection(1)
                              .addParagraph()
                              .addSentence(new Sentence("Re:VIEW フォーマットが好きだ。", 1)).build());
        Configuration config = Configuration.builder("ja")
                               .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol")).build();

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }
}

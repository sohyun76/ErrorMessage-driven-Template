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
package cc.redpen.model;

import cc.redpen.tokenizer.NeologdJapaneseTokenizer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {
    @Test
    void testCreateDocument() {
        Document doc = Document.builder()
                        .setFileName("Foobar").build();
        assertEquals(0, doc.size());
    }

    @Test
    void testCreateDocumentWithList() {
        Document doc = Document.builder()
                .setFileName("Foobar")
                .addSection(0)
                .addSectionHeader("baz")
                .addParagraph()
                .addSentence(new Sentence("sentence0", 1))
                .addSentence(new Sentence("sentence1", 2))
                .addListBlock()
                .addListElement(0, "list0")
                .addListElement(0, "list1")
                .addListElement(1, "list2")
                .build();

        assertEquals(1, doc.size());
        assertEquals(0, doc.getSection(0).getLevel());
        assertEquals(Optional.of("Foobar"), doc.getFileName());
        assertEquals("baz", doc.getSection(0).getHeaderContent(0).getContent());
        assertEquals(1, doc.getSection(0).getNumberOfParagraphs());
        assertEquals("sentence0", doc.getSection(0).getParagraph(0).getSentence(0).getContent());
        assertEquals(true, doc.getSection(0).getParagraph(0).getSentence(0).isFirstSentence());
        assertEquals(1, doc.getSection(0).getParagraph(0).getSentence(0).getLineNumber());
        assertEquals("sentence1", doc.getSection(0).getParagraph(0).getSentence(1).getContent());
        assertEquals(false, doc.getSection(0).getParagraph(0).getSentence(1).isFirstSentence());
        assertEquals(2, doc.getSection(0).getParagraph(0).getSentence(1).getLineNumber());
        assertEquals(1, doc.getSection(0).getNumberOfLists());
        assertEquals(3, doc.getSection(0).getListBlock(0).getNumberOfListElements());
        assertEquals(0, doc.getSection(0).getListBlock(0).getListElement(0).getLevel());
        assertEquals("list0", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals(0, doc.getSection(0).getListBlock(0).getListElement(1).getLevel());
        assertEquals("list1", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(2).getLevel());
        assertEquals("list2", doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getContent());
    }

    @Test
    void testSentenceIsTokenized() {
        Document doc = Document.builder()
                .setFileName("foobar")
                .addSection(0)
                .addSectionHeader("baz")
                .addParagraph()
                .addSentence(new Sentence("This is a foobar.", 1))
                .build();
        assertEquals(1, doc.size());
        assertEquals(5, doc.getSection(0).getParagraph(0).getSentence(0).getTokens().size());
    }

    @Test
    void testJapaneseSentenceIsTokenized() {
        Document doc = Document.builder(new NeologdJapaneseTokenizer())
                .setFileName("今日")
                .addSection(0)
                .addSectionHeader("天気")
                .addParagraph()
                .addSentence(new Sentence("今日は晴天だ。", 1))
                .build();
        assertEquals(1, doc.size());
        assertEquals(5, doc.getSection(0).getParagraph(0).getSentence(0).getTokens().size());
    }

    void testCreateParagraphBeforeSection() {
        Document document = Document.builder()
                .setFileName("Foobar")
                .addParagraph()
                .addSection(1)
                .build();
        assertEquals(2,document.size());
        assertEquals(0, document.getSection(0).getLevel());
        assertEquals(1, document.getSection(1).getLevel());
    }

    @Test
    void testCreateListBlockBeforeSection() {
        assertThrows(IllegalStateException.class, ()->{
            Document.builder()
                    .setFileName("Foobar")
                    .addListBlock()
                    .addSection(0)
                    .build();
        });
    }

    @Test
    void testCreateListElementBeforeListBlock() {
        assertThrows(IllegalStateException.class, ()-> {
            Document.builder()
                    .setFileName("Foobar")
                    .addListElement(0, "foo")
                    .addListBlock()
                    .build();
        });
    }
}

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

import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.model.Document;
import cc.redpen.model.Section;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.SentenceExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class ReSTParserTest {
    @Test
    void testSections() {
        String sampleText = "" +
                "sub section\n" +
                "-----------\n" +
                "\n" +
                "blah\n" +
                "\n" +
                "subsub section\n" +
                "~~~~~~~~~~~~~~\n" +
                "\n" +
                "blah blah\n" +
                "\n" +
                "subsubsub section\n" +
                "^^^^^^^^^^^^^^^^^\n" +
                "\n" +
                "blah blah blah";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(3, doc.size());

        final Section firstSection = doc.getSection(0);
        assertEquals(1, firstSection.getHeaderContentsListSize());
        assertEquals("sub section", firstSection.getHeaderContent(0).getContent());
        assertEquals("blah", firstSection.getParagraph(0).getSentence(0).getContent());

        final Section secondSection = doc.getSection(1);
        assertEquals(1, firstSection.getHeaderContentsListSize());
        assertEquals("subsub section", secondSection.getHeaderContent(0).getContent());
        assertEquals("blah blah", secondSection.getParagraph(0).getSentence(0).getContent());

        final Section thirdSection = doc.getSection(2);
        assertEquals(1, thirdSection.getHeaderContentsListSize());
        assertEquals("subsubsub section", thirdSection.getHeaderContent(0).getContent());
        assertEquals("blah blah blah", thirdSection.getParagraph(0).getSentence(0).getContent());
    }


    @Test
    void testLists() {
        String sampleText = "There are several railway companies in Japan as follows.\n";
        sampleText += "\n";
        sampleText += "* Tokyu\n";
        sampleText += "  * Toyoko Line\n";
        sampleText += "  * Denentoshi Line\n";
        sampleText += "* Keio\n";
        sampleText += "  * Odakyu\n";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        assertEquals(5, doc.getSection(0).getListBlock(0).getNumberOfListElements());
        assertEquals("Tokyu", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(0).getLevel());
        assertEquals(3, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getStartPositionOffset());

        assertEquals("Toyoko Line", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(1).getLevel());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getLineNumber());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getStartPositionOffset());

        assertEquals("Denentoshi Line", doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(2).getLevel());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getLineNumber());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getStartPositionOffset());

        assertEquals("Keio", doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(3).getLevel());
        assertEquals(6, doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getStartPositionOffset());

        assertEquals("Odakyu", doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(4).getLevel());
        assertEquals(7, doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getLineNumber());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getStartPositionOffset());
    }

    @Test
    void testNumberedLists() {
        String sampleText = "There are several railway companies in Japan as follows.\n";
        sampleText += "\n";
        sampleText += "1. Tokyu\n";
        sampleText += "  1. Toyoko Line\n";
        sampleText += "  2. Denentoshi Line\n";
        sampleText += "1. Keio\n";
        sampleText += "  1. Odakyu\n";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        assertEquals(5, doc.getSection(0).getListBlock(0).getNumberOfListElements());
        assertEquals("Tokyu", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(0).getLevel());
        assertEquals(3, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getLineNumber());
        assertEquals(3, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getStartPositionOffset());

        assertEquals("Toyoko Line", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(1).getLevel());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getLineNumber());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getStartPositionOffset());

        assertEquals("Denentoshi Line", doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(2).getLevel());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getLineNumber());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getStartPositionOffset());

        assertEquals("Keio", doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(3).getLevel());
        assertEquals(6, doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getLineNumber());
        assertEquals(3, doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getStartPositionOffset());

        assertEquals("Odakyu", doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(4).getLevel());
        assertEquals(7, doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getLineNumber());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getStartPositionOffset());
    }

    @Test
    void testDefinitionLists() {
        String sampleText = "There are several railway companies in Japan as follows.\n";
        sampleText += "\n";
        sampleText += "Tokyu\n";
        sampleText += "  Toyoko Line\n";
        sampleText += "  Denentoshi Line\n";
        sampleText += "\n";
        sampleText += "Keio\n";
        sampleText += "  Odakyu\n";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());
        assertEquals(3, doc.getSection(0).getListBlock(0).getNumberOfListElements());

        assertEquals("Toyoko Line", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(0).getLevel());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getStartPositionOffset());

        assertEquals("Denentoshi Line", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(1).getLevel());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getStartPositionOffset());

        assertEquals("Odakyu", doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(2).getLevel());
        assertEquals(8, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getStartPositionOffset());
    }

    @Test
    void testNormalTables() {
        String sampleText = "Before table.\n";
        sampleText += "\n";
        sampleText += "+------------------------+------------+----------+----------+\n" +
                "| Header row, column 1   | Header 2   | Header 3 | Header 4 |\n" +
                "| (header rows optional) |            |          |          |\n" +
                "+========================+============+==========+==========+\n" +
                "| body row 1, column 1   | column 2   | column 3 | column 4 |\n" +
                "+------------------------+------------+----------+----------+\n" +
                "| body row 2             | ...        | ...      |          |\n" +
                "+------------------------+------------+----------+----------+\n" +
                "\n" +
                "Finished table yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(2, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before table.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("Finished table yay!", section.getParagraph(1).getSentence(0).getContent());
    }

    @Test
    void testCSVTables() {
        String sampleText = "Before table.\n";
        sampleText += "\n";
        sampleText += "=====  =====  =======\n" +
                      "A      B      A and B\n" +
                      "=====  =====  =======\n" +
                      "False  False  False\n" +
                      "True   False  False\n" +
                      "=====  =====  =======\n" +
                      "\n" +
                      "Finished table yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(2, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before table.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("Finished table yay!", section.getParagraph(1).getSentence(0).getContent());
    }

    @Test
    void testDirectives() {
        String sampleText = "Before directive.\n";
        sampleText += "\n";
        sampleText +=
                ".. code-block:: python\n" +
                "   :emphasize-lines: 4,5\n" +
                "\n" +
                "   def function():\n" +
                "       interesting = True\n" +
                "       print('This is a very important function.')\n" +
                "\n" +
                "Finished a directive yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(2, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before directive.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("Finished a directive yay!", section.getParagraph(1).getSentence(0).getContent());
    }

    @Test
    void testNewLineStyle() {
        String sampleText = "Before directive.\n";
        sampleText += "| This is is a sentence.\n";
        sampleText += "| This is is another sentence.\n";
        Document doc = createFileContent(sampleText);
        assertEquals(1, doc.size());
        Section section = doc.getSection(0);
        assertEquals(1, section.getParagraphs().size());
        assertEquals(3, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before directive.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(" This is is a sentence.", section.getParagraph(0).getSentence(1).getContent());
        assertEquals(" This is is another sentence.", section.getParagraph(0).getSentence(2).getContent());
    }

    @Test
    void testComments() {
        String sampleText = "Before comments.\n";
        sampleText += "\n";
        sampleText +=
                ".. This is a comment.\n" +
                "   And still this is also a comment.\n" +
                "\n" +
                "Finished a comment yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(2, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before comments.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("Finished a comment yay!", section.getParagraph(1).getSentence(0).getContent());
    }

    @Test
    void testLiterals() {
        String sampleText = "Before literals.\n";
        sampleText += "\n";
        sampleText += "::\n" +
                "\n" +
                "   This is in a literal block.\n" +
                "\n" +
                "Finished a literal yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(2, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before literals.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("Finished a literal yay!", section.getParagraph(1).getSentence(0).getContent());
    }

    @Test
    void testLineBlock() {
        String sampleText = "Before line block.\n";
        sampleText += "\n";
        sampleText += "::\n" +
                "\n" +
                "| This is in a line block.\n" +
                "| This is also in a line block.\n" +
                "\n" +
                "Finished a line block yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(2, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before line block.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("Finished a line block yay!", section.getParagraph(1).getSentence(0).getContent());
    }


    @Test
    void testFootnote() {
        String sampleText =
                "Before footnote.\n" +
                        "\n" +
                        "This power is gravity[*]_.\n" +
                        "\n" +
                        ".. [*] Gravity is blah blah\n" +
                        "\n" +
                        "Finished a footnote yay!";

        Document doc = createFileContent(sampleText);
        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        Section section = doc.getSection(0);
        assertEquals(4, section.getParagraphs().size());
        assertEquals(1, section.getParagraph(0).getNumberOfSentences());
        assertEquals("Before footnote.", section.getParagraph(0).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(1).getNumberOfSentences());
        assertEquals("This power is gravity.", section.getParagraph(1).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(2).getNumberOfSentences());
        assertEquals("Gravity is blah blah", section.getParagraph(2).getSentence(0).getContent());
        assertEquals(1, section.getParagraph(3).getNumberOfSentences());
        assertEquals("Finished a footnote yay!", section.getParagraph(3).getSentence(0).getContent());
    }

    private Document createFileContent(String inputDocumentString) {
        DocumentParser parser = DocumentParser.REST;
        Document doc = null;
        try {
            Configuration configuration = Configuration.builder().build();
            doc = parser.parse(
                    inputDocumentString,
                    new SentenceExtractor(configuration.getSymbolTable()),
                    configuration.getTokenizer());
        } catch (RedPenException e) {
            e.printStackTrace();
            fail("Exception not expected.");
        }
        return doc;
    }
}

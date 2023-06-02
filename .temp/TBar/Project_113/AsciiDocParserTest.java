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
package cc.redpen;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.*;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.LineOffset;
import cc.redpen.parser.SentenceExtractor;
import cc.redpen.validator.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class AsciiDocParserTest {
    private static final Logger LOG = LoggerFactory.getLogger(AsciiDocParserTest.class);

    @BeforeEach
    void setup() {
    }

    @Test
    void testNullDocument() throws Exception {
        assertThrows(NullPointerException.class, ()-> {
            Configuration configuration = Configuration.builder().build();
            DocumentParser parser = DocumentParser.ASCIIDOC;
            InputStream is = null;
            parser.parse(is, new SentenceExtractor(configuration.getSymbolTable()), configuration.getTokenizer());
        });
    }


    @Test
    void testBasicDocument() throws UnsupportedEncodingException, RedPenException {
        String sampleText = "Instances Overview\n==================\n" + "Author's Name <person@email.address>\nv1.2, 2015-08\n" +
            "\nThis is the optional preamble (an untitled section body). Useful for " +
            "writing simple sectionless documents consisting only of a preamble.\n\n" +

            "NOTE: The abstract, preface, appendix, bibliography, glossary and index section titles are significant ('specialsections').\n" +

            "\n\n:numbered!:\n[abstract]\n" +
            "Instances\n" +
            "---------\n" +
            "In this article, we'll call a computer server that works as a member of a cluster an _instan3ce_. " +
            "for example, as shown in this http://redpen.ignored.url/[mishpelled link], each instance in distributed search engines stores the the fractions of data.\n" +
            "\nSuch distriubuted systems need a component to merge the preliminary results from member instnaces.\n\n\n" +
            ".Instance image\n" +
            "image::images/tiger.png[Instance image]\n\n" +
            "A sample table:\n\n" +
            ".A sample table\n" +
            "[width=\"60%\",options=\"header\"]\n" +
            "|==============================================\n" +
            "| Option     | Description\n" +
            "| GROUP      | The instance group.\n" +
            "|==============================================\n\n" +
            ".example list\n" +
            "===============================================\n" +
            "Lorum ipum...\n" +
            "===============================================\n\n\n" +
            "[bibliography]\n" +
            "- [[[taoup]]] Eric Steven Raymond. 'The Art of Unix\n" +
            "  Programming'. Addison-Wesley. ISBN 0-13-142901-9.\n" +
            "- [[[walsh-muellner]]] Norman Walsh & Leonard Muellner.\n" +
            "  'DocBook - The Definitive Guide'. O'Reilly & Associates. 1999.\n" +
            "  ISBN 1-56592-580-7.\n\n\n" +
            "[glossary]\n" +
            "Example Glossary\n" +
            "----------------\n" +
            "Glossaries are optional. Glossaries entries are an example of a style\n" +
            "of AsciiDoc labeled lists.\n\n" +
            "[glossary]\n" +
            "A glossary term::\n" +
            "  The corresponding (indented) definition.\n\n" +
            "A second glossary term::\n" +
            "  The corresponding (indented) definition.\n\n\n" +
            "ifdef::backend-docbook[]\n" +
            "[index]\n" +
            "Example Index\n" +
            "-------------\n" +
            "////////////////////////////////////////////////////////////////\n" +
            "The index is normally left completely empty, it's contents being\n" +
            "generated automatically by the DocBook toolchain.\n" +
            "////////////////////////////////////////////////////////////////\n" +
            "endif::backend-docbook[]";


        Document doc = createFileContent(sampleText);

        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> assertNotNull(sentence.getContent()));
                section.getHeaderContents().forEach(sentence -> assertNotNull(sentence.getContent()));
                for (ListBlock listBlock : section.getListBlocks()) {
                    for (ListElement listElement : listBlock.getListElements()) {
                        listElement.getSentences().forEach(sentence -> assertNotNull(sentence.getContent()));
                    }
                }
            }
        }

        assertNotNull(doc, "doc is null");
        assertEquals(4, doc.size());

        final Section firstSection = doc.getSection(0);
        assertEquals(1, firstSection.getHeaderContentsListSize());
        assertEquals("Instances Overview", firstSection.getHeaderContent(0).getContent());
        assertEquals(0, firstSection.getNumberOfLists());
        assertEquals(2, firstSection.getNumberOfParagraphs());
        assertEquals(0, firstSection.getNumberOfSubsections());

        Configuration configuration = Configuration.builder()
            .addValidatorConfig(new ValidatorConfiguration("Spelling"))
            .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol")).build();

        RedPen redPen = new RedPen(configuration);
        List<ValidationError> errors = redPen.validate(doc);
    }

    @Test
    void testBlocks() throws UnsupportedEncodingException, RedPenException {
        String sampleText = "= Test Blocks\n" +
            "\n" +
            "The following block should be ignored.\n" +
            "\n" +
            "[source,fake]\n" +
            "----\n" +
            "Ingredient dashi = KitchenImplements.grate(Fish.KATSUO);\n" +
            "----\n" +
            "\n" +
            "This text is after the block. It appears on line ten.\n" +
            "\n" +
            "This text is after the text after the block. It appears on line twelve.";

        Document doc = createFileContent(sampleText);

        assertNotNull(doc, "doc is null");
        assertEquals(1, doc.size());

        final Section firstSection = doc.getSection(0);
        assertEquals(1, firstSection.getHeaderContentsListSize());
        assertEquals("Test Blocks", firstSection.getHeaderContent(0).getContent());

        assertEquals(3, firstSection.getNumberOfParagraphs());

        assertEquals(10, firstSection.getParagraph(1).getSentence(0).getLineNumber());
        assertEquals(12, firstSection.getParagraph(2).getSentence(0).getLineNumber());

    }

    @Test
    void testRemoveTextDecoration() throws UnsupportedEncodingException {
        String sampleText = "About _Gekioko_.\n";
        Document doc = createFileContent(sampleText);
        assertEquals(1, doc.size());
        assertEquals("About Gekioko.", doc.getSection(0).getParagraph(0).getSentence(0).getContent());
    }

    @Test
    void testSectionHeader() throws UnsupportedEncodingException {
        String sampleText = "= About _Gekioko_.\n\n" +
            "Gekioko means angry.";

        Document doc = createFileContent(sampleText);

        assertEquals(1, doc.size());
        assertEquals("About Gekioko.", doc.getSection(0).getHeaderContent(0).getContent());
    }

    @Test
    void testSectionHeaderOffsetPosition() throws UnsupportedEncodingException {
        String sampleText = "= About _Gekioko_.\n\n" +
            "Gekioko means angry.";

        Document doc = createFileContent(sampleText);
        assertEquals(1, doc.size());
        assertEquals(2, doc.getSection(0).getHeaderContent(0).getOffset(0).get().offset);
    }

    @Test
    void testWrappedSentence() throws UnsupportedEncodingException {
        String sampleText = "Tokyu is a good railway company. The company is reliable. In addition it\n";
        sampleText += "is rich. I like the company. However someone does not like it.";
        Document doc = createFileContent(sampleText);
        Section firstSections = doc.getSection(0);
        Paragraph firstParagraph = firstSections.getParagraph(0);
        assertEquals(5, firstParagraph.getNumberOfSentences());

        assertEquals("Tokyu is a good railway company.", doc.getSection(0).getParagraph(0).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getParagraph(0).getSentence(0).getLineNumber());
        assertEquals(0, doc.getSection(0).getParagraph(0).getSentence(0).getStartPositionOffset());

        assertEquals(" The company is reliable.", doc.getSection(0).getParagraph(0).getSentence(1).getContent());
        assertEquals(1, doc.getSection(0).getParagraph(0).getSentence(1).getLineNumber());
        assertEquals(32, doc.getSection(0).getParagraph(0).getSentence(1).getStartPositionOffset());

        assertEquals(" In addition it is rich.", doc.getSection(0).getParagraph(0).getSentence(2).getContent());
        assertEquals(1, doc.getSection(0).getParagraph(0).getSentence(2).getLineNumber());
        assertEquals(57, doc.getSection(0).getParagraph(0).getSentence(2).getStartPositionOffset());

        assertEquals(" I like the company.", doc.getSection(0).getParagraph(0).getSentence(3).getContent());
        assertEquals(2, doc.getSection(0).getParagraph(0).getSentence(3).getLineNumber());
        assertEquals(8, doc.getSection(0).getParagraph(0).getSentence(3).getStartPositionOffset());

        assertEquals(" However someone does not like it.", doc.getSection(0).getParagraph(0).getSentence(4).getContent());
        assertEquals(2, doc.getSection(0).getParagraph(0).getSentence(4).getLineNumber());
        assertEquals(28, doc.getSection(0).getParagraph(0).getSentence(4).getStartPositionOffset());
    }

    @Test
    void testGenerateDocumentWithTwoSentencesWithMultipleShortLine() {
        String sampleText = "Tokyu\n" +
            "is a good\n" +
            "railway company. But there\n" +
            "are competitors.";
        Document doc = createFileContent(sampleText);
        Section firstSections = doc.getSection(0);
        Paragraph firstParagraph = firstSections.getParagraph(0);
        assertEquals(2, firstParagraph.getNumberOfSentences());

        assertEquals("Tokyu is a good railway company.", doc.getSection(0).getParagraph(0).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getParagraph(0).getSentence(0).getLineNumber());
        assertEquals(0, doc.getSection(0).getParagraph(0).getSentence(0).getStartPositionOffset());
        assertEquals(32, doc.getSection(0).getParagraph(0).getSentence(0).getOffsetMapSize());

        assertEquals(" But there are competitors.", doc.getSection(0).getParagraph(0).getSentence(1).getContent());
        assertEquals(3, doc.getSection(0).getParagraph(0).getSentence(1).getLineNumber());
        assertEquals(16, doc.getSection(0).getParagraph(0).getSentence(1).getStartPositionOffset());
    }

    @Test
    void testMappingTableWithShortSentence() {
        String sampleText = "Tsu is a city.";
        Document doc = createFileContent(sampleText);
        Section firstSections = doc.getSection(0);
        Paragraph firstParagraph = firstSections.getParagraph(0);
        assertEquals(1, firstParagraph.getNumberOfSentences());
        assertEquals("Tsu is a city.", doc.getSection(0).getParagraph(0).getSentence(0).getContent());

        assertEquals(1, doc.getSection(0).getParagraph(0).getSentence(0).getLineNumber());
        assertEquals(0, doc.getSection(0).getParagraph(0).getSentence(0).getStartPositionOffset());
        assertEquals(doc.getSection(0).getParagraph(0).getSentence(0).getContent().length(),
            doc.getSection(0).getParagraph(0).getSentence(0).getOffsetMapSize());

    }

    @Test
    void testGenerateDocumentWithList() {
        String sampleText = "There are several railway companies in Japan as follows.\n";
        sampleText += "\n";
        sampleText += "* Tokyu\n";
        sampleText += "  **  Toyoko Line\n";
        sampleText += "** Denentoshi Line\n";
        sampleText += "* Keio\n";
        sampleText += "* Odakyu\n";

        Document doc = createFileContent(sampleText);
        assertEquals(5, doc.getSection(0).getListBlock(0).getNumberOfListElements());
        assertEquals("Tokyu", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(0).getLevel());
        assertEquals(3, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getStartPositionOffset());

        assertEquals("Toyoko Line", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(1).getLevel());
        assertEquals(4, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getLineNumber());
        assertEquals(6, doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getStartPositionOffset());

        assertEquals("Denentoshi Line", doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getContent());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(2).getLevel());
        assertEquals(5, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getLineNumber());
        assertEquals(3, doc.getSection(0).getListBlock(0).getListElement(2).getSentence(0).getStartPositionOffset());

        assertEquals("Keio", doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(3).getLevel());
        assertEquals(6, doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(3).getSentence(0).getStartPositionOffset());

        assertEquals("Odakyu", doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getContent());
        assertEquals(1, doc.getSection(0).getListBlock(0).getListElement(4).getLevel());
        assertEquals(7, doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getLineNumber());
        assertEquals(2, doc.getSection(0).getListBlock(0).getListElement(4).getSentence(0).getStartPositionOffset());

    }

    @Test
    void testLabelledList() {
        String sampleText = "= SampleDoc\n" +
            "v0.0.2, 2015-11-17\n" +
            ":last-update-label!:\n" +
            "\n" +
            "== 用語定義\n" +
            "ユビキタス言語を定義します。\n" +
            "\n" +
            "Some word::\n" +
            "なにかの意味をのせて用例をのせます。\n" +
            "\n" +
            "リリース::\n" +
            "ソフトウェアを顧客に提供することです。\n" +
            "\n" +
            "redpen::\n" +
            "RedPen はオープンソースの校正ツールです。RedPen は技術文書が文書規約に従って書かれているかを自動検査します。 現在の RedPen 日本語ドキュメントは十分検査されておりません。校正にはもう少々時間がかかる予定です。誤りなど見つかりましたら、https://github.com/redpen-cc/redpen-doc-ja に Issue 登録しておしらせ頂けると幸いです。";

        Document doc = createFileContent(sampleText);
        assertEquals(3, doc.getSection(1).getListBlock(0).getNumberOfListElements());
        assertEquals("なにかの意味をのせて用例をのせます。", doc.getSection(1).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals(15, doc.getSection(1).getListBlock(0).getListElement(2).getSentence(0).getLineNumber());

    }

    @Test
    void testEscapedMarkup() {
        String sampleText = "It is a \\*good* day.";
        Document doc = createFileContent(sampleText);
        Paragraph firstParagraph = doc.getSection(0).getParagraph(0);
        assertEquals("It is a *good* day.", firstParagraph.getSentence(0).getContent());

        // the offset of the 8th character (ie: *) is actually 9
        assertEquals(9, firstParagraph.getSentence(0).getOffset(8).get().offset);
    }

    @Test
    void testCommentsAndTables() {
        String sampleText = "// BLAH BLAH" +
                "\n" +
                "Potato" +
                "\n" +
                "|===\n" +
                "|Hex |RGB |CMYK nibble\n" +
                "\n" +
                "|ffffff または #ffffff asd asd\n" +
                "|[255,255,255]\n" +
                "|[0, 0, 0, 0] または [0, 0, 0, 0%]\n" +
                "|===\n" +
                "\n";
        Document doc = createFileContent(sampleText);

        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    assertEquals("Potato", sentence.getContent());
                });
            }
        }
    }

    @Test
    void testTableSimple() {
        String sampleText = "Potato" +
                "\n" +
                "|===" +
                "|Name of Column 1 |Name of Column 2 |Name of Column 3 \n" +
                "|Cell in column 1, row 1\n" +
                "|Cell in column 2, row 1\n" +
                "|Cell in column 3, row 1\n\n" +
                "|Cell in column 1, row 2\n" +
                "|Cell in column 2, row 2\n" +
                "|Cell in column 3, row 2\n" +
                "|===\n" +
                "\n";
        Document doc = createFileContent(sampleText);

        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    assertEquals("Potato", sentence.getContent());
                });
            }
        }
    }

    @Test
    void testTableMultiLineElement() {
        String sampleText = "\n" +
                "|===\n" +
                "|Name\n" +
                "|Category\n" +
                "|Description\n\n" +

                "|Firefox\n" +
                "|Browser\n" +
                "|Mozilla Firefox is an open-source web browser.\n" +
                "It's designed for standards compliance,\n" +
                "performance, portability.\n\n" +
                "|Arquillian" +
                "|Testing" +
                "|An innovative and highly extensible testing platform.\n" +
                "Empowers developers to easily create real, automated tests.\n" +
                "|===\n\n" +
                "Potato";

        Document doc = createFileContent(sampleText);

        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    assertEquals("Potato", sentence.getContent());
                });
            }
        }
    }

    @Test
    void testTableWithTitle() {
        String sampleText = "\n" +
                ".This is a title of Table.\n" +
                "|===\n" +
                "|Name\n" +
                "|Category\n" +
                "|Description\n\n" +

                "|Firefox\n" +
                "|Browser\n" +
                "|Mozilla Firefox is an open-source web browser.\n" +
                "It's designed for standards compliance,\n" +
                "performance, portability.\n\n" +
                "|Arquillian" +
                "|Testing" +
                "|An innovative and highly extensible testing platform.\n" +
                "Empowers developers to easily create real, automated tests.\n" +
                "|===\n\n";
        Document doc = createFileContent(sampleText);
        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    assertEquals("This is a title of Table.", sentence.getContent());
                });
            }
        }
    }


    @Test
    void testTableWithList() {
        String sampleText = "\n" +
        "[cols=\"2,2,5a\"]\n" +
        "|===\n" +
        "|Firefox\n" +
        "|Browser\n" +
        "|Mozilla Firefox is an open-source web browser.\n\n" +
        "It's designed for:\n\n" +
        "* standards compliance\n" +
        "* performance\n" +
        "* portability\n\n" +
        "http://getfirefox.com[Get Firefox]!\n" +
        "|===\n\n" +
        "Potato.";

        Document doc = createFileContent(sampleText);
        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    assertEquals("Potato.", sentence.getContent());
                });
            }
        }
    }

    @Test
    void testDocumentWithItalicWord() {
        String sampleText = "It is a *good* day.";
        Document doc = createFileContent(sampleText);
        Section firstSections = doc.getSection(0);
        Paragraph firstParagraph = firstSections.getParagraph(0);
        assertEquals("It is a good day.", firstParagraph.getSentence(0).getContent());
        List<LineOffset> expectedOffsets = initializeMappingTable(
            new LineOffset(1, 0),
            new LineOffset(1, 1),
            new LineOffset(1, 2),
            new LineOffset(1, 3),
            new LineOffset(1, 4),
            new LineOffset(1, 5),
            new LineOffset(1, 6),
            new LineOffset(1, 7),
            new LineOffset(1, 9),
            new LineOffset(1, 10),
            new LineOffset(1, 11),
            new LineOffset(1, 12),
            new LineOffset(1, 14),
            new LineOffset(1, 15),
            new LineOffset(1, 16),
            new LineOffset(1, 17),
            new LineOffset(1, 18));

        assertEquals(expectedOffsets.size(), firstParagraph.getSentence(0).getOffsetMapSize());
        for (int i = 0; i < expectedOffsets.size(); i++) {
            assertEquals(expectedOffsets.get(i), firstParagraph.getSentence(0).getOffset(i).get());
        }
    }

    @Test
    void testSampleDocuments() {
        checkForStrippedItems(createResourceContent("test_document_1.adoc", "en"));
        checkForStrippedItems(createResourceContent("test_document_2.adoc", "en"));
        checkForStrippedItems(createResourceContent("test_document_3.adoc", "ja"));
    }

    private void checkForStrippedItems(Document doc) {
        // some simple illegal symbol tests
        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    // should be no URLS
                    assertEquals(-1, sentence.getContent().indexOf("http://"), "Possible URL: " + sentence.getContent());
                    assertEquals(-1, sentence.getContent().indexOf("https://"), "Possible URL: " + sentence.getContent());
                    // should be no table markers
                    assertEquals(-1, sentence.getContent().indexOf("|="), "Possible table symbol: " + sentence.getContent());
                    // should be no heading symbols
                    assertEquals(-1, sentence.getContent().indexOf("----"), "Possible header symbol: " + sentence.getContent());
                    assertEquals(-1, sentence.getContent().indexOf("****"), "Possible header symbol: " + sentence.getContent());
                    assertEquals(-1, sentence.getContent().indexOf("==="), "Possible header symbol: " + sentence.getContent());
                });
            }
        }
    }

    @Test
    void testLabeledListBlock() {
        String sampleText = "\n" +
                "first term::\n" +
                "definition of first term\n" +
                "second term::\n" +
                "definition of second term\n";

        Document doc = createFileContent(sampleText);
        assertEquals(2, doc.getSection(0).getListBlock(0).getNumberOfListElements());
        assertEquals("definition of first term ", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals("definition of second term", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
    }

    @Test
    void testSingleLineLabeledListBlock() {
        String sampleText = "\n" +
                "first term:: definition of first term\n" +
                "second term:: definition of second term\n\n" +
                "Potato\n";

        Document doc = createFileContent(sampleText);
        assertEquals(2, doc.getSection(0).getListBlock(0).getNumberOfListElements());
        assertEquals("definition of first term", doc.getSection(0).getListBlock(0).getListElement(0).getSentence(0).getContent());
        assertEquals("definition of second term", doc.getSection(0).getListBlock(0).getListElement(1).getSentence(0).getContent());
        for (Section section : doc) {
            for (Paragraph paragraph : section.getParagraphs()) {
                paragraph.getSentences().forEach(sentence -> {
                    assertEquals("Potato", sentence.getContent());
                });
            }
        }
    }

    @Test
    void testSimpleImage() {
        String sampleText = "\n" +
                "image:images/logo.png[Company Logo]\n\n" +
                "Potato\n";
        Document doc = createFileContent(sampleText);
        assertEquals("Potato", doc.getLastSection().getParagraph(0).getSentence(0).getContent());
    }

    @Test
    void testImageWithAttributes() {
        String sampleText = "\n" +
                "image:images/logo.png[scaledwidth=\"75%\",alt=\"Company Logo\"]\n\n" +
                "Potato\n";
        Document doc = createFileContent(sampleText);
        assertEquals("Potato", doc.getLastSection().getParagraph(0).getSentence(0).getContent());
    }

    private Document createResourceContent(String filename) {
        return createResourceContent(filename, "en");
    }

    private Document createResourceContent(String filename, String lang) {
        DocumentParser parser = DocumentParser.ASCIIDOC;

        Document doc = null;
        try {
            InputStream in = new FileInputStream(this.getClass().getClassLoader().getResource("asciidoc/" + filename).getFile());
            Configuration configuration = Configuration.builder().setLanguage(lang).build();
            doc = parser.parse(
                in,
                new SentenceExtractor(configuration.getSymbolTable()),
                configuration.getTokenizer());
        } catch (Exception e) {
            e.printStackTrace();
            fail("exception is not expected.");
        }

        return doc;
    }

    private Document createFileContent(String inputDocumentString,
                                       Configuration config) {
        DocumentParser parser = DocumentParser.ASCIIDOC;

        try {
            return parser.parse(inputDocumentString, new SentenceExtractor(config.getSymbolTable()), config.getTokenizer());
        } catch (RedPenException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Document createFileContent(String inputDocumentString) {
        DocumentParser parser = DocumentParser.ASCIIDOC;
        Document doc = null;
        try {
            Configuration configuration = Configuration.builder().build();
            doc = parser.parse(
                inputDocumentString,
                new SentenceExtractor(configuration.getSymbolTable()),
                configuration.getTokenizer());
        } catch (RedPenException e) {
            e.printStackTrace();
            fail("Exception is not expected.");
        }
        return doc;
    }

    private static List<LineOffset> initializeMappingTable(LineOffset... offsets) {
        List<LineOffset> offsetTable = new ArrayList<>();
        for (LineOffset offset : offsets) {
            offsetTable.add(offset);
        }
        return offsetTable;
    }
}

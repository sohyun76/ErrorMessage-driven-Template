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
package cc.redpen.formatter;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Sentence;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.SentenceExtractor;
import cc.redpen.tokenizer.WhiteSpaceTokenizer;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.Validator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class XMLFormatterTest extends Validator {

    @Test
    void testConvertValidationError() throws RedPenException {
        List<ValidationError> errors = new ArrayList<>();
        setErrorList(errors);
        addLocalizedError(new Sentence("This is a sentence", 0));
        XMLFormatter formatter = new XMLFormatter();
        cc.redpen.model.Document document1 = new cc.redpen.model.Document.DocumentBuilder(new WhiteSpaceTokenizer())
                .setFileName("foobar.md").build();
        List<ValidationError> validationErrors = Arrays.asList(errors.get(0));
        String resultString = formatter.format(document1, validationErrors);

        Document document = extractDocument(resultString);
        assertEquals(1, document.getElementsByTagName("error").getLength());
        assertEquals(1, document.getElementsByTagName("message").getLength());
        assertEquals("Fatal Error",
                document.getElementsByTagName("message").item(0).getTextContent());
        assertEquals(1, document.getElementsByTagName("file").getLength());
        assertEquals("foobar.md",
                document.getElementsByTagName("file").item(0).getTextContent());
        assertEquals(1, document.getElementsByTagName("lineNum").getLength());
        assertEquals("0",
                document.getElementsByTagName("lineNum").item(0).getTextContent());
        assertEquals(1, document.getElementsByTagName("level").getLength());
        assertEquals("Error",
                document.getElementsByTagName("level").item(0).getTextContent());
        assertEquals(1, document.getElementsByTagName("sentence").getLength());
        assertEquals("This is a sentence",
                document.getElementsByTagName("sentence").item(0).getTextContent());
        assertEquals(1, document.getElementsByTagName("validator").getLength());
        assertEquals(this.getClass().getSimpleName(),
                document.getElementsByTagName("validator").item(0).getTextContent());
    }

    @Test
    void testConvertValidationErrorWithoutFileName() throws RedPenException {
        List<ValidationError> errors = new ArrayList<>();
        setErrorList(errors);
        addLocalizedError(new Sentence("text", 0));
        XMLFormatter formatter = new XMLFormatter();
        List<ValidationError> validationErrors = Arrays.asList(errors.get(0));
        String resultString = formatter.format(new cc.redpen.model.Document.DocumentBuilder(new WhiteSpaceTokenizer()).build(), validationErrors);

        Document document = extractDocument(resultString);
        assertEquals(1, document.getElementsByTagName("error").getLength());
        assertEquals(1, document.getElementsByTagName("message").getLength());
        assertEquals("Fatal Error",
                document.getElementsByTagName("message").item(0).getTextContent());
        assertEquals(0, document.getElementsByTagName("file").getLength());
        assertEquals(1, document.getElementsByTagName("lineNum").getLength());
        assertEquals(1, document.getElementsByTagName("level").getLength());
        assertEquals(1, document.getElementsByTagName("validator").getLength());
        assertEquals("Error",
                document.getElementsByTagName("level").item(0).getTextContent());
        assertEquals("0",
                document.getElementsByTagName("lineNum").item(0).getTextContent());
        assertEquals(this.getClass().getSimpleName(),
                document.getElementsByTagName("validator").item(0).getTextContent());
    }

    @Test
    void testConvertedValidationErrorWithErrorPosition() throws RedPenException {
        // TODO: shorten the procedure before getting formatter result.
        String sampleText = "This is a good day。\n"; // invalid end of sentence symbol
        Configuration conf = Configuration.builder().build();
        Configuration configuration = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .build();

        List<cc.redpen.model.Document> documents = new ArrayList<>();
        DocumentParser parser = DocumentParser.MARKDOWN;
        documents.add(parser.parse(sampleText,
                new SentenceExtractor(conf.getSymbolTable()), conf.getTokenizer()));
        RedPen redPen = new RedPen(configuration);
        List<ValidationError> errors = redPen.validate(documents).get(documents.get(0));

        XMLFormatter formatter = new XMLFormatter();
        String resultString = formatter.format(new cc.redpen.model.Document.DocumentBuilder(
                new WhiteSpaceTokenizer()).build(), errors);

        Document document = extractDocument(resultString);
        assertEquals(1, document.getElementsByTagName("error").getLength());
        assertEquals(1, document.getElementsByTagName("message").getLength());
        assertEquals(0, document.getElementsByTagName("file").getLength());
        assertEquals(1, document.getElementsByTagName("lineNum").getLength());
        assertEquals(1, document.getElementsByTagName("validator").getLength());
        assertEquals("1",
                document.getElementsByTagName("lineNum").item(0).getTextContent());
        assertEquals("InvalidSymbol",
                document.getElementsByTagName("validator").item(0).getTextContent());
        assertEquals("1,18",
                document.getElementsByTagName("errorStartPosition").item(0).getTextContent());
        assertEquals("1,19",
                document.getElementsByTagName("errorEndPosition").item(0).getTextContent());
    }

    @Test
    void testConvertedValidationErrorChangingLevel() throws RedPenException {
        String sampleText = "This is a good day。\n"; // invalid end of sentence symbol
        Configuration conf = Configuration.builder().build();
        Configuration configuration = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol").setLevel(ValidatorConfiguration.LEVEL.INFO))
                .build();

        List<cc.redpen.model.Document> documents = new ArrayList<>();
        DocumentParser parser = DocumentParser.MARKDOWN;
        documents.add(parser.parse(sampleText,
                new SentenceExtractor(conf.getSymbolTable()), conf.getTokenizer()));
        RedPen redPen = new RedPen(configuration);
        List<ValidationError> errors = redPen.validate(documents, "info").get(documents.get(0));

        XMLFormatter formatter = new XMLFormatter();
        String resultString = formatter.format(new cc.redpen.model.Document.DocumentBuilder(
                new WhiteSpaceTokenizer()).build(), errors);

        Document document = extractDocument(resultString);
        assertEquals("Info",
                document.getElementsByTagName("level").item(0).getTextContent());
    }

    private Document extractDocument(String resultString) {
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            fail("Exception not expected.");
            e.printStackTrace();
        }

        Document document = null;
        try {
            document = docBuilder.parse(new ByteArrayInputStream(resultString.getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            fail("Exception not expected.");
        }
        return document;
    }
}

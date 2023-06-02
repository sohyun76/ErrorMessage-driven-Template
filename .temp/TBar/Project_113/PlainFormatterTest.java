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

import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Document;
import cc.redpen.model.Sentence;
import cc.redpen.tokenizer.WhiteSpaceTokenizer;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.Validator;
import cc.redpen.validator.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlainFormatterTest extends Validator {
    @BeforeEach
    void setUp() throws Exception {
        formatter = new PlainFormatter();
    }

    @Test
    void testConvertValidationError() {
        List<ValidationError> errors = new ArrayList<>();
        setErrorList(errors);
        addLocalizedError(new Sentence("This is a sentence", 0));
        Document document = new cc.redpen.model.Document.DocumentBuilder(new WhiteSpaceTokenizer())
                .setFileName("foobar.md").build();
        List<ValidationError> validationErrors = Arrays.asList(errors.get(0));
        String resultString = formatter.format(document, validationErrors);
        assertEquals("foobar.md:0: ValidationError[PlainFormatterTest], plain test error at line: This is a sentence\n", resultString);
    }

    @Test
    void testConvertValidationErrorWithoutFileName() {
        List<ValidationError> errors = new ArrayList<>();
        setErrorList(errors);
        addLocalizedError(new Sentence("This is a sentence", 0));
        Document document = new cc.redpen.model.Document.DocumentBuilder(new WhiteSpaceTokenizer()).build();
        List<ValidationError> validationErrors = Arrays.asList(errors.get(0));
        String resultString = formatter.format(document, validationErrors);
        assertEquals("0: ValidationError[PlainFormatterTest], plain test error at line: This is a sentence\n", resultString);
    }

    @Test
    void testConvertValidationErrorChangingErrorLevel() throws RedPenException {
        Configuration config = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("SentenceLength").setLevel(ValidatorConfiguration.LEVEL.INFO)).build();
        Validator validator = ValidatorFactory.getInstance(config.getValidatorConfigs().get(0), config);
        Sentence sentence = new Sentence("This is a long long long long long long long long long long long long" +
                " long long long long long long long long long long long long long long long long long long long long sentence", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(sentence);
        Document document = new cc.redpen.model.Document.DocumentBuilder(new WhiteSpaceTokenizer()).build();
        List<ValidationError> validationErrors = Arrays.asList(errors.get(0));
        String resultString = formatter.format(document, validationErrors);
        assertTrue(resultString.indexOf("ValidationInfo[SentenceLength]") > 0 );
    }

    private Formatter formatter;
}

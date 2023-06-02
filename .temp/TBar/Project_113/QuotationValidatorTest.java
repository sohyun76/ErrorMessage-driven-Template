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

import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.config.Symbol;
import cc.redpen.config.SymbolType;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Sentence;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.Validator;
import cc.redpen.validator.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuotationValidatorTest {
    @Test
    void testDoubleQuotationMakrs() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said “That is true”.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testSingleQuotationMakrs() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ‘that is true’.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testDoubleQuotationMakrWithoutRight() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said “That is true.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testSingleQuotationMakrWithoutRight() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ‘that is true.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testDoubleQuotationMakrWithoutLeft() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said That is true”.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testSingleQuotationMakrkWithoutLeft() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said that is true’.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testExceptionCase() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I’m a jedi knight.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testQuotedExceptionCase() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("he said ‘I’m a jedi knight’.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testDoubleLeftSingleQuotationMakrk() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ‘that is true‘.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testDoubleLeftDoubleQuotationMakrk() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said “that is true.“", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testDoubleRightSingleQuotationMakrk() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ’that is true’.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testDoubleRightDoubleQuotationMakrk() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ”that is true”.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testAsciiExceptionCase() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", false))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);
        Sentence str = new Sentence("I'm a jedi knight.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testAsciiDoubleQuotationMakrk() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", false))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);
        Sentence str = new Sentence("I said \"that is true\".", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testNoQuotationMakrk() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", true))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);
        Sentence str = new Sentence("I said that is true.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testNoInput() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", true))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);
        Sentence str = new Sentence("", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testTwiceQuotations() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ‘that is true’ and not said ‘that is false’", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testOneOfFailureInTwiceQuotations() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said ‘that is true and not said ‘that is false’", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testLeftDoubleQuotationsWihtoutSpace() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said“that is true”.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testLeftAsciiDoubleQuotationsWihtoutSpace() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", true))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);
        Sentence str = new Sentence("I said\"that is true\".", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testRightDoubleQuotationsWihtoutSpace() throws RedPenException {
        Validator validator = ValidatorFactory.getInstance("Quotation");
        Sentence str = new Sentence("I said “that is true”is true.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testRightAsciiDoubleQuotationsWihtoutSpace() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", true))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);
        Sentence str = new Sentence("I said \"that is true\"is true.", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void testDoubleQuotationsWithNonAsciiPeriod() throws RedPenException {
        Configuration conf = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("Quotation").addProperty("use_ascii", true))
                .addSymbol(new Symbol(SymbolType.FULL_STOP, '。'))
                .build();
        Validator validator = ValidatorFactory.getInstance(conf.getValidatorConfigs().get(0), conf);

//        QuotationValidator validator =
//                new QuotationValidator(true, '。');
        Sentence str = new Sentence("I said \"that is true\"。", 0);
        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(str);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }
}

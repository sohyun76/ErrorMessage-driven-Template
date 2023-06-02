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
package cc.redpen.validator.document;

import cc.redpen.model.Document;
import cc.redpen.validator.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GappedSectionValidatorTest {
    private GappedSectionValidator validator = new GappedSectionValidator();

    @Test
    void testInvalid() {
        Document document =
                Document.builder()
                        .addSection(1)
                        .addSectionHeader("Chapter 1")
                        .addSection(3)
                        .addSectionHeader("Section 1.1.1")
                        .build();

        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(document);

        assertEquals(1, errors.size());
    }

    @Test
    void testValid() {
        Document document =
                Document.builder()
                        .addSection(1)
                        .addSectionHeader("Chapter 1")
                        .addSection(2)
                        .addSectionHeader("Section 1.1")
                        .addSection(3)
                        .addSectionHeader("Section 1.1.1")
                        .build();

        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(document);

        assertEquals(0, errors.size());
    }

    @Test
    void testFailureCase() {
        Document document =
                Document.builder()
                        .addSection(2)
                        .addSectionHeader("Section 1.1")
                        .addSection(3)
                        .addSectionHeader("Section 1.1.1")
                        .build();

        List<ValidationError> errors = new ArrayList<>();
        validator.setErrorList(errors);
        validator.validate(document);

        assertEquals(0, errors.size());
    }

}

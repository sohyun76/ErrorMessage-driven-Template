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
package cc.redpen.tokenizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenElementTest {
    @Test
    void testTagsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TokenElement token = new TokenElement("foobar", java.util.Arrays.asList("tag"), 0);
            List<String> tags = token.getTags();
            tags.add("baz");
        });
    }

    @Test
    void testSurfaceImmutable() {
        TokenElement token = new TokenElement("foobar", java.util.Arrays.asList("tag"), 0);
        String surface = token.getSurface();
        surface = "baz";
        assertEquals("foobar", token.getSurface());
    }
}

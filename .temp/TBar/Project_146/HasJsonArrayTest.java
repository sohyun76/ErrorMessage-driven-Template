/**
 * Copyright © 2010-2011 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.restdriver.matchers;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class HasJsonArrayTest {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private HasJsonArray matcher;
    
    @Before
    public void before() {
        matcher = new HasJsonArray("array", new WithSize(is(2)));
    }
    
    @Test
    public void matcherShouldDescribeItselfCorrectly() {
        Description description = new StringDescription();
        matcher.describeTo(description);
        
        assertThat(description.toString(), is("JsonNode with 'array' matching: A JSON array with size: is <2>"));
    }
    
    @Test
    public void matcherShouldFailWhenNodeDoesntContainFieldWithGivenName() {
        assertThat(matcher.matches(object("foo", new TextNode("bar"))), is(false));
    }
    
    @Test
    public void matcherShouldFailWhenAskedToMatchNonArrayNode() {
        assertThat(matcher.matches(object("array", new TextNode("foo"))), is(false));
    }
    
    @Test
    public void matcherShouldFailWhenArrayDoesNotMatch() {
        assertThat(matcher.matches(object("array", array("foobar"))), is(false));
    }
    
    @Test
    public void matcherShouldPassWhenArrayMatches() {
        assertThat(matcher.matches(object("array", array("foo", "bar"))), is(true));
    }
    
    private ObjectNode object(String name, JsonNode value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(name, value);
        return node;
    }
    
    private ArrayNode array(String... items) {
        ArrayNode node = MAPPER.createArrayNode();
        for (String item : items) {
            node.add(item);
        }
        return node;
    }
    
}

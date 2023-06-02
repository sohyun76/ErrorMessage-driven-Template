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
package cc.redpen.parser;

import cc.redpen.parser.asciidoc.AsciiDocParser;
import cc.redpen.parser.rest.ReSTParser;
import cc.redpen.parser.review.ReVIEWParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * This class wraps a buffered reader. It looks for preprocessor instructions in the input text and
 * converts this to a list of PreprocessorRules
 */
public class PreprocessingReader implements AutoCloseable {

    private BufferedReader reader;
    private Set<PreprocessorRule> preprocessorRules = new HashSet<>();
    private int lineNumber = 0;
    private PreprocessorRule lastRule = null;
    private DocumentParser parser = null;

    public PreprocessingReader(Reader reader, DocumentParser parser) {
        this.reader = new BufferedReader(reader);
        this.parser = parser;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public String readLine() throws IOException {
        String line = reader.readLine();
        lineNumber++;
        if (line == null) { return line; }

        String ruleText = line;
        if (parser instanceof AsciiDocParser) {
            if (ruleText.toLowerCase().startsWith("[suppress")) {
                addAsciiDocAttributeSuppressRule(ruleText);
                return "";
            }
        } else if (parser instanceof MarkdownParser) {
            if (ruleText.matches("^ *<!-- *@suppress (.*)-->")) {
                ruleText = line
                        .replaceAll("^ *<!--", "")
                        .replaceAll("-->", "")
                        .trim();
                addCommentSuppressRule(ruleText);
            }
        } else if (parser instanceof ReVIEWParser) {
            if (ruleText.matches("^#@# *@suppress(.*)")) {
                ruleText = line
                        .replaceAll("^#@#", "")
                        .trim();
                addCommentSuppressRule(ruleText);
            }
        } else if (parser instanceof LaTeXParser) {
            if (ruleText.matches("^% *@suppress(.*)")) {
                ruleText = line
                        .replaceAll("^%", "")
                        .trim();
                addCommentSuppressRule(ruleText);
            }
        }  else if (parser instanceof ReSTParser) {
            if (ruleText.matches("[.][.] *@suppress(.*)")) {
                ruleText = line
                        .replaceAll("[.][.]", "")
                        .trim();
                addCommentSuppressRule(ruleText, 3);
            }
        }
        return line;
    }

    private void addAsciiDocAttributeSuppressRule(String ruleString) {
        PreprocessorRule rule = new PreprocessorRule(PreprocessorRule.RuleType.SUPPRESS, lineNumber);
        if (lastRule != null) { lastRule.setLineNumberLimit(lineNumber); }
        lastRule = rule;
        String[] parts = ruleString.split("=");
        if (parts.length > 1) {
            String[] parameters = parts[1].replaceAll("[\"'\\]]", "").split(" ");
            for (String parameter : parameters) {
                if (!parameter.isEmpty()) {
                    rule.addParameter(parameter);
                }
            }
        }
        preprocessorRules.add(rule);
    }

    private void addCommentSuppressRule(String ruleString) {
        addCommentSuppressRule(ruleString, 0);
    }

    private void addCommentSuppressRule(String ruleString, int gap) {
        PreprocessorRule rule = new PreprocessorRule(PreprocessorRule.RuleType.SUPPRESS, lineNumber + gap);
        if (lastRule != null) { lastRule.setLineNumberLimit(lineNumber); }
        lastRule = rule;
        String[] parts = ruleString.split(" ");
        if (parts.length > 1) {
            for (int i=1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    rule.addParameter(parts[i]);
                }
            }
        }
        preprocessorRules.add(rule);
    }

    public Set<PreprocessorRule> getPreprocessorRules() {
        return preprocessorRules;
    }
}

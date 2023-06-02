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

package cc.redpen.parser.common;

import java.util.ArrayList;
import java.util.List;

public class Line {
    // value returned for comparison if a character is escaped
    static final char ESCAPED_CHARACTER_VALUE = 'ø';

    // a list of offsets for each character
    protected List<Integer> offsets = new ArrayList<>();
    // the character list for the line
    protected List<Character> characters = new ArrayList<>();
    // the text of the line
    protected String text;
    // marks erased characters as invalid
    protected List<Boolean> valid = new ArrayList<>();
    // remembers which characters were escaped in the original string
    protected List<Boolean> escaped = new ArrayList<>();
    // Whole line is erased.
    protected boolean erased = false;

    protected int lineNo = 0;
    protected boolean allSameCharacter = false;
    protected boolean inBlock = false;
    protected int sectionLevel = 0;
    protected int listLevel = 0;
    protected boolean listStart = false;
    protected String inlineMarkupDelimiters = " ";

    /**
     * The different ways embedded inline markers can be erased
     */
    public enum EraseStyle {
        All,
        None,
        Markers,
        InlineMarkup,
        PreserveLabel,
        PreserveAfterLabel, CloseMarkerContainsDelimiters
    }

    public Line(String str, int lineNo) {
        this.lineNo = lineNo;
        this.text = str;
    }

    /**
     * Erase length characters in the line, starting at pos
     *
     * @param pos start position
     * @param length length to erase
     */
    public void erase(int pos, int length) {
        if ((pos >= 0) && (pos < valid.size())) {
            for (int i = pos; (i < valid.size()) && (i < pos + length); i++) {
                valid.set(i, false);
            }
        }
    }

    /**
     * Erase the whole line
     */
    public void erase() {
        for (int i = 0; i < valid.size(); i++) {
            valid.set(i, false);
        }
        erased = true;
    }

    /**
     * Erase all occurrences of the given string
     *
     * @param segment segment to be erased
     */
    public void erase(String segment) {
        for (int i = 0; i < characters.size(); i++) {
            boolean found = true;
            for (int j = 0; j < segment.length(); j++) {
                if (charAt(j + i) != segment.charAt(j)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                erase(i, segment.length());
                i += segment.length();
            }
        }
    }

    /**
     * Return the length of the line
     *
     * @return length of the line
     */
    public int length() {
        return characters.size();
    }

    /**
     * Return the character at the given position. Erase characters will return 0 rather
     * than the actual character
     *
     * @param i index
     * @return extracted character
     */
    public char charAt(int i) {
        return charAt(i, false);
    }

    /**
     * Return the character at the given position, optionally including erased characters
     *
     * @param i index
     * @param includeInvalid true if include invalid
     * @return extracted character
     */
    public char charAt(int i, boolean includeInvalid) {
        if ((i >= 0) && (i < characters.size())) {
            if (escaped.get(i)) {
                return ESCAPED_CHARACTER_VALUE;
            }
            if (includeInvalid || valid.get(i)) {
                return characters.get(i);
            }
        }
        return 0;
    }

    /**
     * Does the line start with the given string?
     *
     * @param s string to test
     * @return true if the string starts with the specified string
     */
    public boolean startsWith(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (charAt(i) != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the raw character at the specified position, ignoring its validity.
     *
     * @param i index
     * @return raw character at the specified position
     */
    public char rawCharAt(int i) {
        if ((i >= 0) && (i < characters.size())) {
            return characters.get(i);
        }
        return ' ';
    }

    /**
     * Is the character at the given position valid?
     *
     * @param i index
     * @return true if the character at the given position valid
     */
    public boolean isValid(int i) {
        if ((i >= 0) && (i < characters.size())) {
            return valid.get(i);
        }
        return false;
    }

    /**
     * Is the character at the given position empty (ie: whitespace or invalid)
     *
     * @return true if it's empty
     */
    public boolean isEmpty() {
        for (int i = 0; i < characters.size(); i++) {
            if (!Character.isWhitespace(characters.get(i)))
                if (valid.get(i)) {
                    return false;
                }
        }

        return true;
    }

    /**
     * Return the offset for the character at the given position
     *
     * @param i index
     * @return offset
     */
    public int getOffset(int i) {
        if (i >= 0) {
            if (i < offsets.size()) {
                return offsets.get(i);
            }
            else {
                return offsets.size();
            }
        }
        return 0;
    }

    /**
     * Is this line a repeating set of the same character?
     *
     * @return true is all same character
     */
    public boolean isAllSameCharacter() {
        return allSameCharacter;
    }

    /**
     * Has this line been completely erased?
     *
     * @return true if erased
     */
    public boolean isErased() {
        return erased;
    }

    /**
     * Is this line in a block?
     *
     * @return true is it's in a block
     */
    public boolean isInBlock() {
        return inBlock;
    }


    /**
     * Is this line the start of a new list item?
     *
     * @return true if it's list start
     */
    public boolean isListStart() {
        return listStart;
    }

    /**
     * Return the list level for this line, or zero if it is not in a list.
     *
     * @return the list level, or zero if not in a list
     */
    public int getListLevel() {
        return listLevel;
    }

    /**
     * If the line is a section header, return the section level,
     * or zero if the line is not a section header
     *
     * @return the section level or zero if not a section header
     */
    public int getSectionLevel() {
        return sectionLevel;
    }

    public void setInBlock(boolean inBlock) {
        this.inBlock = inBlock;
    }

    public void setSectionLevel(int newSectionLevel) {
        this.sectionLevel = newSectionLevel;
    }

    public void setListLevel(int newListLevel) {
        this.listLevel = newListLevel;
    }

    public void setListStart(boolean listStart) {
        this.listStart = listStart;
    }

    public String getText() {
        return text;
    }

    /**
     * Erase the open and close markers, and optionally all the text inside them
     * Returns the position of the first enclosure or -1 if no enclosure was found
     *
     * @param open open marker to be erased
     * @param close close marker to be erased
     * @param eraseStyle erase style
     * @return position of first enclosure
     */
    public int eraseEnclosure(String open,
                              String close,
                              EraseStyle eraseStyle) {
        boolean inEnclosure = false;
        int firstEnclosurePosition = -1;
        int lastCommaPosition = -1;
        int enclosureStart = 0;
        for (int i = 0; i < length(); i++) {
            if (!valid.get(i)) {
                continue;
            }
            if (!inEnclosure) {
                // look for the open string
                boolean foundOpen = true;
                for (int j = 0; j < open.length(); j++) {
                    if (charAt(i + j) != open.charAt(j)) {
                        foundOpen = false;
                        break;
                    }
                }
                // inline requires start of line or a space before the marker
                if (foundOpen && (eraseStyle == EraseStyle.InlineMarkup)) {
                    if ((i != 0) &&
                            inlineMarkupDelimiters.length() > 0 &&
                            (inlineMarkupDelimiters.indexOf(charAt(i - 1)) == -1)) {
                        foundOpen = false;
                    }
                }
                if (foundOpen) {
                    enclosureStart = i;
                    inEnclosure = true;
                    firstEnclosurePosition = i;
                }
            }
            else {
                // look for the close string
                boolean foundClose = true;
                if (eraseStyle == EraseStyle.CloseMarkerContainsDelimiters) {
                    foundClose = (i == length() - 1) || (close.indexOf(charAt(i)) != -1);
                }
                else {
                    for (int j = 0; j < close.length(); j++) {
                        if (charAt(i + j) != close.charAt(j)) {
                            foundClose = false;
                            break;
                        }
                    }
                }

                if (foundClose && (eraseStyle == EraseStyle.InlineMarkup)) {
                    if ((i != length() - 1) && inlineMarkupDelimiters.length() > 0 &&
                            (inlineMarkupDelimiters.indexOf(charAt(i + close.length())) == -1)) {
                        foundClose = false;
                    }
                }

                if (foundClose) {
                    eraseWithStyle(open, close, eraseStyle, lastCommaPosition, enclosureStart, i);
                    inEnclosure = false;
                    lastCommaPosition = -1;
                }
                else if (charAt(i) == ',') {
                    lastCommaPosition = i;
                }
            }
        }
        return firstEnclosurePosition;
    }

    private void eraseWithStyle(String open,
                                String close,
                                EraseStyle eraseStyle,
                                int lastCommaPosition,
                                int enclosureStart,
                                int cloaseStart) {
        switch (eraseStyle) {
            case All:
                erase(enclosureStart, (cloaseStart - enclosureStart) + close.length());
                break;
            case Markers:
            case InlineMarkup:
                erase(enclosureStart, open.length());
                erase(cloaseStart, close.length());
                break;
            case PreserveLabel:
                if (lastCommaPosition != -1) {
                    erase(enclosureStart, (lastCommaPosition + 1) - enclosureStart);
                    erase(cloaseStart, close.length());
                }
                else {
                    erase(enclosureStart, open.length());
                    erase(cloaseStart, close.length());
                }
                break;
            case PreserveAfterLabel:
                erase(enclosureStart, open.length());
                erase(lastCommaPosition, cloaseStart - lastCommaPosition);
                erase(cloaseStart, close.length());
                break;
            case CloseMarkerContainsDelimiters:
                erase(enclosureStart, (cloaseStart == length() - 1)
                        ? (length() - enclosureStart)
                        : (cloaseStart - enclosureStart));
                break;
            case None:
                break;
        }
    }


    /**
     * Return the original line number for this line
     *
     * @return line number
     */
    public int getLineNo() {
        return lineNo;
    }

    /**
     * Render the line as a string, showing what's been erased
     * <p>
     * X         = whole line erased
     * [         = block
     * nn-nnn-nn = section - listlevel - lineno
     * *         = list item
     * ·         = next character erased
     *
     * @return string representation of this instance
     */
    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < characters.size(); i++) {
            if (valid.get(i)) {
                result += characters.get(i);
            }
            else {
                result += "·" + characters.get(i);
            }
        }
        return (erased ? "X" : " ") +
                (inBlock ? "[" : " ") +
                sectionLevel + "-" +
                listLevel + "-" +
                String.format("%03d", lineNo) +
                (listStart ? "*" : ":") +
                " " +
                result;
    }

}

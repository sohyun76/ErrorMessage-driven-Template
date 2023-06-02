/*
 * Copyright (C) 2011 Tatsuhiro Tsujikawa
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.sourceforge.argparse4j.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Locale;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import org.junit.Test;


public class ArgumentImplTest {

    private ArgumentParserConfigurationImpl config_ = ((ArgumentParserImpl) ArgumentParsers
            .newFor("argparse4j").locale(Locale.US).build()).getConfig();

    @Test
    public void testArgumentWithName() {
        ArgumentImpl arg = new ArgumentImpl(config_, "foo");
        assertEquals("foo", arg.getName());
        assertEquals("foo", arg.getDest());
        assertEquals("foo", arg.textualName());
    }

    @Test
    public void testArgumentWithDashSeparatedName() {
        ArgumentImpl arg = new ArgumentImpl(config_, "foo-bar");
        assertEquals("foo-bar", arg.getName());
        assertEquals("foo_bar", arg.getDest());
        assertEquals("foo-bar", arg.textualName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArgumentWithNoNameOrFlags() {
        new ArgumentImpl(config_);
    }

    @Test
    public void testArgumentWithFlags() {
        ArgumentImpl arg = new ArgumentImpl(config_, "-f", "--foo-bar",
                "--foo");
        assertNull(arg.getName());
        assertEquals("foo_bar", arg.getDest());
        assertEquals("FOO_BAR", arg.resolveMetavar()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArgumentWithBadFlags() {
        new ArgumentImpl(config_, "f", "-f");
    }

    @Test
    public void testArgumentWithPrefix() {
        ArgumentParserConfigurationImpl config = ((ArgumentParserImpl) ArgumentParsers
                .newFor("argparse4j").prefixChars("-+").locale(Locale.US)
                .build()).getConfig();
        ArgumentImpl arg = new ArgumentImpl(config, "-f", "-+foo-bar", "++foo");
        assertNull(arg.getName());
        assertEquals("foo_bar", arg.getDest());
        assertEquals("FOO_BAR", arg.resolveMetavar()[0]);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNargsWithZero() {
        ArgumentImpl arg = new ArgumentImpl(config_, "--foo");
        arg.nargs(0);
    }

    @Test
    public void testResolveMetavar() {
        ArgumentImpl arg = new ArgumentImpl(config_, "--foo");
        assertEquals("FOO", arg.resolveMetavar()[0]);
        arg.dest("bar");
        assertEquals("BAR", arg.resolveMetavar()[0]);
        arg.choices("alpha", "bravo");
        assertEquals("{alpha,bravo}", arg.resolveMetavar()[0]);
        arg.metavar("baz");
        assertEquals("baz", arg.resolveMetavar()[0]);
        
        arg = new ArgumentImpl(config_, "foo");
        assertEquals("foo", arg.resolveMetavar()[0]);
        arg.dest("bar");
        assertEquals("bar", arg.resolveMetavar()[0]);
        arg.choices("alpha", "bravo");
        assertEquals("{alpha,bravo}", arg.resolveMetavar()[0]);
        arg.metavar("baz");
        assertEquals("baz", arg.resolveMetavar()[0]);
    }
    
    @Test
    public void testTextualName() {
        ArgumentImpl arg = new ArgumentImpl(config_, "f");
        assertEquals("f", arg.textualName());
        arg = new ArgumentImpl(config_, "-f", "--foo", "--foo-bar");
        assertEquals("-f/--foo/--foo-bar", arg.textualName());
    }

    @Test
    public void testFormatMetavar() {
        ArgumentImpl arg = new ArgumentImpl(config_, "--foo");
        assertEquals("FOO", arg.formatMetavar());
        arg.dest("BAZ");
        assertEquals("BAZ", arg.formatMetavar());
        arg.metavar("BAR");
        assertEquals("BAR", arg.formatMetavar());
        arg.nargs(3);
        assertEquals("BAR BAR BAR", arg.formatMetavar());
        arg.metavar("ALPHA", "BRAVO", "CHARLIE");
        assertEquals("ALPHA BRAVO CHARLIE", arg.formatMetavar());
        arg.nargs(4);
        assertEquals("ALPHA BRAVO CHARLIE CHARLIE", arg.formatMetavar());
        arg.nargs("?");
        assertEquals("[ALPHA]", arg.formatMetavar());
        arg.nargs("*");
        assertEquals("[ALPHA [BRAVO ...]]", arg.formatMetavar());
        arg.nargs("+");
        assertEquals("ALPHA [BRAVO ...]", arg.formatMetavar());
    }

    @Test
    public void testFormatMetavarWithMetavarInference() {
        ArgumentImpl arg = new ArgumentImpl(config_, "--foo")
                .type(Boolean.class);
        assertEquals("{true,false}", arg.formatMetavar());
    }

    @Test
    public void testConvert() throws ArgumentParserException {
        ArgumentImpl arg = new ArgumentImpl(config_, "--foo");
        assertEquals("hello", arg.convert(null, "hello"));
        arg.choices("world");
        try {
            ArgumentParserImpl ap = new ArgumentParserImpl(config_);
            arg.convert(ap, "hello");
            fail();
        } catch(ArgumentParserException e) {
            assertEquals("argument --foo: invalid choice: 'hello' (choose from {world})", e.getMessage());
        }
    }

    @Test
    public void testPrimitiveTypes() throws ArgumentParserException {
        ArgumentImpl arg = new ArgumentImpl(config_, "foo").type(int.class);
        assertEquals(Integer.MAX_VALUE, arg.convert(null, Integer.toString(Integer.MAX_VALUE)));

        arg.type(boolean.class);
        assertEquals(true, arg.convert(null, Boolean.toString(Boolean.TRUE)));

        arg.type(byte.class);
        assertEquals(Byte.MAX_VALUE, arg.convert(null, Byte.toString(Byte.MAX_VALUE)));

        arg.type(short.class);
        assertEquals(Short.MAX_VALUE, arg.convert(null, Short.toString(Short.MAX_VALUE)));

        arg.type(long.class);
        assertEquals(Long.MAX_VALUE, arg.convert(null, Long.toString(Long.MAX_VALUE)));

        arg.type(float.class);
        assertEquals(Float.MAX_VALUE, arg.convert(null, Float.toString(Float.MAX_VALUE)));

        arg.type(double.class);
        assertEquals(Double.MAX_VALUE, arg.convert(null, Double.toString(Double.MAX_VALUE)));

        try {
            arg.type(char.class);
        } catch(IllegalArgumentException e) {
            assertEquals("unexpected primitive type", e.getMessage());
        }

        try {
            arg.type(void.class);
        } catch(IllegalArgumentException e) {
            assertEquals("unexpected primitive type", e.getMessage());
        }
    }
}

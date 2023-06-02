/**
 * Copyright (c) 2013-2014 Santiago M. Mola <santi@mola.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package io.mola.galimatias.canonicalize;

import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.URL;

public class StripPartCanonicalizer implements URLCanonicalizer {

    public static enum Part {
        USERNAME,
        PASSWORD,
        PORT,
        PATH,
        QUERY,
        FRAGMENT
    }

    private final Part part;

    public StripPartCanonicalizer(final Part part) {
        this.part = part;
    }

    @Override
    public URL canonicalize(final URL input) throws GalimatiasParseException {
        switch (part) {
            case USERNAME:
                return input.withUsername(null);
            case PASSWORD:
                return input.withPassword(null);
            case PORT:
                return input.withPort(-1);
            case PATH:
                return input.withPath("/");
            case QUERY:
                return input.withQuery(null);
            case FRAGMENT:
                return input.withFragment(null);
            default:
                return input;
        }
    }

}

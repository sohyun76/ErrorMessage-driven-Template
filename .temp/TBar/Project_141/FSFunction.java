/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.css.parser;

import java.util.List;

public class FSFunction {
    private final String _name;
    private final List<PropertyValue> _parameters;

    public FSFunction(String name, List<PropertyValue> parameters) {
        _name = name;
        _parameters = parameters;
    }
    
    public String getName() {
        return _name;
    }
    
    public List<PropertyValue> getParameters() {
        return _parameters;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(_name);
        result.append('(');
        for (int i = 0; i < getParameters().size(); i++) {
            result.append(getParameters().get(i));
            if (i < getParameters().size() - 1) {
                result.append(',');
            }
        }
        result.append(')');
        return result.toString();
    }
}

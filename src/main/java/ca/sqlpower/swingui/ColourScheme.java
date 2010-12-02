/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.util.WebColour;

/**
 * Collections of colours that aren't alarmingly poor when viewed near
 * each other.
 */
public class ColourScheme {

    public static final WebColour[] BACKGROUND_COLOURS = {
        new WebColour("#eeeeee"),
        new WebColour("#c7d0df"),
        new WebColour("#bfd6ff"),
        new WebColour("#b9bbf6"),
        new WebColour("#c8b7ea"),
        new WebColour("#e8cfdb"),
        new WebColour("#f6bcbc"),
        new WebColour("#fbdabb"),
        new WebColour("#eec994"),
        new WebColour("#e6d794"),
        new WebColour("#e4e4b8"),
        new WebColour("#dfedce"),
        new WebColour("#c8dccc"),
        new WebColour("#ffffff")
    };
    
    public static final WebColour[] FOREGROUND_COLOURS = {
        new WebColour("#000000"),
        new WebColour("#5a6986"),
        new WebColour("#206cff"),
        new WebColour("#0000cc"),
        new WebColour("#5229a3"),
        new WebColour("#854f61"),
        new WebColour("#cc0000"),
        new WebColour("#ec7000"),
        new WebColour("#b36d00"),
        new WebColour("#ab8b00"),
        new WebColour("#636330"),
        new WebColour("#64992c"),
        new WebColour("#006633")
    };

    /**
     * This is the official SQL Power orange colour on the website and used
     * throughout our products as a highlight colour.
     */
    public static final WebColour SQLPOWER_ORANGE = new WebColour(0xff, 0x66, 0x00);
    
    /**
     * Brewer Colour Scheme "set19".  A nice collection of colours for colour coding
     * sets of information with up to 9 elements.
     */
    public static final List<WebColour> BREWER_SET19 = Collections.unmodifiableList(Arrays.asList(
        new WebColour("#e41a1c"),
        new WebColour("#377eb8"),
        new WebColour("#4daf4a"),
        new WebColour("#80b1d3"),
        new WebColour("#984ea3"),
        new WebColour("#ff7f00"),
        new WebColour("#ffff33"),
        new WebColour("#a65628"),
        new WebColour("#f781bf"),
        new WebColour("#999999")
    ));
    
    public static final WebColour[] HEADER_COLOURS = {
    	new WebColour("#e2ecfa"),
    	new WebColour("#e8e8e8"),
    	new WebColour("#cedff7"),
    	new WebColour("#dddddd"),
    	new WebColour("#bbd3f4"),
    	new WebColour("#d2d2d2"),
    	new WebColour("#a8c6f1"),
    	new WebColour("#c6c6c6")
    };
}

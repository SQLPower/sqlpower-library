/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        new WebColour("#c8dccc")
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
}

/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of SQL Power Wabit.
 *
 * SQL Power Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.dao.session;

import java.awt.Font;

public interface SPFontLoader {

	/**
	 * Loads a font by his family name.
	 * @param fontName The font name.
	 * @return A Font object corresponding to the one asked for.
	 */
	public Font loadFontFromName(String fontName);

	/**
	 * Loads a font from a specification String as the ones used in
	 * the TTF font naming convention. Usually something like: Arial-BOLD-10.
	 * @param fontSpecs The font specs.
	 * @return A Font object corresponding to the one asked for.
	 */
	public Font loadFontFromSpecs(String fontSpecs);
}
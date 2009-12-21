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

package ca.sqlpower.dao;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import ca.sqlpower.dao.SPPersister.DataType;
import ca.sqlpower.dao.session.BidirectionalConverter;
import ca.sqlpower.object.SPObject;

/**
 * Utilities that are used by {@link SPPersister}s. 
 */
public class PersisterUtils {
	
	private PersisterUtils() {
		//cannot instantiate this class as it is just static utility methods.
	}

	/**
	 * Converts an image to an output stream to be persisted in some way.
	 * 
	 * @param img
	 *            The image to convert to an output stream for persisting.
	 * @return An output stream containing an encoding of the image as PNG.
	 */
	public static ByteArrayOutputStream convertImageToStreamAsPNG(Image img) {
		BufferedImage image;
        if (img instanceof BufferedImage) {
            image = (BufferedImage) img;
        } else {
            image = new BufferedImage(img.getWidth(null), 
            		img.getHeight(null), BufferedImage.TYPE_INT_ARGB); 
            final Graphics2D g = image.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        if (image != null) {
        	try {
				ImageIO.write(image, "PNG", byteStream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
        }
        return byteStream;
	}
	
	/**
	 * Splits a string by the converter delimiter and checks that the correct
	 * number of pieces are returned or it throws an
	 * {@link IllegalArgumentException}. This is a simple place to do general
	 * error checking when first converting a string into an object.
	 * 
	 * @param toSplit
	 *            The string to split by the delimiter.
	 * @param numPieces
	 *            The number of pieces the string must be split into.
	 * @return The pieces the string is split into.
	 */
	public static String[] splitByDelimiter(String toSplit, int numPieces) {
		String[] pieces = toSplit.split(BidirectionalConverter.DELIMITER);

		if (pieces.length > numPieces) {
			throw new IllegalArgumentException("Cannot convert string \""
					+ toSplit + "\" with an invalid number of properties.");
		} else if (pieces.length < numPieces) {
			//split will strip off empty space that comes after a delimiter instead of
			//appending an empty string to the array so we have to do that ourselves.
			String[] allPieces = new String[numPieces];
			
			int i = 0;
			for (String piece : pieces) {
				allPieces[i] = piece;
				i++;
			}
			for (; i < numPieces; i++) {
				allPieces[i] = "";
			}
			return allPieces;
		}
		return pieces;
	}
	
    /**
     * Gets the correct data type based on the given class for the {@link SPPersister}.
     */
    public static DataType getDataType(Class<? extends Object> classForDataType) {
    	if (Integer.class.isAssignableFrom(classForDataType)) {
    		return DataType.INTEGER;
    	} else if (Boolean.class.isAssignableFrom(classForDataType)) {
    		return DataType.BOOLEAN;
    	} else if (Double.class.isAssignableFrom(classForDataType)) {
    		return DataType.DOUBLE;
    	} else if (String.class.isAssignableFrom(classForDataType)) {
    		return DataType.STRING;
    	} else if (Image.class.isAssignableFrom(classForDataType)) {
    		return DataType.PNG_IMG;
    	} else if (SPObject.class.isAssignableFrom(classForDataType)) {
    		return DataType.REFERENCE;
    	} else if (Void.class.isAssignableFrom(classForDataType)) {
    		return DataType.NULL;
    	} else {
    		return DataType.STRING;
    	}
    }

}

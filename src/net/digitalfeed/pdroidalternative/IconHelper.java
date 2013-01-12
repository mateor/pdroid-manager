/**
 * Copyright (C) 2012 Simeon J. Morgan (smorgan@digitalfeed.net)
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 * The software has the following requirements (GNU GPL version 3 section 7):
 * You must retain in pdroid-manager, any modifications or derivatives of
 * pdroid-manager, or any code or components taken from pdroid-manager the author
 * attribution included in the files.
 * In pdroid-manager, any modifications or derivatives of pdroid-manager, or any
 * application utilizing code or components taken from pdroid-manager must include
 * in any display or listing of its creators, authors, contributors or developers
 * the names or pseudonyms included in the author attributions of pdroid-manager
 * or pdroid-manager derived code.
 * Modified or derivative versions of the pdroid-manager application must use an
 * alternative name, rather than the name pdroid-manager.
 */

/**
 * @author Simeon J. Morgan <smorgan@digitalfeed.net>
 */
package net.digitalfeed.pdroidalternative;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class IconHelper {
    
    public static int BUFFER_SIZE = 4096;
    
	public static Bitmap getIconBitmap(Drawable icon, int maxSize) {
		int iconWidth = icon.getIntrinsicWidth();
		int iconHeight = icon.getIntrinsicHeight();
		if (maxSize > -1) {
			iconWidth = (iconWidth > maxSize) ? maxSize : iconWidth;
			iconHeight = (iconHeight > maxSize) ? maxSize : iconHeight;
		}
		
		if (iconWidth == 0 || iconHeight == 0) {
		    return null;
		}
		
		//Thanks go to André on http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
        //Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);
        return bitmap;
	}
	
	public static byte[] getIconByteArray(Drawable icon, int maxSize) {
		Bitmap bitmap = getIconBitmap(icon, maxSize);
		if (bitmap == null) {
		    return null;
		}
		ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, DBInterface.ApplicationTable.COMPRESS_ICON_QUALITY, byteArrayBitmapStream);
		return byteArrayBitmapStream.toByteArray();
	}
	
	/**
	 * Converts an inputstream to a byte array. This is basically lifted from the Apache Commons IOUtils
	 * @param input
	 * @return
	 */
	public static byte[] getByteArray(InputStream input) {
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Log.e(GlobalConstants.LOG_TAG,"IconHelper:getByteArray: IOException while converting InputStream to byte array", e);
            buffer = null;
        }
        return buffer;
	}
}

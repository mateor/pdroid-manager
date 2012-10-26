package net.digitalfeed.pdroidalternative;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class IconHelper {
	public static Bitmap getIconBitmap(Drawable icon) {
		//Thanks go to André on http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
        Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);
        return bitmap;
	}
	
	public static byte[] getIconByteArray(Drawable icon) {
		Bitmap bitmap = getIconBitmap(icon);
		ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, DBInterface.ApplicationTable.COMPRESS_ICON_QUALITY, byteArrayBitmapStream);
		return byteArrayBitmapStream.toByteArray();
	}

}

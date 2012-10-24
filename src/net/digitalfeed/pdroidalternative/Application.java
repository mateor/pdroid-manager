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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class Application {
	public static final int APP_FLAG_IS_SYSTEM_APP = 0x1;
	public static final int APP_FLAG_HAS_INTERNET = 0x2;
	public static final int STATUS_FLAG_IS_UNTRUSTED = 0x1;
	public static final int STATUS_FLAG_NOTIFY_ON_ACCESS = 0x2;
	

	//indicates whether this has a full data complement, or just the minimum data set
	private boolean isStub;
	
	private long id;
	private String packageName;
	private String label;
	private int versionCode;
	private int appFlags;
	private int statusFlags;
	int uid;
	private Drawable icon;
	
	//The value in permissions is only valid if the Application entry is not a stub
	private String[] permissions;
	
	public long getId() {
		return this.id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public int getAppFlags() {
		return this.appFlags;
	}

	public void setAppFlags(int appFlags) {
		this.appFlags = appFlags;
	}

	public boolean getIsSystemApp() {
		return (this.appFlags & APP_FLAG_IS_SYSTEM_APP) == APP_FLAG_IS_SYSTEM_APP;
	}
	
	public void setIsSystemApp(boolean isSystemApp) {
		this.appFlags = this.appFlags & ~APP_FLAG_IS_SYSTEM_APP;
	}

	public boolean getHasInternet() {
		return (this.appFlags & APP_FLAG_HAS_INTERNET) == APP_FLAG_HAS_INTERNET;
	}

	public void setHasInternet(boolean hasInternet) {
		this.appFlags = this.appFlags & ~APP_FLAG_HAS_INTERNET;
	} 
	
	public boolean getIsUntrusted() {
		return (this.statusFlags & STATUS_FLAG_IS_UNTRUSTED) == STATUS_FLAG_IS_UNTRUSTED;
	}
	
	public void setIsUntrusted(boolean isUntrusted) {
		this.statusFlags = this.statusFlags & ~STATUS_FLAG_IS_UNTRUSTED;
	} 

	public boolean getNotifyOnAccess() {
		return (this.statusFlags & STATUS_FLAG_NOTIFY_ON_ACCESS) == STATUS_FLAG_NOTIFY_ON_ACCESS;
	}
	
	public void setNotifyOnAccess(boolean isUntrusted) {
		this.statusFlags = this.statusFlags & ~STATUS_FLAG_NOTIFY_ON_ACCESS;
	}
	
	public int getUid() {
		return this.uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public Drawable getIcon() {
		return this.icon;
	}

	public Bitmap getIconBitmap() {
		//Thanks go to André on http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
        Bitmap bitmap = Bitmap.createBitmap(this.icon.getIntrinsicWidth(), this.icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);
        return bitmap;
	}
	
	public byte[] getIconByteArray() {
		Bitmap bitmap = getIconBitmap();
		ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, DBInterface.ApplicationTable.COMPRESS_ICON_QUALITY, byteArrayBitmapStream);
		return byteArrayBitmapStream.toByteArray();
	}
	
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public String[] getPermissions() {
		return this.permissions;
	}

	public void setPermissions(String[] permissions) {
		this.permissions = permissions;
	}

	public String getPackageName() {
		return this.packageName;
	}
	
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	Application(String packageName, String label, int versionCode, int appFlags, int statusFlags, int uid, Drawable icon) {
		this.isStub = true;
		this.packageName = packageName;
		this.label = label;
		this.versionCode = versionCode;
		this.appFlags = appFlags;
		this.statusFlags = statusFlags;
		this.uid = uid;
		this.icon = icon;
	}	
	
	Application(String packageName, String label, int versionCode, int appFlags, int statusFlags, int uid, Drawable icon, String[] permissions) {
		this.isStub = false;
		this.packageName = packageName;
		this.label = label;
		this.versionCode = versionCode;
		this.appFlags = appFlags;
		this.statusFlags = statusFlags;
		this.uid = uid;
		this.icon = icon;
		if (permissions != null) { 
			this.permissions = permissions.clone();
		} else {
			this.permissions = null;
		}
	}	
}

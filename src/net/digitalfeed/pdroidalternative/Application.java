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

import android.graphics.drawable.Drawable;

public class Application {
	public static final int FLAG_IS_SYSTEM_APP = 0x1;
	public static final int FLAGS_UNTRUSTED = 0x2;
	public static final int HAS_INTERNET = 0x4;
	
	private long id;
	private String packageName;
	private String name;
	private int versionCode;
	private int flags;
	int uid;
	private Drawable icon;
	private String[] permissions;

	//private String label;
	//private String versionName;
	//private boolean isSystemApp;
	
	public long getId() {
		return this.id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public Drawable getIcon() {
		return icon;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public String[] getPermissions() {
		return permissions;
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
	
	Application(String packageName, String name, String label, int versionCode, String versionName, boolean isSystemApp, int uid, Drawable icon, String[] permissions) {
		this.packageName = packageName;
		this.name = name;
		//this.label = label;
		this.versionCode = versionCode;
		//this.isSystemApp = isSystemApp;
		//this.versionName = versionName;
		this.uid = uid;
		this.icon = icon;
		if (this.permissions != null) { 
			this.permissions = permissions.clone();
		} else {
			this.permissions = null;
		}
	}
	
}

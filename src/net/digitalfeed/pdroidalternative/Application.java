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


import net.digitalfeed.pdroidalternative.DBInterface.DBHelper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

/**
 * This represents a single application.
 * @author smorgan
 *
 */
//TODO: Consider thread safety: this is not presently a thread-safe object, but it is used concurrently
//by multiple threads (e.g. AppListActivity triggers an update, which causes the object to be updated)
public class Application {
	public static final int APP_FLAG_IS_SYSTEM_APP = 1;
	public static final int APP_FLAG_HAS_INTERNET = 2;
	public static final int STATUS_FLAG_IS_UNTRUSTED = 1; //indicates that one or more settings are not the 'trusted' setting in PDroid Core
	public static final int STATUS_FLAG_HAS_PRIVACYSETTINGS = 8; //indicates that no privacysettings have been saved in the PDroid Core
	public static final int STATUS_FLAG_NOTIFY_ON_ACCESS = 2;
	public static final int STATUS_FLAG_LOG_ON_ACCESS = 4;
	public static final int STATUS_FLAG_NEW = 8;
	public static final int STATUS_FLAG_UPDATED = 16;
	
	public static final int TARGET_ICON_SIZE = 32;
	
	//indicates whether this has a full data complement, or just the minimum data set
	//private boolean isStub;
	
	private volatile Integer _id; //right now, the sole reason that there is an_id at all is to differentiate new 'Application' objects from ones retrieved from the database
	private volatile String packageName;
	private volatile String label;
	private volatile int versionCode;
	private volatile int appFlags;
	private volatile int statusFlags;
	private volatile int uid;
	private volatile Drawable icon;
	
	static final int FLAG_APPLICATION_VALUES_CHANGED = 1; //this is packageName, label, versionCode, appFlags, uid
	static final int FLAG_ICON_CHANGED = 2;
	static final int FLAG_PERMISSIONS_CHANGED = 4;
	static final int FLAG_STATUSFLAGS_CHANGED = 8;
	// keeps track of what has changed, so that when it is time to perform an update
	// only the necessary parts of the database need to change
	private volatile int updatedFlags; 
	
	//The value in permissions is only valid if the Application entry is not a stub
	private String[] permissions;

	public String getLabel() {
		return this.label;
	}
	
	public void setLabel(String label) {
		if (!label.equals(this.label)) {
			this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
			this.label = label;
		}
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		if (versionCode == this.versionCode) {
			this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
			this.versionCode = versionCode;
		}
	}

	public int getAppFlags() {
		return this.appFlags;
	}

	public void setAppFlags(int appFlags) {
		if (appFlags != this.appFlags) {
			this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
			this.appFlags = appFlags;
		}
	}

	public boolean getIsSystemApp() {
		return (0 != (this.appFlags & APP_FLAG_IS_SYSTEM_APP));
	}
	
	public void setIsSystemApp(boolean newValue) {
		if (newValue) {
			if (0 == (this.appFlags & APP_FLAG_IS_SYSTEM_APP)) {
				this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
				this.appFlags |= APP_FLAG_IS_SYSTEM_APP;
			}
		} else { 
			if (0 != (this.appFlags & APP_FLAG_IS_SYSTEM_APP)) {
				this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
				this.appFlags &= ~APP_FLAG_IS_SYSTEM_APP;
			}
		}
	}

	public boolean getHasInternet() {
		return (0 != (this.appFlags & APP_FLAG_HAS_INTERNET));
	}

	public void setHasInternet(boolean newValue) {
		if (newValue) {
			if (0 == (this.appFlags & APP_FLAG_HAS_INTERNET)) {
				this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
				this.appFlags |= APP_FLAG_HAS_INTERNET;
			}
		} else { 
			if (0 != (this.appFlags & APP_FLAG_HAS_INTERNET)) {
				this.updatedFlags  |= FLAG_APPLICATION_VALUES_CHANGED;
				this.appFlags &= ~APP_FLAG_HAS_INTERNET;
			}
		}
	}
	
	public void setStatusFlags(int statusFlags) {
		if (statusFlags != this.statusFlags) {
			this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
			this.statusFlags = statusFlags; 
		}
	}

	public int getStatusFlags() {
		return this.statusFlags; 
	}
	
	public boolean getIsUntrusted() {
		return (0 != (this.statusFlags & STATUS_FLAG_IS_UNTRUSTED));
	}
	
	public void setIsUntrusted(boolean newValue) {
		if (newValue) {
			if (0 == (this.statusFlags & STATUS_FLAG_IS_UNTRUSTED)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags |= STATUS_FLAG_IS_UNTRUSTED;
			}
		} else { 
			if (0 != (this.statusFlags & STATUS_FLAG_IS_UNTRUSTED)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags &= ~STATUS_FLAG_IS_UNTRUSTED;
			}
		}
	} 

	public boolean getHasSettings() {
		return (0 != (this.statusFlags & STATUS_FLAG_HAS_PRIVACYSETTINGS));
	}
	
	public void setHasSettings(boolean newValue) {
		if (newValue) {
			if (0 == (this.statusFlags & STATUS_FLAG_HAS_PRIVACYSETTINGS)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags |= STATUS_FLAG_HAS_PRIVACYSETTINGS;
			}
		} else { 
			if (0 != (this.statusFlags & STATUS_FLAG_HAS_PRIVACYSETTINGS)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags &= ~STATUS_FLAG_HAS_PRIVACYSETTINGS;
			}
		}
	}
	
	
	public boolean getNotifyOnAccess() {
		return (0 != (this.statusFlags & STATUS_FLAG_NOTIFY_ON_ACCESS));
	}
	
	public void setNotifyOnAccess(boolean newValue) {
		if (newValue) {
			if (0 == (this.statusFlags & STATUS_FLAG_NOTIFY_ON_ACCESS)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags |= STATUS_FLAG_NOTIFY_ON_ACCESS;
			}
		} else { 
			if (0 != (this.statusFlags & STATUS_FLAG_NOTIFY_ON_ACCESS)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags &= ~STATUS_FLAG_NOTIFY_ON_ACCESS;
			}
		}
	}

	public boolean getLogOnAccess() {
		return (0 != (this.statusFlags & STATUS_FLAG_LOG_ON_ACCESS));
	}
	
	public void setLogOnAccess(boolean newValue) {
		if (newValue) {
			if (0 == (this.statusFlags & STATUS_FLAG_LOG_ON_ACCESS)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags |= STATUS_FLAG_LOG_ON_ACCESS;
			}
		} else { 
			if (0 != (this.statusFlags & STATUS_FLAG_LOG_ON_ACCESS)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags &= ~STATUS_FLAG_LOG_ON_ACCESS;
			}
		}
	}

	public boolean getIsNew() {
		return (0 != (this.statusFlags & STATUS_FLAG_NEW));
	}
	
	public void setIsNew(boolean newValue) {
		if (newValue) {
			if (0 == (this.statusFlags & STATUS_FLAG_NEW)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags |= STATUS_FLAG_NEW;
			}
		} else { 
			if (0 != (this.statusFlags & STATUS_FLAG_NEW)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags &= ~STATUS_FLAG_NEW;
			}
		}
	}

	public boolean getIsUpdated() {
		return (0 != (this.statusFlags & STATUS_FLAG_UPDATED));
	}
	
	public void setIsUpdated(boolean newValue) {
		if (newValue) {
			if (0 == (this.statusFlags & STATUS_FLAG_UPDATED)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags |= STATUS_FLAG_UPDATED;
			}
		} else { 
			if (0 != (this.statusFlags & STATUS_FLAG_UPDATED)) {
				this.updatedFlags  |= FLAG_STATUSFLAGS_CHANGED;
				this.statusFlags &= ~STATUS_FLAG_UPDATED;
			}
		}
	}
	
	public int getUid() {
		return this.uid;
	}

	public void setUid(int uid) {
		if (uid != this.uid) {
			this.updatedFlags |= FLAG_APPLICATION_VALUES_CHANGED;
			this.uid = uid;
		}
	}

	public Drawable getIcon() {
		return this.icon;
	}

	public Bitmap getIconBitmap(int maxSize) {
        return IconHelper.getIconBitmap(this.icon, maxSize);
	}
	
	public byte[] getIconByteArray(int maxSize) {
		return IconHelper.getIconByteArray(this.icon, maxSize);
	}

	public Bitmap getIconBitmap() {
        return IconHelper.getIconBitmap(this.icon, -1);
	}
	
	public byte[] getIconByteArray() {
		return IconHelper.getIconByteArray(this.icon, -1);
	}

	
	public void setIcon(Drawable icon) {
		this.updatedFlags |= FLAG_ICON_CHANGED;
		this.icon = icon;
	}

	public String[] getPermissions() {
		return this.permissions;
	}

	public void setPermissions(String[] permissions) {
		this.updatedFlags |= FLAG_PERMISSIONS_CHANGED;
		this.permissions = permissions;
	}

	public String getPackageName() {
		return this.packageName;
	}
	
	public void setPackageName(String packageName) {
		if (!packageName.equals(this.packageName)) {
			this.updatedFlags |= FLAG_APPLICATION_VALUES_CHANGED;
			this.packageName = packageName;
		}
	}
	
	int getUpdatedFlags() {
		return this.updatedFlags;
	}
	
/*	public Application(String packageName, String label, int versionCode, int appFlags, int statusFlags, int uid, Drawable icon) {
		//this.isStub = true;
		this.packageName = packageName;
		this.label = label;
		this.versionCode = versionCode;
		this.appFlags = appFlags;
		this.statusFlags = statusFlags;
		this.uid = uid;
		this.icon = icon;
	}*/	
	
	public Application(String packageName, String label, int versionCode, int appFlags, int statusFlags, int uid, Drawable icon, String[] permissions) {
		//this.isStub = false;
		// if this constructor is being used, this must be a whole new application. In that case, everything needs to be saved.
		this.updatedFlags = FLAG_APPLICATION_VALUES_CHANGED | FLAG_ICON_CHANGED | FLAG_PERMISSIONS_CHANGED | FLAG_STATUSFLAGS_CHANGED;
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
	
	Application(Integer _id, String packageName, String label, int versionCode, int appFlags, int statusFlags, int uid, Drawable icon, String[] permissions) {
		//this.isStub = false;
		this.updatedFlags = 0;
		this._id = _id;
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
	
	boolean isNew() {
		return _id == null;
	}
	/***
	 * Creates a new Application object for the application with the package name passed.
	 * Currently assumes the app is new, and so doesn't try to check if it is trusted or not -
	 * instead assumes it is trusted, and no notifications are required
	 * 
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static Application fromPackageName(Context context, String packageName) {
		Application app = null;
		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Application.fromPackageName: Loading package from OS: " + packageName);
		try {
			PackageManager pkgMgr = context.getPackageManager();
			PackageInfo pkgInfo = pkgMgr.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
			ApplicationInfo appInfo = pkgInfo.applicationInfo;
			
			int appFlags = 0;
			if (0 != (appInfo.flags & ApplicationInfo.FLAG_SYSTEM)) { 
				appFlags = Application.APP_FLAG_IS_SYSTEM_APP;
			}
			
			if (pkgMgr.checkPermission("android.permission.INTERNET", appInfo.packageName) == PackageManager.PERMISSION_GRANTED) {
				appFlags = appFlags | Application.APP_FLAG_HAS_INTERNET;
			}
			
			int statusFlags = 0;
			//TODO: Check if there are actually PDroid settings for the app,
			//and don't just assume the trust state. It is likely to be wrong.
			
			app = new Application(
					packageName,
					pkgMgr.getApplicationLabel(appInfo).toString(),
					pkgInfo.versionCode,
					appFlags,
					statusFlags,
					appInfo.uid,
					pkgMgr.getApplicationIcon(appInfo.packageName),
					pkgInfo.requestedPermissions
					); 
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Application.fromPackageName: Object created from OS");

		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "NameNotFoundException when trying to generate an app from package name");
			e.printStackTrace();
		}
		
		return app; 
	}
	
	/***
	 * Loads an Application object for the application with the package name passed from the database
	 * 
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static Application fromDatabase(Context context, String packageName) {
		Application app = null;
		SQLiteDatabase db = null;
		Cursor cursor = null;
		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Application.fromDatabase: Loading package: " + packageName);
		try {
			DBHelper dbHelper = DBInterface.getInstance(context).getDBHelper();
			db = dbHelper.getReadableDatabase();
			cursor = db.rawQuery(DBInterface.QUERY_GET_APPS_BY_PACKAGENAME_WITH_STATUS, new String[] {packageName});

			if (cursor.getCount() > 0) {
				cursor.moveToFirst();

		    	String permissions = cursor.getString(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS));
		    	String [] permissionsArray = null; 
				if (permissions != null) {
					permissionsArray = TextUtils.split(permissions, ",");
				}
		    	
		    	byte[] iconBlob = cursor.getBlob(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON));
		    	Drawable icon = new BitmapDrawable(context.getResources(),BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length));
		    	
		    	app = new Application(
		    			cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ROWID)),
		    			cursor.getString(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME)),
		    			cursor.getString(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL)),
		    			cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE)),
		    			cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS)),
		    			cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS)),
		    			cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID)),
		    			icon,
		    			permissionsArray
		    		);
		    	if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Application.fromDatabase: Loaded package from DB");
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			/*if (db != null && db.isOpen()) {
				db.close();
			}*/
		}
		
		return app;
	}
	
	
}

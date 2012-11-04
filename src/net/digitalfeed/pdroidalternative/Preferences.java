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

import java.security.InvalidParameterException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

public class Preferences {
	public static final String APPLIST_LAST_APP_TYPE_SYSTEM = "system";
	public static final String APPLIST_LAST_APP_TYPE_USER = "user";
	public static final String APPLIST_LAST_APP_TYPE_ALL = "all";
	
	private static final String SHARED_PREFERENCES_NAME = "net.digitalfeed.pdroidalternative";
	private static final String IS_CACHE_VALID = "isCacheValid";
	private static final String LAST_RUN_DATABASE_VERSION = "lastDatabaseVersion";
	private static final String NOTIFICATION_DURATION = "notificationDuration";
	private static final String APP_NOTIFICATION_SETTING_PREFIX = "notifyOnAccessFor";
	private static final String APP_LOG_SETTING_PREFIX = "logOnAccessFor";
	private static final String APPLIST_LAST_APP_TYPE = "appListLastAppType"; //can be the 'system', 'user', or 'both'
	private SharedPreferences prefs;
	
	public Preferences(Context context) {
		this.prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
	}
	
	public Boolean getIsApplicationListCacheValid() {
		return this.prefs.getBoolean(IS_CACHE_VALID, false);
	}

	public void setIsApplicationListCacheValid(Boolean isValid) {
		Editor editor = this.prefs.edit();
		editor.putBoolean(IS_CACHE_VALID, isValid);
		editor.commit();
	}
	
	public int getLastRunDatabaseVersion() {
		return this.prefs.getInt(LAST_RUN_DATABASE_VERSION, 0);
	}
	
	public void setLastRunDatabaseVersion(int newDatabaseVersion) {
		Editor editor = this.prefs.edit();
		editor.putInt(LAST_RUN_DATABASE_VERSION, newDatabaseVersion);
		editor.commit();
	}
	
	public boolean getDoNotifyForPackage(String packageName) {
		return this.prefs.getBoolean(APP_NOTIFICATION_SETTING_PREFIX + packageName, false);
	}
	
	public void setDoNotifyForPackage(String packageName, boolean doNotify) {
		Editor editor = this.prefs.edit();
		editor.putBoolean(APP_NOTIFICATION_SETTING_PREFIX + packageName, doNotify);
		editor.commit();
	}
	
	public boolean getDoLogForPackage(String packageName) {
		return this.prefs.getBoolean(APP_LOG_SETTING_PREFIX + packageName, false);
	}
	
	public void setDoLogForPackage(String packageName, boolean doLog) {
		Editor editor = this.prefs.edit();
		editor.putBoolean(APP_LOG_SETTING_PREFIX + packageName, doLog);
		editor.commit();
	}
	
	public int getNotificationDuration() {
		return this.prefs.getInt(NOTIFICATION_DURATION, Toast.LENGTH_SHORT);
	}
	
	public void setNotificationDuration(int notificationDuration) {
		if (notificationDuration != Toast.LENGTH_SHORT && notificationDuration != Toast.LENGTH_LONG) {
			throw new InvalidParameterException("Notification duration must be Toast.LENGTH_SHORT or Toast.LENGTH_LONG");
		}
		Editor editor = this.prefs.edit();
		editor.putInt(NOTIFICATION_DURATION, notificationDuration);
		editor.commit();
	}
	
	public String getLastAppListType() {
		return this.prefs.getString(APPLIST_LAST_APP_TYPE, APPLIST_LAST_APP_TYPE_ALL);
	}
	
	public void setLastAppListType(String appListType) {
		if (appListType != APPLIST_LAST_APP_TYPE_ALL &&
				appListType != APPLIST_LAST_APP_TYPE_USER &&
				appListType != APPLIST_LAST_APP_TYPE_SYSTEM) {
			throw new InvalidParameterException("AppListType can only be ALL, USER, or SYSTEM (check your Prefs insertion!)");
		}
		Editor editor = this.prefs.edit();
		editor.putString(APPLIST_LAST_APP_TYPE, appListType);
		editor.commit();
	}
}

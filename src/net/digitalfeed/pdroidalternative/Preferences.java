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
	private static final String APP_LAST_NOTIFICATION_TIME_PREFIX = "appLastNotificationTime";
	private static final String CURRENT_TOAST_COUNT = "currentToastCount";
	private static final String LAST_TOAST_TIME = "lastToastTime";
	private static final String LAST_RUN_LANGUAGE = "lastRunLanguage";
	private static final String FORCED_LANGUAGE = "forcedLanguage"; //if present, then a 'preferred' language has been specified
	private static final Object lock = new Object();
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
	
	public long getLastNotificationTime(String packageName, String dataType) {
		//using '-' because they are not valid in a package name
		return this.prefs.getLong(APP_LAST_NOTIFICATION_TIME_PREFIX + "-" + packageName + "-" + dataType, -1);
	}
	
	public void setLastNotificationTime(String packageName, String dataType, long newTime) {
		//using '-' because they are not valid in a package name
		Editor editor = this.prefs.edit();
		editor.putLong(APP_LAST_NOTIFICATION_TIME_PREFIX + "-" + packageName + "-" + dataType, newTime);
		editor.commit();
	}
	
	public void clearLastNotificationTime(String packageName, String dataType) {
		Editor editor = this.prefs.edit();
		editor.remove(APP_LAST_NOTIFICATION_TIME_PREFIX + "-" + packageName + "-" + dataType);
		editor.commit();
	}
	
	public String getLastRunLanguage() {
		return this.prefs.getString(LAST_RUN_LANGUAGE, "");
	}
	
	public void setLastRunLanguage(String languageName) {
		Editor editor = this.prefs.edit();
		editor.putString(LAST_RUN_LANGUAGE, languageName);
		editor.commit();
	}

	/**
	 * Returns the language which has been set as preferred for the application,
	 * if such a preference has been defined.
	 * @return  Language name if a language is forced, otherwise null
	 */
	public String getForcedLanguage() {
		String forcedLang = this.prefs.getString(FORCED_LANGUAGE, null);
		return this.prefs.getString(FORCED_LANGUAGE, null);
	}
	
	/**
	 * Sets the language which should always be used
	 * @param languageName
	 */
	public void setForcedLanguage(String languageName) {
		Editor editor = this.prefs.edit();
		editor.putString(FORCED_LANGUAGE, languageName);
		editor.commit();
	}
	
	/**
	 * Clears the setting to always use a particular specified language
	 */
	public void clearForcedLanguage() {
		Editor editor = this.prefs.edit();
		editor.remove(FORCED_LANGUAGE);
		editor.commit();
	}

	
	/**
	 * This function is used for calculating the offset of the Toast, by checking 
	 * how many are on-screen. It locks (along with closeToast) to ensure that there are
	 * not concurrency problems with the preference which may lead to 
	 * 'residual' toasts - i.e. toasts no longer shown but that the system thinks
	 * are shown.
	 * There is also time checking 'just in case' I screwed up the concurrency, so that
	 * it will clear after the longest period that a toast can be on screen.
	 * @return
	 */
	public int openToast() {
		int toastCount;
		
		synchronized(lock) {
			Editor editor = this.prefs.edit();
			toastCount = this.prefs.getInt(CURRENT_TOAST_COUNT, 0);
			long currentTime = System.currentTimeMillis();
			
			if (toastCount > 0) {
				long lastToastTime = this.prefs.getLong(LAST_TOAST_TIME, 0);
				
				//Toast LONG_DELAY = 3.5s, SHORT_DELAY = 2 seconds
				switch (getNotificationDuration()) {
				case Toast.LENGTH_SHORT:
					lastToastTime += 2000;
					break;
				case Toast.LENGTH_LONG:
					lastToastTime += 3500;
					break;
				}
				
				if (lastToastTime < currentTime) {
					toastCount = 0;
				}
			}
			
			editor.putInt(CURRENT_TOAST_COUNT, toastCount++);
			editor.putLong(LAST_TOAST_TIME, currentTime);
			editor.commit();
		}
		return toastCount;
	}
	
	public void closeToast() {
		synchronized(lock) {
			Editor editor = this.prefs.edit();
			int toastCount = this.prefs.getInt(CURRENT_TOAST_COUNT, 0);
			if (toastCount > 0) {
				editor.putInt(CURRENT_TOAST_COUNT, --toastCount);
				editor.commit();
			}
		}
	}
}

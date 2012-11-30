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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.util.Log;


/**
 * Loads the settings list for a single application from the database.
 * @author smorgan
 */
public class AppSettingsSaveTask extends AsyncTask<PDroidAppSetting, Integer, Void> {
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	final String packageName;
	final boolean notifySetting;
	final Integer uid;
	
	public AppSettingsSaveTask(Context context, String packageName, Integer uid, boolean notifySetting, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
		this.notifySetting = notifySetting;
		
		//TODO: Exception handling: null packageName
		this.packageName = packageName;
		this.uid = uid;
	}
		
	@Override
	protected Void doInBackground(PDroidAppSetting... appSettings) {
		//TODO: Exception handling: null appSettings

		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		PrivacySettings privacySettings = privacySettingsManager.getSettings(packageName);
		
		
		//There are no existing privacy settings for this app - we need to create them
		if (privacySettings == null) {
			if (uid == null) {
				throw new RuntimeException("A UID must be provided when a new PrivacySettings is being created");
			}
			privacySettings = new PrivacySettings(null, packageName, uid);
		}

		Method setMethod;
		Class<?> privacySettingsClass = privacySettings.getClass();
		
		if (notifySetting) {
			privacySettings.setNotificationSetting(PrivacySettings.SETTING_NOTIFY_ON);
		} else {
			privacySettings.setNotificationSetting(PrivacySettings.SETTING_NOTIFY_OFF);
		}
		
		for (PDroidAppSetting appSetting : appSettings) {
			try {
				setMethod = privacySettingsClass.getMethod("set" + appSetting.getSettingFunctionName(), byte.class);
				switch (appSetting.getSelectedOptionBit()) {
				case PDroidSetting.OPTION_FLAG_ALLOW:
				case PDroidSetting.OPTION_FLAG_YES:
					setMethod.invoke(privacySettings, PrivacySettings.REAL);
					break;
				case PDroidSetting.OPTION_FLAG_CUSTOM:
				case PDroidSetting.OPTION_FLAG_CUSTOMLOCATION:
					setMethod.invoke(privacySettings, PrivacySettings.CUSTOM);
					for (SimpleImmutableEntry<String, String> settingValue : appSetting.getCustomValues()) {
						setMethod = privacySettingsClass.getMethod("set" + appSetting.getValueFunctionNameStub() + settingValue.getKey(), String.class);
						setMethod.invoke(privacySettings, settingValue.getValue());
					}
					break;				
				case PDroidSetting.OPTION_FLAG_DENY:
				case PDroidSetting.OPTION_FLAG_NO:
					setMethod.invoke(privacySettings, PrivacySettings.EMPTY);
					break;
				case PDroidSetting.OPTION_FLAG_RANDOM:
					setMethod.invoke(privacySettings, PrivacySettings.RANDOM);
					break;
				}
			} catch (NoSuchMethodException e) {
			   Log.d("PDroidAlternative","PrivacySettings object of privacy service is missing the expected method " + appSetting.getSettingFunctionName());
			   e.printStackTrace();
			} catch (IllegalArgumentException e) {
				Log.d("PDroidAlternative","Illegal arguments when calling " + appSetting.getSettingFunctionName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d("PDroidAlternative","Illegal access when calling " + appSetting.getSettingFunctionName());
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.d("PDroidAlternative","InvocationTargetException when calling " + appSetting.getSettingFunctionName());
				e.printStackTrace();
			}
		}
		privacySettingsManager.saveSettings(privacySettings);

		//Update the app settings based on the new data: 1. check if trusted and 2. it now does have settings
		Application app = Application.fromDatabase(context, packageName);
		PermissionSettingHelper psh = new PermissionSettingHelper();
		DBInterface dbinterface = DBInterface.getInstance(context);
		SQLiteDatabase write_db = dbinterface.getDBHelper().getWritableDatabase();
		app.setIsUntrusted(psh.isPrivacySettingsUntrusted(write_db, privacySettings));
		app.setHasSettings(true);
		dbinterface.updateApplicationRecord(app);
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

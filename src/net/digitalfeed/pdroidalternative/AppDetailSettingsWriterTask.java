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
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.util.Log;


/**
 * Loads the settings list for a single application from the database.
 * @author smorgan
 */
public class AppDetailSettingsWriterTask extends AsyncTask<AppSetting, Integer, Void> {
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	final String packageName;
	
	public AppDetailSettingsWriterTask(Context context, String packageName, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
		
		//TODO: Exception handling: null packageName
		this.packageName = packageName;
	}
		
	@Override
	protected Void doInBackground(AppSetting... appSettings) {
		//TODO: Exception handling: null appSettings

		Log.d("PDroidAlternative","Running update of settings for " + packageName);
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		PrivacySettings privacySettings = privacySettingsManager.getSettings(packageName);

		Method setMethod;
		Class<?> privacySettingsClass = privacySettings.getClass();
		
		for (AppSetting appSetting : appSettings) {
			Log.d("PDroidAlternative","Processing setting " + appSetting.getId());
			try {
				setMethod = privacySettingsClass.getMethod("set" + appSetting.getSettingFunctionName(), byte.class);
				Log.d("PDroidAlternative","Get method: " + appSetting.getSettingFunctionName());
				switch (appSetting.getSelectedOptionBit()) {
				case Setting.OPTION_FLAG_ALLOW:
				case Setting.OPTION_FLAG_YES:
					setMethod.invoke(privacySettings, PrivacySettings.REAL);
					Log.d("PDroidAlternative","Invoked set" + appSetting.getSettingFunctionName() + " for REAL");
					break;
				case Setting.OPTION_FLAG_CUSTOM:
				case Setting.OPTION_FLAG_CUSTOMLOCATION:
					setMethod.invoke(privacySettings, PrivacySettings.CUSTOM);
					Log.d("PDroidAlternative","Invoked set" + appSetting.getSettingFunctionName() + " for CUSTOM");
					for (SimpleImmutableEntry<String, String> settingValue : appSetting.getCustomValues()) {
						setMethod = privacySettingsClass.getMethod("set" + appSetting.getValueFunctionNameStub() + settingValue.getKey(), String.class);
						setMethod.invoke(privacySettings, settingValue.getValue());
						Log.d("PDroidAlternative","Invoked set" + appSetting.getValueFunctionNameStub() + settingValue.getKey());
					}
					break;				
				case Setting.OPTION_FLAG_DENY:
				case Setting.OPTION_FLAG_NO:
					setMethod.invoke(privacySettings, PrivacySettings.EMPTY);
					Log.d("PDroidAlternative","Invoked set" + appSetting.getSettingFunctionName() + " for DENY");
					break;
				case Setting.OPTION_FLAG_RANDOM:
					Log.d("PDroidAlternative","Invoked set" + appSetting.getSettingFunctionName() + " for RANDOM");
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
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

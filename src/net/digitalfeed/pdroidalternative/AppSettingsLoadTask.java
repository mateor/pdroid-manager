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
import java.security.InvalidParameterException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Loads the settings list for a single application from the database.
 * @author smorgan
 */
public class AppSettingsLoadTask extends AsyncTask<String, Integer, List<PDroidAppSetting>> {
	
	IAsyncTaskCallback<List<PDroidAppSetting>> listener;
	
	Context context;
	
	public AppSettingsLoadTask(Context context, IAsyncTaskCallback<List<PDroidAppSetting>> listener) {
		this.context = context;
		this.listener = listener;
	}
		
	@Override
	protected List<PDroidAppSetting> doInBackground(String... selectPackageName) {
		if (selectPackageName == null || selectPackageName.length != 1) {
			throw new InvalidParameterException("One and only one package name must be provided to the AppDetailSettingsLoader");
		}
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		//Keep in mind that getSettings will return null when the package has no settings yet. 
		PrivacySettings privacySettings = privacySettingsManager.getSettings(selectPackageName[0]);
		
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		
		Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_BY_PACKAGENAME, selectPackageName);
    	int idColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_ID);
    	int nameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_NAME);
    	int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
    	int valueFunctionNameStubColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_VALUEFUNCTIONNAMESTUB);
    	int titleColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TITLE);
    	int groupColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_ID);
    	int groupTitleColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
    	int optionsColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_OPTIONS);
    	int trustedOptionColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TRUSTED_OPTION);
    	
		cursor.moveToFirst();
		List<PDroidAppSetting> settingSet = new ArrayList<PDroidAppSetting>(cursor.getCount());
		
		Method method;
		
		do {
			String id = cursor.getString(idColumn);
			String name = cursor.getString(nameColumn);
			String settingFunctionName = cursor.getString(settingFunctionNameColumn);
			String valueFunctionNameStub = cursor.getString(valueFunctionNameStubColumn);
			String title = cursor.getString(titleColumn);
			String group = cursor.getString(groupColumn);
			String groupTitle = cursor.getString(groupTitleColumn);
			String options = cursor.getString(optionsColumn);
			String trustedOption = cursor.getString(trustedOptionColumn);

			String [] optionsArray = null;
			if (options != null) {
				optionsArray = TextUtils.split(options, ",");
			}
						
			int selectedOption = PDroidSetting.OPTION_FLAG_ALLOW;
			String customValue = null;
			LinkedList<SimpleImmutableEntry<String, String>> customValues = null;

			if (privacySettings != null) {
				/*
				 * Reflection may not be the best way of doing this, but otherwise this is going to be a great
				 * big select statement, which ties us more closely to the specific set of options (which are
				 * right now fairly loosely coupled - although to entirely loosely coupled)
				 * 
				 * It would work even better if the function names were actually consistent
				 * i.e. not 'setAndroidIdSetting' for the setting and 'setAndroidID' for the value.
				 * This makes it necessary to include separate content in the database for the
				 * 'get..setting' and 'set..setting' vs value setting functions, or have a special
				 * 'catch' case when reading or writing.
				 */

				List<String> tmpList = Arrays.asList(optionsArray);
				try {
					method = privacySettings.getClass().getMethod("get" + settingFunctionName);
					byte pdroidCoreSetting = (Byte)method.invoke(privacySettings);
					switch (pdroidCoreSetting) {
					case PrivacySettings.REAL:
						if (tmpList.contains(PDroidSetting.OPTION_TEXT_ALLOW)) {
							selectedOption = PDroidSetting.OPTION_FLAG_ALLOW;
						} else if (tmpList.contains(PDroidSetting.OPTION_TEXT_YES)) {
							selectedOption = PDroidSetting.OPTION_FLAG_YES;
						} else {
							//I don't think this is the best exception to be using here, but I
							//am not sure which is a better one
							throw new RuntimeException("Invalid 'real' setting for " + settingFunctionName);
						}
						break;
					case PrivacySettings.CUSTOM:
						if (tmpList.contains(PDroidSetting.OPTION_TEXT_CUSTOM)) {
							method = privacySettings.getClass().getMethod("get" + valueFunctionNameStub);
							customValue = (String)method.invoke(privacySettings);
							selectedOption = PDroidSetting.OPTION_FLAG_CUSTOM;
						} else if (tmpList.contains(PDroidSetting.OPTION_TEXT_CUSTOMLOCATION)) {
							customValues = new LinkedList<SimpleImmutableEntry<String, String>>();
							method = privacySettings.getClass().getMethod("get" + valueFunctionNameStub + "Lat");
							customValues.add(new SimpleImmutableEntry<String, String>("Lat", (String)method.invoke(privacySettings)));
							method = privacySettings.getClass().getMethod("get" + valueFunctionNameStub + "Lon");
							customValues.add(new SimpleImmutableEntry<String, String>("Lon", (String)method.invoke(privacySettings)));
							selectedOption = PDroidSetting.OPTION_FLAG_CUSTOMLOCATION;
						} else {
							//I don't think this is the best exception to be using here, but I
							//am not sure which is a better one
							throw new RuntimeException("Invalid 'custom' setting for " + settingFunctionName);
						}
						break;
					case PrivacySettings.EMPTY:
						if (tmpList.contains(PDroidSetting.OPTION_TEXT_DENY)) {
							selectedOption = PDroidSetting.OPTION_FLAG_DENY;
						} else if (tmpList.contains(PDroidSetting.OPTION_TEXT_NO)) {
							selectedOption = PDroidSetting.OPTION_FLAG_NO;
						} else {
							//I don't think this is the best exception to be using here, but I
							//am not sure which is a better one
							throw new RuntimeException("Invalid 'empty' setting for " + settingFunctionName);
						}
						break;
					case PrivacySettings.RANDOM:
						selectedOption = PDroidSetting.OPTION_FLAG_RANDOM;
						break;
					default:
						if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Unrecognised Privacy Setting type");
					}
				} catch (NoSuchMethodException e) {
				   if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"PrivacySettings object of privacy service is missing the expected method " + settingFunctionName);
				   e.printStackTrace();
				} catch (IllegalArgumentException e) {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Illegal arguments when calling " + settingFunctionName);
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Illegal access when calling " + settingFunctionName);
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"InvocationTargetException when calling " + settingFunctionName);
					e.printStackTrace();
				}
			}
			
			if (customValues != null) {
				settingSet.add(new PDroidAppSetting(id, name, settingFunctionName, valueFunctionNameStub, title, group, groupTitle, optionsArray, trustedOption, selectedOption, customValues));
			} else {
				settingSet.add(new PDroidAppSetting(id, name, settingFunctionName, valueFunctionNameStub, title, group, groupTitle, optionsArray, trustedOption, selectedOption, customValue));
			}
		} while (cursor.moveToNext());
		cursor.close();
		//db.close();
		return settingSet;
	}
	
	@Override
	protected void onPostExecute(List<PDroidAppSetting> result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

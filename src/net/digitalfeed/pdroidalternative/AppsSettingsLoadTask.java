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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
public class AppsSettingsLoadTask extends AsyncTask<String, Integer, List<PDroidAppSetting>> {

	IAsyncTaskCallback<List<PDroidAppSetting>> listener;

	Context context;

	public AppsSettingsLoadTask(Context context, IAsyncTaskCallback<List<PDroidAppSetting>> listener) {
		this.context = context;
		this.listener = listener;
	}

	@Override
	protected List<PDroidAppSetting> doInBackground(String... packageNames) {
		if (packageNames == null || packageNames.length < 1) {
			throw new InvalidParameterException("One ore more package names must be provided to the AppsDetailSettingsLoader");
		}

		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();

		List<PDroidAppSetting> settingSet = new LinkedList<PDroidAppSetting>();
		Set<String> includedSettingNames = new HashSet<String>(); 

		for (String packageName : packageNames) {
			Log.d(GlobalConstants.LOG_TAG, "Searching database for settings associated with package " + packageName);
			Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_BY_PACKAGENAME, new String [] {packageName});
			if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {
				int idColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_ID);
				int nameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_NAME);
				int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
				int valueFunctionNameStubColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_VALUEFUNCTIONNAMESTUB);
				int titleColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TITLE);
				int groupColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_ID);
				int groupTitleColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
				int optionsColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_OPTIONS);
				int trustedOptionColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TRUSTED_OPTION);

				do {
					String id = cursor.getString(idColumn);
					if (!includedSettingNames.contains(id)) {
						String name = cursor.getString(nameColumn);
						String settingFunctionName = cursor.getString(settingFunctionNameColumn);
						String valueFunctionNameStub = cursor.getString(valueFunctionNameStubColumn);
						String title = cursor.getString(titleColumn);
						String group = cursor.getString(groupColumn);
						String groupTitle = cursor.getString(groupTitleColumn);
						String options = cursor.getString(optionsColumn);
						String trustedOption = cursor.getString(trustedOptionColumn);
						int selectedOption = PDroidSetting.OPTION_FLAG_NO_CHANGE;
						String customValue = null;

						String [] optionsArray = null;
						if (options != null) {
							optionsArray = TextUtils.split(options, ",");
						}

						settingSet.add(new PDroidAppSetting(id, name, settingFunctionName, valueFunctionNameStub, title, group, groupTitle, optionsArray, trustedOption, selectedOption, (String)null));
					}
				} while (cursor.moveToNext());
				cursor.close();
				cursor = null;
			}

			//db.close();
		}
		return settingSet;
	}

	@Override
	protected void onPostExecute(List<PDroidAppSetting> result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}

}

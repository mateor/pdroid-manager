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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;

/**
 * Updates all the settings of a single application with the PDroid core, based on the
 * option passed through. Typically, this will be Setting.OPTION_FLAG_ALLOW or
 * Settings.OPTION_FLAG_DENY although others are allowed. 
 * @author smorgan
 *
 */
public class AppListUpdateAllSettingsTask extends AsyncTask<Application, Void, Void> { 
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	final int newOption; 
	
	/**
	 * Constructor
	 * @param context Context used to obtain a database connection (beware threading problems!)
	 * @param newOption The new Setting.OPTION_FLAG_xxx to set (which is mapped to the PDroid core constant)
	 * @param listener Listener implementing IAsyncTaskCallback to which the asyncTaskComplete call will be made
	 */
	public AppListUpdateAllSettingsTask(Context context, int newOption, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
		this.newOption = newOption;
	}
	
	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	@Override
	protected Void doInBackground(Application... inApps) {
		if (inApps == null) {
			throw new NullPointerException("No apps were provided to the AppListUpdateAllSettingsTask");
		}
		
		//TODO: Update the application_status record with the new trusted/untrusted status when updating the privacy settings

		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		PrivacySettings privacySettings;
		
		PermissionSettingHelper helper = new PermissionSettingHelper();
		String packageName;
		
		for (Application app : inApps) {
			packageName = app.getPackageName();
			
			privacySettings = privacySettingsManager.getSettings(app.getPackageName());
			helper.setPrivacySettings(db, privacySettings, newOption);
			privacySettingsManager.saveSettings(privacySettings);
		}
		db.close();
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

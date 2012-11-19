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
import android.privacy.PrivacySettingsManager;
import android.util.Log;


/**
 * Deletes the settings for a single app (identified by packageName) from PrivacySettingsManager.
 * 
 * @author smorgan
 */
public class AppSettingsDeleteTask extends AsyncTask<Void, Void, Void> {
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	final String packageName;
	
	public AppSettingsDeleteTask(Context context, String packageName, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
		
		//TODO: Exception handling: null packageName
		this.packageName = packageName;
	}
	
	//TODO: Update the status flag in the DB for the application for which the Privacy settings are being deletede
	@Override
	protected Void doInBackground(Void... params) {
		Log.d("PDroidAlternative","Running update of settings for " + packageName);
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		//TODO: check what happens if settings don't exist for the package
		privacySettingsManager.deleteSettings(packageName);

		//Set the app status to specify that it has no privacy settings
		Application app = Application.fromDatabase(context, packageName);
		DBInterface dbinterface = DBInterface.getInstance(context);
		app.setIsUntrusted(false);
		app.setHasSettings(false);
		dbinterface.updateApplicationRecord(app);
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

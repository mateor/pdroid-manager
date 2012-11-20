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
import android.os.AsyncTask;
import android.privacy.PrivacySettingsManager;

/**
 * Deletes privacy setting of one or more applications from the PDroid core
 * and updates the database (and application objects) to match
 * 
 * @author smorgan
 *
 */
public class ApplicationsDeleteSettingsTask extends AsyncTask<Application, Void, Void> { 
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	
	/**
	 * Constructor
	 * @param context Context used to obtain a database connection (beware threading problems!)
	 * @param listener Listener implementing IAsyncTaskCallback to which the asyncTaskComplete call will be made
	 */
	
	public ApplicationsDeleteSettingsTask(Context context, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	@Override
	protected Void doInBackground(Application... inApps) {
		if (inApps == null) {
			throw new NullPointerException("No apps were provided");
		}
		
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		DBInterface dbinterface = DBInterface.getInstance(context);

		for (Application app : inApps) {
			privacySettingsManager.deleteSettings(app.getPackageName());
			app.setHasSettings(false);
			app.setIsUntrusted(false); //An app with no settings is not untrusted
			dbinterface.updateApplicationRecord(app);
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

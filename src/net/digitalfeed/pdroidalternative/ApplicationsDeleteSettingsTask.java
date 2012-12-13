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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettingsManager;
import android.util.Log;

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
		//this is here just to trigger a purge when completing removal of *all* settings
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		
		boolean coreBatchSupport = false;
		boolean deletingAll = false;
		
		try {
			PrivacySettingsManager.class.getMethod(GlobalConstants.CORE_SUPPORTS_EXTENSION_FUNCTION);
			coreBatchSupport = privacySettingsManager.supportsExtension("batch");
		} catch (NoSuchMethodException e) {
			if (GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "No extension support in this version of PDroid");
		}
		
		DBInterface dbinterface = DBInterface.getInstance(context);

		if (inApps == null) {
			//if no apps provided, then wipe settings for ALL apps.
			deletingAll = true;
			
			SQLiteDatabase write_db = dbinterface.getDBHelper().getWritableDatabase();
			write_db.rawQuery(
					"UPDATE " + DBInterface.ApplicationStatusTable.TABLE_NAME +
					" SET " + DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS + 
					" = " + DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS +
					" & ?", new String [] { Integer.toString(~(Application.STATUS_FLAG_HAS_PRIVACYSETTINGS | Application.STATUS_FLAG_IS_UNTRUSTED))});
		}
		
		if (deletingAll && coreBatchSupport) {
			// the convenient new function 'deleteSettingsAll' makes this particularly quick and easy, don't you think?
			//dbinterface.updateAllApplicationStatus()
			privacySettingsManager.deleteSettingsAll();
		} else {
			if (deletingAll) {
				// TODO: need to add an easy way to update the status for all apps somehow,
				// without directly attacking the database			
				//to do this, we use the ApplicationsDatabaseFiller task to get all the apps,
				//but run the function directly rather than using .execute because we want to block while
				//waiting for a response.
				//TODO: Move the code for these many AsyncTasks out of the tasks so they are easier to use
				ApplicationsObjectLoaderTask filler = new ApplicationsObjectLoaderTask(context, null);
				Map<String, Application> appMap = filler.doInBackground((Void)null);
				inApps = appMap.values().toArray(new Application [appMap.size()]);
			}
			
			List<Application> appsToUpdate = new LinkedList<Application>();
			List<String> packagesForDeletion = null;
			if (coreBatchSupport) {
				packagesForDeletion = new LinkedList<String>();
			}
			
			for (Application app : inApps) {
				// If we have core batch support, add the package name to a list for deletion
				// If not, we have to delete one at a time
				if (coreBatchSupport) {
					packagesForDeletion.add(app.getPackageName());
				} else {
					privacySettingsManager.deleteSettings(app.getPackageName());
				}
				appsToUpdate.add(app);
				app.setHasSettings(false);
				app.setIsUntrusted(false); //An app with no settings is not untrusted
			}
			
			dbinterface.updateApplicationStatus(appsToUpdate);
			if (coreBatchSupport) {
				privacySettingsManager.deleteSettingsMany(packagesForDeletion);
			}
		}
		
		if (deletingAll) {
			//If the input list of applications was null (i.e. delete ALL settings) then we should also purge unused settings from the core 
			privacySettingsManager.purgeSettings();
		}		
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

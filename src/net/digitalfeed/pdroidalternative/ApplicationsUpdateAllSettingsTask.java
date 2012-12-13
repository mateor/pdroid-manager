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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.digitalfeed.pdroidalternative.PermissionSettingHelper.TrustState;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.util.Log;

/**
 * Updates all settings of a one or more applications to match the trusted or 
 * untrusted states (depending on parameter passed to the constructor).
 * The 'untrusted' state is derived by reversing the 'trusted' state internally.
 * See the PermissionSettingHelper class for more details.
 * 
 * 
 * @author smorgan
 *
 */
public class ApplicationsUpdateAllSettingsTask extends AsyncTask<Application, Void, Void> { 
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	final TrustState newTrustState; 
	
	/**
	 * Constructor
	 * @param context Context used to obtain a database connection (beware threading problems!)
	 * @param newTrustState  The new 'trust state' to change settings to
	 * @param listener Listener implementing IAsyncTaskCallback to which the asyncTaskComplete call will be made
	 */
	
	public ApplicationsUpdateAllSettingsTask(Context context, TrustState newTrustState, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
		this.newTrustState = newTrustState;
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
		
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		if (privacySettingsManager == null) {
			return null;
		}
		
		boolean coreBatchSupport = false;
		
		try {
			PrivacySettingsManager.class.getMethod(GlobalConstants.CORE_SUPPORTS_EXTENSION_FUNCTION);
			coreBatchSupport = privacySettingsManager.supportsExtension("batch");
		} catch (NoSuchMethodException e) {
			if (GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "No extension support in this version of PDroid");
		}
		
		DBInterface dbinterface = DBInterface.getInstance(context);
		SQLiteDatabase db = dbinterface.getDBHelper().getWritableDatabase();
		//List<String> packagesToUpdate = new LinkedList<String>();
		Map<String, Integer> packagesToUpdate = null;
		PermissionSettingHelper helper = new PermissionSettingHelper();
		
		if (coreBatchSupport) {
			packagesToUpdate = new HashMap<String, Integer>();
		}
		List<Application> appsToUpdate = new LinkedList<Application>();
		PrivacySettings privacySettings;
		
		for (Application app : inApps) {
			if (coreBatchSupport) {
				// We have core batch support, we stash the package details for mass handling,
				packagesToUpdate.put(app.getPackageName(), app.getUid());
			} else {
				// No core batch support: have to process the apps one at a time
				privacySettings = privacySettingsManager.getSettings(app.getPackageName());
				//There are no existing privacy settings for this app - we need to create them
				if (privacySettings == null) {
					privacySettings = new PrivacySettings(null, app.getPackageName(), app.getUid());
				}
				helper.setPrivacySettingsToTrustState(db, privacySettings, newTrustState);
				privacySettingsManager.saveSettings(privacySettings);
			}
			
			appsToUpdate.add(app);
			app.setHasSettings(true);
			switch (newTrustState) {
			case TRUSTED:
				app.setIsUntrusted(false); //set the app to being trusted in memory
				break;
			case UNTRUSTED:
				app.setIsUntrusted(true); //set the app to being trusted in memory
				break;
			default:
				break;
			}
		}
		
		dbinterface.updateApplicationStatus(appsToUpdate);

		if (coreBatchSupport) {
			// with core batch support, we can now get settings for all the packages
			// for which there are settings, create them where necessary,
			// and save them all very efficiently - i.e. only two calls to the privacy
			// service for any number of package updates
			List<PrivacySettings> privacySettingsList = privacySettingsManager.getSettingsMany(new LinkedList<String>(packagesToUpdate.keySet()));		 
			if (privacySettingsList != null) {
				for (PrivacySettings privacySettingsBatch : privacySettingsList) {
					helper.setPrivacySettingsToTrustState(db, privacySettingsBatch, newTrustState);
					packagesToUpdate.remove(privacySettingsBatch.getPackageName());
				}
			}
			
			for (String packageName : packagesToUpdate.keySet()) {
				privacySettings = new PrivacySettings(null, packageName, packagesToUpdate.get(packageName));
				helper.setPrivacySettingsToTrustState(db, privacySettings, newTrustState);
				privacySettingsList.add(privacySettings);
			}

			privacySettingsManager.saveSettingsMany(privacySettingsList);
		}
		//db.close();
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

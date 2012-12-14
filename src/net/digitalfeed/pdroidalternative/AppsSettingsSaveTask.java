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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;


/**
 * Saves a set of PDroidAppSettings to one or more packages
 * TODO: Optimise to use the "batch" extension of my mauled PDroid core
 * @author smorgan
 */
public class AppsSettingsSaveTask extends AsyncTask<PDroidAppSetting, Integer, Void> {
	
	final IAsyncTaskCallback<Void> listener;
	final Context context;
	final String [] packageNames;
	final boolean notifySetting;
	
	public AppsSettingsSaveTask(Context context, String [] packageNames, boolean notifySetting, IAsyncTaskCallback<Void> listener) {
		this.context = context;
		this.listener = listener;
		this.notifySetting = notifySetting;
		
		//TODO: Exception handling: null packageName
		this.packageNames = packageNames;
	}
		
	@Override
	protected Void doInBackground(PDroidAppSetting... appSettings) {
		//TODO: Exception handling: null appSettings

		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");

		PackageManager pkgMgr = null;
		int uid;

		//Work out the new trust state of the apps
		boolean isTrusted = true;
		for (PDroidAppSetting appSetting : appSettings) {
			if (appSetting.trustedOptionBit != appSetting.selectedOptionBit) {
				isTrusted = false;
				break;
			}
		}
		
		PermissionSettingHelper psh = new PermissionSettingHelper();
		DBInterface dbinterface = DBInterface.getInstance(context);

		//update the privacySetting objects for the packages
		for (String packageName : packageNames) {
			PrivacySettings privacySettings = privacySettingsManager.getSettings(packageName);
			
			//There are no existing privacy settings for this app - we need to create them
			if (privacySettings == null) {
				if (pkgMgr == null) {
					pkgMgr = context.getPackageManager();
				}
				if (pkgMgr != null) {
					try {
						uid = pkgMgr.getPackageInfo(packageName, 0).applicationInfo.uid;
					} catch (NameNotFoundException e) {
						e.printStackTrace();
						throw new RuntimeException("UID could not be obtained for package");
					}
					privacySettings = new PrivacySettings(null, packageName, uid);
				} else {
					throw new RuntimeException("PackageManager could not be obtained");
				}
			}
	
			if (notifySetting) {
				privacySettings.setNotificationSetting(PrivacySettings.SETTING_NOTIFY_ON);
			} else {
				privacySettings.setNotificationSetting(PrivacySettings.SETTING_NOTIFY_OFF);
			}
			
			
			psh.setPrivacySettings(privacySettings, appSettings);
			privacySettingsManager.saveSettings(privacySettings);
	
			//Update the app settings based on the new data: 1. check if trusted and 2. it now does have settings
			Application app = Application.fromDatabase(context, packageName);
			app.setIsUntrusted(!isTrusted);
			app.setHasSettings(true);
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

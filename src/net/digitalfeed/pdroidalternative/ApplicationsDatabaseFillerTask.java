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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

import net.digitalfeed.pdroidalternative.DBInterface.ApplicationStatusTable;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Generates a list of applications from the OS and writes them to the database.
 * 
 * This provides much of the same functionality as the Application.fromPackageName
 * function, but also checks the 'trust' state from the PDroid core, and writes the
 * result to the database. It uses optimisations like using transactions, and 
 * insert helpers, to speed up the process.
 * 
 * @author smorgan
 *
 */
public class ApplicationsDatabaseFillerTask extends AsyncTask<Void, Integer, HashMap<String, Application>> {
		
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL = 0;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 1;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID = 2;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE = 3;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS = 4;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON = 5;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS = 6;
	
	protected static final int APPLICATION_STATUS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 0;
	protected static final int APPLICATION_STATUS_TABLE_COLUMN_NUMBER_OFFSET_FLAGS = 1;
	
	IAsyncTaskCallbackWithProgress<HashMap<String, Application>> listener;
	
	Context context;
	int includeAppTypes;
	
	public ApplicationsDatabaseFillerTask(Context context, IAsyncTaskCallbackWithProgress<HashMap<String, Application>> listener) {
		this.context = context;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	/**
	 * Retrieves the list of applications, and returns as an array of Application objects
	 */
	@Override
	protected HashMap<String, Application> doInBackground(Void... params) {
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		
		HashMap<String, Application> appList = new HashMap<String, Application>();
		PackageManager pkgMgr = context.getPackageManager();
		
		List<ApplicationInfo> installedApps = pkgMgr.getInstalledApplications(PackageManager.GET_META_DATA);

		Integer[] progressObject = new Integer[2];
		progressObject[0] = 0;
		progressObject[1] = installedApps.size();
		
		publishProgress(progressObject.clone());

		SQLiteDatabase write_db = DBInterface.getInstance(context).getDBHelper().getWritableDatabase();
		//Being inside a transaction speeds things up (see http://www.outofwhatbox.com/blog/2010/12/android-using-databaseutils-inserthelper-for-faster-insertions-into-sqlite-database/)
		//I haven't personally checked this
		write_db.beginTransaction();
		//Clear the application list before putting in a new list.
		write_db.delete(DBInterface.ApplicationTable.TABLE_NAME, null, null);
		write_db.delete(DBInterface.ApplicationStatusTable.TABLE_NAME, null, null);
		write_db.delete(DBInterface.PermissionApplicationTable.TABLE_NAME, null, null);

		InsertHelper applicationsInsertHelper = new InsertHelper(write_db, DBInterface.ApplicationTable.TABLE_NAME);
		int [] applicationTableColumnNumbers = new int[7];
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);


		InsertHelper applicationStatusInsertHelper = new InsertHelper(write_db, DBInterface.ApplicationStatusTable.TABLE_NAME);
		int [] applicationStatusTableColumnNumbers = new int[2];
		applicationStatusTableColumnNumbers[APPLICATION_STATUS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME] = applicationStatusInsertHelper.getColumnIndex(DBInterface.ApplicationStatusTable.COLUMN_NAME_PACKAGENAME);
		applicationStatusTableColumnNumbers[APPLICATION_STATUS_TABLE_COLUMN_NUMBER_OFFSET_FLAGS] = applicationStatusInsertHelper.getColumnIndex(DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS);
		
		
		InsertHelper permissionsInsertHelper = new InsertHelper(write_db, DBInterface.PermissionApplicationTable.TABLE_NAME);
		int [] permissionsTableColumnNumbers = new int[2];
		//I was thinking about using enums instead of static finals here, but apparently the performance in android for enums is not so good??
		permissionsTableColumnNumbers[DBInterface.PermissionApplicationTable.OFFSET_PACKAGENAME] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PACKAGENAME);
		permissionsTableColumnNumbers[DBInterface.PermissionApplicationTable.OFFSET_PERMISSION] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PERMISSION);
		
		/*
		 * We don't (and can't) filter out apps based on permissions, because some things we provide settings for
		 * don't require any permissions (e.g. ANDROID_ID)
		 */
		
		PermissionSettingHelper psh = new PermissionSettingHelper();
		
		DisplayMetrics metrics = context.getResources().getDisplayMetrics(); 
		 
		switch(metrics.densityDpi){ 
		     case DisplayMetrics.DENSITY_LOW: 
		                break; 
		     case DisplayMetrics.DENSITY_MEDIUM: 
		                 break; 
		     case DisplayMetrics.DENSITY_HIGH: 
		                 break;
		     case DisplayMetrics.DENSITY_XHIGH: 
                 break;
		} 
		
		//This doesn't work properly at the moment, because unlike what the documentation says, 'density' returns the *exact*
		//density, and not the 'quantized' density - at least for my Nexus 7
		//int iconSizePx = (int)(context.getResources().getDisplayMetrics().density * (float)Application.TARGET_ICON_SIZE);
		//this makes sure the images are larger than needed, but it is a pretty rubbish temporary solution. It uses more ram than necessary, and the icons may not scale well
		int iconSizePx = Application.TARGET_ICON_SIZE * 2;
		byte [] blankIcon = null;
		
		
		for (ApplicationInfo appInfo : installedApps) {
			try {
				PackageInfo pkgInfo = pkgMgr.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);
				
				int appFlags = 0;
				if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) { 
					appFlags = Application.APP_FLAG_IS_SYSTEM_APP;
				}
				
				if (pkgMgr.checkPermission("android.permission.INTERNET", appInfo.packageName) == PackageManager.PERMISSION_GRANTED) {
					appFlags = appFlags | Application.APP_FLAG_HAS_INTERNET;
				}

				int statusFlags = 0;
				
				PrivacySettings privacySettings = privacySettingsManager.getSettings(appInfo.packageName);
				if (privacySettings != null) {
					//I would prefer to be getting a new readable database handle for this, but
					//if I do that, then call close on it, it closes my writable handle too.
					if (psh.isPrivacySettingsUntrusted(write_db, privacySettings)) {
						statusFlags = statusFlags | ApplicationStatusTable.FLAG_IS_UNTRUSTED;
					}
					
					if (privacySettings.getNotificationSetting() == PrivacySettings.SETTING_NOTIFY_ON) {
						statusFlags = statusFlags | ApplicationStatusTable.FLAG_NOTIFY_ON_ACCESS;
					}
					statusFlags = statusFlags | ApplicationStatusTable.FLAG_HAS_PRIVACYSETTINGS;
				}
				
				/*
				 * An alternative to putting the apps in the database and simultaneously creating a list of them
				 * to return is to just write to the database, then re-read them afterwards. Since we
				 * want the app list anyway, this makes more sense to me.
				 */
				
				applicationsInsertHelper.prepareForInsert();
				String [] permissions = pkgInfo.requestedPermissions;
				if (permissions != null) {
					applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS], TextUtils.join(",", pkgInfo.requestedPermissions));
					for (String permission : permissions) {
						permissionsInsertHelper.prepareForInsert();
						permissionsInsertHelper.bind(permissionsTableColumnNumbers[DBInterface.PermissionApplicationTable.OFFSET_PACKAGENAME], appInfo.packageName);
						permissionsInsertHelper.bind(permissionsTableColumnNumbers[DBInterface.PermissionApplicationTable.OFFSET_PERMISSION], permission);
						permissionsInsertHelper.execute();
					}
				} else {
					applicationsInsertHelper.bindNull(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS]);
				}
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL], pkgMgr.getApplicationLabel(appInfo).toString());
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME], appInfo.packageName);
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID], appInfo.uid);
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE], pkgInfo.versionCode);
				
				// Try to get the application icon as a byte array. If it fails (i.e. returns null) then use the provided 'blank' image.
				byte [] iconByteArray = IconHelper.getIconByteArray(pkgMgr.getApplicationIcon(appInfo.packageName), iconSizePx);
				if (iconByteArray == null) {
				    if (blankIcon == null) {
				        blankIcon = IconHelper.getByteArray(context.getResources().openRawResource(R.raw.blank));
				    }
				    iconByteArray = blankIcon;
				}
				
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON], iconByteArray);
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS], appFlags);
				applicationsInsertHelper.execute();

				applicationStatusInsertHelper.prepareForInsert();
				applicationStatusInsertHelper.bind(applicationStatusTableColumnNumbers[APPLICATION_STATUS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME], appInfo.packageName);
				applicationStatusInsertHelper.bind(applicationStatusTableColumnNumbers[APPLICATION_STATUS_TABLE_COLUMN_NUMBER_OFFSET_FLAGS], statusFlags);
				applicationStatusInsertHelper.execute();
				
				Application app = new Application(
						appInfo.packageName,
						pkgMgr.getApplicationLabel(appInfo).toString(),
						pkgInfo.versionCode,
						appFlags,
						statusFlags,
						appInfo.uid,
						pkgMgr.getApplicationIcon(appInfo.packageName),
						permissions
						);
				appList.put(appInfo.packageName, app);
				
				//Below is the alterative to using the insert helper. It is 'neater' but is almost certainly slower because
				//it doesn't use compiled SQL queries
				//write_db.insert(DBInterface.ApplicationTable.TABLE_NAME, null, DBInterface.ApplicationTable.getContentValues(app));
			} catch (NameNotFoundException e) {	
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, String.format("Application %s went missing from installed applications list", appInfo.packageName));
			}
			progressObject[0] += 1;
			publishProgress(progressObject.clone());
		}
		
		applicationsInsertHelper.close();
		applicationStatusInsertHelper.close();
		permissionsInsertHelper.close();
		
		write_db.setTransactionSuccessful();
		write_db.endTransaction();
		//write_db.close();
		return appList;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		listener.asyncTaskProgressUpdate(progress);
	}
	
	@Override
	protected void onPostExecute(HashMap<String, Application> result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
}
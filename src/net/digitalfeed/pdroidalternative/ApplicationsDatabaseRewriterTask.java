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
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.DisplayMetrics;

/**
 * Purges all applications from the database, and writes a replacement set
 * from the array of Application objects provided.
 * 
 * @author smorgan
 *
 */
public class ApplicationsDatabaseRewriterTask extends AsyncTask<Application, Void, Void> {
	protected static final int PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 0;
	protected static final int PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION = 1;
	
	private Context context;
	
	public ApplicationsDatabaseRewriterTask(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Application... params) {
		
		int iconSizePx = context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_MEDIUM * Application.TARGET_ICON_SIZE;
		
		SQLiteDatabase write_db = DBInterface.getInstance(context).getDBHelper().getWritableDatabase();
		//Clear the application list before putting in a new list.
		write_db.delete(DBInterface.ApplicationTable.TABLE_NAME, null, null);
		//Being inside a transaction speeds things up (see http://www.outofwhatbox.com/blog/2010/12/android-using-databaseutils-inserthelper-for-faster-insertions-into-sqlite-database/)
		//I haven't personally checked this
		write_db.beginTransaction();

		InsertHelper applicationsInsertHelper = new InsertHelper(write_db, DBInterface.ApplicationTable.TABLE_NAME);
		int [] applicationTableColumnNumbers = new int[7];
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_LABEL] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_PACKAGENAME] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME);
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_UID] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID);
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_VERSIONCODE] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_PERMISSIONS] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS);
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_ICON] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON);
		applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_APPFLAGS] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);

		InsertHelper permissionsInsertHelper = new InsertHelper(write_db, DBInterface.PermissionApplicationTable.TABLE_NAME);
		int [] permissionsTableColumnNumbers = new int[2];
		//I was thinking about using enums instead of static finals here, but apparently the performance in android for enums is not so good??
		permissionsTableColumnNumbers[PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PACKAGENAME);
		permissionsTableColumnNumbers[PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PERMISSION);

		for (Application app : params) {
			applicationsInsertHelper.prepareForInsert();
			String [] permissions = app.getPermissions();
			if (permissions != null) {
				applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_PERMISSIONS], TextUtils.join(",", permissions));
				for (String permission : permissions) {
					permissionsInsertHelper.prepareForInsert();
					permissionsInsertHelper.bind(PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME, app.getPackageName());
					permissionsInsertHelper.bind(PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION, permission);
					permissionsInsertHelper.execute();
				}
			} else {
				applicationsInsertHelper.bindNull(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_PERMISSIONS]);
			}
			applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_LABEL], app.getLabel());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_PACKAGENAME], app.getPackageName());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_UID], app.getUid());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_VERSIONCODE], app.getVersionCode());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_ICON], app.getIconByteArray(iconSizePx));
			applicationsInsertHelper.bind(applicationTableColumnNumbers[DBInterface.ApplicationTable.COLUMN_NUMBER_OFFSET_APPFLAGS], app.getAppFlags());
			applicationsInsertHelper.execute();
		}
		
		write_db.rawQuery(DBInterface.QUERY_DELETE_APPS_WITHOUT_STATUS, null);
		write_db.setTransactionSuccessful();
		write_db.endTransaction();
		//write_db.close();
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		Preferences prefs = new Preferences(context);
		prefs.setIsApplicationListCacheValid(true);
	}
}

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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;


/**
 * Loads Application object for a single app by package name. Assumes the app exists in the database.
 * This essentially duplicates the functionality of Application.fromDatabase and should
 * probably be, or modified to use that function.
 * @author smorgan
 */
public class ApplicationLoadTask extends AsyncTask<String, Integer, Application> {
	
	IAsyncTaskCallback<Application> listener;
	
	Context context;
	
	public ApplicationLoadTask(Context context, IAsyncTaskCallback<Application> listener) {
		this.context = context;
		this.listener = listener;
	}
		
	@Override
	protected Application doInBackground(String... selectPackageName) {
		Log.d("PDroidAlternative","Looking up package name: " + selectPackageName[0]);
		
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
    	Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_APPS_BY_PACKAGENAME_WITH_STATUS, selectPackageName);
    	Log.d("PDroidAlternative","Returned item count: " + Integer.toString(cursor.getCount()));
		
		cursor.moveToFirst();
		
		String label = cursor.getString(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL)
				);
		String packageName = cursor.getString(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME)
				);
		int uid = cursor.getInt(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID)
				);
		int versionCode = cursor.getInt(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE)
				);
		int appFlags = cursor.getInt(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS)
				);
		byte[] iconBlob = cursor.getBlob(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON)
				);
		String permissions = cursor.getString(
				cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS)
				);

		Drawable icon = new BitmapDrawable(context.getResources(),BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length));
		
		String [] permissionsArray = null;
		if (permissions != null) {
			permissionsArray = TextUtils.split(permissions, ",");
		}
	
		cursor.close();
    	//db.close();
    	
    	
    	return new Application(packageName, label, versionCode, appFlags, 0, uid, icon, permissionsArray);
	}
	
	@Override
	protected void onPostExecute(Application result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

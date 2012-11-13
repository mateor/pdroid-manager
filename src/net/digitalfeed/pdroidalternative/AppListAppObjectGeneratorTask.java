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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Returns a HashMap of package name and in-memory Application objects for all applications currently
 * recorded in the database. This allows smaller queries retrieving only package names to be used
 * to identify packages of interest to list in the UI, rather than having to regenerate
 * application objects each time.
 * 
 * @author smorgan
 *
 */
public class AppListAppObjectGeneratorTask extends AsyncTask<Void, Integer, HashMap<String, Application>> {
	
	IAsyncTaskCallback<HashMap<String, Application>> listener;
	
	Context context;
	
	public AppListAppObjectGeneratorTask(Context context, IAsyncTaskCallback<HashMap<String, Application>> listener) {
		this.context = context;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	@Override
	protected HashMap<String, Application> doInBackground(Void... params) {
		AppQueryBuilder queryBuilder = new AppQueryBuilder();
		
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_APP);
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_STATUSFLAGS);

		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		HashMap<String, Application> appList = new HashMap<String, Application>();
		
		Cursor cursor = queryBuilder.doQuery(db);
    	
    	if (cursor.getCount() < 1) {
    		throw new DatabaseUninitialisedException("No applications are listed in the database matching the query");
    	}
    	
		cursor.moveToFirst();
		int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME);
    	int labelColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
    	int versionCodeColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
    	int appFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);
    	int statusFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS);
    	int uidColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID);
    	int iconColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON);

    	do {
    		String packageName = cursor.getString(packageNameColumn);
    		String label = cursor.getString(labelColumn);
    		int versionCode = cursor.getInt(versionCodeColumn);
    		int uid = cursor.getInt(uidColumn);
    		int appFlags = cursor.getInt(appFlagsColumn);
    		int statusFlags = cursor.getInt(statusFlagsColumn);
    		byte[] iconBlob = cursor.getBlob(iconColumn);

    		Drawable icon = new BitmapDrawable(context.getResources(),BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length));
    		appList.put(packageName, new Application(packageName, label, versionCode, appFlags, statusFlags, uid, icon));
    	} while (cursor.moveToNext());

    	cursor.close();
    	//db.close();
    	
    	Log.d("PDroidAlternative","Got matching applications: " + appList.size());
    	
    	return appList;
	}
	
	@Override
	protected void onPostExecute(HashMap<String, Application> result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

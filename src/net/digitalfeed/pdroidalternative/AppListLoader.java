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

import java.security.InvalidParameterException;
import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class AppListLoader {

	private Context context;
	AppQueryBuilder queryBuilder;

	public static final String APP_TYPE_USER = "0";
	public static final String APP_TYPE_SYSTEM = "1";
	
	enum SearchType { ALL, PACKAGE_NAME, PERMISSION, SETTING_GROUP }
	enum AppType { ALL, USER, SYSTEM }
	enum ResultsType { ALL, PACKAGE_NAME }

	public AppListLoader(Context context, AppQueryBuilder queryBuilder) throws InvalidParameterException {
		this.context = context;
		this.queryBuilder = queryBuilder;
	}
	
	public Application [] getMatchingApplications() {

		LinkedList<Application> appList = new LinkedList<Application>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
    	
    	Cursor cursor = this.queryBuilder.doQuery(db);
    	
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
    		appList.add(new Application(packageName, label, versionCode, appFlags, statusFlags, uid, icon));
    	} while (cursor.moveToNext());

    	cursor.close();
    	db.close();
    	
    	Log.d("PDroidAlternative","Got matching applications: " + appList.size());
    	
    	return appList.toArray(new Application[appList.size()]);
	}
}

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
	private SearchType searchType;
	private String [] projectionIn;

	enum SearchType { ALL, PACKAGE_NAME, PERMISSION, TYPE }
	enum ResultsType { ALL, PACKAGE_NAME }

	public AppListLoader(Context context, SearchType searchType, String [] projectionIn) throws InvalidParameterException {
		this.context = context;
		this.searchType = searchType;
		if (projectionIn != null) {
			this.projectionIn = projectionIn.clone();
		} else {
			this.projectionIn = null;
		}
	}
	
	public Application [] getMatchingApplications() {
		String query;
		switch (searchType) {
		case ALL:
			query = DBInterface.QUERY_GET_ALL_APPS_WITH_STATUS;
			break;
		case PACKAGE_NAME:
			query = DBInterface.QUERY_GET_APPS_BY_LABEL_WITH_STATUS;
			break;
		case PERMISSION:
			query = DBInterface.QUERY_GET_APPS_BY_PERMISSION_WITH_STATUS;
			break;
		case TYPE:
			query = DBInterface.QUERY_GET_APPS_BY_TYPE_WITH_STATUS;
			break;
		default:
			throw new InvalidParameterException("Unsupported application list search type");
		}

		
		LinkedList<Application> appList = new LinkedList<Application>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		
    	Log.d("PDroidAlternative","Executing Query " + query);
    	
    	Cursor cursor = db.rawQuery(query, this.projectionIn);
    	
		cursor.moveToFirst();
    	int labelColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
    	int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME); 
    	int uidColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID);
    	int versionCodeColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
    	int appFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);
    	int statusFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS);
    	int iconColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON);

    	do {
    		String label = cursor.getString(labelColumn);
    		String packageName = cursor.getString(packageNameColumn);
    		int uid = cursor.getInt(uidColumn);
    		int versionCode = cursor.getInt(versionCodeColumn);
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
	
	public LinkedList<String> getMatchingPackageNames() {
		String query = "";
		switch (searchType) {
		case ALL:
			query = DBInterface.QUERYPART_GET_ALL_APPS_PACKAGENAME_ONLY;
			break;
		case PACKAGE_NAME:
			query = DBInterface.QUERY_GET_APPS_BY_LABEL_PACKAGENAME_ONLY;
			break;
		case PERMISSION:
			query = DBInterface.QUERY_GET_APPS_BY_PERMISSION_PACKAGENAME_ONLY;
			break;
		case TYPE:
			query = DBInterface.QUERY_GET_APPS_BY_TYPE_PACKAGENAME_ONLY;
			break;
		default:
			throw new InvalidParameterException("Unsupported application list search type");
		}

		LinkedList<String> packageList = new LinkedList<String>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		    	
    	Cursor cursor = db.rawQuery(query, this.projectionIn);
    	
		cursor.moveToFirst();
    	int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME); 

    	do {
    		String packageName = cursor.getString(packageNameColumn);
    		packageList.add(packageName);
    	} while (cursor.moveToNext());

    	cursor.close();
    	db.close();
    	    	
    	return packageList;
	}
}

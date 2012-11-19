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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

/**
 * Performs a filtered query on applications in the database (using the parameter to .execute
 * for filtering) and returns the packageNames of matching applications, with case-insensitive
 * alpha sorting by application label. Case-insensitivity is provided by the collation of the 
 * Application table label column collation.
 *  
 * @author smorgan
 *
 */
public class ApplicationsDatabaseSearchTask extends AsyncTask<AppQueryBuilder, Integer, List<String>> {
	
	IAsyncTaskCallback<List<String>> listener;
	
	Context context;
	
	public ApplicationsDatabaseSearchTask(Context context, IAsyncTaskCallback<List<String>> listener) {
		this.context = context;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	@Override
	protected List<String> doInBackground(AppQueryBuilder... params) {
		if (params == null) {
			throw new NullPointerException("No AppListLoader was provided to the AppListLoaderTask");
		}
		
		LinkedList<String> packageNames = null;
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		
		Cursor cursor = params[0].doQuery(db);
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			packageNames = new LinkedList<String>();
			
			int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME);

			do {
	    		packageNames.add(cursor.getString(packageNameColumn));
	    	} while (cursor.moveToNext());

	    	cursor.close();
	    	//db.close();
		}
		return packageNames;
	}
	
	@Override
	protected void onPostExecute(List<String> result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

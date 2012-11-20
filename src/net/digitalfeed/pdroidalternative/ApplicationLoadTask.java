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
import android.os.AsyncTask;
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
		
		return Application.fromDatabase(context, selectPackageName[0]);
	}
	
	@Override
	protected void onPostExecute(Application result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}

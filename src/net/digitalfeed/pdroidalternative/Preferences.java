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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Preferences {
	
	private static final String SHARED_PREFERENCES_NAME = "net.digitalfeed.pdroidalternative";
	private static final String IS_CACHE_VALID = "isCacheValid";
	private static final String LAST_RUN_DATABASE_VERSION = "lastDatabaseVersion"; 
	private SharedPreferences prefs;
	
	public Preferences(Context context) {
		this.prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
	}
	
	public Boolean getIsApplicationListCacheValid() {
		return this.prefs.getBoolean(IS_CACHE_VALID, false);
	}

	public void setIsApplicationListCacheValid(Boolean isValid) {
		Editor editor = this.prefs.edit();
		editor.putBoolean(IS_CACHE_VALID, isValid);
		editor.commit();
	}
	
	public int getLastRunDatabaseVersion() {
		return this.prefs.getInt(LAST_RUN_DATABASE_VERSION, 0);
	}
	
	public void setLastRunDatabaseVersion(int newDatabaseVersion) {
		Editor editor = this.prefs.edit();
		editor.putInt(LAST_RUN_DATABASE_VERSION, newDatabaseVersion);
		editor.commit();
	}
	
}

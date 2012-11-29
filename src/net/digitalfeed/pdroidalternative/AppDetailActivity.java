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

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

/**
 * Activity to display the interface for changing the settings of a single
 * application.
 * Pass a bundle containing the package name in BUNDLE_PACKAGE_NAME to load
 * the interface for that package.
 * To provide 'up' button functionality in the action bar when within the app,
 * the boolean BUNDLE_IN_APP can be passed ('up' button is used when BUNDLE_IN_APP
 * is true, and thus the activity is running inside th setting of the app itself, 
 * rather than from a notification).
 * 
 * @author smorgan
 *
 */
public class AppDetailActivity extends Activity implements AppDetailFragment.OnDetailActionListener {

	public static final String BUNDLE_PACKAGE_NAME = "packageName";
	public static final String BUNDLE_IN_APP = "inApp";
	
	private boolean inApp = false;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.app_detail_frame_layout);
    }
	
	@Override
	public void onDetailUp() {
        Intent parentActivityIntent = new Intent(this, AppListActivity.class);
        parentActivityIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK); //Not sure if we need the ACTIVITY_NEW_TASK
        startActivity(parentActivityIntent);
        finish();
	}
	
	@Override
	public void onDetailSave() {
		returnToAppList();
	}
	
	@Override
	public void onDetailClose() {
		returnToAppList();
	}
	
	@Override
	public void onDetailDelete() {
		returnToAppList();
	}
	
	private void returnToAppList() {
		if (inApp) {
			//We should return to the parent activity when finishing
            Intent parentActivityIntent = new Intent(this, AppListActivity.class);
            parentActivityIntent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK); //Not sure if we need the ACTIVITY_NEW_TASK
            startActivity(parentActivityIntent);
		}
        finish();
	}
}
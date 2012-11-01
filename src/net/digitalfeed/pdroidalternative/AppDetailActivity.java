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

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

public class AppDetailActivity extends Activity {
	
	public static final String BUNDLE_PACKAGE_NAME = "packageName";
	public static final String BUNDLE_IN_APP = "inApp";
	
	private Application application;
	private AppSetting [] settingList;
	private String packageName;
	private ListView listView;
	private Context context;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);
    }

    @Override
    public void onStart() {
    	super.onStart();
    	context = this;
        Bundle bundle = getIntent().getExtras();
        packageName = bundle.getString(BUNDLE_PACKAGE_NAME);
        
        /*
         * If this action has been called from an app listing, then the action bar should
         * have the 'up' functionality which returns to the app listing.
         */
        //hav
        if (bundle.getBoolean(BUNDLE_IN_APP, false)) {
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
        	getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        //this.setTitle(packageName);
        AppDetailAppLoaderTask appDetailAppLoader = new AppDetailAppLoaderTask(this, new AppDetailAppLoaderTaskCompleteHandler());
        appDetailAppLoader.execute(packageName);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                Intent parentActivityIntent = new Intent(this, AppListActivity.class);
                parentActivityIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK); //Not sure if we need the ACTIVITY_NEW_TASK
                startActivity(parentActivityIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_app_detail, menu);
        return true;
    }

    class AppDetailAppLoaderTaskCompleteHandler implements IAsyncTaskCallback<Application>
    {
		@Override
		public void asyncTaskComplete(Application inApplication) {
			setTitle(inApplication.getLabel());
			application = inApplication;
			AppDetailSettingsLoaderTask appDetailSettingsLoader = new AppDetailSettingsLoaderTask(context, new AppDetailSettingsLoaderTaskCompleteHandler());
			appDetailSettingsLoader.execute(application.getPackageName());
			
		}
    }
    
    class AppDetailSettingsLoaderTaskCompleteHandler implements IAsyncTaskCallback<LinkedList<AppSetting>>
    {
		@Override
		public void asyncTaskComplete(LinkedList<AppSetting> inSettingList) {
			if (inSettingList != null) {
				settingList = inSettingList.toArray(new AppSetting[inSettingList.size()]); 
				listView = (ListView)findViewById(R.id.settingList);
				listView.setAdapter(new AppDetailAdapter(context, R.layout.setting_list_row_standard, settingList));
			}
		}
    }
}
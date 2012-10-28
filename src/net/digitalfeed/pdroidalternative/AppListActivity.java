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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class AppListActivity extends Activity {

	String[] appTitleList;
	ListView listView;
	Context context;
	Application[] appList;
	ProgressDialog progDialog;
	DBInterface dbInterface;
	Preferences prefs;
	
	int pkgCounter = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("PDroidAlternative", "Getting preferences");
        prefs = new Preferences(this);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        listView = (ListView)findViewById(R.id.applicationList);
        
        context = this;
        /* 
         * need to force the application cache to be invalid
         * if incrementing the version number, at present. This is simply because
         * tables get deleted, etc, and content is not regenerated unless the case
         * is considered invalid. This will be sorted when the DB upgrades are actually real
         * not just purging
         */
        //prefs.setIsApplicationListCacheValid(false); 
        
        //Do we have an application list already? is it valid?
        if (appList == null || !prefs.getIsApplicationListCacheValid()) {
            //Either we don't have an app list, or it isn't valid
        	if (!prefs.getIsApplicationListCacheValid()) {
        		//The app list isn't valid, so we need to rebuild it
	            rebuildApplicationList();
        	} else {
        		Log.d("PDroidAlternative", "appList is null");
        		//the app list is valid: we need to load it from the db
        		loadApplicationList(new AppListLoader(this, AppListLoader.SearchType.ALL, null));
        	}
        } else {
        	listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
        }
        
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
	    		//PrivacySettingsManager psm = (PrivacySettingsManager)context.getSystemService("privacy");
	    		//PrivacySettings pset = psm.getSettings(appList[position].getPackageName());
	    		//if (pset != null) {
		    	//	Log.d("PDroidAlternative", appList[position].getPackageName() + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    	//	Toast.makeText(context, appList[position].getPackageName() + ": " + Byte.toString(pset.getOutgoingCallsSetting()), Toast.LENGTH_SHORT).show();
	    		//} else {
	    		//	Log.d("PDroidAlternative", appList[position].getPackageName() + " null");
	    		//	Toast.makeText(context, appList[position].getPackageName() + ": null =(", Toast.LENGTH_SHORT).show();
	    		//}
				Intent intent = new Intent(context, AppDetailActivity.class);
				intent.putExtra(AppDetailActivity.BUNDLE_PACKAGE_NAME, appList[position].getPackageName());
				startActivity(intent);
			}
        });
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d("PDroidAlternative", "Destroying");
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	switch (item.getItemId()) {
    	case R.id.appListMenuRefresh:
    		rebuildApplicationList();
    		break;
    	case R.id.systemAppsOnly:
    		loadApplicationList(new AppListLoader(this, AppListLoader.SearchType.TYPE, new String [] {"1"}));
    		break;
    	case R.id.userAppsOnly:
    		loadApplicationList(new AppListLoader(this, AppListLoader.SearchType.TYPE, new String [] {"0"}));
    		break;
    	case R.id.systemAndUserApps:
    		loadApplicationList(new AppListLoader(this, AppListLoader.SearchType.ALL, null));
    		break;
    	}
    	return false;
    }
    
    private void updateProgressDialog(int currentValue, int maxValue) {
		if (progDialog != null) {
			if (progDialog.isShowing()) {
				progDialog.setProgress(currentValue);
			} else {
				progDialog.setMax(maxValue);
				progDialog.setProgress(currentValue);
				progDialog.show();
			}
		}
    }
    
    class AppListGeneratorCallback implements IAsyncTaskCallbackWithProgress<Application []>{
    	@Override
    	public void asyncTaskComplete(Application[] returnedAppList) {
    		if (progDialog != null) {
    			progDialog.dismiss();
    		}
    		
    		appList = returnedAppList;
    		
    		listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, appList));
    		prefs.setIsApplicationListCacheValid(true);
    	}
    	
    	@Override
    	public void asyncTaskProgressUpdate(Integer... progress) {
    		updateProgressDialog(progress[0], progress[1]);
    	}
    }

    class AppListLoaderCallback implements IAsyncTaskCallback<Application []>{
    	@Override
    	public void asyncTaskComplete(Application[] returnedAppList) {
    		Log.d("PDroidAlternative","Got result from app list load: length " + returnedAppList.length);
    		appList = returnedAppList;
    		listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, appList));
    	}
    }
    
    public void loadApplicationList(AppListLoader appListLoader) {
    	Log.d("PDroidAlternative","About to start load");
    	AppListLoaderTask appListLoaderTask = new AppListLoaderTask(this, new AppListLoaderCallback());
    	Log.d("PDroidAlternative","Created the task");
    	appListLoaderTask.execute(appListLoader);
    }
    
    public void rebuildApplicationList() {
        this.progDialog = new ProgressDialog(this);
        this.progDialog.setMessage("Generating Application List");
        this.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    	AppListGeneratorTask appListGenerator = new AppListGeneratorTask(this, new AppListGeneratorCallback());
    	appListGenerator.execute();
    }
    
    public void showSystemAppsOnly(View view) {}
    public void showUserAppsOnly(View view) {}
    public void showSystemAndUserApps(View view) {}
}

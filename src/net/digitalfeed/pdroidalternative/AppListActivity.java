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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class AppListActivity extends Activity implements IAppListListener {

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
        
        //Do we have an application list already? is it valid?
        Log.d("PDroidAlternative", "Checking cache, and appList");
        prefs.setIsApplicationListCacheValid(false);
        if (appList == null || !prefs.getIsApplicationListCacheValid()) {
        	Log.d("PDroidAlternative", "Cache invalid or appList not present");
            //Either we don't have an app list, or it isn't valid
        	if (!prefs.getIsApplicationListCacheValid()) {
        		Log.d("PDroidAlternative", "Cache invalid");
        		//The app list isn't valid, so we need to rebuild it
	            rebuildApplicationList();
        	} else {
        		Log.d("PDroidAlternative", "appList is null");
        		//the app list is valid: we need to load it from the db
        		loadApplicationList();
        	}
        } else {
        	Log.d("PDroidAlternative", "Cache valid and applist not null");
        	Log.d("PDroidAlternative", "Setting listview adapter");
        	listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
        }
        context = this;
        
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
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void loadApplicationList() {
        this.progDialog = new ProgressDialog(this);
        this.progDialog.setMessage("Loading Application List");
        this.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    	AppListLoader applicationListLoader = new AppListLoader(this, this);
    	applicationListLoader.execute();
    }
    
    public void rebuildApplicationList() {
        this.progDialog = new ProgressDialog(this);
        this.progDialog.setMessage("Generating Application List");
        this.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    	AppListGenerator applicationListRetriever = new AppListGenerator(this, this);
    	applicationListRetriever.execute();
    }

	@Override
	public void appListLoadCompleted(Application[] appList) {
		if (this.progDialog != null) {
			this.progDialog.dismiss();
		}
		
		this.appList = appList;
		
		//If the list was rebuilt because the cache was invalid, then we should rewrite to the database
		if (!prefs.getIsApplicationListCacheValid()) {
			//We've built the app list. Now we write it to the DB, and mark it as valid in the prefs
			AppListWriter applicationListWriter = new AppListWriter(this);
			//this is using a shallow clone, so if the Application objects change then there could be trouble
			//Also, the application objects are not synchronized, so if they change while being written to the database
			//then there could be trouble
			applicationListWriter.execute(this.appList.clone());
		}
		listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
	}
	
	@Override
	public void appListProgressUpdate(Integer... progress) {
		if (this.progDialog != null) {
			if (this.progDialog.isShowing()) {
				this.progDialog.setProgress(progress[0]);
			} else {
				this.progDialog.setMax(progress[1]);
				this.progDialog.setProgress(progress[0]);
				this.progDialog.show();
			}
		}
	}
}

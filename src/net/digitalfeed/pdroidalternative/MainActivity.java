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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.digitalfeed.pdroidalternative.DBInterface.DBHelper;


import android.os.AsyncTask;
import android.os.Bundle;
import android.R.anim;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;


public class MainActivity extends Activity implements IAppListListener {

	String[] appTitleList;
	ListView listView;
	Context context;
	SQLiteDatabase db;
	Application[] appList;
	ProgressDialog progDialog;
	DBInterface dbInterface;
	Preferences prefs;
	
	int pkgCounter = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("PDroidAlternative", "Getting dbInterface instance");
        dbInterface = DBInterface.getInstance(this);
        Log.d("PDroidAlternative", "Getting db");
        if (db == null) {
        	db = dbInterface.getDBHelper().getReadableDatabase();
        }
        Log.d("PDroidAlternative", "Getting preferences");
        prefs = new Preferences(this);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        Log.d("PDroidAlternative", "got context");
        listView = (ListView)findViewById(R.id.applicationList);
        Log.d("PDroidAlternative", "got listview");
        
        //Do we have an application list already? is it valid?
        Log.d("PDroidAlternative", "Checking cache, and appList");
        if (appList == null || !prefs.getIsApplicationListCacheValid()) {
        	Log.d("PDroidAlternative", "Cache invalid or appList not present");
            //Either we don't have an app list, or it isn't valid
        	if (!prefs.getIsApplicationListCacheValid()) {
        		Log.d("PDroidAlternative", "Cache invalid");
        		//The app list isn't valid, so we need to rebuild it
	            this.progDialog = new ProgressDialog(this);
	            this.progDialog.setMessage("Loading Application List");
	            this.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	            rebuildApplicationList();
        	} else {
        		Log.d("PDroidAlternative", "appList is null");
        		//We'll load the app list even though it is already in the database, just because the DB part isn't done yet
	            loadApplicationListFromDB();
        		//the app list is valid: we need to load it from the db
        		//TODO: load app list from the database here
        	}
        } else {
        	Log.d("PDroidAlternative", "Cache valid and applist not null");
        	Log.d("PDroidAlternative", "Setting listview adapter");
        	listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
        }
        context = this;
        Button btn = (Button)findViewById(R.id.button1);
        btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	if (appList != null) {
		    		PrivacySettingsManager psm = (PrivacySettingsManager)context.getSystemService("privacy");
		    		PrivacySettings pset = psm.getSettings(appList[pkgCounter].getPackageName());
		    		if (pset != null) {
			    		Log.d("PDroidAlternative", appList[pkgCounter].getLabel() + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    		} else {
		    			Log.d("PDroidAlternative", appList[pkgCounter].getLabel() + " null");
		    		}
		    		pkgCounter = pkgCounter++ % appList.length;
		    	}
			}
		});
        
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
	    		PrivacySettingsManager psm = (PrivacySettingsManager)context.getSystemService("privacy");
	    		PrivacySettings pset = psm.getSettings(appList[position].getPackageName());
	    		if (pset != null) {
		    		Log.d("PDroidAlternative", appList[position].getPackageName() + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    		Toast.makeText(context, appList[position].getPackageName() + ": " + Byte.toString(pset.getOutgoingCallsSetting()), Toast.LENGTH_SHORT).show();
	    		} else {
	    			Log.d("PDroidAlternative", appList[position].getPackageName() + " null");
	    			Toast.makeText(context, appList[position].getPackageName() + ": null =(", Toast.LENGTH_SHORT).show();
	    		}
			}
        	
        });
    }

    @Override
    public void onDestroy() {
    	Log.d("PDroidAlternative", "Destroying");
    	if (db != null) {
    		db.close();
    	}
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void loadApplicationListFromDB() {
    	SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    	queryBuilder.setTables(DBInterface.ApplicationTable.TABLE_NAME + "," + DBInterface.ApplicationStatusTable.TABLE_NAME);
    	String[] projection = new String[] {
    			DBInterface.ApplicationTable.COLUMN_NAME_LABEL,
    			DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME,
    			DBInterface.ApplicationTable.COLUMN_NAME_UID,
    			DBInterface.ApplicationTable.COLUMN_NAME_ICON,
    			DBInterface.ApplicationTable.COLUMN_NAME_FLAGS,
    			DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS
    	};
    	Cursor cursor = queryBuilder.query(db, projection, null, null, null, null, null);
    	Application[] appList = new Application[cursor.getCount()];
    	do {
    		String packageName = cursor.getString(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME));
    		String label = cursor.getString(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL));
    		int versionCode = cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE));
    		Boolean isSystemApp = (cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS)) & DBInterface.ApplicationTable.FLAG_IS_SYSTEM_APP) == DBInterface.ApplicationTable.FLAG_IS_SYSTEM_APP;
    		int uid = cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID));
    		byte[] iconBlob = cursor.getBlob(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON));
    		Drawable icon;
    		//Application app = new Application()
    				
    				
    				
    				
    				
    		
    	} while (cursor.moveToNext());   	
    }
    
    public void rebuildApplicationList() {
    	Log.d("PDroidAlternative", "starting updateApplicationList");
    	//int appTypes = ApplicationListRetriever.TYPE_USER | ApplicationListRetriever.TYPE_SYSTEM;
    	int appTypes = AppListGenerator.TYPE_USER;
    	//int appTypes = ApplicationListRetriever.TYPE_SYSTEM;
    	Log.d("PDroidAlternative", "set app types");
    	AppListGenerator applicationListRetriever = new AppListGenerator(this, this);
    	Log.d("PDroidAlternative", "Created applicationListRetriever");
    	applicationListRetriever.execute(appTypes);
    }

	@Override
	public void appListLoadCompleted(Application[] appList) {
		this.progDialog.dismiss();
		this.appList = appList;
		
		//We've built the app list. Now we write it to the DB, and mark it as valid in the prefs 
		SQLiteDatabase write_db = DBInterface.getInstance(this).getDBHelper().getWritableDatabase(); 
		for (Application app : appList) {
			db.insert(DBInterface.ApplicationTable.TABLE_NAME, null, DBInterface.ApplicationTable.getContentValues(app));
		}
		write_db.close();
		prefs.setIsApplicationListCacheValid(true);
		listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
	}
	
	@Override
	public void appListProgressUpdate(Integer... progress) {
		if (this.progDialog.isShowing()) {
			this.progDialog.setProgress(progress[0]);
		} else {
			this.progDialog.setMax(progress[1]);
			this.progDialog.setProgress(progress[0]);
			this.progDialog.show();
		}
	}
}

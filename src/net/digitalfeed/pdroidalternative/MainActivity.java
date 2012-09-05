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


import android.os.AsyncTask;
import android.os.Bundle;
import android.R.anim;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

	Application[] appList; 
	
	int pkgCounter = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        Log.d("PDroidAlternative", "got context");
        listView = (ListView)findViewById(R.id.applicationList);
        Log.d("PDroidAlternative", "got listview");
        if (appList == null) {
        	updateApplicationList();
        }
        context = this;
        Button btn = (Button)findViewById(R.id.button1);
        btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	if (appList != null) {
		    		PrivacySettingsManager psm = (PrivacySettingsManager)context.getSystemService("privacy");
		    		PrivacySettings pset = psm.getSettings(appList[pkgCounter].packageName);
		    		if (pset != null) {
			    		Log.d("PDroidAlternative", appList[pkgCounter].label + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    		} else {
		    			Log.d("PDroidAlternative", appList[pkgCounter].label + " null");
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
	    		PrivacySettings pset = psm.getSettings(appList[position].packageName);
	    		if (pset != null) {
		    		Log.d("PDroidAlternative", appList[position].packageName + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    		Toast.makeText(context, appList[position].packageName + ": " + Byte.toString(pset.getOutgoingCallsSetting()), Toast.LENGTH_SHORT).show();
	    		} else {
	    			Log.d("PDroidAlternative", appList[position].packageName + " null");
	    			Toast.makeText(context, appList[position].packageName + ": null =(", Toast.LENGTH_SHORT).show();
	    		}
			}
        	
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void updateApplicationList() {
    	Log.d("PDroidAlternative", "starting updateApplicationList");
    	//int appTypes = ApplicationListRetriever.TYPE_USER | ApplicationListRetriever.TYPE_SYSTEM;
    	int appTypes = AppListRetriever.TYPE_USER;
    	//int appTypes = ApplicationListRetriever.TYPE_SYSTEM;
    	Log.d("PDroidAlternative", "set app types");
    	AppListRetriever applicationListRetriever = new AppListRetriever(this, this);
    	Log.d("PDroidAlternative", "Created applicationListRetriever");
    	applicationListRetriever.execute(appTypes);
    }

	@Override
	public void appListLoadCompleted(Application[] appList) {
		this.appList = appList;
		listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
	}
}

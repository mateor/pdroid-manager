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
import java.util.ArrayList;
import java.util.List;

import net.digitalfeed.pdroidalternative.PermissionSettingHelper.TrustState;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class AppListActivity extends Activity {

	/*
	 * If you change these numbers, you also need to change Arrays.xml to update
	 * the sequence of text as well
	 */
	public static final int APP_TYPE_USER_OPTION_POSITION = 0;
	public static final int APP_TYPE_SYSTEM_OPTION_POSITION = 1;
	public static final int APP_TYPE_ALL_OPTION_POSITION = 2;
	
	String[] appTitleList;
	ListView listView;
	Context context;
	Application[] appList;
	ProgressDialog progDialog;
	DBInterface dbInterface;
	Preferences prefs;
	ActionBar actionBar;
	boolean readyForInput = false;
	String currentAppType;
	String currentSettingGroup;
	List<String> settingGroups;
	
	int pkgCounter = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        
        Log.d("PDroidAlternative", "Getting preferences");
        prefs = new Preferences(this);
        
        currentAppType = prefs.getLastAppListType();
        /*
         * If the database version has changed, we will need to rebuild the application cache 
         */
        if (prefs.getLastRunDatabaseVersion() != DBInterface.DBHelper.DATABASE_VERSION) {
        	Log.d("PDroidAlternative", "Defined database version has changed since last run; we need to rebuild the cache");
        	prefs.setIsApplicationListCacheValid(false);
        	prefs.setLastRunDatabaseVersion(DBInterface.DBHelper.DATABASE_VERSION);
        }
        
        setContentView(R.layout.activity_main);
        
    	actionBar = getActionBar();
    	actionBar.setDisplayShowTitleEnabled(false);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        listView = (ListView)findViewById(R.id.applicationList);
        
        //Do we have an application list already? is it valid?
        if (appList == null || !prefs.getIsApplicationListCacheValid()) {
            //Either we don't have an app list, or it isn't valid
        	if (!prefs.getIsApplicationListCacheValid()) {
        		//The app list isn't valid, so we need to rebuild it
	            rebuildApplicationList();
        	} else {
        		loadApplicationList();
        	}
        } else {
        	listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, this.appList));
        }
        
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				openDetailInterface(appList[position]);
			}
        });
        
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
        	
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
					long id) {
				showPopupMenu(view, position);
				return true;
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
        
        MenuItem mSpinner = menu.findItem(R.id.appListMenu_appTypeSpinner);
        final Spinner appTypeSpinner = (Spinner)mSpinner.getActionView();
        final SpinnerAdapter spinnerAdapter = (SpinnerAdapter) ArrayAdapter.createFromResource(this,
				R.array.app_type_selection_options,
				android.R.layout.simple_spinner_dropdown_item);
        appTypeSpinner.setAdapter(spinnerAdapter);
        /*
         * Set the app spinner to be the correct selected option
         */
        if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
        	appTypeSpinner.setSelection(APP_TYPE_USER_OPTION_POSITION);
        } else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
        	appTypeSpinner.setSelection(APP_TYPE_SYSTEM_OPTION_POSITION);
        } else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
        	appTypeSpinner.setSelection(APP_TYPE_ALL_OPTION_POSITION);
        }
        appTypeSpinner.setOnItemSelectedListener(new AppTypeSpinnerListener());
		
        
        //TODO: move the query parts of this off into the DBInterface, where they really belong
        mSpinner = menu.findItem(R.id.appListMenu_filterByGroupSpinner);
        final Spinner groupSpinner = (Spinner)mSpinner.getActionView();
        
        Cursor groupNamesCursor = DBInterface.getInstance(context).getDBHelper().getReadableDatabase().rawQuery("SELECT DISTINCT " + DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE + " FROM " + DBInterface.SettingTable.TABLE_NAME + " ORDER BY " + DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE, null);
        //List<SimpleImmutableEntry<String, String>> settingGroups = new ArrayList<SimpleImmutableEntry<String, String>>();  
        if (groupNamesCursor == null || groupNamesCursor.getCount() < 1) {
        	throw new DatabaseUninitialisedException("The database has no setting groups. I'm not comfortable with this situation.");
        }
        
        /*if (groupNamesCursor.moveToFirst()) {
        	int groupTitleColumnNum = groupNamesCursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
        	int groupIdColumnNum = groupNamesCursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_ID);
        	do {
        		settingGroups.add(new SimpleImmutableEntry<String,String>(
        				groupNamesCursor.getString(groupTitleColumnNum),
        				groupNamesCursor.getString(groupIdColumnNum)
        				));
        	} while (groupNamesCursor.moveToNext());
        }*/
        
        if (this.settingGroups == null) {
        	this.settingGroups = new ArrayList<String>();
        } else {
        	this.settingGroups.clear();
        }
        
        if (groupNamesCursor.moveToFirst()) {
	    	int groupTitleColumnNum = groupNamesCursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
	    	do {
	    		settingGroups.add(groupNamesCursor.getString(groupTitleColumnNum));
	    	} while (groupNamesCursor.moveToNext());
    	}
        
        groupNamesCursor.close();
        //groupSpinner.setAdapter((SpinnerAdapter)new SimpleAdapter(context, groupSpinner.getId(), groupNamesCursor, new String[] {DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE}, new int [] {android.R.id.text1}, 0));
        final SpinnerAdapter groupSpinnerAdapter = (SpinnerAdapter) new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, settingGroups);
        groupSpinner.setAdapter(groupSpinnerAdapter);
        
        groupSpinner.setOnItemSelectedListener(new GroupSpinnerListener());
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	if (readyForInput) {
	    	switch (item.getItemId()) {
	    	case R.id.appListMenuRefresh:
	    		rebuildApplicationList();
	    		break;
	    	}
    	}
    	
	    return false;
    }
    
    private void showPopupMenu(View view, int position){
    	PopupMenu popupMenu = new PopupMenu(context, view);
    	      popupMenu.getMenuInflater().inflate(R.menu.activity_applist_longpress_menu, popupMenu.getMenu());
    	      
    	      final Application targetApp = appList[position];

    	      popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
    	    	  @Override
    	    	  public boolean onMenuItemClick(MenuItem item) {    	    	      
    	    		  TrustState newTrustState;
    	    		  
    	    		  switch (item.getItemId()) {
    	    		  case R.id.applist_popupmenu_set_trusted_values:
    	    			  newTrustState = TrustState.TRUSTED;
    	    			  targetApp.setIsUntrusted(false);
    	    			  break;
    	    		  case R.id.applist_popupmenu_set_untrusted_values:
    	    			  newTrustState = TrustState.UNTRUSTED;
    	    			  targetApp.setIsUntrusted(false);
    	    			  break;
    	    	      default:
        	    		  throw new InvalidParameterException();
    	    		  }

    	    		  progDialog = new ProgressDialog(context);
    	    	      progDialog.setMessage("Updating settings");
    	    	      progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	    	      AppListUpdateAllSettingsTask updateAllSettingsTask = new AppListUpdateAllSettingsTask(context, newTrustState, new AppListUpdateAllSettingsCallback());
    	    	      updateAllSettingsTask.execute(targetApp);
    	    		  return true;
    	   }
    	  });
    	    
    	  popupMenu.show();
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
    
    private void loadApplicationList() {
    	Log.d("PDroidAlternative","About to start load");
    	
    	AppQueryBuilder queryBuilder = new AppQueryBuilder();
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_APP);
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_STATUSFLAGS);
		
		if (this.currentSettingGroup != null) {
			//TODO: Right now, the 'general' option is serving as an 'all' option, because all apps include
			//items from the General group. This is not a good solution.
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_SETTING_GROUP_TITLE, currentSettingGroup);
		}
					
		if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_TYPE, AppListLoader.APP_TYPE_SYSTEM);
		} else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_TYPE, AppListLoader.APP_TYPE_USER);
		} else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
			//This is just here for the moment for exclusion purposes
		} else {
			Log.d("PDroidAlternative","You shouldn't be here!");
		}

		AppListLoaderTask appListLoaderTask = new AppListLoaderTask(this, new AppListLoaderCallback());
    	Log.d("PDroidAlternative","Created the task");
    	appListLoaderTask.execute(new AppListLoader(context, queryBuilder));
    }
    
    private void rebuildApplicationList() {
        this.progDialog = new ProgressDialog(this);
        this.progDialog.setMessage("Generating Application List");
        this.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    	AppListGeneratorTask appListGenerator = new AppListGeneratorTask(this, new AppListGeneratorCallback());
    	appListGenerator.execute();
    }
    
    private void openDetailInterface(Application application) {
    	Intent intent = new Intent(context, AppDetailActivity.class);
		intent.putExtra(AppDetailActivity.BUNDLE_PACKAGE_NAME, application.getPackageName());
		intent.putExtra(AppDetailActivity.BUNDLE_IN_APP, true);
		startActivity(intent);
    }
    
    class AppListGeneratorCallback implements IAsyncTaskCallbackWithProgress<Application []>{
    	@Override
    	public void asyncTaskComplete(Application[] returnedAppList) {
    		if (progDialog != null) {
    			progDialog.dismiss();
    		}
    		
    		prefs.setIsApplicationListCacheValid(true);
    		loadApplicationList();
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
    		readyForInput = true;
    	}
    }

    class AppListUpdateAllSettingsCallback implements IAsyncTaskCallback<Void>{
    	@Override
    	public void asyncTaskComplete(Void result) {
    		//TODO: might be worth adding a toast here to notify the settings have been updated?
    		if (progDialog != null) {
    			progDialog.dismiss();
    		}
    		listView.invalidate();
    		Log.d("PDroidAlternative","Updated all settings.");
    	}
    }
    
    class AppTypeSpinnerListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int itemPosition,
				long id) {
			//need to be able to disable this, just in case there is a time when
			//it is possible to change the entry, but there is already an operation underway
			if (readyForInput) {
				
	 			switch (itemPosition) {
				case APP_TYPE_USER_OPTION_POSITION:
					if (!currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
	        			prefs.setLastAppListType(Preferences.APPLIST_LAST_APP_TYPE_USER);
						currentAppType = Preferences.APPLIST_LAST_APP_TYPE_USER;
						loadApplicationList();
					}
					break;
				case APP_TYPE_SYSTEM_OPTION_POSITION:
					if (!currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
						prefs.setLastAppListType(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM);
						currentAppType = Preferences.APPLIST_LAST_APP_TYPE_SYSTEM;
						loadApplicationList();
					}
					break;
				case APP_TYPE_ALL_OPTION_POSITION:
					if (!currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
						prefs.setLastAppListType(Preferences.APPLIST_LAST_APP_TYPE_ALL);
						currentAppType = Preferences.APPLIST_LAST_APP_TYPE_ALL;
						loadApplicationList();
					}
					break;
				}
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> view) {
			// TODO Auto-generated method stub
			
		}
    };
    
    class GroupSpinnerListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int itemPosition,
				long id) {
			Log.d("PDroidAlternative","Group selected: " + settingGroups.get(itemPosition));
			
			if (readyForInput) {        		
				currentSettingGroup = settingGroups.get(itemPosition);
				loadApplicationList();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> view) {
			// TODO Auto-generated method stub
		}
    };
}

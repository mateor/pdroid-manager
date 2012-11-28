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

import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

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
public class AppDetailActivity extends Activity {
	
	public static final String BUNDLE_PACKAGE_NAME = "packageName";
	public static final String BUNDLE_IN_APP = "inApp";
	
	private Application application;
	private List<AppSetting> settingList;
	private boolean settingsAreLoaded = false;
	private String packageName;
	private ListView listView;
	private Context context;
	private boolean inApp;
	
	private ProgressDialog progDialog ;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        
        setContentView(R.layout.activity_app_detail);
        Bundle bundle = getIntent().getExtras();
        packageName = bundle.getString(BUNDLE_PACKAGE_NAME);
        
        /*
         * If this action has been called from an app listing, then the action bar should
         * have the 'up' functionality which returns to the app listing.
         */
        inApp = bundle.getBoolean(BUNDLE_IN_APP, false);
        if (inApp) {
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
        	getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        //this.setTitle(packageName);
        ApplicationLoadTask appDetailAppLoader = new ApplicationLoadTask(this, new AppDetailAppLoaderTaskCompleteHandler());
        appDetailAppLoader.execute(packageName);
        
    	Preferences prefs = new Preferences(context);
    	CheckBox checkbox = (CheckBox)findViewById(R.id.detail_notify_on_access);
    	checkbox.setChecked(prefs.getDoNotifyForPackage(packageName));
    	checkbox = (CheckBox)findViewById(R.id.detail_log_on_access);
    	checkbox.setChecked(prefs.getDoLogForPackage(packageName));
    	checkbox = null;
    	prefs = null;
    }

    @Override
    public void onStart() {
    	super.onStart();
    	if (!settingsAreLoaded) {
    		showDialog(null, getString(R.string.detail_dialog_loading_message));
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.detailCloseButton:
            	//Should we be checking if the settings have changed and prompting the user?
            	//I think that would probably be excessive...

            	//if we are in an app, we should just finish. Otherwise, the behaviour should
            	//be the same as pressing the 'home' button.
            	if (!inApp) {
	            	finish();
	            	return true;
            	}
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
            case R.id.detailDeleteButton:
        		showDialog(getString(R.string.detail_dialog_saving_title),
        				getString(R.string.detail_dialog_saving_message));

            	AppSettingsDeleteTask settingsDeleterTask = new AppSettingsDeleteTask(context, packageName, new AppDetailSettingActionTaskCompleteHandler());
            	settingsDeleterTask.execute();
            	break;
            case R.id.detailSaveButton:
        		showDialog(getString(R.string.detail_dialog_saving_title),
        				getString(R.string.detail_dialog_saving_message));
            	
            	Preferences prefs = new Preferences(context);
            	CheckBox checkbox = (CheckBox)findViewById(R.id.detail_notify_on_access);
            	prefs.setDoNotifyForPackage(packageName, checkbox.isChecked());
            	boolean setNotifyTo = checkbox.isChecked();
            	checkbox = (CheckBox)findViewById(R.id.detail_log_on_access);
            	setNotifyTo |= checkbox.isChecked();
            	prefs.setDoLogForPackage(packageName, checkbox.isChecked());
            	checkbox = null;
            	
            	AppSetting [] toAsyncTask = this.settingList.toArray(new AppSetting [this.settingList.size()]);
            	this.settingList = null;
            	AppSettingsSaveTask settingsWriterTask = new AppSettingsSaveTask(context, packageName, application.getUid(), setNotifyTo, new AppDetailSettingActionTaskCompleteHandler());
            	settingsWriterTask.execute(toAsyncTask);
            	break;
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
			if (inApplication == null) {
				Log.d("PDroidAlternative", "inApplication is null: the app could have disappeared between the intent being created and the task running?");
				if (progDialog != null && progDialog.isShowing()) {
					progDialog.dismiss();
				}
			} else {
				setTitle(inApplication.getLabel());
				application = inApplication;
				AppSettingsLoadTask appDetailSettingsLoader = new AppSettingsLoadTask(context, new AppDetailSettingsLoaderTaskCompleteHandler());
				appDetailSettingsLoader.execute(application.getPackageName());
			}
		}
    }
    
    class AppDetailSettingsLoaderTaskCompleteHandler implements IAsyncTaskCallback<List<AppSetting>>
    {
		@Override
		public void asyncTaskComplete(List<AppSetting> inSettingList) {
			settingsAreLoaded = true;
			if (progDialog != null && progDialog.isShowing()) {
				progDialog.dismiss();
			}
			
			if (inSettingList == null) {
				Log.d("PDroidAlternative","AppDetailSettingsLoaderTask returned null");
			} else if (inSettingList.size() == 0) {
				Log.d("PDroidAlternative","AppDetailSettingsLoaderTask returned no AppSettings (size = 0)");
			} else {
				settingList = inSettingList;
				listView = (ListView)findViewById(R.id.detail_setting_list);
				listView.setAdapter(new AppDetailAdapter(context, R.layout.setting_list_row_standard, settingList));
			}
		}
    }
    
    class AppDetailSettingActionTaskCompleteHandler implements IAsyncTaskCallback<Void>
    {	
		@Override
		public void asyncTaskComplete(Void param) {
			if (progDialog != null && progDialog.isShowing()) {
				progDialog.dismiss();
			}
			
			/*
			 * TODO: add some means for the AppListActivity to update the database (or
			 * better yet, do it here)
			 */
			
			if (inApp) {
				//We should return to the parent activity when finishing
	            Intent parentActivityIntent = new Intent(context, AppListActivity.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK); //Not sure if we need the ACTIVITY_NEW_TASK
	            startActivity(parentActivityIntent);
			}
			
			//All done - finish
            finish();
		}
    }
    
    /**
     * Helper to show a non-cancellable spinner progress dialog
     * 
     * @param title  Title for the progress dialog (or null for none)
     * @param message  Message for the progress dialog (or null for none)
     */
	private void showDialog(String title, String message) {
		if (this.progDialog != null && this.progDialog.isShowing()) {
			this.progDialog.dismiss();
		}
		this.progDialog = new ProgressDialog(context);
		this.progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		if (title != null) {
			progDialog.setTitle(title);
		}
		if (message != null) {
			progDialog.setMessage(message);
		}
    	progDialog.setCancelable(false);
    	progDialog.show();
	}
}
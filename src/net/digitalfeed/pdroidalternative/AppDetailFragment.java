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
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class AppDetailFragment extends Fragment {
		
	private Application application;
	private List<AppSetting> settingList;
	private boolean settingsAreLoaded = false;
	private String packageName = null;
	
	private View rootView;
	private ListView listView;
	
	private Context context;
	private boolean inApp = false;
	
	private ProgressDialog progDialog ;
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		
        Bundle bundle = activity.getIntent().getExtras();
        //if we have a bundle, we can load the package. Otherwise, we do no such thing
        if (bundle != null) {
        	this.packageName = bundle.getString(AppDetailActivity.BUNDLE_PACKAGE_NAME);
        
	        /*
	         * If this action has been called from an app listing, then the action bar should
	         * have the 'up' functionality which returns to the app listing.
	         */
	        this.inApp = bundle.getBoolean(AppDetailActivity.BUNDLE_IN_APP, false);
        } else {
        	settingsAreLoaded = true;
        }

	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if the packageName is null, then we just hang out until we get something from the other frame
        if (this.packageName != null) {
	        ApplicationLoadTask appDetailAppLoader = new ApplicationLoadTask(context, new AppDetailAppLoaderTaskCompleteHandler());
	        appDetailAppLoader.execute(packageName);
        }
    }

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		this.rootView = inflater.inflate(R.layout.activity_app_detail, container);
		this.listView = (ListView)this.rootView.findViewById(R.id.detail_setting_list);
		
    	Preferences prefs = new Preferences(context);
    	CheckBox checkbox = (CheckBox)this.rootView.findViewById(R.id.detail_notify_on_access);
    	checkbox.setChecked(prefs.getDoNotifyForPackage(packageName));
    	checkbox = (CheckBox)this.rootView.findViewById(R.id.detail_log_on_access);
    	checkbox.setChecked(prefs.getDoLogForPackage(packageName));
    	checkbox = null;
    	prefs = null;

		return this.rootView;
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
	            	//finish();
	            	return true;
            	}
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                Intent parentActivityIntent = new Intent(context, AppListActivity.class);
                parentActivityIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK); //Not sure if we need the ACTIVITY_NEW_TASK
                startActivity(parentActivityIntent);
                //finish();
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
            	CheckBox checkbox = (CheckBox)rootView.findViewById(R.id.detail_notify_on_access);
            	prefs.setDoNotifyForPackage(packageName, checkbox.isChecked());
            	boolean setNotifyTo = checkbox.isChecked();
            	checkbox = (CheckBox)rootView.findViewById(R.id.detail_log_on_access);
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
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_app_detail, menu);
    }

    /**
     * Triggers the load of details for the incoming application, totally ignoring
     * the state of the current application that may or may not have
     * been modified. Used to receive input from other fragments
     * 
     * @param packageName
     */
    public void loadApplicationDetail(String packageName) {
    	Log.d("PDroidAlternative","Fragment has been told to load " + packageName);
    	if (packageName != null) {
    		showDialog(null, getString(R.string.detail_dialog_loading_message));
    		this.packageName = packageName;
    		Log.d("PDroidAlternative","Fragment is about to load " + packageName);
	        ApplicationLoadTask appDetailAppLoader = new ApplicationLoadTask(context, new AppDetailAppLoaderTaskCompleteHandler());
	        appDetailAppLoader.execute(packageName);
    	}
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
				//setTitle(inApplication.getLabel());
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
            //finish();
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
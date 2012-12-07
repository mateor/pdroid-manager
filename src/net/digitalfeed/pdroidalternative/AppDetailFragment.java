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
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

/**
 * Fragment for viewing and modifying the settings of an application.
 * I plan to make a superclass which allows for managing settings more
 * generically, rather than attached to a specific application. That superclass
 * could then be used for modifying profiles, setting up batch operations, etc.
 * @author smorgan
 *
 */
public class AppDetailFragment extends Fragment {
		
	private String packageName = null; //package name of the app being loaded/displayed
	private Application application; //stores an application object for the app being displayed
	private List<PDroidAppSetting> settingList; //List of settings objects
	private boolean settingsAreLoaded = false; //Used to determine whether the loading dialog should be displayed when the app becomes visible
	
	private View rootView;
	private ListView listView;
	
	private Context context;
	private boolean inApp = false; //identifies whether in an app or not, which changes the 'up' or 'back' button behaviour 
	
	private ProgressDialog progDialog; //a holder for progress dialogs which are displayed
	
	OnDetailActionListener callback; //callback for when an action occurs (i.e. save, close, delete, up).
	//Interface for callbacks
	public interface OnDetailActionListener {
		public void onDetailSave();
		public void onDetailClose();
		public void onDetailDelete();
		public void onDetailUp();
	}
	
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		
		//TODO: Move this to the activity, and make a function call to the fragment to load the app?
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
        	//if no bundle has been provided, then there is no need for the dialog, so make sure it doesn't display on load
        	settingsAreLoaded = true;
        }
        
        // Check the container activity implements the callback interface
		// Thank you Google for the example code: https://developer.android.com/training/basics/fragments/communicating.html
        try {
            callback = (OnDetailActionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnActionListener");
        }

	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if the packageName is null, then we just hang out until we get something from the other frame
        //otherwise, load the application
        if (this.packageName != null) {
	        ApplicationLoadTask appDetailAppLoader = new ApplicationLoadTask(context, new AppDetailAppLoaderTaskCompleteHandler());
	        appDetailAppLoader.execute(packageName);
        }
        
        // need to notify do this to notify the activity that
        // this fragment will contribute to the action menu
        setHasOptionsMenu(true);
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
    		DialogHelper.showDialog(context, null, getString(R.string.detail_dialog_loading_message), progDialog);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.detailCloseButton:
            	//Should we be checking if the settings have changed and prompting the user?
            	//I think that would probably be excessive...

            	//if we are in an app, we should just finish. Otherwise, the behaviour should
            	//be the same as pressing the 'back' button.
            	if (!inApp) {
            		callback.onDetailClose();
	            	return true;
            	}
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                callback.onDetailUp();
                return true;
            case R.id.detailDeleteButton:
        		this.progDialog = DialogHelper.showDialog(context, getString(R.string.detail_dialog_saving_title),
        				getString(R.string.detail_dialog_saving_message), progDialog);

            	AppSettingsDeleteTask settingsDeleterTask = new AppSettingsDeleteTask(context, packageName, new DeleteCompleteHandler());
            	settingsDeleterTask.execute();
            	break;
            case R.id.detailSaveButton:
        		this.progDialog = DialogHelper.showDialog(context, getString(R.string.detail_dialog_saving_title),
        				getString(R.string.detail_dialog_saving_message), progDialog);
            	
            	Preferences prefs = new Preferences(context);
            	CheckBox checkbox = (CheckBox)rootView.findViewById(R.id.detail_notify_on_access);
            	prefs.setDoNotifyForPackage(packageName, checkbox.isChecked());
            	boolean setNotifyTo = checkbox.isChecked();
            	checkbox = (CheckBox)rootView.findViewById(R.id.detail_log_on_access);
            	setNotifyTo |= checkbox.isChecked();
            	prefs.setDoLogForPackage(packageName, checkbox.isChecked());
            	checkbox = null;
            	
            	PDroidAppSetting [] toAsyncTask = this.settingList.toArray(new PDroidAppSetting [this.settingList.size()]);
            	this.settingList = null;
            	AppSettingsSaveTask settingsWriterTask = new AppSettingsSaveTask(context, packageName, application.getUid(), setNotifyTo, new SaveCompleteHandler());
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
     * been modified. Used to receive input from other fragments.
     * 
     * @param packageName
     */
    public void loadApplicationDetail(String packageName) {
    	if (packageName != null) {
    		progDialog = DialogHelper.showDialog(context, null, getString(R.string.detail_dialog_loading_message), progDialog);
    		this.packageName = packageName;
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
				DialogHelper.dismissDialog(progDialog);
			} else {
				//setTitle(inApplication.getLabel());
				application = inApplication;
				AppSettingsLoadTask appDetailSettingsLoader = new AppSettingsLoadTask(context, new AppDetailSettingsLoaderTaskCompleteHandler());
				appDetailSettingsLoader.execute(application.getPackageName());
			}
		}
    }
    
    class AppDetailSettingsLoaderTaskCompleteHandler implements IAsyncTaskCallback<List<PDroidAppSetting>>
    {
		@Override
		public void asyncTaskComplete(List<PDroidAppSetting> inSettingList) {
			settingsAreLoaded = true;
			DialogHelper.dismissDialog(progDialog);
			
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
    
    class DeleteCompleteHandler implements IAsyncTaskCallback<Void>
    {	
		@Override
		public void asyncTaskComplete(Void param) {
			DialogHelper.dismissDialog(progDialog);
			callback.onDetailDelete();
		}
    }
    
    class SaveCompleteHandler implements IAsyncTaskCallback<Void>
    {	
		@Override
		public void asyncTaskComplete(Void param) {
			DialogHelper.dismissDialog(progDialog);
			callback.onDetailSave();
		}
    }
}
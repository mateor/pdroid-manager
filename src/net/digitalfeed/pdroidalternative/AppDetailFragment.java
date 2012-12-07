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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

/**
 * Fragment for viewing and modifying the settings of an application.
 * I plan to make a superclass which allows for managing settings more
 * generically, rather than attached to a specific application. That superclass
 * could then be used for modifying profiles, setting up batch operations, etc.
 * @author smorgan
 *
 */
public class AppDetailFragment extends PDroidSettingListFragment {

	private String packageName = null; //package name of the app being loaded/displayed
	private Application application; //stores an application object for the app being displayed
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		
		//TODO: Move this to the activity, and make a function call to the fragment to load the app?
        Bundle bundle = activity.getIntent().getExtras();
        //if we have a bundle, we can load the package. Otherwise, we do no such thing
        if (bundle != null) {
        	this.packageName = bundle.getString(AppDetailActivity.BUNDLE_PACKAGE_NAME);
        	//may still be loading onStart, so show dialog then
        	showDialogOnStart = true;
	        /*
	         * If this action has been called from an app listing, then the action bar should
	         * have the 'up' functionality which returns to the app listing.
	         */
	        this.inApp = bundle.getBoolean(AppDetailActivity.BUNDLE_IN_APP, false);
        }
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if the packageName is null, then we just hang out until we get something from the other frame
        //otherwise, load the application
        if (this.packageName != null) {
	        ApplicationLoadTask appDetailAppLoader = new ApplicationLoadTask(context, new AppLoadCompleteHandler());
	        appDetailAppLoader.execute(packageName);
        }        
    }
	
    
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
    	Preferences prefs = new Preferences(context);
    	if (this.notifyOnAccessCheckbox != null) {
    		this.notifyOnAccessCheckbox.setChecked(prefs.getDoNotifyForPackage(packageName));
    	}
    	if (this.logOnAccessCheckbox != null) {
    		this.logOnAccessCheckbox.setChecked(prefs.getDoLogForPackage(packageName));
    	}
    	prefs = null;

		return this.rootView;
	}
	
	@Override
	void doLoad() {}

	@Override
	boolean doDelete() {
		this.progDialog = DialogHelper.showDialog(context, getString(R.string.detail_dialog_saving_title),
				getString(R.string.detail_dialog_saving_message), progDialog);

    	AppSettingsDeleteTask settingsDeleterTask = new AppSettingsDeleteTask(context, packageName, new DeleteCompleteHandler());
    	settingsDeleterTask.execute();
    	return true;
	}

	@Override
	boolean doSave() {
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
    	return true;
	}

	@Override
	boolean doClose() {
    	if (!inApp) {
    		callback.onDetailClose();
        	return true;
    	} else {
    		return false;
    	}
	}
	
	@Override
	boolean doUp() {
        callback.onDetailUp();
        return true;
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}
	
    class AppLoadCompleteHandler implements IAsyncTaskCallback<Application>
    {
		@Override
		public void asyncTaskComplete(Application inApplication) {
			if (inApplication == null) {
				Log.d("PDroidAlternative", "inApplication is null: the app could have disappeared between the intent being created and the task running?");
				DialogHelper.dismissDialog(progDialog);
			} else {
				//setTitle(inApplication.getLabel());
				application = inApplication;
				AppSettingsLoadTask appDetailSettingsLoader = new AppSettingsLoadTask(context, new LoadCompleteHandler());
				appDetailSettingsLoader.execute(application.getPackageName());
			}
		}
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
	        ApplicationLoadTask appDetailAppLoader = new ApplicationLoadTask(context, new AppLoadCompleteHandler());
	        appDetailAppLoader.execute(packageName);
    	}
    }

}
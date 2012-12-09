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

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

/**
 * Fragment for viewing and modifying the settings of an application.
 * I plan to make a superclass which allows for managing settings more
 * generically, rather than attached to a specific application. That superclass
 * could then be used for modifying profiles, setting up batch operations, etc.
 * @author smorgan
 *
 */
public class AppDetailBatchFragment extends PDroidSettingListFragment {
	
	private List<String> packageNames = null; //list of package names for the apps being handled
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		rowLayout = R.layout.setting_list_row_batch;
		//TODO: Move this to the activity, and make a function call to the fragment to load the app?
        Bundle bundle = activity.getIntent().getExtras();
        //if we have a bundle, we can load the package. Otherwise, we do no such thing
        if (bundle != null) {
        	this.packageNames = Arrays.asList(bundle.getStringArray(AppDetailActivity.BUNDLE_PACKAGE_NAMES));
        	//may still be loading onStart, so show dialog then
        	showDialogOnStart = true;
	        /*
	         * If this action has been called from an app listing, then the action bar should
	         * have the 'up' functionality which returns to the app listing.
	         */
	        this.inApp = bundle.getBoolean(AppDetailActivity.BUNDLE_IN_APP, false);
        }
        getActivity().setTitle(getTitle());
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if the packageName is null, then we just hang out until we get something from the other frame
        //otherwise, load the application
        if (this.packageNames != null) {
			AppsSettingsLoadTask appDetailSettingsLoader = new AppsSettingsLoadTask(context, new LoadCompleteHandler());
			appDetailSettingsLoader.execute(this.packageNames.toArray(new String [this.packageNames.size()]));
		}
    }        
	
    
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		/*
    	Preferences prefs = new Preferences(context);
    	if (this.notifyOnAccessCheckbox != null) {
    		this.notifyOnAccessCheckbox.setChecked(prefs.getDoNotifyForPackage(packageName));
    	}
    	if (this.logOnAccessCheckbox != null) {
    		this.logOnAccessCheckbox.setChecked(prefs.getDoLogForPackage(packageName));
    	}
    	prefs = null;
    	*/

		return this.rootView;
	}
	
    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_app_detail_batch, menu);
    }
	
	@Override
	void doLoad() {}

	@Override
	boolean doDelete() {
		return true;
	}

	@Override
	boolean doSave() {
		DialogHelper.showProgressDialog(context, getString(R.string.detail_dialog_saving_title),
				getString(R.string.detail_dialog_saving_message));
    	
    	Preferences prefs = new Preferences(context);
    	boolean notifyOnAccess = notifyOnAccessCheckbox.isChecked();
    	boolean logOnAccess = logOnAccessCheckbox.isChecked();
    	boolean setNotifyTo = notifyOnAccess || logOnAccess;
    	
    	for (String packageName : packageNames) {
	    	prefs.setDoNotifyForPackage(packageName, notifyOnAccess);
	    	prefs.setDoLogForPackage(packageName, logOnAccessCheckbox.isChecked());
    	}
    	
    	PDroidAppSetting [] toAsyncTask = this.settingList.toArray(new PDroidAppSetting [this.settingList.size()]);
    	String [] pkgNamesToTask = packageNames.toArray(new String [packageNames.size()]);  
    	this.settingList = null;
    	AppsSettingsSaveTask settingsWriterTask = new AppsSettingsSaveTask(context, pkgNamesToTask, setNotifyTo, new SaveCompleteHandler());
    	settingsWriterTask.execute(toAsyncTask);
    	return true;
	}

	@Override
	boolean doClose() {
		callback.onDetailClose();
    	return true;
	}
	
	@Override
	boolean doUp() {
        callback.onDetailUp();
        return true;
	}

	@Override
	public String getTitle() {
		return getString(R.string.detail_actionbar_heading_batch);
	}
	
	@Override
	OnDetailRowActionListener getRowCallback() {
		return new DetailRowActionHandler();
	}
    
    /**
     * Triggers the load of details for the incoming application, totally ignoring
     * the state of the current application that may or may not have
     * been modified. Used to receive input from other fragments.
     * 
     * @param packageName
     */
    public void loadApplicationDetail(List<String> packageNames) {
    	if (packageNames != null && packageNames.size() > 0) {
    		DialogHelper.showProgressDialog(context, null, getString(R.string.detail_dialog_loading_message));
    		this.packageNames = packageNames;
	        //appDetailAppLoader.execute(packageNames);
    	}
    }

	class DetailRowActionHandler implements OnDetailRowActionListener {
		
		@Override
		public void onRadioButtonClick(RadioGroup group, int checkedId,
				int position, CheckedOption checkedOption) {
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"AppDetailBatchFragment:DetailRowActionHandler:onCheckboxChange");
			if (settingList == null || settingList.size() <= position || position < 0) {
				return;
			}
			
			PDroidAppSetting setting = settingList.get(position); 
			switch (checkedOption) {
			case ALLOW:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_ALLOW) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_ALLOW);
				}
				break;
			case YES:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_YES) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_YES);
				}
				break;
			case CUSTOM:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_CUSTOM) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_CUSTOM);
				}
				break;
			case CUSTOMLOCATION:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_CUSTOMLOCATION) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_CUSTOMLOCATION);
				}
				break;
			case RANDOM:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_RANDOM) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_RANDOM);
				}
				break;
			case DENY:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_DENY) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_DENY);
				}
				break;
			case NO:
				if (setting.getSelectedOptionBit() == PDroidSetting.OPTION_FLAG_NO) {
					group.check(R.id.option_nochange);
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				} else {
					setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO);
				}
				break;
			case NO_CHANGE:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				break;
			}	
		}

		@Override
		public void onInfoButtonPressed(int position) {
			if (settingList == null || settingList.size() <= position || position < 0) {
				return;
			}
			showInfo(settingList.get(position));
			
		}
    };
    
}
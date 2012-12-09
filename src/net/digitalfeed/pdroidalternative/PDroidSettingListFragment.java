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

import java.util.LinkedList;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Fragment for viewing and modifying the settings of an application.
 * I plan to make a superclass which allows for managing settings more
 * generically, rather than attached to a specific application. That superclass
 * could then be used for modifying profiles, setting up batch operations, etc.
 * @author smorgan
 *
 */
abstract class PDroidSettingListFragment extends Fragment {

	Context context;
	
	List<PDroidAppSetting> settingList; //List of settings objects
	boolean showDialogOnStart = false; //Used to determine whether the loading dialog should be displayed when the app becomes visible
	boolean inApp = false; //identifies whether in an app or not, which changes the 'up' or 'back' button behaviour 

	//by default, the 'normal' views will be used
	int mainLayout = R.layout.activity_app_detail;
	int rowLayout = R.layout.setting_list_row_standard;
	View rootView;
	ListView listView;
	CheckBox notifyOnAccessCheckbox;
	CheckBox logOnAccessCheckbox;
	
	static boolean hasOptionsMenu = true;
	
	OnDetailActionListener callback; //callback for when an action occurs (i.e. save, close, delete, up).
	OnDetailRowActionListener rowCallback;
	
	//Interface for callbacks
	public interface OnDetailActionListener {
		public void onDetailSave();
		public void onDetailClose();
		public void onDetailDelete();
		public void onDetailUp();
	}
	
	public interface OnDetailRowActionListener {
		enum CheckedOption {NO_CHANGE, ALLOW, DENY, YES, NO, CUSTOM, CUSTOMLOCATION, RANDOM};
		public void onRadioButtonClick(RadioGroup group, int checkedId, int position, CheckedOption checkedOption);
		public void onInfoButtonPressed(int position);
	}
	
	/**
	 * Stub to provide functionality to load the list of settings.
	 * Should use an asynctask, and the asynctask should call 
	 */
	abstract void doLoad();
	
	/**
	 * 
	 * @return
	 */
	abstract boolean doDelete();
	
	/**
	 * 
	 * @return
	 */
	abstract boolean doSave();

	/**
	 * 
	 * @return
	 */
	abstract boolean doClose();

	/**
	 * Used to obtain the title of the current fragment to be displayed in the action bar.
	 * To be called by the parent activity, frame manager, or so forth.
	 * @return
	 */
	public abstract String getTitle();
	
	OnDetailRowActionListener getRowCallback() {
		return new DetailRowActionHandler();
	}
	
	boolean doUp() { return doClose(); }
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onAttach");
		
		this.context = activity;

        // Check the container activity implements the callback interface
		// Thank you Google for the example code: https://developer.android.com/training/basics/fragments/communicating.html
        try {
            callback = (OnDetailActionListener) activity;
        } catch (ClassCastException e) {
        	callback = null;
        }
        
        this.rowCallback = getRowCallback();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onCreate");
        
        // need to notify do this to notify the activity that
        // this fragment will contribute to the action menu
        setHasOptionsMenu(hasOptionsMenu);
    }

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onCreateView");
		
		this.rootView = inflater.inflate(this.mainLayout, container);
		this.listView = (ListView)this.rootView.findViewById(R.id.detail_setting_list);

		if (this.rootView.findViewById(R.id.detail_notify_on_access) != null) {
			this.notifyOnAccessCheckbox = (CheckBox)this.rootView.findViewById(R.id.detail_notify_on_access);
		}
		if (this.rootView.findViewById(R.id.detail_log_on_access) != null) {
			this.logOnAccessCheckbox = (CheckBox)this.rootView.findViewById(R.id.detail_log_on_access);
		}
		
		return this.rootView;
	}

    @Override
    public void onStart() {
    	super.onStart();
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onStart");
    	
    	if (showDialogOnStart) {
    		DialogHelper.showProgressDialog(getActivity(), null, getString(R.string.detail_dialog_loading_message));
    	}
    }
        
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.detailCloseButton:
            	return doClose();
            case android.R.id.home:
            	return doUp();
            case R.id.detailDeleteButton:
            	return doDelete();
            case R.id.detailSaveButton:
        		return doSave();
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onCreateOptionsMenu");
        inflater.inflate(R.menu.activity_app_detail, menu);
    }

    public void setOnDetailActionListener(OnDetailActionListener callback) {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:setOnDetailActionListener");
        try {
            this.callback = (OnDetailActionListener) callback;
        } catch (ClassCastException e) {
        	throw new ClassCastException("Callback handler must implement OnDetailActionListener");
        }
    }

    void applyAdapter() {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:applyAdapter");
    	if (listView != null) {
    		listView.setAdapter(new PDroidSettingListAdapter(context, this.rowLayout, settingList, rowCallback));
    	}
    }

    void onDeleteComplete() {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onDeleteComplete");
		DialogHelper.dismissProgressDialog();
		callback.onDetailDelete();
    }

    class DeleteCompleteHandler implements IAsyncTaskCallback<Void>
    {	
		@Override
		public void asyncTaskComplete(Void param) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:DeleteCompleteHandler:asyncTaskComplete");
			onDeleteComplete();
		}
    }
   
    
    void onSaveComplete() {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onSaveComplete");
		DialogHelper.dismissProgressDialog();
		callback.onDetailSave();
    }
    
    class SaveCompleteHandler implements IAsyncTaskCallback<Void>
    {	
		@Override
		public void asyncTaskComplete(Void param) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:SaveCompleteHandler:asyncTaskComplete");
			onSaveComplete();
		}
    }
    
    void onLoadComplete(List<PDroidAppSetting> inSettingList) {
    	//Load has completed, so the 'loading' dialog need not show
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onLoadComplete");
    	this.showDialogOnStart = false;
    	
		if (inSettingList == null) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onLoadComplete:inSettingList is null");
		} else if (inSettingList.size() == 0) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:onLoadComplete:inSettingList is of size 0");
		} else {
			this.settingList = inSettingList;
			this.applyAdapter();
		}
		
		DialogHelper.dismissProgressDialog();

    }
    
    class LoadCompleteHandler implements IAsyncTaskCallback<List<PDroidAppSetting>>
    {
		@Override
		public void asyncTaskComplete(List<PDroidAppSetting> inSettingList) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:LoadCompleteHandler:onAsyncTaskComplete");
			onLoadComplete(inSettingList);
		}
    }
	
	class DetailRowActionHandler implements OnDetailRowActionListener {
		
		@Override
		public void onRadioButtonClick(RadioGroup group, int checkedId,
				int position, CheckedOption checkedOption) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:DetailRowActionHandler:onRadioButtonClick");
			if (settingList == null || settingList.size() <= position || position < 0) {
				return;
			}
			
			PDroidAppSetting setting = settingList.get(position); 
			switch (checkedOption) {
			case ALLOW:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_ALLOW);
				break;
			case YES:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_YES);
				break;
			case CUSTOM:
				showCustomValueBox(setting);
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_CUSTOM);
				break;
			case CUSTOMLOCATION:
				showCustomValueBox(setting);
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_CUSTOMLOCATION);
				break;
			case RANDOM:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_RANDOM);
				break;
			case DENY:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_DENY);
				break;
			case NO:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO);
				break;
			case NO_CHANGE:
				setting.setSelectedOptionBit(PDroidSetting.OPTION_FLAG_NO_CHANGE);
				break;
			}	
		}

		@Override
		public void onInfoButtonPressed(int position) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:DetailRowActionHandler:onInfoButtonPressed");
			if (settingList == null || settingList.size() <= position || position < 0) {
				return;
			}
			showInfo(settingList.get(position));
			
		}
    };

    /**
     * Show information relevant to a particular settings when the
     * 'information' button is clicked. Possibly should be relegated to a child class?
     * @param setting  Setting for which to show info (a.k.a. help)
     */
	void showInfo(PDroidSetting setting) {
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:showInfo");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(setting.getTitle());
		Resources res = context.getResources();
		int helpStringId = res.getIdentifier(PDroidSetting.SETTING_HELP_STRING_PREFIX + setting.getId(), "string", context.getPackageName());
		if (helpStringId != 0) {
			builder.setMessage(res.getString(helpStringId));
		} else {
			builder.setMessage(setting.getId());
		}
		builder.setPositiveButton(R.string.help_dialog_OKtext, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
	               // User clicked OK button
	           }
	       });
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	//TODO: Convert this to a DialogFragment?
	//TODO: Handle when the user presses cancel by returning to the previous setting?
	void showCustomValueBox(PDroidAppSetting appSetting) {
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"PDroidSettingListFragment:showCustomValueBox");
		List<SimpleImmutableEntry<String,String>> customValues = appSetting.getCustomValues();
		if (customValues == null) {
			customValues = new LinkedList<SimpleImmutableEntry<String,String>>();
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"No custom setting presents: setting them up");
			customValues = new LinkedList<SimpleImmutableEntry<String,String>>();
			for (String customFieldName : appSetting.getCustomFieldNames()) {
				customValues.add(new SimpleImmutableEntry<String,String>(customFieldName,""));
			}
		}
		
		final PDroidAppSetting innerAppSetting = appSetting;
    	AlertDialog.Builder valueInput = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
    	valueInput.setTitle(appSetting.getTitle());
    	//final RelativeLayout layout = new RelativeLayout(context);
    	final LinearLayout layout = new LinearLayout(context);
    	LinearLayout sublayout = null;
    	layout.setOrientation(LinearLayout.VERTICAL);
    	LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
			     LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	//layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	//layout.setLayoutParams(layoutParams);
    	TextView label = null;
    	EditText input = null;
    	final List<SimpleImmutableEntry<Integer, Integer>> inputsList = new LinkedList<SimpleImmutableEntry<Integer, Integer>>();
    	int viewId = 1;
    	for (SimpleImmutableEntry<String, String> entryItem : customValues) {
    		sublayout = new LinearLayout(context);
    		sublayout.setOrientation(LinearLayout.HORIZONTAL);
    		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Creating new text view/edit text for " + entryItem.getKey());
    		label = new TextView(context);
    		label.setId(viewId++);
    		label.setGravity(Gravity.LEFT);
	    	input = new EditText(context);
	    	input.setId(viewId++);
	    	label.setGravity(Gravity.RIGHT);
	    	
	    	label.setText(entryItem.getKey());
	    	if (entryItem.getValue() != null) {
	    		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Previous value is not null: " + entryItem.getKey());
	    		input.setText(entryItem.getValue());
    		}
	    	
	    	sublayout.addView(label);
	    	sublayout.addView(input, layoutParams);
	    	layout.addView(sublayout, layoutParams);
			//layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			//layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    		//if (lastInputs != null) {
    		//	if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Previous input is not null: going below");
    		//	layoutParams.addRule(RelativeLayout.BELOW, lastInputs.getKey());
    		//}
    		//layout.addView(label, layoutParams);
	    	
    		//layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    		//layoutParams.addRule(RelativeLayout.RIGHT_OF, label.getId());
    		//layout.addView(input, layoutParams);
    		
    		inputsList.add(new SimpleImmutableEntry<Integer, Integer>(label.getId(), input.getId()));
    	}

    	label = null;
    	input = null;
    	sublayout = null;
    	layoutParams = null;
    	
    	valueInput.setView(layout);
    	
    	valueInput.setPositiveButton(R.string.detail_custom_input_OKtext, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final List<SimpleImmutableEntry<String, String>> customValues = new LinkedList<SimpleImmutableEntry<String, String>>();
				for (SimpleImmutableEntry<Integer, Integer> inputs : inputsList) {
					customValues.add(new SimpleImmutableEntry<String, String>(
							((TextView)layout.findViewById(inputs.getKey())).getText().toString(),
							((TextView)layout.findViewById(inputs.getValue())).getText().toString()
						));
				}
				innerAppSetting.setCustomValues(customValues);
				dialog.dismiss();
			}
		});
    	valueInput.show();
    }
	
}
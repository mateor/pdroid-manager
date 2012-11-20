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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class AppDetailAdapter extends ArrayAdapter<AppSetting>{
	protected static final int VIEW_TYPE_STANDARD = 0;
	protected static final int VIEW_TYPE_LOCATION = 1;
	protected static final int VIEW_TYPE_YESNO = 2;
	
	
	private final Context context;
	private final int standardResourceId;
	private final AppSetting[] settingList;
	private OnCheckedChangeListener checkbuttonChangeListener;
	
	public AppDetailAdapter(Context context, int standardResourceId, AppSetting[] settingList) {
		super(context, standardResourceId, settingList);
		
		this.context = context;
		this.standardResourceId = standardResourceId;
		this.settingList = settingList;
		this.checkbuttonChangeListener = new CheckbuttonChangeListener();
	}
	
	@Override
	public int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public int getItemViewType(int position) {
		return 0;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		SettingHolder holder;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(this.standardResourceId, parent, false);

			holder = new SettingHolder();	
			holder.settingName = (TextView)row.findViewById(R.id.option_title);
			holder.radioGroup = (RadioGroup)row.findViewById(R.id.setting_choice);
			
			holder.allowOption = row.findViewById(R.id.option_allow);
			holder.yesOption = row.findViewById(R.id.option_yes);
			holder.customOption = row.findViewById(R.id.option_custom);
			holder.customLocationOption = row.findViewById(R.id.option_customlocation);
			holder.randomOption = row.findViewById(R.id.option_random);
			holder.denyOption = row.findViewById(R.id.option_deny);
			holder.noOption = row.findViewById(R.id.option_no); 
			row.setTag(holder);
		} else {
			holder = (SettingHolder)row.getTag();
		}

		//This approach to identified the clicked 'position' is based on http://stackoverflow.com/questions/9392511/how-to-handle-oncheckedchangelistener-for-a-radiogroup-in-a-custom-listview-adap
		//I am not entirely comfortable with always resetting this tag, but I'm not sure creating a class
		//to hold stuff in there and updating it would be better
		holder.radioGroup.setTag(Integer.valueOf(position));
		holder.radioGroup.setOnCheckedChangeListener(null);
		AppSetting setting = settingList[position];
		holder.settingName.setText(setting.getTitle());
		switch (setting.getSelectedOptionBit()){
		case Setting.OPTION_FLAG_ALLOW:
			holder.radioGroup.check(R.id.option_allow);
			break;
		case Setting.OPTION_FLAG_YES:
			holder.radioGroup.check(R.id.option_yes);
			break;
		case Setting.OPTION_FLAG_CUSTOM:
			holder.radioGroup.check(R.id.option_custom);
			break;
		case Setting.OPTION_FLAG_CUSTOMLOCATION:
			holder.radioGroup.check(R.id.option_customlocation);
			break;
		case Setting.OPTION_FLAG_RANDOM:
			holder.radioGroup.check(R.id.option_random);
			break;
		case Setting.OPTION_FLAG_DENY:
			holder.radioGroup.check(R.id.option_deny);
			break;
		case Setting.OPTION_FLAG_NO:
			holder.radioGroup.check(R.id.option_no);
			break;
		default:
			holder.radioGroup.check(R.id.option_allow);
			break;
		}
		
		int optionsBits = setting.getOptionsBits();
		if (0 == (optionsBits & Setting.OPTION_FLAG_ALLOW)) {
			holder.allowOption.setVisibility(View.GONE);
		} else {
			holder.allowOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & Setting.OPTION_FLAG_YES)) {
			holder.yesOption.setVisibility(View.GONE);
		} else {
			holder.yesOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & Setting.OPTION_FLAG_CUSTOM)) {
			holder.customOption.setVisibility(View.GONE);
		} else {
			holder.customOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & Setting.OPTION_FLAG_CUSTOMLOCATION)) {
			holder.customLocationOption.setVisibility(View.GONE);
		} else {
			holder.customLocationOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & Setting.OPTION_FLAG_RANDOM)) {
			holder.randomOption.setVisibility(View.GONE);
		} else {
			holder.randomOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & Setting.OPTION_FLAG_DENY)) {
			holder.denyOption.setVisibility(View.GONE);
		} else {
			holder.denyOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & Setting.OPTION_FLAG_NO)) {
			holder.noOption.setVisibility(View.GONE);
		} else {
			holder.noOption.setVisibility(View.VISIBLE);
		}
		 
		holder.radioGroup.setOnCheckedChangeListener(this.checkbuttonChangeListener);
		return row;
	}

	static class SettingHolder
	{
		TextView settingName;
		RadioGroup radioGroup;
		View allowOption;
		View yesOption;
		View customOption;
		View customLocationOption;
		View randomOption;
		View denyOption;
		View noOption;
	}
	
	public void showCustomValueBox(AppSetting appSetting) {
		List<SimpleImmutableEntry<String,String>> customValues = appSetting.getCustomValues();
		if (customValues == null) {
			Log.d("PDroidAlternative","No custom setting presents: setting them up");
			customValues = new LinkedList<SimpleImmutableEntry<String,String>>();
			if (0 != (appSetting.getSelectedOptionBit() & AppSetting.OPTION_FLAG_CUSTOM)) {
				Log.d("PDroidAlternative","Single custom setting");
				customValues.add(new SimpleImmutableEntry<String,String>("",""));
			} else if (0 != (appSetting.getSelectedOptionBit() & AppSetting.OPTION_FLAG_CUSTOMLOCATION)) {
				Log.d("PDroidAlternative","Lat/Long custom setting");
				customValues.add(new SimpleImmutableEntry<String,String>("Lat",""));
				customValues.add(new SimpleImmutableEntry<String,String>("Lon",""));
			}
		}
		
		final AppSetting innerAppSetting = appSetting;
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
    		Log.d("PDroidAlternative","Creating new text view/edit text for " + entryItem.getKey());
    		label = new TextView(context);
    		label.setId(viewId++);
    		label.setGravity(Gravity.LEFT);
	    	input = new EditText(context);
	    	input.setId(viewId++);
	    	label.setGravity(Gravity.RIGHT);
	    	
	    	label.setText(entryItem.getKey());
	    	if (entryItem.getValue() != null) {
	    		Log.d("PDroidAlternative","Previous value is not null: " + entryItem.getKey());
	    		input.setText(entryItem.getValue());
    		}
	    	
	    	sublayout.addView(label);
	    	sublayout.addView(input, layoutParams);
	    	layout.addView(sublayout, layoutParams);
			//layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			//layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    		//if (lastInputs != null) {
    		//	Log.d("PDroidAlternative","Previous input is not null: going below");
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
	
	private class CheckbuttonChangeListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			int position = (Integer)group.getTag();
			int currentSelectedId = settingList[position].getSelectedOptionBit();
			switch (checkedId){
			case R.id.option_allow:
				if (currentSelectedId != Setting.OPTION_FLAG_ALLOW) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_ALLOW);
				break;
			case R.id.option_yes:
				if (currentSelectedId != Setting.OPTION_FLAG_YES) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_YES);
				break;
			case R.id.option_custom:
				if (currentSelectedId != Setting.OPTION_FLAG_CUSTOM) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_CUSTOM);
				showCustomValueBox(settingList[position]);
				break;
			case R.id.option_customlocation:
				if (currentSelectedId != Setting.OPTION_FLAG_CUSTOMLOCATION) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_CUSTOMLOCATION);
				showCustomValueBox(settingList[position]);
				break;
			case R.id.option_random:
				if (currentSelectedId != Setting.OPTION_FLAG_RANDOM) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_RANDOM);
				break;
			case R.id.option_deny:
				if (currentSelectedId != Setting.OPTION_FLAG_DENY) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_DENY);
				break;
			case R.id.option_no:
				if (currentSelectedId != Setting.OPTION_FLAG_NO) settingList[position].setSelectedOptionBit(Setting.OPTION_FLAG_NO);
				break;
			}
		}
	};
}
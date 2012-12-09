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
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;


/**
 * Adapter to handle the display of PDroidAppSetting details:
 * It would be better if it were able to handle PDroidSetting and
 * all subclasses for more flexibility.
 * 
 * Right now, the way that onclick handlers are built into the
 * ArrayAdapter doesn't seem like a good idea - it would probably be
 * better to have them out in the parent object. Not sure
 * what the Android idiom is for this.
 * 
 * @author smorgan
 *
 */
public class PDroidSettingListAdapter extends ArrayAdapter<PDroidAppSetting>{
	protected static final String SETTING_HELP_STRING_PREFIX = "SETTING_HELP_";
	
	private final Context context;
	private final int standardResourceId;
	private final List<PDroidAppSetting> settingList;
	private OnCheckedChangeListener checkbuttonChangeListener;
	private OnClickListener radioButtonClickListener;
	private OnClickListener helpButtonClickListener;
	private PDroidSettingListFragment.OnDetailRowActionListener rowActionListener;
	
	public PDroidSettingListAdapter(Context context, int standardResourceId, List<PDroidAppSetting> settingList, PDroidSettingListFragment.OnDetailRowActionListener rowActionListener) {
		super(context, standardResourceId, settingList);
		
		this.context = context;
		this.standardResourceId = standardResourceId;
		this.settingList = settingList;
		this.helpButtonClickListener = new HelpButtonClickListener();
		this.radioButtonClickListener = new RadioButtonClickListener();
		this.rowActionListener = rowActionListener;
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
			holder.customValuePretext = (TextView)row.findViewById(R.id.option_custom_value_pretext);
			holder.customValue = (TextView)row.findViewById(R.id.option_custom_value);
			holder.radioGroup = (RadioGroup)row.findViewById(R.id.setting_choice);
			holder.helpButton = (ImageButton)row.findViewById(R.id.help_button);
			holder.helpButton.setOnClickListener(this.helpButtonClickListener);
			
			holder.noChangeOption = row.findViewById(R.id.option_nochange);
			
			holder.allowOption = row.findViewById(R.id.option_allow);
			holder.allowOption.setOnClickListener(radioButtonClickListener);
			holder.yesOption = row.findViewById(R.id.option_yes);
			holder.yesOption.setOnClickListener(radioButtonClickListener);
			holder.customOption = row.findViewById(R.id.option_custom);
			holder.customOption.setOnClickListener(radioButtonClickListener);
			holder.customLocationOption = row.findViewById(R.id.option_customlocation);
			holder.customLocationOption.setOnClickListener(radioButtonClickListener);
			holder.randomOption = row.findViewById(R.id.option_random);
			holder.randomOption.setOnClickListener(radioButtonClickListener);
			holder.denyOption = row.findViewById(R.id.option_deny);
			holder.denyOption.setOnClickListener(radioButtonClickListener);
			holder.noOption = row.findViewById(R.id.option_no);
			holder.noOption.setOnClickListener(radioButtonClickListener);
			row.setTag(holder);
		} else {
			holder = (SettingHolder)row.getTag();
		}

		//This approach to identified the clicked 'position' is based on http://stackoverflow.com/questions/9392511/how-to-handle-oncheckedchangelistener-for-a-radiogroup-in-a-custom-listview-adap
		//I am not entirely comfortable with always resetting this tag, but I'm not sure creating a class
		//to hold stuff in there and updating it would be better
		holder.radioGroup.setTag(Integer.valueOf(position));
		holder.helpButton.setTag(Integer.valueOf(position));
		holder.radioGroup.setOnCheckedChangeListener(null); //temporarily disable on-click listener in the hope that it will not trigger unnecessarily
		PDroidAppSetting setting = settingList.get(position);
		
		holder.settingName.setText(setting.getTitle());
		
		String customValueText = null;
		switch (setting.getSelectedOptionBit()){
		case PDroidSetting.OPTION_FLAG_NO_CHANGE:
			holder.radioGroup.check(R.id.option_nochange);
			break;
		case PDroidSetting.OPTION_FLAG_ALLOW:
			holder.radioGroup.check(R.id.option_allow);
			break;
		case PDroidSetting.OPTION_FLAG_YES:
			holder.radioGroup.check(R.id.option_yes);
			break;
		case PDroidSetting.OPTION_FLAG_CUSTOM:
			holder.radioGroup.check(R.id.option_custom);
			customValueText = getCustomValuesText(setting);
			break;
		case PDroidSetting.OPTION_FLAG_CUSTOMLOCATION:
			holder.radioGroup.check(R.id.option_customlocation);
			customValueText = getCustomValuesText(setting);
			break;
		case PDroidSetting.OPTION_FLAG_RANDOM:
			holder.radioGroup.check(R.id.option_random);
			break;
		case PDroidSetting.OPTION_FLAG_DENY:
			holder.radioGroup.check(R.id.option_deny);
			break;
		case PDroidSetting.OPTION_FLAG_NO:
			holder.radioGroup.check(R.id.option_no);
			break;
		default:
			holder.radioGroup.check(R.id.option_allow);
			break;
		}
		
		if (customValueText == null) {
			holder.customValue.setVisibility(View.GONE);
			holder.customValuePretext.setVisibility(View.GONE);
		} else {
			holder.customValue.setText(customValueText);
			holder.customValue.setVisibility(View.VISIBLE);
			holder.customValuePretext.setVisibility(View.VISIBLE);
		}
		
		int optionsBits = setting.getOptionsBits();
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_ALLOW)) {
			holder.allowOption.setVisibility(View.GONE);
		} else {
			holder.allowOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_YES)) {
			holder.yesOption.setVisibility(View.GONE);
		} else {
			holder.yesOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_CUSTOM)) {
			holder.customOption.setVisibility(View.GONE);
		} else {
			holder.customOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_CUSTOMLOCATION)) {
			holder.customLocationOption.setVisibility(View.GONE);
		} else {
			holder.customLocationOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_RANDOM)) {
			holder.randomOption.setVisibility(View.GONE);
		} else {
			holder.randomOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_DENY)) {
			holder.denyOption.setVisibility(View.GONE);
		} else {
			holder.denyOption.setVisibility(View.VISIBLE);
		}
		if (0 == (optionsBits & PDroidSetting.OPTION_FLAG_NO)) {
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
		TextView customValue;
		TextView customValuePretext;
		ImageButton helpButton;
		RadioGroup radioGroup;
		View noChangeOption;
		View allowOption;
		View yesOption;
		View customOption;
		View customLocationOption;
		View randomOption;
		View denyOption;
		View noOption;
	}

	private class RadioButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (v instanceof RadioButton) {
				
				if (rowActionListener == null) return;

				ViewParent parent = v.getParent();
				if (!(parent instanceof RadioGroup)) return;
				RadioGroup group = (RadioGroup)parent;

				Object tag = group.getTag();
				if (!(tag instanceof Integer)) return;
				
				int position = (Integer)tag;
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"RadioButton clicked " + Boolean.toString(((RadioButton)v).isChecked()));
				int checkedId = v.getId();
				
				switch (checkedId){
				case R.id.option_nochange:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.NO_CHANGE);
					break;
				case R.id.option_allow:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.ALLOW);
					break;
				case R.id.option_yes:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.YES);
					break;
				case R.id.option_custom:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.CUSTOM);
					break;
				case R.id.option_customlocation:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.CUSTOMLOCATION);
					break;
				case R.id.option_random:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.RANDOM);
					break;
				case R.id.option_deny:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.DENY);
					break;
				case R.id.option_no:
					rowActionListener.onRadioButtonClick(group, checkedId, position,
							PDroidSettingListFragment.OnDetailRowActionListener.CheckedOption.NO);
					break;
				}
				notifyDataSetChanged();
			}
		}
	};
	
	
	private class HelpButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (rowActionListener == null) return;
			int position = (Integer)v.getTag();
			rowActionListener.onInfoButtonPressed(position);
		}
	}
		
	private String getCustomValuesText(PDroidAppSetting setting) {
		if (setting.getCustomValues() != null && setting.getCustomValues().size() > 0) {
			List<String> customValueStrings = new LinkedList<String>();
			for (SimpleImmutableEntry<String, String> customValue : setting.getCustomValues()) {
				SpannableStringBuilder builder = new SpannableStringBuilder();
				if (customValue.getKey() != null && !(customValue.getKey().isEmpty())) {
					builder.append(customValue.getKey()).append(":");
				}
				if (customValue.getValue() != null && !(customValue.getValue().isEmpty())) {
					builder.append(customValue.getValue());
					builder.setSpan(new StyleSpan(Typeface.ITALIC), builder.length() - customValue.getValue().length(), builder.length(), 0);
				}
				
				if (!builder.toString().isEmpty()) {
					customValueStrings.add(builder.toString());
				}
			}
			
			if (customValueStrings.size() > 0) {
				return TextUtils.join(context.getString(R.string.detail_custom_value_spacer), customValueStrings);
			}
		}
		return null;
	}

}
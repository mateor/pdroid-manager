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

import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;

import android.privacy.PrivacySettings;

class PDroidSetting implements Comparable<PDroidSetting> {
	
	protected Method getSettingMethod = null;
	protected Method setSettingMethod = null;
	protected HashMap<String, Method> getValueMethods = null;
	protected HashMap<String, Method> setValueMethods = null;

	protected static final String SETTING_HELP_STRING_PREFIX = "SETTING_HELP_";
	
	protected String id;
	protected String name;
	protected String settingFunctionName;
	protected String valueFunctionNameStub;
	protected String title;
	protected String group;
	protected String groupTitle;
	protected String [] customFieldNames = null;
	protected int optionsBits;
	protected int trustedOptionBit;
	protected int sort;
	
	public static final int OPTION_FLAG_ALLOW = 1;
	public static final String OPTION_TEXT_ALLOW = "allow";
	public static final int OPTION_FLAG_CUSTOM = 2;
	public static final String OPTION_TEXT_CUSTOM = "custom";
	public static final int OPTION_FLAG_RANDOM = 4;
	public static final String OPTION_TEXT_RANDOM = "random";
	public static final int OPTION_FLAG_DENY = 8;
	public static final String OPTION_TEXT_DENY = "deny";
	public static final int OPTION_FLAG_CUSTOMLOCATION = 16;
	public static final String OPTION_TEXT_CUSTOMLOCATION = "customlocation";
	public static final int OPTION_FLAG_YES = 32;
	public static final String OPTION_TEXT_YES = "yes";
	public static final int OPTION_FLAG_NO = 64;
	public static final String OPTION_TEXT_NO = "no";
	public static final int OPTION_FLAG_NO_CHANGE = 128;
	public static final String OPTION_TEXT_NO_CHANGE = "nochange"; //this shouldn't go into the database. ever.
	public static final int OPTION_FLAG_UNSET = 256;
	public static final String OPTION_TEXT_UNSET = "unset"; //this shouldn't go into the database. ever.
	protected static final int OPTION_FLAG_NUMBITS = 9;
	
	public PDroidSetting(String id, String name, String settingFunctionName, String valueFunctionNameStub, String title, String group, String groupTitle, String [] options, String trustedOption, int sort) {
		this.id = id;
		this.name = name;
		this.settingFunctionName = settingFunctionName;
		this.valueFunctionNameStub = valueFunctionNameStub;
		this.title = title;
		this.group = group;
		this.groupTitle = groupTitle;
		
		this.optionsBits = optionsToBits(options);
		this.trustedOptionBit = optionToBit(trustedOption);
		this.sort = sort;
		//
		if (0 != (this.optionsBits & OPTION_FLAG_CUSTOMLOCATION)) {
			this.customFieldNames = new String[] {"Lat","Lon"};		
		} else if (0 != (this.optionsBits & OPTION_FLAG_CUSTOM)) {
			//I don't really like that 'blank' is used to define 'just one simple value' but
			//because I'm using the strings to postfix the functions I need it to be blank or
			//a known value
			this.customFieldNames = new String[] {""};
		}
	}
	
	public static int optionsToBits(String [] options) {
		if (options == null) return 0;
		
		int bits = 0;
		for (String option : options) {
			if (option.equals(OPTION_TEXT_ALLOW)) {
				bits |= OPTION_FLAG_ALLOW; 
			}
			if (option.equals(OPTION_TEXT_CUSTOM)) {
				bits |= OPTION_FLAG_CUSTOM; 
			}
			if (option.equals(OPTION_TEXT_CUSTOMLOCATION)) {
				bits |= OPTION_FLAG_CUSTOMLOCATION; 
			}
			if (option.equals(OPTION_TEXT_DENY)) {
				bits |= OPTION_FLAG_DENY; 
			}
			if (option.equals(OPTION_TEXT_NO)) {
				bits |= OPTION_FLAG_NO; 
			}
			if (option.equals(OPTION_TEXT_RANDOM)) {
				bits |= OPTION_FLAG_RANDOM; 
			}
			if (option.equals(OPTION_TEXT_YES)) {
				bits |= OPTION_FLAG_YES; 
			}
		}
		return bits;
	}
		
	public static String [] bitsToOptions(int bits) {
		if (bits == 0) return null;
		
		LinkedList<String> optionsList = new LinkedList<String>(); 
		if (0 != (bits & OPTION_FLAG_ALLOW)) {
			optionsList.add(OPTION_TEXT_ALLOW);
		}
		if (0 != (bits & OPTION_FLAG_CUSTOM)) {
			optionsList.add(OPTION_TEXT_CUSTOM);
		}
		if (0 != (bits & OPTION_FLAG_CUSTOMLOCATION)) {
			optionsList.add(OPTION_TEXT_CUSTOMLOCATION);
		}
		if (0 != (bits & OPTION_FLAG_DENY)) {
			optionsList.add(OPTION_TEXT_DENY);
		}
		if (0 != (bits & OPTION_FLAG_NO)) {
			optionsList.add(OPTION_TEXT_NO);
		}
		if (0 != (bits & OPTION_FLAG_RANDOM)) {
			optionsList.add(OPTION_TEXT_RANDOM);
		}
		if (0 != (bits & OPTION_FLAG_YES)) {
			optionsList.add(OPTION_TEXT_YES);
		}

		return optionsList.toArray(new String[optionsList.size()]);
	}
	
	public static int optionToBit(String option) {
		if (option == null) return 0;
		if (option.equals(OPTION_TEXT_ALLOW)) {
			return OPTION_FLAG_ALLOW; 
		}
		if (option.equals(OPTION_TEXT_CUSTOM)) {
				return OPTION_FLAG_CUSTOM; 
		}
		if (option.equals(OPTION_TEXT_CUSTOMLOCATION)) {
			return OPTION_FLAG_CUSTOMLOCATION; 
		}
		if (option.equals(OPTION_TEXT_DENY)) {
			return OPTION_FLAG_DENY; 
		}
		if (option.equals(OPTION_TEXT_NO)) {
			return OPTION_FLAG_NO; 
		}
		if (option.equals(OPTION_TEXT_RANDOM)) {
			return OPTION_FLAG_RANDOM; 
		}
		if (option.equals(OPTION_TEXT_YES)) {
			return OPTION_FLAG_YES; 
		}
		throw new InvalidParameterException("The option passed to optionToBit must be one of the OPTION_TEXT_* group");
	}
		
	public static String bitToOption(final int bit) {
		if (0 == bit) return null;
		
		switch (bit) {
		case OPTION_FLAG_ALLOW:
			return OPTION_TEXT_ALLOW;
		case OPTION_FLAG_CUSTOM:
			return OPTION_TEXT_CUSTOM;
		case OPTION_FLAG_CUSTOMLOCATION:
			return OPTION_TEXT_CUSTOMLOCATION;
		case OPTION_FLAG_DENY:
			return OPTION_TEXT_DENY;
		case OPTION_FLAG_NO:
			return OPTION_TEXT_NO;
		case OPTION_FLAG_RANDOM:
			return OPTION_TEXT_RANDOM;
		case OPTION_FLAG_YES:
			return OPTION_TEXT_YES;
		default:
			throw new InvalidParameterException("A bit must be provided to bitToOption");
		}
	}
	
	public byte optionBitToCoreOption(int optionBit) {
		switch (optionBit) {
		case PDroidSetting.OPTION_FLAG_ALLOW:
		case PDroidSetting.OPTION_FLAG_YES:
			return PrivacySettings.REAL;
		case PDroidSetting.OPTION_FLAG_DENY:
		case PDroidSetting.OPTION_FLAG_NO:
			return PrivacySettings.EMPTY;
		case PDroidSetting.OPTION_FLAG_CUSTOM:
		case PDroidSetting.OPTION_FLAG_CUSTOMLOCATION:
			return PrivacySettings.CUSTOM;
		case PDroidSetting.OPTION_FLAG_RANDOM:
			return PrivacySettings.RANDOM;
		default:
			throw new RuntimeException("The Setting option must be a recognised option type");
		}
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getSettingFunctionName() {
		return this.settingFunctionName;
	}
	
	public String getValueFunctionNameStub() {
		return this.valueFunctionNameStub;
	}
	
	public String getGroup() {
		return this.group;
	}
	
	public String getGroupTitle() {
		return this.groupTitle;
	}

	public String getTitle() {
		return this.title;
	}
	
	public String [] getOptions() {
		return bitsToOptions(this.optionsBits);
	}
	
	public int getOptionsBits() {
		return this.optionsBits;
	}
	
	public String getTrustedOption() {
		return bitToOption(this.trustedOptionBit);
	}
	
	public int getTrustedOptionBit() {
		return this.trustedOptionBit;
	}
	
	public String [] getCustomFieldNames() {
		return this.customFieldNames;
	}
	
	public int getSort() {
		return this.sort;
	}
	
	public Method getGetSettingMethod() {
		if (this.getSettingMethod == null) {
			try {
				this.getSettingMethod = PrivacySettings.class.getMethod("get" + settingFunctionName);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.getSettingMethod;
	}
	
	public Method getSetSettingMethod() {
		if (this.setSettingMethod == null) {
			try {
				this.setSettingMethod = PrivacySettings.class.getMethod("set" + settingFunctionName, byte.class);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.setSettingMethod;
	}
	
	
	public Method getGetValueMethod(String valueName) {
		if (this.getValueMethods == null) {
			this.getValueMethods = new HashMap<String, Method>();
		}
		
		StringBuilder methodName = new StringBuilder("get").append(valueFunctionNameStub);
		if (!valueName.equals("")) {
			methodName.append(valueName);
		}
		Method getValueMethod = null;
		
		if (!this.getValueMethods.containsKey(valueName)) {
			try {
				getValueMethod = PrivacySettings.class.getMethod(methodName.toString());
				this.getValueMethods.put(valueName, getValueMethod);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  else {
			getValueMethod = this.getValueMethods.get(valueName);
		}
		return getValueMethod;
	}
	
	
	public Method getSetValueMethod(String valueName) {
		if (this.setValueMethods == null) {
			this.setValueMethods = new HashMap<String, Method>();
		}
		
		StringBuilder methodName = new StringBuilder("set").append(valueFunctionNameStub);
		if (!valueName.equals("")) {
			methodName.append(valueName);
		}
		Method setValueMethod = null;
		
		if (!this.setValueMethods.containsKey(valueName)) {
			try {
				setValueMethod = PrivacySettings.class.getMethod(methodName.toString(), String.class);
				this.setValueMethods.put(valueName, setValueMethod);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  else {
			setValueMethod = this.setValueMethods.get(valueName);
		}
		return setValueMethod;
	}
	
	@Override
	public int compareTo(PDroidSetting another) {
		return this.id.compareTo(another.id);
	}
}

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
import java.util.LinkedList;

class Setting implements Comparable<Setting> {
	protected String id;
	protected String name;
	protected String settingFunctionName;
	protected String valueFunctionNameStub;
	protected String title;
	protected String group;
	protected String groupTitle;
	protected int optionsBits;
	protected int trustedOptionBit;
	
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
	protected static final int OPTION_FLAG_NUMBITS = 7;
	
	public Setting(String id, String name, String settingFunctionName, String valueFunctionNameStub, String title, String group, String groupTitle, String [] options, String trustedOption) {
		this.id = id;
		this.name = name;
		this.settingFunctionName = settingFunctionName;
		this.valueFunctionNameStub = valueFunctionNameStub;
		this.title = title;
		this.group = group;
		this.groupTitle = groupTitle;
		
		this.optionsBits = optionsToBits(options);
		this.trustedOptionBit = optionToBit(trustedOption);
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
	
	@Override
	public int compareTo(Setting another) {
		return this.id.compareTo(another.id);
	}
}

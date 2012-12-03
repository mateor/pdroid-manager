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
import java.util.HashMap;
import android.privacy.PrivacySettings;

class PDroidSettingWithFunctions extends PDroidSetting {
	protected Method getSettingMethod = null;
	protected Method setSettingMethod = null;
	protected HashMap<String, Method> getValueMethods = null;
	protected HashMap<String, Method> setValueMethods = null;
	

	public PDroidSettingWithFunctions(String id, String name, String settingFunctionName, String valueFunctionNameStub, String title, String group, String groupTitle, String [] options, String trustedOption) {
		super(id, name, settingFunctionName, valueFunctionNameStub, title, group, groupTitle, options, trustedOption);
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
}

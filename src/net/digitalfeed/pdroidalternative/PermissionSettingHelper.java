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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.privacy.PrivacySettings;
import android.text.TextUtils;
import android.util.Log;

import java.util.AbstractMap.SimpleImmutableEntry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A helper class providing a set of functions to update or parse information from
 * PrivacySettings objects
 * 
 * @author smorgan
 *
 */
class PermissionSettingHelper {

	public enum TrustState {TRUSTED, UNTRUSTED};
	public static final String BACKUP_SETTING_REAL = "real";
	public static final String BACKUP_SETTING_CUSTOM  = "custom";
	public static final String BACKUP_SETTING_RANDOM = "random";
	public static final String BACKUP_SETTING_EMPTY = "empty";
	
	public static final String XML_DEFAULTVALUE_TAG_NAME = "default";
	
	//TODO: instead of using four lists with different structures, just use PDroidSettingWithFunctions lists for everything?
	private List<SimpleImmutableEntry<Method,String>> permissionReadMethods = null;
	private List<SimpleImmutableEntry<Method,String>> permissionWriteMethods = null;
	private List<PDroidSettingWithFunctions> settingList = null;
	private HashMap<String, PDroidSettingWithFunctions> settingMap= null;
	
	public PermissionSettingHelper() {};
	
	/**
	 * Retrieves a list of permissions to which settings are attached (e.g.
	 * android.permission.READ_PHONE_STATE is a permission of interest
	 * because it is tied to the ALLOW_READ_DEVICE_ID setting and others).
	 * See permission_setting_map.xml for the relationships.
	 * 
	 * @param db
	 * @return
	 */
	public static HashSet<String> getPermissionsOfInterest(SQLiteDatabase db) {
		HashSet <String> permissionsOfInterest = new HashSet<String>();
		
		Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_PERMISSIONS_OF_INTEREST, null);
		if (cursor.getCount() > 0) {
			int permissionColumnNum = cursor.getColumnIndex(DBInterface.PermissionSettingTable.COLUMN_NAME_PERMISSION);
			cursor.moveToFirst();
			{
				permissionsOfInterest.add(cursor.getString(permissionColumnNum));
			} while (cursor.moveToNext());
		}
		
		return permissionsOfInterest;
	}
	

	
	/**
	 * Calls the 'setting function' on a PrivacyObject for each known setting (using reflection)
	 * and compares the result against the 'trusted' state for that setting.
	 * The individual functions of the PrivacySettings object are cached for re-use
	 * to optimise the checking of large numbers of privacySettings objects
	 * (e.g. when generating the application list in the first place).
	 *    
	 * @param db SQLite database to use - this will *not* be closed at the end
	 * @param privacySettings - privacySettings object to check
	 * @return
	 */
	//TODO: There is a bug when all settings are updated to 'trusted', such that the trust detection identifies the app as 'untrusted'.
	public boolean isPrivacySettingsUntrusted (SQLiteDatabase db, PrivacySettings privacySettings) {
		if (db == null) {
			throw new InvalidParameterException("database passed to isPrivacySettingsUntrusted must not be null");
		}
		if (privacySettings == null) {
			throw new InvalidParameterException("Privacy settings passed to isPrivacySettingsUntrusted must not be null");
		}

		//If we do not already have handles to the relevant methods, then we need to get them
		//They are cached on the first run
		if (this.permissionReadMethods == null) {
			this.permissionReadMethods = new LinkedList<SimpleImmutableEntry<Method,String>>();
			Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_FUNCTION_NAMES, null);
	    	int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
	    	int settingTrustedOptionColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TRUSTED_OPTION);
	    	
	    	if (cursor.getCount() < 1) {
	    		//This will happen if the database is not initialised for some reason
	    		throw new DatabaseUninitialisedException("No settings are present in the database: it must not be initialised correctly");
	    	}
	    	
			cursor.moveToFirst();
			do {
				String settingFunctionName = cursor.getString(settingFunctionNameColumn);
				String settingTrustedOption = cursor.getString(settingTrustedOptionColumn);
				try {
					permissionReadMethods.add(
							new SimpleImmutableEntry<Method, String>(privacySettings.getClass().getMethod("get" + settingFunctionName), settingTrustedOption)
							);
				} catch (NoSuchMethodException e) {
				   Log.d("PDroidAlternative","PrivacySettings object of privacy service is missing the expected method " + settingFunctionName);
				   e.printStackTrace();
				} catch (IllegalArgumentException e) {
					Log.d("PDroidAlternative","Illegal arguments when calling " + settingFunctionName);
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
			cursor.close();
		}
		
		for (SimpleImmutableEntry<Method, String> row : this.permissionReadMethods) {
			//Reflection may not be the best way of doing this, but otherwise this is going to be a great
			//big select statement, which ties us more closely to the specific set of options (which are
			//right now fairly loosely coupled)
			//The result is short-circuted (i.e. a 'true' is returned when the first untrusted setting is encountered)
			try {
				byte pdroidCoreSetting = (Byte)row.getKey().invoke(privacySettings);
				switch (pdroidCoreSetting) {
				case PrivacySettings.REAL:
					if (!row.getValue().equals(PDroidSetting.OPTION_TEXT_ALLOW) &&
						!row.getValue().equals(PDroidSetting.OPTION_TEXT_YES)) {
						return true;
					}
					break;
				case PrivacySettings.CUSTOM:
					return true;
				case PrivacySettings.RANDOM:
					return true;
				case PrivacySettings.EMPTY:
					if (!row.getValue().equals(PDroidSetting.OPTION_TEXT_DENY) &&
						!row.getValue().equals(PDroidSetting.OPTION_TEXT_NO)) {
						return true;
					}
				}
			} catch (IllegalArgumentException e) {
				Log.d("PDroidAlternative","Illegal arguments when calling " + row.getKey().getName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d("PDroidAlternative","Illegal access when calling " + row.getKey().getName());
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.d("PDroidAlternative","InvocationTargetException when calling " + row.getKey().getName());
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
/**
 * Updates all privacy settings on a PrivacySettings object to a new state/value. Note that this does
 * not actually save the changes back to the service - this needs to be done outside this function.
 * The functions on the PrivacySettings (accessed using reflection) are cached for multiple calls
 * to the function (i.e. if running operations on multiple apps)
 * This was originally added to handle 'deny all' and 'allow all', but this has been replaced in function
 * by 'setPrivacySettingsToTrustState' which handles reversal of 'trusted' options.
 * 
 * @param db Readable database handle used to get the name of settings functions
 * @param privacySettings PrivacySettings object for the relevant application retrieved from the privacy service
 * @param newOption The new Setting.OPTION_FLAG_xxx to set (which is mapped to the PDroid core constant)
 */
	public void setPrivacySettings (SQLiteDatabase db, PrivacySettings privacySettings, int newOption) {
		//If we do not already have handles to the relevant methods, then we need to get them
		//They are cached on the first run
		if (this.permissionWriteMethods == null) {
			this.permissionWriteMethods = new LinkedList<SimpleImmutableEntry<Method,String>>();

			Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_FUNCTION_NAMES, null);
	    	int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
	    	int settingTrustedOptionColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TRUSTED_OPTION);
	    	
	    	if (cursor.getCount() < 1) {
	    		//This will happen if the database is not initialised for some reason
	    		throw new DatabaseUninitialisedException("No settings are present in the database: it must not be initialised correctly");
	    	}
	    	
			cursor.moveToFirst();
			do {
				String settingFunctionName = cursor.getString(settingFunctionNameColumn);
				String settingTrustedOption = cursor.getString(settingTrustedOptionColumn);
				try {
					permissionWriteMethods.add(
							new SimpleImmutableEntry<Method,String>(privacySettings.getClass().getMethod("set" + settingFunctionName, byte.class),settingTrustedOption)
							);
				} catch (NoSuchMethodException e) {
				   Log.d("PDroidAlternative","PrivacySettings object of privacy service is missing the expected method " + settingFunctionName);
				   e.printStackTrace();
				} catch (IllegalArgumentException e) {
					Log.d("PDroidAlternative","Illegal arguments when calling " + settingFunctionName);
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
			cursor.close();
		}
				
		byte newValueByte;
		switch (newOption) {
		case PDroidSetting.OPTION_FLAG_ALLOW:
		case PDroidSetting.OPTION_FLAG_YES:
			newValueByte = PrivacySettings.REAL;
			break;
		case PDroidSetting.OPTION_FLAG_CUSTOM:
		case PDroidSetting.OPTION_FLAG_CUSTOMLOCATION:
			newValueByte = PrivacySettings.CUSTOM;
			break;				
		case PDroidSetting.OPTION_FLAG_DENY:
		case PDroidSetting.OPTION_FLAG_NO:
			newValueByte = PrivacySettings.EMPTY;
			break;
		case PDroidSetting.OPTION_FLAG_RANDOM:
			newValueByte = PrivacySettings.RANDOM;
			break;
		default:
			throw new InvalidParameterException("The supplied parameter two update settings was invalid");
		}
		
		for (SimpleImmutableEntry<Method, String> row : this.permissionWriteMethods) {
			//Reflection may not be the best way of doing this, but otherwise this is going to be a great
			//big select statement, which ties us more closely to the specific set of options (which are
			//right now fairly loosely coupled - although to entirely loosely coupled)
			try {
				row.getKey().invoke(privacySettings, newValueByte);
			} catch (IllegalArgumentException e) {
				Log.d("PDroidAlternative","Illegal arguments when calling " + row.getKey().getName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d("PDroidAlternative","Illegal access when calling " + row.getKey().getName());
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.d("PDroidAlternative","InvocationTargetException when calling " + row.getKey().getName());
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Updates all privacy settings on a PrivacySettings object to either 'trusted' or 'untrusted'.
	 * Because there can be multiple 'untrusted' options, the 'untrusted' option is assumed to be
	 * the inverse of the 'trusted' option: i.e. yes -> no; allow -> deny; the inverse of custom and random is
	 * also allow (but it would then reverse from allow to deny).
	 * This is used by 'allow all' and 'deny all' - even though strictly speaking it doesn't allow
	 * or deny all.
	 * 
	 * @param db Readable database handle used to get the name of settings functions
	 * @param privacySettings PrivacySettings object for the relevant application retrieved from the privacy service
	 * @param toTrusted True to set all values to their 'trusted' state, false to set to their 'untrusted' state.
	 */
		public void setPrivacySettingsToTrustState (SQLiteDatabase db, PrivacySettings privacySettings, TrustState newTrustState) {
			//If we do not already have handles to the relevant methods, then we need to get them
			//They are cached on the first run
			if (this.permissionWriteMethods == null) {
				this.permissionWriteMethods = new LinkedList<SimpleImmutableEntry<Method,String>>();

				Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_FUNCTION_NAMES, null);
		    	int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
		    	int settingTrustedOptionColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TRUSTED_OPTION);
		    	
		    	if (cursor.getCount() < 1) {
		    		//This will happen if the database is not initialised for some reason
		    		throw new DatabaseUninitialisedException("No settings are present in the database: it must not be initialised correctly");
		    	}
		    	
				cursor.moveToFirst();
				do {
					String settingFunctionName = cursor.getString(settingFunctionNameColumn);
					String settingTrustedOption = cursor.getString(settingTrustedOptionColumn);
					try {
						permissionWriteMethods.add(
								new SimpleImmutableEntry<Method,String>(privacySettings.getClass().getMethod("set" + settingFunctionName, byte.class), settingTrustedOption)
								);
					} catch (NoSuchMethodException e) {
					   Log.d("PDroidAlternative","PrivacySettings object of privacy service is missing the expected method " + settingFunctionName);
					   e.printStackTrace();
					} catch (IllegalArgumentException e) {
						Log.d("PDroidAlternative","Illegal arguments when calling " + settingFunctionName);
						e.printStackTrace();
					}
				} while (cursor.moveToNext());
				cursor.close();
			}

			byte newValueByte;
			for (SimpleImmutableEntry<Method, String> row : this.permissionWriteMethods) {
				//Reflection may not be the best way of doing this, but otherwise this is going to be a great
				//big select statement, which ties us more closely to the specific set of options (which are
				//right now fairly loosely coupled - although to entirely loosely coupled)

				if (TrustState.TRUSTED == newTrustState) {
					if (row.getValue().equals(PDroidSetting.OPTION_TEXT_ALLOW) ||
							row.getValue().equals(PDroidSetting.OPTION_TEXT_YES)) {
						newValueByte = PrivacySettings.REAL;
					} else if (row.getValue().equals(PDroidSetting.OPTION_TEXT_DENY) ||
							row.getValue().equals(PDroidSetting.OPTION_TEXT_NO)) {
						newValueByte = PrivacySettings.EMPTY;
					} else if (row.getValue().equals(PDroidSetting.OPTION_TEXT_CUSTOM) ||
							row.getValue().equals(PDroidSetting.OPTION_TEXT_CUSTOMLOCATION)) {
						newValueByte = PrivacySettings.CUSTOM;
					} else if (row.getValue().equals(PDroidSetting.OPTION_TEXT_RANDOM)) {
						newValueByte = PrivacySettings.RANDOM;
					} else {
						throw new RuntimeException("The Setting trusted option must be a recognised option type");
					}
				} else if (TrustState.UNTRUSTED == newTrustState) {
					if (row.getValue().equals(PDroidSetting.OPTION_TEXT_ALLOW) ||
							row.getValue().equals(PDroidSetting.OPTION_TEXT_YES)) {
						newValueByte = PrivacySettings.EMPTY;
					} else if (row.getValue().equals(PDroidSetting.OPTION_TEXT_DENY) ||
							row.getValue().equals(PDroidSetting.OPTION_TEXT_NO)) {
						newValueByte = PrivacySettings.REAL;
					} else if (row.getValue().equals(PDroidSetting.OPTION_TEXT_CUSTOM) ||
							row.getValue().equals(PDroidSetting.OPTION_TEXT_CUSTOMLOCATION)) {
						newValueByte = PrivacySettings.EMPTY;
					} else if (row.getValue().equals(PDroidSetting.OPTION_TEXT_RANDOM)) {
						newValueByte = PrivacySettings.EMPTY;
					} else {
						throw new RuntimeException("The Setting trusted option must be a recognised option type");
					}
				} else {
					throw new RuntimeException("In invalid trust state was somehow used.");
				}
				
				try {
					row.getKey().invoke(privacySettings, newValueByte);
				} catch (IllegalArgumentException e) {
					Log.d("PDroidAlternative","Illegal arguments when calling " + row.getKey().getName());
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					Log.d("PDroidAlternative","Illegal access when calling " + row.getKey().getName());
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					Log.d("PDroidAlternative","InvocationTargetException when calling " + row.getKey().getName());
					e.printStackTrace();
				}
			}
		}
		
		private class SettingFunctions {
			public String name;
			public Method settingFunctionName;
			public Method valueFunctionNameStub;
			
			public SettingFunctions(String name, Method settingFunctionName, Method valueFunctionNameStub) {
				this.name = name;
				this.settingFunctionName = settingFunctionName;
				this.valueFunctionNameStub = valueFunctionNameStub;
			}
		}
		
		/**
		 * Calls the 'setting function' on a PrivacyObject for each known setting (using reflection)
		 * and builds an XML node tree recording the settings
		 * The individual functions of the PrivacySettings object are cached for re-use
		 * to optimise the checking of large numbers of privacySettings objects
		 *    
		 * @param db SQLite database to use - this will *not* be closed at the end
		 * @param privacySettings - privacySettings object to check
		 * @param document XML document from which the returned Element will be created 
		 * @return XML element containing settings
		 */
		public Element getPrivacySettingsXml (SQLiteDatabase db, PrivacySettings privacySettings, Document document) {
			if (db == null) {
				throw new InvalidParameterException("database passed to getPrivacySettingsXml must not be null");
			}
			if (privacySettings == null) {
				throw new InvalidParameterException("Privacy settings passed to getPrivacySettingsXml must not be null");
			}
						
			if (this.settingList == null) {
				loadSettingsList(db);
			}

			
			Element app = document.createElement(PreferencesListFragment.BACKUP_XML_APP_NODE);
			app.setAttribute("packagename", privacySettings.getPackageName());
			Element setting;
			
			byte pdroidCoreSetting = 0;
			String pdroidCoreValue = null; 
			
			for (PDroidSettingWithFunctions row : this.settingList) {
				try {
					pdroidCoreSetting = (Byte)row.getGetSettingMethod().invoke(privacySettings);
				} catch (IllegalArgumentException e) {
					Log.d("PDroidAlternative","Illegal arguments when calling " + row.getGetSettingMethod().getName());
					e.printStackTrace();
					throw new RuntimeException("Illegal arguments when calling " + row.getGetSettingMethod().getName());
				} catch (IllegalAccessException e) {
					Log.d("PDroidAlternative","Illegal access when calling " + row.getGetSettingMethod().getName());
					e.printStackTrace();
					throw new RuntimeException("Illegal access when calling " + row.getGetSettingMethod().getName());
				} catch (InvocationTargetException e) {
					Log.d("PDroidAlternative","InvocationTargetException when calling " + row.getGetSettingMethod().getName());
					e.printStackTrace();
					throw new RuntimeException("InvocationTargetException when calling " + row.getGetSettingMethod().getName());
				}

				setting = document.createElement(row.name);

				switch (pdroidCoreSetting) {
				case PrivacySettings.REAL:
					setting.setAttribute(PreferencesListFragment.BACKUP_XML_SETTING_ATTRIBUTE, BACKUP_SETTING_REAL);
					break;
				case PrivacySettings.CUSTOM:
					setting.setAttribute(PreferencesListFragment.BACKUP_XML_SETTING_ATTRIBUTE, BACKUP_SETTING_CUSTOM);
					break;
				case PrivacySettings.RANDOM:
					setting.setAttribute(PreferencesListFragment.BACKUP_XML_SETTING_ATTRIBUTE, BACKUP_SETTING_RANDOM);
					break;
				case PrivacySettings.EMPTY:
					setting.setAttribute(PreferencesListFragment.BACKUP_XML_SETTING_ATTRIBUTE, BACKUP_SETTING_EMPTY);
					break;
				}
				String [] customValueFieldNames = row.getCustomFieldNames();
				if (customValueFieldNames != null) {
					Element customValueElement;
					for (String customValueField : customValueFieldNames) {
						if (row.valueFunctionNameStub != null) {
							try {
								pdroidCoreValue = (String)row.getGetValueMethod(customValueField).invoke(privacySettings);
							} catch (IllegalArgumentException e) {
								Log.d("PDroidAlternative","Illegal arguments when calling " + row.getGetValueMethod(customValueField).getName());
								e.printStackTrace();
								throw new RuntimeException("Illegal arguments when calling " + row.getGetValueMethod(customValueField).getName());
							} catch (IllegalAccessException e) {
								Log.d("PDroidAlternative","Illegal access when calling " + row.getGetValueMethod(customValueField).getName());
								e.printStackTrace();
								throw new RuntimeException("Illegal access when calling " + row.getGetValueMethod(customValueField).getName());
							} catch (InvocationTargetException e) {
								Log.d("PDroidAlternative","InvocationTargetException when calling " + row.getGetValueMethod(customValueField).getName());
								e.printStackTrace();
								throw new RuntimeException("InvocationTargetException when calling " + row.getGetValueMethod(customValueField).getName());
							}

							if (pdroidCoreValue != null && !pdroidCoreValue.isEmpty()) {
								//create an element for the value. We could probably do away with the 'default element' but that would do
								//funny things to the nesting.
								if (customValueField == "") {
									customValueElement = document.createElement(XML_DEFAULTVALUE_TAG_NAME);	
								} else {
									customValueElement = document.createElement(customValueField);
								}
								customValueElement.setTextContent(pdroidCoreValue);
								setting.appendChild(customValueElement);
							}
						}
					}
				}

				app.appendChild(setting);
				
			}
			
			return app;
		}
		
		/**
		 * Calls the 'setting function' on a PrivacyObject for each known setting (using reflection)
		 * restoring data from the provided XML node
		 * The individual functions of the PrivacySettings object are cached for re-use
		 * to optimise the checking of large numbers of privacySettings objects
		 *    
		 * @param db SQLite database to use - this will *not* be closed at the end
		 * @param privacySettings - privacySettings object to check or update?
		 * @param XML element representing the app for which settings are being restored 
		 */
		public void setPrivacySettingsFromXml (SQLiteDatabase db, PrivacySettings privacySettings, Element element) {
			if (db == null) {
				throw new InvalidParameterException("database passed to getPrivacySettingsXml must not be null");
			}
			if (privacySettings == null) {
				throw new InvalidParameterException("Privacy settings passed to getPrivacySettingsXml must not be null");
			}

			//If we have not loaded the list of settings, do so now.
			//This is held across runs to improve performance. The objects stored in the
			//list and map also grab the methods for reflection on demand and cache them
			//for subsequent runs
			if (this.settingMap == null) {
				loadSettingsList(db);
			}
			
			if (element.hasChildNodes()) {
				NodeList settings = element.getChildNodes();
				Element setting;
				byte pdroidCoreSetting = 0;
				String pdroidCoreValue = null;
				String pdroidSettingValue;
				String pdroidSettingName;
				PDroidSettingWithFunctions settingWithFunctions;

				for (int settingNum = 0; settingNum < settings.getLength(); settingNum++) {
					Node node = settings.item(settingNum);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						setting = (Element)node; //TODO: verify this is actually an element
						pdroidSettingName = setting.getTagName();
						
						if (this.settingMap.containsKey(pdroidSettingName)) {
							settingWithFunctions = this.settingMap.get(pdroidSettingName);
							
							pdroidSettingValue = setting.getAttribute(PreferencesListFragment.BACKUP_XML_SETTING_ATTRIBUTE);
							pdroidCoreValue = setting.getTextContent();
	
							if (pdroidSettingValue.equals(BACKUP_SETTING_REAL)) {
								pdroidCoreSetting = PrivacySettings.REAL;
							} else if (pdroidSettingValue.equals(BACKUP_SETTING_CUSTOM)) {
								pdroidCoreSetting = PrivacySettings.CUSTOM;
							} else if (pdroidSettingValue.equals(BACKUP_SETTING_RANDOM)) {
								pdroidCoreSetting = PrivacySettings.RANDOM;
							} else if (pdroidSettingValue.equals(BACKUP_SETTING_EMPTY)) {
								pdroidCoreSetting = PrivacySettings.EMPTY;
							} else {
								throw new RuntimeException("Unrecognised setting");
							}
							
							try {
								settingWithFunctions.getSetSettingMethod().invoke(privacySettings, pdroidCoreSetting);
							} catch (IllegalArgumentException e) {
								Log.d("PDroidAlternative","Illegal arguments when calling " + settingWithFunctions.getGetSettingMethod().getName());
								e.printStackTrace();
								throw new RuntimeException("Illegal arguments when calling " + settingWithFunctions.getGetSettingMethod().getName());
							} catch (IllegalAccessException e) {
								Log.d("PDroidAlternative","Illegal access when calling " + settingWithFunctions.getGetSettingMethod().getName());
								e.printStackTrace();
								throw new RuntimeException("Illegal access when calling " + settingWithFunctions.getGetSettingMethod().getName());
							} catch (InvocationTargetException e) {
								Log.d("PDroidAlternative","InvocationTargetException when calling " + settingWithFunctions.getGetSettingMethod().getName());
								e.printStackTrace();
								throw new RuntimeException("InvocationTargetException when calling " + settingWithFunctions.getGetSettingMethod().getName());
							}

							if (setting.hasChildNodes()) {
								NodeList customValueNodes = setting.getChildNodes();
								for (int customValueNum = 0; customValueNum < customValueNodes.getLength(); settingNum++) {

									Node customValueNode = customValueNodes.item(customValueNum);
									if (customValueNode.getNodeType() == Node.ELEMENT_NODE) {
										Element customValueElement = (Element)customValueNode;
										String customValueName = customValueElement.getNodeName();
										//have to have some tag name for the 'default' values - i.e. unnamed values.
										if (customValueName.equals(XML_DEFAULTVALUE_TAG_NAME)) {
											customValueName = "";
										}
										pdroidCoreValue = customValueElement.getTextContent();
										Method customSettingMethod = settingWithFunctions.getGetValueMethod(customValueName);

										try {
											if (customSettingMethod != null)
												if (pdroidCoreValue == null || !pdroidCoreValue.isEmpty()) {
													customSettingMethod.invoke(privacySettings, "");
												} else {
													customSettingMethod.invoke(privacySettings, pdroidCoreValue);	
												}
										} catch (IllegalArgumentException e) {
											Log.d("PDroidAlternative","Illegal arguments when calling " + customSettingMethod.getName());
											e.printStackTrace();
											throw new RuntimeException("Illegal arguments when calling " + customSettingMethod.getName());
										} catch (IllegalAccessException e) {
											Log.d("PDroidAlternative","Illegal access when calling " + customSettingMethod.getName());
											e.printStackTrace();
											throw new RuntimeException("Illegal access when calling " + customSettingMethod.getName());
										} catch (InvocationTargetException e) {
											Log.d("PDroidAlternative","InvocationTargetException when calling " + customSettingMethod.getName());
											e.printStackTrace();
											throw new RuntimeException("InvocationTargetException when calling " + customSettingMethod.getName());
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		private void loadSettingsList(SQLiteDatabase db) {			
			this.settingList = new LinkedList<PDroidSettingWithFunctions>();
			this.settingMap = new HashMap<String, PDroidSettingWithFunctions>();
			PDroidSettingWithFunctions setting = null;

			//If we do not already have handles to the relevant methods, then we need to get them
			//They are cached on the first run
			if (this.settingMap == null) {
				this.settingMap = new HashMap<String, PDroidSettingWithFunctions>();
				Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_AND_VALUE_FUNCTIONS, null);

				if (cursor != null) {
					if (cursor.getCount() < 1) {
						//This will happen if the database is not initialised for some reason
						throw new DatabaseUninitialisedException("No settings are present in the database: it must not be initialised correctly");
					}

					if (cursor.moveToFirst()) {
						int idColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_ID);
						int nameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_NAME);
						int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
						int valueFunctionNameStubColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_VALUEFUNCTIONNAMESTUB);
						int titleColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TITLE);
						int groupColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_ID);
						int groupTitleColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
						int optionsColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_OPTIONS);
						int trustedOptionColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_TRUSTED_OPTION);


						do {
							String id = cursor.getString(idColumn);
							String name = cursor.getString(nameColumn);
							String settingFunctionName = cursor.getString(settingFunctionNameColumn);
							String valueFunctionNameStub = cursor.getString(valueFunctionNameStubColumn);
							String title = cursor.getString(titleColumn);
							String group = cursor.getString(groupColumn);
							String groupTitle = cursor.getString(groupTitleColumn);
							String options = cursor.getString(optionsColumn);
							String trustedOption = cursor.getString(trustedOptionColumn);

							String [] optionsArray = null;
							if (options != null) {
								optionsArray = TextUtils.split(options, ",");
							}

							setting = new PDroidSettingWithFunctions(id, name, settingFunctionName, valueFunctionNameStub, title, group, groupTitle, optionsArray, trustedOption);
							this.settingMap.put(name, setting);
							this.settingList.add(setting);
						} while (cursor.moveToNext());
						cursor.close();
					}
				}
			}
		}
}
/*
Configurable items:
Label					Notification			Setting var name			Relevant permission
Device ID				DATA_DEVICE_ID			deviceIdSetting				android.permission.READ_PHONE_STATE
Phone Number			DATA_LINE_1_NUMBER		line1NumberSetting			android.permission.READ_PHONE_STATE
Sim Card Serial			DATA_SIM_SERIAL			simSerialNumberSetting		android.permission.READ_PHONE_STATE
Subscriber ID			DATA_SUBSCRIBER_ID		subscriberIdSetting			android.permission.READ_PHONE_STATE
Incoming Call Number	DATA_INCOMING_CALL		incomingCallsSetting		android.permission.READ_PHONE_STATE
Outgoing Call Number	DATA_OUTGOING_CALL		outgoingCallsSetting		android.permission.PROCESS_OUTGOING_CALLS 
Call Phone				DATA_PHONE_CALL			phoneCallSetting			android.permission.CALL_PHONE, android.permission.CALL_PRIVILEGED
Gps Location			DATA_LOCATION_GPS		locationGpsSetting			android.permission.ACCESS_FINE_LOCATION
Network Location		DATA_LOCATION_NETWORK	locationNetworkSetting		android.permission.ACCESS_COARSE_LOCATION, android.permission.ACCESS_FINE_LOCATION
Accounts				DATA_ACCOUNTS_LIST		accountsSetting				android.permission.ACCOUNT_MANAGER, android.permission.MANAGE_ACCOUNTS, android.permission.GET_ACCOUNTS
Account Credentials		DATA_AUTH_TOKENS		accountsAuthTokensSetting	android.permission.USE_CREDENTIALS, android.permission.ACCOUNT_MANAGER, android.permission.AUTHENTICATE_ACCOUNTS, android.permission.MANAGE_ACCOUNTS
Contacts				DATA_CONTACTS			contactsSetting				android.permission.READ_CONTACTS
Call Log				DATA_CALL_LOG			callLogSetting				android.permission.READ_CALL_LOG
Calendar				DATA_CALENDAR			calendarSetting				android.permission.READ_CALENDAR
Access Sms				DATA_SMS				smsSetting					android.permission.READ_SMS, android.permission.RECEIVE_SMS
Send Sms				DATA_SEND_SMS			smsSendSetting				android.permission.SEND_SMS
Access Mms				DATA_MMS				mmsSetting					android.permission.READ_SMS, android.permission.RECEIVE_SMS, android.permission.RECEIVE_MMS, android.permission.RECEIVE_WAP_PUSH
Send Mms				DATA_MMS_SEND			sendMmsSetting				android.permission.SEND_SMS
Record Audio			DATA_RECORD_AUDIO		recordAudioSetting			android.permission.RECORD_AUDIO
Camera					DATA_CAMERA				cameraSetting				android.permission.CAMERA
Bookmarks and History	DATA_BOOKMARKS			bookmarksSetting			com.android.browser.permission.READ_HISTORY_BOOKMARKS
System Logs				DATA_SYSTEM_LOGS		systemLogsSetting			android.permission.READ_LOGS
Wifi Info				DATA_WIFI_INFO			wifiInfoSetting				android.permission.ACCESS_WIFI_STATE
Start on Boot			DATA_INTENT_BOOT_COMPLETED intentBootCompletedSetting	android.permission.RECEIVE_BOOT_COMPLETED
Switch Network State	DATA_SWITCH_CONNECTIVITY	switchConnectivitySetting	android.permission.CHANGE_NETWORK_STATE
Switch Wifi State		DATA_SWITCH_WIFI_STATE	switchWifiStateSetting		android.permission.CHANGE_WIFI_STATE, android.permission.CHANGE_WIFI_MULTICAST_STATE
Force Online State		DATA_NETWORK_INFO_CURRENT	forceOnlineState		android.permission.ACCESS_NETWORK_STATE
Sim Info				DATA_NETWORK_INFO_SIM	simInfoSetting
Network Info			DATA_NETWORK_INFO_CURRENT	networkInfoSetting 
ICC Access				DATA_ICC_ACCESS			iccAccessSetting
IP Tables				DATA_IP_TABLES			ipTableProtectSetting
Android ID				DATA_ANDROID_ID			androidIdSetting


android.permission.READ_PHONE_STATE: Device ID, Phone Number, Sim Card Serial, Subscriber ID, Incoming Call Number
android.permission.PROCESS_OUTGOING_CALLS: Outgoing Call Number

android.permission.CALL_PHONE: Call Phone
android.permission.CALL_PRIVILEGED: Call Phone

android.permission.ACCESS_COARSE_LOCATION: Network Location
android.permission.ACCESS_FINE_LOCATION: Network Location, Gps Location

android.permission.USE_CREDENTIALS: Accounts Credentials
android.permission.ACCOUNT_MANAGER: Accounts Credentials, Accounts
android.permission.AUTHENTICATE_ACCOUNTS: Accounts Credentials
android.permission.MANAGE_ACCOUNTS: Accounts Credentials, Accounts
android.permission.GET_ACCOUNTS: Accounts

android.permission.READ_CONTACTS: Contacts, Call Log
android.permission.READ_CALL_LOG: Call Log
android.permission.READ_CALENDAR: Calendar

android.permission.READ_SMS: Access Sms, Access Mms
android.permission.RECEIVE_SMS: Access Sms, Access Mms
android.permission.RECEIVE_MMS: Access Mms
android.permission.RECEIVE_WAP_PUSH: Access Mms
android.permission.SEND_SMS: Send Sms, Send Mms

com.android.browser.permission.READ_HISTORY_BOOKMARKS: Bookmarks and History
android.permission.READ_LOGS: System Logs
android.permission.RECORD_AUDIO: Record Audio
android.permission.CAMERA: Camera
android.permission.RECEIVE_BOOT_COMPLETED: Start on Boot

android.permission.ACCESS_WIFI_STATE: Wifi Info
android.permission.CHANGE_WIFI_STATE: Switch Wifi State
android.permission.CHANGE_WIFI_MULTICAST_STATE: Switch Wifi State

android.permission.CHANGE_NETWORK_STATE: Switch Network State
android.permission.ACCESS_NETWORK_STATE: Force Online State

ALL: Sim Info, Network Info, ICC Access, IP Tables, Android ID
*/
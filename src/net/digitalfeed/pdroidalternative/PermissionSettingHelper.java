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
import java.util.HashSet;
import java.util.LinkedList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.privacy.PrivacySettings;
import android.util.Log;

class PermissionSettingHelper {

	public LinkedList<Method> permissionReadMethods = null;
	public LinkedList<Method> permissionWriteMethods = null;
	
	public PermissionSettingHelper() {};
	
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
	 * An annoying function to fish through all the settings in a 'PrivacySettings' object to check if it is 'trusted' or 'untrusted'.
	 * It would be much more helpful if there were functions in the core of PDroid for this (and I'll probably add
	 * them at some point).
	 */
	public boolean isPrivacySettingsUntrusted (SQLiteDatabase db, PrivacySettings privacySettings) {
		//If we do not already have handles to the relevant methods, then we need to get them
		//They are cached on the first run
		if (this.permissionReadMethods == null) {
			this.permissionReadMethods = new LinkedList<Method>();
			Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGSFUNCTIONNAMES, null);
	    	int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
			cursor.moveToFirst();
			do {
				String settingFunctionName = cursor.getString(settingFunctionNameColumn);
				try {
					permissionReadMethods.add(privacySettings.getClass().getMethod("get" + settingFunctionName));
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
		
		for (Method method : this.permissionReadMethods) {
			//Reflection may not be the best way of doing this, but otherwise this is going to be a great
			//big select statement, which ties us more closely to the specific set of options (which are
			//right now fairly loosely coupled - although to entirely loosely coupled)
			try {
				byte pdroidCoreSetting = (Byte)method.invoke(privacySettings);
				if (pdroidCoreSetting != PrivacySettings.REAL) return true;
			} catch (IllegalArgumentException e) {
				Log.d("PDroidAlternative","Illegal arguments when calling " + method.getName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d("PDroidAlternative","Illegal access when calling " + method.getName());
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.d("PDroidAlternative","InvocationTargetException when calling " + method.getName());
				e.printStackTrace();
			}
		}
		
		return false;
		
		/*
		
		if (privacySettings.getSwitchWifiStateSetting() == PrivacySettings.REAL &&
				//privacySettings.getForceOnlineState == PrivacySettings.REAL && //I'm leaving this one out because it doesn't really make sense to include
																 //That, and what does 'real', etc mean in that context...?
				privacySettings.getSendMmsSetting() == PrivacySettings.REAL &&
				privacySettings.getSwitchConnectivitySetting() == PrivacySettings.REAL &&
				privacySettings.getAndroidIdSetting() == PrivacySettings.REAL &&
				privacySettings.getWifiInfoSetting() == PrivacySettings.REAL &&
				privacySettings.getIpTableProtectSetting() == PrivacySettings.REAL && //I need to check what the 'real' states, etc mean for this too. Does REAL mean 'allow the app access?'
				privacySettings.getIccAccessSetting() == PrivacySettings.REAL &&
				privacySettings.getSmsSendSetting() == PrivacySettings.REAL &&
				privacySettings.getPhoneCallSetting() == PrivacySettings.REAL &&
				privacySettings.getRecordAudioSetting() == PrivacySettings.REAL &&
				privacySettings.getCameraSetting() == PrivacySettings.REAL &&
				privacySettings.getDeviceIdSetting() == PrivacySettings.REAL &&
				privacySettings.getLine1NumberSetting() == PrivacySettings.REAL &&
				privacySettings.getLocationGpsSetting() == PrivacySettings.REAL &&
				privacySettings.getLocationNetworkSetting() == PrivacySettings.REAL &&
				privacySettings.getNetworkInfoSetting() == PrivacySettings.REAL &&
				privacySettings.getSimInfoSetting() == PrivacySettings.REAL &&
				privacySettings.getSimSerialNumberSetting() == PrivacySettings.REAL &&
				privacySettings.getSubscriberIdSetting() == PrivacySettings.REAL &&
				privacySettings.getAccountsSetting() == PrivacySettings.REAL &&
				privacySettings.getAccountsAuthTokensSetting() == PrivacySettings.REAL &&
				privacySettings.getOutgoingCallsSetting() == PrivacySettings.REAL &&
				privacySettings.getIncomingCallsSetting() == PrivacySettings.REAL &&
				privacySettings.getContactsSetting() == PrivacySettings.REAL &&
				privacySettings.getCalendarSetting() == PrivacySettings.REAL &&
				privacySettings.getMmsSetting() == PrivacySettings.REAL &&
				privacySettings.getSmsSetting() == PrivacySettings.REAL &&
				privacySettings.getCallLogSetting() == PrivacySettings.REAL &&
				privacySettings.getBookmarksSetting() == PrivacySettings.REAL &&
				privacySettings.getSystemLogsSetting() == PrivacySettings.REAL &&
				privacySettings.getIntentBootCompletedSetting() == PrivacySettings.REAL
				) {
					return false;
		} else {
			return true;
		}*/
	}
	
/**
 * Updates all privacy settings on a PrivacySettings object to a new state/value. Note that this does
 * not actually save the changes back to the service - this needs to be done outside this function.
 * The functions on the PrivacySettings (accessed using reflection) are cached for multiple calls
 * to the function (i.e. if running operations on multiple apps)
 * 
 * @param db Readable database handle used to get the name of settings functions
 * @param privacySettings PrivacySettings object for the relevant application retrieved from the privacy service
 * @param newOption The new Setting.OPTION_FLAG_xxx to set (which is mapped to the PDroid core constant)
 */
	public void setPrivacySettings (SQLiteDatabase db, PrivacySettings privacySettings, int newOption) {
		//If we do not already have handles to the relevant methods, then we need to get them
		//They are cached on the first run
		if (this.permissionWriteMethods == null) {
			this.permissionWriteMethods = new LinkedList<Method>();
			Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGSFUNCTIONNAMES, null);
	    	int settingFunctionNameColumn = cursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
			cursor.moveToFirst();
			do {
				String settingFunctionName = cursor.getString(settingFunctionNameColumn);
				try {
					permissionWriteMethods.add(privacySettings.getClass().getMethod("set" + settingFunctionName, byte.class));
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
		case Setting.OPTION_FLAG_ALLOW:
		case Setting.OPTION_FLAG_YES:
			newValueByte = PrivacySettings.REAL;
			break;
		case Setting.OPTION_FLAG_CUSTOM:
		case Setting.OPTION_FLAG_CUSTOMLOCATION:
			newValueByte = PrivacySettings.CUSTOM;
			break;				
		case Setting.OPTION_FLAG_DENY:
		case Setting.OPTION_FLAG_NO:
			newValueByte = PrivacySettings.EMPTY;
			break;
		case Setting.OPTION_FLAG_RANDOM:
			newValueByte = PrivacySettings.RANDOM;
			break;
		default:
			throw new InvalidParameterException("The supplied parameter two update settings was invalid");
		}
		
		for (Method method : this.permissionWriteMethods) {
			//Reflection may not be the best way of doing this, but otherwise this is going to be a great
			//big select statement, which ties us more closely to the specific set of options (which are
			//right now fairly loosely coupled - although to entirely loosely coupled)
			try {
				method.invoke(privacySettings, newValueByte);
			} catch (IllegalArgumentException e) {
				Log.d("PDroidAlternative","Illegal arguments when calling " + method.getName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d("PDroidAlternative","Illegal access when calling " + method.getName());
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.d("PDroidAlternative","InvocationTargetException when calling " + method.getName());
				e.printStackTrace();
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
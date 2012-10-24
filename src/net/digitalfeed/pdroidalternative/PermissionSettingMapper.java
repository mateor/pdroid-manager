package net.digitalfeed.pdroidalternative;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.collect.HashMultimap;

class PermissionSettingMapper {
	private static HashMap<String, Setting> settingsCollection;
	private static HashMultimap<String, Setting> permissionToSettingsMap;
	private static HashMultimap<Setting, String> settingToPermissionsMap;
	private static HashMultimap<String, Setting> settingGroupsMap;
	private static PermissionSettingMapper theMapper;
	
	private PermissionSettingMapper(Context context) {
		settingsCollection = new HashMap<String, Setting>();
		permissionToSettingsMap = HashMultimap.create();
		settingToPermissionsMap = HashMultimap.create();
		settingGroupsMap = HashMultimap.create();
		
		Resources resources = context.getResources();
		String packageName = context.getPackageName();
		
		XmlResourceParser xrp = resources.getXml(R.xml.pdroid_settings);
		try {
			int eventType = xrp.next();
			while(!(eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) && eventType != XmlResourceParser.END_DOCUMENT) {
				eventType = xrp.next();
			}
			while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) {
	        	String name = xrp.getAttributeValue(null, "name");
	        	String id = xrp.getIdAttribute();
	        	String group = xrp.getAttributeValue(null, "group");
	        	//I wish there were a nicer way to get this string. Maybe a pair of arrays - one with identifiers, one with labels?
	        	String label = resources.getString(resources.getIdentifier("SETTING_LABEL_" + id, "string", packageName));
	        	eventType = xrp.next();
	 			while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
	 				eventType = xrp.next();
	 	 		}
		        LinkedList<String> options = new LinkedList<String>();
	        	while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("option")) {
	        		options.add(xrp.getText());
		        	eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
			        if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("option")) {
			       	eventType = xrp.next();
						while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
							eventType = xrp.next();
						}
			        } else {
			        	break;
			        }
	        	}
		        if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("setting")) {
		       	eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
		        } else {
		        	break;
		        }
				Log.d("PDroidAlternative","New Setting: id=" + id + " name=" + name + " group=" + group + " label=" + label + " options=" + TextUtils.join(",",options.toArray(new String[options.size()])));
		        Setting newSetting = new Setting(id, name, group, label, options.toArray(new String[options.size()]));
		        settingsCollection.put(id, newSetting);
		        settingGroupsMap.put(group, newSetting);
	       }
	       
			xrp = resources.getXml(R.xml.permission_setting_map);
			eventType = xrp.next();
			while(!(eventType == XmlResourceParser.START_TAG && xrp.getName().equals("permission")) && eventType != XmlResourceParser.END_DOCUMENT) {
				eventType = xrp.next();
			}
			while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("permission")) {
				String id = xrp.getIdAttribute();
				Log.d("PDroidAlternative","ID:" + id);

				eventType = xrp.next();
				while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
					eventType = xrp.next();
				}
				while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) {
					Log.d("PDroidAlternative","permission ID:" + id + " setting ID: " + xrp.getIdAttribute());
					permissionToSettingsMap.put(id, settingsCollection.get(xrp.getIdAttribute()));
					eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
			        if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("setting")) {
			        	eventType = xrp.next();
						while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
							eventType = xrp.next();
						}
			        } else {
			        	break;
			        }
				}
				if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("setting")) {
					eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
				} else {
					break;
				}
			}
		} catch (Exception e) {
			Log.d("PDroidAlternative",e.getMessage());
			//TODO: Exception handling, mayhaps?
		}
	}
	
	public static PermissionSettingMapper getMapper(Context context) {
		if (theMapper == null) {
			theMapper = new PermissionSettingMapper(context);
		}
		
		return theMapper;
	}
	
	public Set<Setting> getSettings(String permission) {	
		return permissionToSettingsMap.get(permission);
	}
	
	public Set<Setting> getSettings(String [] permissions) {
		HashSet<Setting> permissionSet = new HashSet<Setting>();
		for (String permission : permissions) {
			for (Setting setting : getSettings(permission)) {
				permissionSet.add(setting);
			}
		}
		return permissionSet;
	}
	
	public Set<String> getPermissions(Setting setting) {	
		return settingToPermissionsMap.get(setting);
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
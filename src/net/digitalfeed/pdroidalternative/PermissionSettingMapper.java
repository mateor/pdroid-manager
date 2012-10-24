package net.digitalfeed.pdroidalternative;

import java.util.HashMap;
import java.util.HashSet;

public class PermissionSettingMapper {
	private static HashMap<String, String[]> permissionToSettingsMap;
	private static HashMap<String, String[]> settingToPermissionsMap;
	
	
	public static String[] getSettings(String permission) {	
		if (permissionToSettingsMap == null) {
			buildMaps();
		}
		return permissionToSettingsMap.get(permission);
	}
	
	public static Setting[] getSettingsObjects(String [] permissions) {
		if (permissions == null) {
			return null;
		}
		
		if (permissionToSettingsMap == null) {
			buildMaps();
		}
		
		HashSet<Setting> settingsSet = new HashSet<Setting>();

		if (permissions != null) {
			for (String permission : permissions) {
				String [] settingsForPermission = PermissionSettingMapper.getSettings(permission);
				if (settingsForPermission != null) {
					for (String setting : settingsForPermission) {
						settingsSet.add(new Setting(setting));
					}
				}
			}
		}
		return settingsSet.toArray(new Setting[settingsSet.size()]);	
	}
	
	public static String[] getPermissions(String setting) {	
		if (settingToPermissionsMap == null) {
			buildMaps();
		}
		return settingToPermissionsMap.get(setting);
	}

	private static void buildMaps() {
		permissionToSettingsMap = new HashMap<String, String[]>();
		settingToPermissionsMap = new HashMap<String, String[]>();
		
		settingToPermissionsMap.put(Setting.ALLOW_READ_DEVICE_ID, new String[]{"android.permission.READ_PHONE_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_PHONE_NUMBER, new String[]{"android.permission.READ_PHONE_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_SIM_SERIAL, new String[]{"android.permission.READ_PHONE_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_SUBSCRIBER_ID, new String[]{"android.permission.READ_PHONE_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_INCOMING_CALL_NUMBER, new String[]{"android.permission.READ_PHONE_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_OUTGOING_CALL_NUMBER, new String[]{"android.permission.PROCESS_OUTGOING_CALLS"});
		settingToPermissionsMap.put(Setting.ALLOW_INITIATE_PHONE_CALL, new String[]{"android.permission.CALL_PHONE", "android.permission.CALL_PRIVILEGED"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_NETWORK_LOCATION, new String[]{"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_GPS_LOCATION, new String[]{"android.permission.ACCESS_FINE_LOCATION"});
		settingToPermissionsMap.put(Setting.ALLOW_USE_ACCOUNT_CREDENTIALS, new String[]{"android.permission.USE_CREDENTIALS","android.permission.ACCOUNT_MANAGER","android.permission.AUTHENTICATE_ACCOUNTS","android.permission.MANAGE_ACCOUNTS"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_ACCOUNTS_LIST, new String[]{"android.permission.ACCOUNT_MANAGER","android.permission.MANAGE_ACCOUNTS","android.permission.GET_ACCOUNTS"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_CONTACTS, new String[]{"android.permission.READ_CONTACTS"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_CALL_LOG, new String[]{"android.permission.READ_CONTACTS","android.permission.READ_CALL_LOG"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_CALENDAR, new String[]{"android.permission.READ_CALENDAR"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_SMS, new String[]{"android.permission.READ_SMS","android.permission.RECEIVE_SMS"});
		settingToPermissionsMap.put(Setting.ALLOW_SEND_SMS, new String[]{"android.permission.SEND_SMS"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_MMS, new String[]{"android.permission.READ_SMS","android.permission.RECEIVE_SMS","android.permission.RECEIVE_MMS","android.permission.RECEIVE_WAP_PUSH"});
		settingToPermissionsMap.put(Setting.ALLOW_SEND_MMS, new String[]{"android.permission.SEND_SMS"});
		settingToPermissionsMap.put(Setting.ALLOW_RECORD_AUDIO, new String[]{"android.permission.RECORD_AUDIO"});
		settingToPermissionsMap.put(Setting.ALLOW_CAMERA, new String[]{"android.permission.CAMERA"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_BOOKMARKS_AND_HISTORY, new String[]{"com.android.browser.permission.READ_HISTORY_BOOKMARKS"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_SYSTEM_LOGS, new String[]{"android.permission.READ_LOGS"});
		settingToPermissionsMap.put(Setting.ALLOW_RECEIVE_BOOT_COMPLETED_INTENT, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_WIFI_INFO, new String[]{"android.permission.ACCESS_WIFI_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_CHANGE_WIFI_STATE, new String[]{"android.permission.CHANGE_WIFI_STATE","android.permission.CHANGE_WIFI_MULTICAST_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_CHANGE_MOBILE_DATA_STATE, new String[]{"android.permission.CHANGE_NETWORK_STATE"});
		settingToPermissionsMap.put(Setting.ALLOW_READ_NETWORK_INFO, null);
		settingToPermissionsMap.put(Setting.ALLOW_READ_ANDROID_ID, null);
		settingToPermissionsMap.put(Setting.ALLOW_READ_SIM_INFO, null);
		settingToPermissionsMap.put(Setting.ALLOW_ACCESS_IP_TABLES, null);
		settingToPermissionsMap.put(Setting.ALLOW_READ_ICC_ID, null);
		settingToPermissionsMap.put(Setting.FAKE_ALWAYS_ONLINE, new String[]{"android.permission.ACCESS_NETWORK_STATE"});
		
		permissionToSettingsMap.put("android.permission.READ_PHONE_STATE", new String[]{Setting.ALLOW_READ_DEVICE_ID,
				Setting.ALLOW_READ_PHONE_NUMBER,
				Setting.ALLOW_READ_SIM_SERIAL,
				Setting.ALLOW_READ_SUBSCRIBER_ID,
				Setting.ALLOW_READ_INCOMING_CALL_NUMBER});
		permissionToSettingsMap.put("android.permission.PROCESS_OUTGOING_CALLS", new String[]{Setting.ALLOW_READ_OUTGOING_CALL_NUMBER});
		permissionToSettingsMap.put("android.permission.CALL_PHONE", new String[]{Setting.ALLOW_INITIATE_PHONE_CALL});
		permissionToSettingsMap.put("android.permission.CALL_PRIVILEGED", new String[]{Setting.ALLOW_INITIATE_PHONE_CALL});
		permissionToSettingsMap.put("android.permission.ACCESS_COARSE_LOCATION", new String[]{Setting.ALLOW_READ_NETWORK_LOCATION});
		permissionToSettingsMap.put("android.permission.ACCESS_FINE_LOCATION", new String[]{Setting.ALLOW_READ_NETWORK_LOCATION, Setting.ALLOW_READ_GPS_LOCATION});
		permissionToSettingsMap.put("android.permission.USE_CREDENTIALS", new String[]{Setting.ALLOW_USE_ACCOUNT_CREDENTIALS});
		permissionToSettingsMap.put("android.permission.ACCOUNT_MANAGER", new String[]{Setting.ALLOW_USE_ACCOUNT_CREDENTIALS, Setting.ALLOW_READ_ACCOUNTS_LIST});
		permissionToSettingsMap.put("android.permission.AUTHENTICATE_ACCOUNTS", new String[]{Setting.ALLOW_USE_ACCOUNT_CREDENTIALS});
		permissionToSettingsMap.put("android.permission.MANAGE_ACCOUNTS", new String[]{Setting.ALLOW_USE_ACCOUNT_CREDENTIALS, Setting.ALLOW_READ_ACCOUNTS_LIST});
		permissionToSettingsMap.put("android.permission.GET_ACCOUNTS", new String[]{Setting.ALLOW_READ_ACCOUNTS_LIST});
		permissionToSettingsMap.put("android.permission.READ_CONTACTS", new String[]{Setting.ALLOW_READ_CONTACTS, Setting.ALLOW_READ_CALL_LOG});
		permissionToSettingsMap.put("android.permission.READ_CALL_LOG", new String[]{Setting.ALLOW_READ_CALL_LOG});
		permissionToSettingsMap.put("android.permission.READ_CALENDAR", new String[]{Setting.ALLOW_READ_CALENDAR});
		permissionToSettingsMap.put("android.permission.READ_SMS", new String[]{Setting.ALLOW_READ_SMS, Setting.ALLOW_READ_MMS});
		permissionToSettingsMap.put("android.permission.RECEIVE_SMS", new String[]{Setting.ALLOW_READ_SMS, Setting.ALLOW_READ_MMS});
		permissionToSettingsMap.put("android.permission.RECEIVE_MMS", new String[]{Setting.ALLOW_READ_MMS});
		permissionToSettingsMap.put("android.permission.RECEIVE_WAP_PUSH", new String[]{Setting.ALLOW_READ_MMS});
		permissionToSettingsMap.put("android.permission.SEND_SMS", new String[]{Setting.ALLOW_SEND_SMS, Setting.ALLOW_SEND_MMS});
		permissionToSettingsMap.put("android.permission.RECORD_AUDIO", new String[]{Setting.ALLOW_RECORD_AUDIO});
		permissionToSettingsMap.put("android.permission.CAMERA", new String[]{Setting.ALLOW_CAMERA});
		permissionToSettingsMap.put("com.android.browser.permission.READ_HISTORY_BOOKMARKS", new String[]{Setting.ALLOW_READ_BOOKMARKS_AND_HISTORY});
		permissionToSettingsMap.put("android.permission.READ_LOGS", new String[]{Setting.ALLOW_READ_SYSTEM_LOGS});
		permissionToSettingsMap.put("android.permission.RECEIVE_BOOT_COMPLETED", new String[]{Setting.ALLOW_RECEIVE_BOOT_COMPLETED_INTENT});
		permissionToSettingsMap.put("android.permission.ACCESS_WIFI_STATE", new String[]{Setting.ALLOW_READ_WIFI_INFO});
		permissionToSettingsMap.put("android.permission.CHANGE_WIFI_STATE", new String[]{Setting.ALLOW_CHANGE_WIFI_STATE});
		permissionToSettingsMap.put("android.permission.CHANGE_WIFI_MULTICAST_STATE", new String[]{Setting.ALLOW_CHANGE_WIFI_STATE});
		permissionToSettingsMap.put("android.permission.CHANGE_NETWORK_STATE", new String[]{Setting.ALLOW_CHANGE_MOBILE_DATA_STATE});
		permissionToSettingsMap.put(null, new String[]{Setting.ALLOW_READ_NETWORK_INFO, Setting.ALLOW_READ_ANDROID_ID, Setting.ALLOW_READ_SIM_INFO, Setting.ALLOW_ACCESS_IP_TABLES, Setting.ALLOW_READ_ICC_ID});
		permissionToSettingsMap.put("android.permission.ACCESS_NETWORK_STATE", new String[]{Setting.FAKE_ALWAYS_ONLINE});
	}
	
	private PermissionSettingMapper() {
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
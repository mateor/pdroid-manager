pdroid-manager  
==============  
  
A replacement for the PDroid 2.0 application by CollegeDev (used to configure the updated PDroid service).   
  
NOTE: THIS SOFTWARE IS PRERELEASE - IT IS CURRENTLY KNOWN TO HAVE BUGS (which you are welcome to fix).  
Because of the signature protection used in other PDroid applications (e.g. PDroid 2.0 by CollegeDev), it is necessary to uninstall these other apps prior to installing PDroid Manager. I don't like it this way, but right now there is no straightforward and safe way to get around it.  
  
Unless explicitly noted in the file headers, the following license applies:  
Copyright (C) 2012 Simeon J. Morgan (smorgan@digitalfeed.net)  
This program is free software; you can redistribute it and/or modify it under  
the terms of the GNU General Public License as published by the Free Software  
Foundation; either version 3 of the License, or (at your option) any later version.  
This program is distributed in the hope that it will be useful, but WITHOUT ANY  
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A  
PARTICULAR PURPOSE. See the GNU General Public License for more details.  
You should have received a copy of the GNU General Public License along with  
this program; if not, see <http://www.gnu.org/licenses>.  
The software has the following requirements (GNU GPL version 3 section 7):  
You must retain in pdroid-manager, any modifications or derivatives of  
pdroid-manager, or any code or components taken from pdroid-manager the author  
attribution included in the files.  
In pdroid-manager, any modifications or derivatives of pdroid-manager, or any  
application utilizing code or components taken from pdroid-manager must include  
in any display or listing of its creators, authors, contributors or developers  
the names or pseudonyms included in the author attributions of pdroid-manager  
or pdroid-manager derived code.  
Modified or derivative versions of the pdroid-manager application must use an  
alternative name, rather than the name pdroid-manager.  
  
Contributions:
Beasty (http://forum.xda-developers.com/member.php?u=276326): Russian translation
appelsson (https://github.com/appelsson): Hungarian Translation    
patrickpr (https://github.com/patrickpr): French translation    
TamCore (https://github.com/TamCore): German translation, Android.mk, formatting of the tables in this file    
wbedard: Help text in original English    
 

Permissions are linked to one or more 'settings', which determine which operations may be performed (e.g. what function calls will return what data)  
The current list of permissions, settings, and affected functions are:  
<table>
<tr>
	<th>
		Label
	</th>
	<th>
		Notification
	</th>
	<th>
		Setting var name
	</th>
	<th>
		Relevant permission
	</th>
</tr>
<tr>
	<td>
		Device ID
	</td>
	<td>
		DATA_DEVICE_ID
	</td>
	<td>
		deviceIdSetting
	</td>
	<td>
		android.permission.READ_PHONE_STATE
	</td>
</tr>
<tr>
	<td>
		Phone Number
	</td>
	<td>
		DATA_LINE_1_NUMBER
	</td>
	<td>
		line1NumberSetting
	</td>
	<td>
		android.permission.READ_PHONE_STATE
	</td>
</tr>
<tr>
	<td>
		Sim Card Serial
	</td>
	<td>
		DATA_SIM_SERIAL
	</td>
	<td>
		simSerialNumberSetting
	</td>
	<td>
		android.permission.READ_PHONE_STATE
	</td>
</tr>
<tr>
	<td>
		Subscriber ID
	</td>
	<td>
		DATA_SUBSCRIBER_ID
	</td>
	<td>
		subscriberIdSetting
	</td>
	<td>
		android.permission.READ_PHONE_STATE
	</td>
</tr>
<tr>
	<td>
		Incoming Call Number
	</td>
	<td>
		DATA_INCOMING_CALL
	</td>
	<td>
		incomingCallsSetting
	</td>
	<td>
		android.permission.READ_PHONE_STATE
	</td>
</tr>
<tr>
	<td>
		Outgoing Call Number
	</td>
	<td>
		DATA_OUTGOING_CALL
	</td>
	<td>
		outgoingCallsSetting
	</td>
	<td>
		android.permission.PROCESS_OUTGOING_CALLS
	</td>
</tr>
<tr>
	<td>
		Call Phone
	</td>
	<td>
		DATA_PHONE_CALL
	</td>
	<td>
	    phoneCallSetting
	</td>
	<td>
		android.permission.CALL_PHONE, android.permission.CALL_PRIVILEGED	
	</td>
</tr>
<tr>
	<td>
		Gps Location
	</td>
	<td>
		DATA_LOCATION_GPS
	</td>
	<td>
		locationGpsSetting
	</td>
	<td>
		android.permission.ACCESS_FINE_LOCATION
	</td>
</tr>
<tr>
	<td>
		Network Location
	</td>
	<td>
		DATA_LOCATION_NETWORK
	</td>
	<td>
		locationNetworkSetting
	</td>
	<td>
		android.permission.ACCESS_COARSE_LOCATION, android.permission.ACCESS_FINE_LOCATION
	</td>
</tr>
<tr>
	<td>
		Accounts
	</td>
	<td>
		DATA_ACCOUNTS_LIST
	</td>
	<td>
		accountsSetting
	</td>
	<td>
		android.permission.ACCOUNT_MANAGER, android.permission.MANAGE_ACCOUNTS, android.permission.GET_ACCOUNTS
	</td>
</tr>
<tr>
	<td>
		Account Credentials
	</td>
	<td>
		DATA_AUTH_TOKENS
	</td>
	<td>
		accountsAuthTokensSetting
	</td>
	<td>
		android.permission.USE_CREDENTIALS, android.permission.ACCOUNT_MANAGER, android.permission.AUTHENTICATE_ACCOUNTS, android.permission.MANAGE_ACCOUNTS
	</td>
</tr>
<tr>
	<td>
		Contacts
	</td>
	<td>
		DATA_CONTACTS
	</td>
	<td>
		contactsSetting
	</td>
	<td>
		android.permission.READ_CONTACTS
	</td>
</tr>
<tr>
	<td>
		Call Log
	</td>
	<td>
		DATA_CALL_LOG
	</td>
	<td>
		callLogSetting
	</td>
	<td>
		android.permission.READ_CALL_LOG
	</td>
</tr>
<tr>
	<td>
		Calendar
	</td>
	<td>
		DATA_CALENDAR
	</td>
	<td>
		calendarSetting
	</td>
	<td>
		android.permission.READ_CALENDAR
	</td>
</tr>
<tr>
	<td>
		Access Sms
	</td>
	<td>
		DATA_SMS
	</td>
	<td>
		smsSetting
	</td>
	<td>
		android.permission.READ_SMS, android.permission.RECEIVE_SMS
	</td>
</tr>
<tr>
	<td>
		Send Sms
	</td>
	<td>
		DATA_SEND_SMS
	</td>
	<td>
		smsSendSetting
	</td>
	<td>
		android.permission.SEND_SMS
	</td>
</tr>
<tr>
	<td>
		Access Mms
	</td>
	<td>
		DATA_MMS
	</td>
	<td>
		mmsSetting
	</td>
	<td>
		android.permission.READ_SMS, android.permission.RECEIVE_SMS, android.permission.RECEIVE_MMS, android.permission.RECEIVE_WAP_PUSH
	</td>
</tr>
<tr>
	<td>
		Send Mms
	</td>
	<td>
		DATA_MMS_SEND
	</td>
	<td>
		sendMmsSetting
	</td>
	<td>
		android.permission.SEND_SMS
	</td>
</tr>
<tr>
	<td>
		Record Audio
	</td>
	<td>
		DATA_RECORD_AUDIO
	</td>
	<td>
		recordAudioSetting
	</td>
	<td>
		android.permission.RECORD_AUDIO
	</td>
</tr>
<tr>
	<td>
		Camera
	</td>
	<td>
		DATA_CAMERA
	</td>
	<td>
		cameraSetting
	</td>
	<td>
		android.permission.CAMERA
	</td>
</tr>
<tr>
	<td>
		Bookmarks and History
	</td>
	<td>
		DATA_BOOKMARKS
	</td>
	<td>
		bookmarksSetting
	</td>
	<td>
		com.android.browser.permission.READ_HISTORY_BOOKMARKS
	</td>
</tr>
<tr>
	<td>
		System Logs
	</td>
	<td>
		DATA_SYSTEM_LOGS
	</td>
	<td>
		systemLogsSetting
	</td>
	<td>
		android.permission.READ_LOGS
	</td>
</tr>
<tr>
	<td>
		Wifi Info
	</td>
	<td>
		DATA_WIFI_INFO
	</td>
	<td>
		wifiInfoSetting
	</td>
	<td>
		android.permission.ACCESS_WIFI_STATE
	</td>
</tr>
<tr>
	<td>
		Start on Boot
	</td>
	<td>
		DATA_INTENT_BOOT_COMPLETED
	</td>
	<td>
		intentBootCompletedSetting
	</td>
	<td>
		android.permission.RECEIVE_BOOT_COMPLETED
	</td>
</tr>
<tr>
	<td>
		Switch Network State
	</td>
	<td>
		DATA_SWITCH_CONNECTIVITY
	</td>
	<td>
		switchConnectivitySetting
	</td>
	<td>
		android.permission.CHANGE_NETWORK_STATE
	</td>
</tr>
<tr>
	<td>
		Switch Wifi State
	</td>
	<td>
		DATA_SWITCH_WIFI_STATE
	</td>
	<td>
		switchWifiStateSetting
	</td>
	<td>
		android.permission.CHANGE_WIFI_STATE, android.permission.CHANGE_WIFI_MULTICAST_STATE
	</td>
</tr>
<tr>
	<td>
		Force Online State
	</td>
	<td>
		DATA_NETWORK_INFO_CURRENT
	</td>
	<td>
		forceOnlineState
	</td>
	<td>
		android.permission.ACCESS_NETWORK_STATE
	</td>
</tr>
<tr>
	<td>
		Sim Info
	</td>
	<td>
		DATA_NETWORK_INFO_SIM
	</td>
	<td>
		simInfoSetting
	</td>
</tr>
<tr>
	<td>
		Network Info
	</td>
	<td>
		DATA_NETWORK_INFO_CURRENT
	</td>
	<td>
		networkInfoSetting
	</td>
</tr>
<tr>
	<td>
		ICC Access
	</td>
	<td>
		DATA_ICC_ACCESS
	</td>
	<td>
		iccAccessSetting
	</td>
</tr>
<tr>
	<td>
		IP Tables
	</td>
	<td>
		DATA_IP_TABLES
	</td>
	<td>
		ipTableProtectSetting
	</td>
</tr>
<tr>
	<td>
		Android ID
	</td>
	<td>
		DATA_ANDROID_ID
	</td>
	<td>
		androidIdSetting
	</td>
</tr>
</table>

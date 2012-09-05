pdroid-manager
==============

A replacement for the original PDroid application by Syvat (used to configure the privacy settings used by the Privacy Settings Manager) 


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


Permissions are linked to one or more 'settings', which determine which operations may be performed (e.g. what function calls will return what data)
The current list of permissions, settings, and affected functions are:
android.permission.ACCESS_COARSE_LOCATION
	getLocationNetworkSetting

android.permission.ACCESS_FINE_LOCATION"
	getLocationGpsSetting

android.permission.GET_ACCOUNTS
	getAccountsSetting
		getAccounts
		getAccountsByType
		hasFeatures
		getAccountsByTypeAndFeatures

android.permission.USE_CREDENTIALS
	getAccountsAuthTokensSetting
		blockingGetAuthToken

android.permission.PROCESS_OUTGOING_CALLS
	getOutgoingCallsSetting

android.permission.READ_CALENDAR
	getCalendarSetting

android.permission.READ_CONTACTS
	getContactsSetting
	getCallLogSetting

android.permission.READ_LOGS
	getSystemLogsSetting

android.permission.READ_PHONE_STATE
	getDeviceIdSetting
	getLine1NumberSetting
	getSimSerialNumberSetting
	getSubscriberIdSetting
	getIncomingCallsSetting

android.permission.READ_SMS
	getSmsSetting

android.permission.RECEIVE_SMS
	getSmsSetting

android.permission.RECEIVE_MMS
	getMmsSetting

android.permission.RECEIVE_WAP_PUSH
	getMmsSetting

com.android.browser.permission.READ_HISTORY_BOOKMARKS
	getBookmarksSetting
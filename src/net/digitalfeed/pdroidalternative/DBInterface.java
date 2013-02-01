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

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

/**
 * Holds all the individual table-related classes for the database (which then hold
 * all the column names, create statements, etc). Poorly implements a singleton pattern,
 * but in order to have initialise-on-demand, the static vars for the singletons are not
 * final. The Initialization-on-demand (https://secure.wikimedia.org/wikipedia/en/wiki/Initialization_on_demand_holder_idiom) 
 * may be better, but the context does need to be stored at some point to allow the database
 * connection to be made.
 * I'm a bit concerned about threading here,
 *  
 * @author smorgan
 */
public class DBInterface {
	private static DBHelper dbhelper = null;
	private static DBInterface dbinterface = null;
	private static final Object lockObject = new Object();
	
	public static final class ApplicationTable {
		public ApplicationTable(){}
		
		public static final int COMPRESS_ICON_QUALITY = 100;
		
		public static final String TABLE_NAME = "application";
		public static final String COLUMN_NAME_ROWID = "_id";
		public static final String COLUMN_NAME_LABEL = "label";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_UID = "uid";
		public static final String COLUMN_NAME_VERSIONCODE = "versionCode";
		public static final String COLUMN_NAME_PERMISSIONS = "permissions";
		public static final String COLUMN_NAME_ICON = "icon";
		public static final String COLUMN_NAME_FLAGS = "appFlags";
		
		public static final int FLAG_IS_SYSTEM_APP = 1;
		public static final int FLAG_HAS_INTERNET_ACCESS = 2;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_LABEL + " TEXT NOT NULL COLLATE NOCASE, " +
				COLUMN_NAME_PACKAGENAME + " TEXT NOT NULL, " + 
				COLUMN_NAME_UID + " INTEGER NOT NULL, " +
				COLUMN_NAME_VERSIONCODE + " INTEGER NOT NULL, " +
				COLUMN_NAME_PERMISSIONS + " TEXT, " + 
				COLUMN_NAME_ICON + " BLOB, " +
				COLUMN_NAME_FLAGS + "  INTEGER NOT NULL" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		public static final String WHERE_CLAUSE_PACKAGENAME = TABLE_NAME + "." + COLUMN_NAME_PACKAGENAME + " = ?";
		
		/*
		 * I realised after starting to move things like this that this may not actually
		 * be the right place for them, because they are query-bound rather than
		 * table-bound. Food for thought.
		 */
		protected static final int COLUMN_NUMBER_OFFSET_LABEL = 0;
		protected static final int COLUMN_NUMBER_OFFSET_PACKAGENAME = 1;
		protected static final int COLUMN_NUMBER_OFFSET_UID = 2;
		protected static final int COLUMN_NUMBER_OFFSET_VERSIONCODE = 3;
		protected static final int COLUMN_NUMBER_OFFSET_PERMISSIONS = 4;
		protected static final int COLUMN_NUMBER_OFFSET_ICON = 5;
		protected static final int COLUMN_NUMBER_OFFSET_APPFLAGS = 6;
		
		public static final ContentValues getContentValues(Application application) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_LABEL, application.getLabel());
			contentValues.put(COLUMN_NAME_PACKAGENAME, application.getPackageName());
			contentValues.put(COLUMN_NAME_UID, application.getUid());
			contentValues.put(COLUMN_NAME_VERSIONCODE, application.getVersionCode());
			contentValues.put(COLUMN_NAME_FLAGS, application.getAppFlags());
			contentValues.put(COLUMN_NAME_ICON, application.getIconByteArray());
			
			String[] permissions = application.getPermissions();
			if (permissions != null) {
				contentValues.put(COLUMN_NAME_PERMISSIONS, TextUtils.join(",", application.getPermissions()));
			}
			return contentValues;
		}
		
		public static final ContentValues getUpdateContentValues(Application application) {
			ContentValues contentValues = new ContentValues();
			
			int updatedFlags = application.getUpdatedFlags();
			
			if (0 != (updatedFlags & Application.FLAG_APPLICATION_VALUES_CHANGED)) {
				contentValues.put(COLUMN_NAME_LABEL, application.getLabel());
				contentValues.put(COLUMN_NAME_PACKAGENAME, application.getPackageName());
				contentValues.put(COLUMN_NAME_UID, application.getUid());
				contentValues.put(COLUMN_NAME_VERSIONCODE, application.getVersionCode());
				contentValues.put(COLUMN_NAME_FLAGS, application.getAppFlags());
			}
			
			if (0 != (updatedFlags & Application.FLAG_ICON_CHANGED)) {
				contentValues.put(COLUMN_NAME_ICON, application.getIconByteArray());
			}
			
			if (0 != (updatedFlags & Application.FLAG_PERMISSIONS_CHANGED)) {
				String[] permissions = application.getPermissions();
				if (permissions != null) {
					contentValues.put(COLUMN_NAME_PERMISSIONS, TextUtils.join(",", application.getPermissions()));
				}
			}
			
			return contentValues;
		}
	}
	
	public static final class ApplicationStatusTable {
		public ApplicationStatusTable(){}
		public static final String TABLE_NAME = "application_status";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_FLAGS = "statusFlags";

		public static final int FLAG_IS_UNTRUSTED = Application.STATUS_FLAG_IS_UNTRUSTED;
		public static final int FLAG_NOTIFY_ON_ACCESS = Application.STATUS_FLAG_NOTIFY_ON_ACCESS;
		public static final int FLAG_HAS_PRIVACYSETTINGS = Application.STATUS_FLAG_HAS_PRIVACYSETTINGS;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				COLUMN_NAME_PACKAGENAME + " TEXT, " + 
				COLUMN_NAME_FLAGS + " INTEGER, " + 
				"FOREIGN KEY(" + COLUMN_NAME_PACKAGENAME + ") REFERENCES " + ApplicationTable.TABLE_NAME + "(packageName)" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
	
		public static final String WHERE_CLAUSE_PACKAGENAME = TABLE_NAME + "." + COLUMN_NAME_PACKAGENAME + " = ?";
		
		public static final ContentValues getContentValues(Application application) {
			ContentValues contentValues = new ContentValues();
						contentValues.put(COLUMN_NAME_PACKAGENAME, application.getPackageName());
			contentValues.put(COLUMN_NAME_FLAGS, application.getStatusFlags());
			return contentValues;
		}
	}
	
	public static final class ApplicationLogTable {
		public ApplicationLogTable(){}
		public static final String TABLE_NAME = "application_log";
		public static final String COLUMN_NAME_DATETIME = "datetime";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_UID = "uid";
		public static final String COLUMN_NAME_VERSIONCODE = "versionCode";
		public static final String COLUMN_NAME_OPERATION = "operation";
		public static final String COLUMN_NAME_FLAGS = "logFlags";
		
		public static final int FLAGS_ALLOWED = 1;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_DATETIME + " REAL, " + 
				COLUMN_NAME_PACKAGENAME + " TEXT, " + 
				COLUMN_NAME_UID + " INTEGER, " +
				COLUMN_NAME_VERSIONCODE + " INTEGER, " +
				COLUMN_NAME_OPERATION + " TEXT, " +
				COLUMN_NAME_FLAGS + " INTEGER" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		public static final ContentValues getContentValues(String packageName, int uid, int versionCode,
				String operation) {
			return getContentValues(packageName, uid, versionCode, operation, 0);	
		}
		
		public static final ContentValues getContentValues(long eventTimestamp, String packageName, int uid, int versionCode,
				String operation, int flags) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_DATETIME, eventTimestamp);
			contentValues.put(COLUMN_NAME_PACKAGENAME, packageName);
			contentValues.put(COLUMN_NAME_UID, uid);
			contentValues.put(COLUMN_NAME_VERSIONCODE, versionCode);
			contentValues.put(COLUMN_NAME_OPERATION, operation);
			contentValues.put(COLUMN_NAME_FLAGS, flags);
			return contentValues;
		}
		
		public static final ContentValues getContentValues(String packageName, int uid, int versionCode,
				String operation, int flags) {
			return getContentValues(System.currentTimeMillis(), packageName, uid, versionCode, operation, flags);
		}
	}
	
	public static final class SettingTable {
		public SettingTable(){}
		public static final String TABLE_NAME = "setting";
		public static final String COLUMN_NAME_ID = "id";
		public static final String COLUMN_NAME_NAME = "name";
		public static final String COLUMN_NAME_SETTINGFUNCTIONNAME = "settingfunctionname"; //this is the name of function call to the 'privacy' service used to access the current 'selected option' for the setting
		public static final String COLUMN_NAME_VALUEFUNCTIONNAMESTUB = "valuefunctionnamestub"; //Used to write a custom value to the PDroid core when possible
		public static final String COLUMN_NAME_TITLE = "title"; //Used to store the 'friendly' title of the setting, which may be language specific.
																//If we start adding support for multiple languages, possibly we should be handling this better; maybe having another table with all the language text in it?
																//The point of this is to avoid using reflection to get the titles from resources all the time
		public static final String COLUMN_NAME_GROUP_ID = "groupId"; //Stored as a string, but maybe better in another table and linked?
		public static final String COLUMN_NAME_GROUP_TITLE = "groupTitle"; //As with the above 'group' column, may be better in a separate column, but then we need to be doing joins.
		public static final String COLUMN_NAME_OPTIONS = "options"; //Options are stored as a string array
		public static final String COLUMN_NAME_TRUSTED_OPTION = "trustedOption"; //Options which qualify as 'trusted' are stored as string array
		public static final String COLUMN_NAME_SORT = "sort"; //A number which indicates the order in which the options should be sorted
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_ID + " TEXT NOT NULL, " + 
				COLUMN_NAME_NAME + " TEXT NOT NULL, " +
				COLUMN_NAME_SETTINGFUNCTIONNAME + " TEXT NOT NULL, " +
				COLUMN_NAME_VALUEFUNCTIONNAMESTUB + " TEXT, " +
				COLUMN_NAME_TITLE + " TEXT, " +
				COLUMN_NAME_GROUP_ID + " TEXT NOT NULL, " +
				COLUMN_NAME_GROUP_TITLE + " TEXT, " +
				COLUMN_NAME_OPTIONS + " TEXT NOT NULL, " +
				COLUMN_NAME_TRUSTED_OPTION + " TEXT NOT NULL, " +
				COLUMN_NAME_SORT + " INTEGER " +
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		private static final int COLUMN_NUMBER_OFFSET_ID = 0;
		private static final int COLUMN_NUMBER_OFFSET_NAME = 1;
		private static final int COLUMN_NUMBER_OFFSET_SETTINGFUNCTIONNAME = 2;
		private static final int COLUMN_NUMBER_OFFSET_VALUEFUNCTIONNAMESTUB = 3;
		private static final int COLUMN_NUMBER_OFFSET_TITLE = 4;
		private static final int COLUMN_NUMBER_OFFSET_GROUP_ID = 5;
		private static final int COLUMN_NUMBER_OFFSET_GROUP_TITLE = 6;
		private static final int COLUMN_NUMBER_OFFSET_OPTIONS = 7;
		private static final int COLUMN_NUMBER_OFFSET_TRUSTED_OPTION = 8;
		private static final int COLUMN_NUMBER_OFFSET_SORT = 9;
		private static final int COLUMN_COUNT = 10;
		
		public static final ContentValues getContentValues(PDroidSetting setting) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_ID, setting.getId());
			contentValues.put(COLUMN_NAME_NAME, setting.getName());
			contentValues.put(COLUMN_NAME_SETTINGFUNCTIONNAME, setting.getSettingFunctionName());
			contentValues.put(COLUMN_NAME_VALUEFUNCTIONNAMESTUB, setting.getValueFunctionNameStub());
			//contentValues.put(COLUMN_NAME_TITLE, setting.getTitle());
			contentValues.put(COLUMN_NAME_GROUP_ID, setting.getGroup());
			//contentValues.put(COLUMN_NAME_GROUP_TITLE, setting.getGroupTitle());
			contentValues.put(COLUMN_NAME_OPTIONS, TextUtils.join(",",setting.getOptions()));
			contentValues.put(COLUMN_NAME_TRUSTED_OPTION, setting.getTrustedOption());
			contentValues.put(COLUMN_NAME_SORT, setting.getSort());
			
			return contentValues;
		}
	}

	public static final class PermissionSettingTable {
		public PermissionSettingTable(){}
		public static final String TABLE_NAME = "permission_setting";
		public static final String COLUMN_NAME_PERMISSION = "permission";
		public static final String COLUMN_NAME_SETTING = "setting";
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_PERMISSION + " TEXT NOT NULL, " + 
				COLUMN_NAME_SETTING + " TEXT NOT NULL, " + 
				"FOREIGN KEY(" + COLUMN_NAME_SETTING + ") REFERENCES " + SettingTable.TABLE_NAME + "(ID)" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		private static final int COLUMN_NUMBER_OFFSET_PERMISSION = 0;
		private static final int COLUMN_NUMBER_OFFSET_SETTING = 1;
		
		public static final ContentValues getContentValues(String permission, String settingId) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_PERMISSION, permission);
			contentValues.put(COLUMN_NAME_SETTING, settingId);		
			return contentValues;
		}
		
		public static final ContentValues getContentValues(String permission, PDroidSetting setting) {
			return getContentValues(permission, setting.getId());
		}

	}
	
	public static final class PermissionApplicationTable {
		public PermissionApplicationTable(){}
		
		public static final int OFFSET_PACKAGENAME = 0;
		public static final int OFFSET_PERMISSION = 1;
		public static final int COLUMN_COUNT = 2;
		
		public static final String TABLE_NAME = "permission_application";
		public static final String COLUMN_NAME_PERMISSION = "permission";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_PERMISSION + " TEXT NOT NULL, " + 
				COLUMN_NAME_PACKAGENAME + " TEXT NOT NULL, " + 
				"FOREIGN KEY(" + COLUMN_NAME_PERMISSION + ") REFERENCES " + PermissionSettingTable.TABLE_NAME + "(" + PermissionSettingTable.COLUMN_NAME_PERMISSION + ")" + 
				"FOREIGN KEY(" + COLUMN_NAME_PACKAGENAME + ") REFERENCES " + ApplicationTable.TABLE_NAME + "(" + ApplicationTable.COLUMN_NAME_PACKAGENAME + ")" +
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		public static final String WHERE_CLAUSE_PACKAGENAME = TABLE_NAME + "." + COLUMN_NAME_PACKAGENAME + " = ?";
		
		public static final ContentValues getContentValues(String permission, String packageName) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_PERMISSION, permission);
			contentValues.put(COLUMN_NAME_PACKAGENAME, packageName);		
			return contentValues;
		}
		
		public static final ContentValues getContentValues(String permission, Application application) {
			return getContentValues(permission, application.getPackageName());
		}
	}

	public static final class ProfileTable {
		public ProfileTable () {}
				
		public static final String TABLE_NAME = "profile";
		public static final String COLUMN_NAME_TITLE = "title";
		public static final String COLUMN_NAME_SETTING_NAME = "settingName";
		public static final String COLUMN_NAME_SETTING_VALUE = "settingValue";
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_TITLE + " TEXT NOT NULL, " +
				COLUMN_NAME_SETTING_NAME + " TEXT NOT NULL, " +
				COLUMN_NAME_SETTING_VALUE + " TEXT NOT NULL, " +
				"FOREIGN KEY(" + COLUMN_NAME_SETTING_NAME + ") REFERENCES " + SettingTable.TABLE_NAME + "(" + SettingTable.COLUMN_NAME_NAME + ")" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		public static final String WHERE_CLAUSE_PACKAGENAME = TABLE_NAME + "." + COLUMN_NAME_TITLE + " = ?";
	}
	
	protected static final String QUERYPART_SELECTPART_COLUMNS_PACKAGENAME = 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME;

	protected static final String QUERYPART_SELECTPART_COLUMNS_LABEL = 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL;
	
	protected static final String QUERYPART_SELECTPART_COLUMNS_APP =
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_ROWID + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL + ", " +  
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + ", " + 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_UID + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_VERSIONCODE + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_ICON + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_FLAGS + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PERMISSIONS; 
	
	protected static final String QUERYPART_SELECTPART_COLUMNS_STATUSFLAGS = 
			QUERYPART_SELECTPART_COLUMNS_APP + ", " + 
			ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_FLAGS;
	
	
	
	
	protected static final String QUERYPART_JOIN_APP_WITH_STATUS = 
			" LEFT OUTER JOIN " + ApplicationStatusTable.TABLE_NAME +
			" ON " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " =  " +
			ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_PACKAGENAME;

	protected static final String QUERYPART_JOIN_APP_WITH_PERMISSION = 
			" INNER JOIN " + PermissionApplicationTable.TABLE_NAME +
			" ON " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " =  " +
			PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PACKAGENAME;
	
	
	
	protected static final String QUERYPART_FILTER_BY_LABEL = 
			" WHERE " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL + " LIKE ?";

	protected static final String QUERYPART_FILTER_BY_PACKAGENAME = 
			" WHERE " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " LIKE ?";

	protected static final String QUERYPART_FILTER_BY_PERMISSION = 
			" INNER JOIN " + PermissionApplicationTable.TABLE_NAME +
			" ON " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " =  " +
			PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PACKAGENAME + 
			" WHERE " + PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PERMISSION + " = ?";

	protected static final String QUERYPART_FILTER_BY_SETTING_GROUP = 
			" INNER JOIN " + PermissionApplicationTable.TABLE_NAME +
			" ON " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " = " +
			PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PACKAGENAME +
			" INNER JOIN " + PermissionSettingTable.TABLE_NAME + 
			" ON " + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_PERMISSION + " = " +
			PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PERMISSION +
			" INNER JOIN " + SettingTable.TABLE_NAME + 
			" ON " + SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_ID + " = " +
			PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_SETTING +
			" WHERE " + SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_TITLE + " = ?";
	
	protected static final String QUERYPART_FILTER_BY_TYPE = 
			" WHERE " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_FLAGS +
			" & " + ApplicationTable.FLAG_IS_SYSTEM_APP + " = CAST(? AS INTEGER)";

	protected static final String QUERYPART_SORT_BY_LABEL = 
			" ORDER BY " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL;

	public static final String QUERYPART_GET_ALL_APPS_WITH_STATUS = "SELECT DISTINCT " +
			QUERYPART_SELECTPART_COLUMNS_STATUSFLAGS + 
			" FROM " + ApplicationTable.TABLE_NAME + 
			QUERYPART_JOIN_APP_WITH_STATUS;

	public static final String QUERYPART_GET_ALL_APPS_PACKAGENAME_ONLY = "SELECT " +
			QUERYPART_SELECTPART_COLUMNS_PACKAGENAME +  
			" FROM " + ApplicationTable.TABLE_NAME;

	public static final String QUERY_GET_ALL_APPS_WITH_STATUS =
			QUERYPART_GET_ALL_APPS_WITH_STATUS + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_ALL_APPS_PACKAGENAME_ONLY =
			QUERYPART_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_PACKAGENAME_WITH_STATUS =
			QUERYPART_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_PACKAGENAME + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_LABEL_WITH_STATUS =
			QUERYPART_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_LABEL + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_LABEL_PACKAGENAME_ONLY =
			QUERYPART_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_FILTER_BY_LABEL + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_PACKAGENAME_LABEL_ONLY = "SELECT " +
					QUERYPART_SELECTPART_COLUMNS_LABEL +  
					" FROM " + ApplicationTable.TABLE_NAME + 
					QUERYPART_FILTER_BY_PACKAGENAME;
	
	public static final String QUERY_GET_APPS_BY_PACKAGENAME_ROWID_ONLY = "SELECT " +
			ApplicationTable.COLUMN_NAME_ROWID +  
			" FROM " + ApplicationTable.TABLE_NAME + 
			QUERYPART_FILTER_BY_PACKAGENAME;

	public static final String QUERY_GET_APPS_BY_PERMISSION_WITH_STATUS =
			QUERYPART_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_PERMISSION + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_PERMISSION_PACKAGENAME_ONLY =
			QUERYPART_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_FILTER_BY_PERMISSION + QUERYPART_SORT_BY_LABEL;

	public static final String QUERY_GET_APPS_BY_TYPE_WITH_STATUS =
			QUERYPART_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_TYPE + QUERYPART_SORT_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_TYPE_PACKAGENAME_ONLY =
			QUERYPART_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_FILTER_BY_TYPE + QUERYPART_SORT_BY_LABEL;

	public static final String QUERY_DELETE_APPS_WITHOUT_STATUS = "DELETE FROM " + 
			ApplicationStatusTable.TABLE_NAME + 
			" WHERE " + ApplicationStatusTable.COLUMN_NAME_PACKAGENAME + 
			" NOT IN (" +
				" SELECT " + ApplicationTable.COLUMN_NAME_PACKAGENAME + 
				" FROM " + ApplicationTable.TABLE_NAME + 
			");";
	
	public static final String QUERY_GET_PERMISSIONS_OF_INTEREST = "SELECT DISTINCT " + 
			PermissionSettingTable.COLUMN_NAME_PERMISSION + 
			" FROM " + PermissionSettingTable.TABLE_NAME;
	
	public static final String QUERY_GET_SETTINGS_BY_PACKAGENAME = "SELECT DISTINCT " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_ID + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_NAME + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_VALUEFUNCTIONNAMESTUB + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_TITLE + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_ID + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_TITLE + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_OPTIONS + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_TRUSTED_OPTION + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SORT +
			" FROM " + SettingTable.TABLE_NAME + 
			" INNER JOIN " + PermissionSettingTable.TABLE_NAME +
			" ON (" + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_SETTING +
			" = " + SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_ID + ")" +
			" WHERE " + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_PERMISSION + 
			" = 'any' OR " + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_PERMISSION + "" +
					" IN (SELECT " + PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PERMISSION +
					" FROM " + PermissionApplicationTable.TABLE_NAME +
					" WHERE " + PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PACKAGENAME + " = ?" + 
					") " +
					" ORDER BY " + SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SORT + " ASC;";

	public static final String QUERY_GET_SETTINGS_FUNCTION_NAMES = "SELECT " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME + "," + 
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_TRUSTED_OPTION +
			" FROM " + SettingTable.TABLE_NAME;

	public static final String QUERY_GET_SETTINGS_AND_VALUE_FUNCTIONS = "SELECT " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_ID + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_NAME + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_VALUEFUNCTIONNAMESTUB + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_TITLE + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_ID + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_TITLE + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_OPTIONS + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_TRUSTED_OPTION + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SORT +
			" FROM " + SettingTable.TABLE_NAME + 
			" ORDER BY " + SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_SORT + " ASC;";

	
	public Context context;
	
	public static DBInterface getInstance(Context context) {
		synchronized (lockObject) {
			if (dbinterface == null) {
				dbinterface = new DBInterface(context.getApplicationContext());
			}
		}
		
		return dbinterface;
	}
	
	public DBHelper getDBHelper() {
		synchronized(this) {
			if (dbhelper == null) {
				dbhelper = new DBHelper(this.context);
			}
		}
		
		return dbhelper;
	}
	
	public void addLogEntry(String packageName, int uid, byte accessMode, String dataType) {
		if (dbhelper == null) {
			getDBHelper();
		}
		
		ContentValues contentValues = ApplicationLogTable.getContentValues(packageName, uid, 0, dataType,
				0);
		
		SQLiteDatabase write_db = dbhelper.getWritableDatabase();
		write_db.insert(ApplicationLogTable.TABLE_NAME, null, contentValues);
		//write_db.close();
	}

	/**
	 * Helper to just get the label of an application - used for notifications, when 
	 * the label is all we care about, so we can display it to the user
	 * @param packageName
	 */
	public String getApplicationLabel(String packageName) {
		if (dbhelper == null) {
			getDBHelper();
		}
		
		SQLiteDatabase db = dbhelper.getReadableDatabase();
		Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_APPS_BY_PACKAGENAME_LABEL_ONLY, new String[]{packageName});
		String label = "";
		
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			label = cursor.getString(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL));
		}
		cursor.close();
		//db.close();
		return label;
	}
	
	/**
	 * Helper to just get the RowID of an application - used for notifications,  
	 * this allows for unique notifications to be generated based on RowID of application.
	 * @param packageName
	 */
	public Integer getApplicationRowId(String packageName) {
		if (dbhelper == null) {
			getDBHelper();
		}
		
		SQLiteDatabase db = dbhelper.getReadableDatabase();
		Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_APPS_BY_PACKAGENAME_ROWID_ONLY, new String[]{packageName});
		Integer NotificationId = 0;
		
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			NotificationId = cursor.getInt(cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ROWID));
		}
		cursor.close();
		//db.close();
		return NotificationId;
	}
	
	
	/**
	 * Helper to delete the details for a single package (used when we receive a notification of
	 * a package being removed).
	 * @param packageName
	 */
	public void deleteApplicationRecord(String packageName) {
		if (dbhelper == null) {
			getDBHelper();
		}

		SQLiteDatabase db = dbhelper.getReadableDatabase();
		db.delete(ApplicationTable.TABLE_NAME, ApplicationTable.WHERE_CLAUSE_PACKAGENAME, new String[]{packageName});
		db.delete(ApplicationStatusTable.TABLE_NAME, ApplicationStatusTable.WHERE_CLAUSE_PACKAGENAME, new String[]{packageName});
		db.delete(PermissionApplicationTable.TABLE_NAME, PermissionApplicationTable.WHERE_CLAUSE_PACKAGENAME, new String[]{packageName});
		//db.close();
	}

	
	public void addApplicationRecord(Application app) {
		if (dbhelper == null) {
			getDBHelper();
		}

		SQLiteDatabase write_db = dbhelper.getWritableDatabase();
		write_db.beginTransaction();

		try {
			write_db.insert(ApplicationTable.TABLE_NAME, null, ApplicationTable.getContentValues(app));
			write_db.insert(ApplicationStatusTable.TABLE_NAME, null, ApplicationStatusTable.getContentValues(app));
			
			InsertHelper permissionsInsertHelper = new InsertHelper(write_db, DBInterface.PermissionApplicationTable.TABLE_NAME);
			int [] permissionsTableColumnNumbers = new int[2];
			//I was thinking about using enums instead of static finals here, but apparently the performance in android for enums is not so good??
			permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PACKAGENAME] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PACKAGENAME);
			permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PERMISSION] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PERMISSION);
			
			for (String permission : app.getPermissions()) {
				permissionsInsertHelper.prepareForInsert();
				permissionsInsertHelper.bind(permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PACKAGENAME], app.getPackageName());
				permissionsInsertHelper.bind(permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PERMISSION], permission);
				permissionsInsertHelper.execute();
			}
			permissionsInsertHelper.close();
			
			write_db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		}
		write_db.endTransaction();
		//write_db.close();
		
	}
	
	/**
	 * Update the status of an application. The application must already have
	 * been saved in the database.
	 * @param app  Application for which to rewrite the status
	 */
	public void updateApplicationStatus(Application app) {
		if (app == null) {
			Log.e(GlobalConstants.LOG_TAG, "DBInterface:updateApplicationStatus: app must be provided");
			return;
		}
		
		if (app.isNew()) {
			Log.e(GlobalConstants.LOG_TAG, "DBInterface:updateApplicationStatus: status can only be updated for already-saved application objects");
			return;
		}
		
		if (dbhelper == null) {
			getDBHelper();
		}

		SQLiteDatabase write_db = dbhelper.getWritableDatabase();
		write_db.beginTransaction();

		ContentValues contentValues = new ContentValues();
		contentValues.put(ApplicationStatusTable.COLUMN_NAME_FLAGS, app.getStatusFlags());
		
		try {
			write_db.update(ApplicationStatusTable.TABLE_NAME, contentValues, ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_PACKAGENAME + " = ?", new String [] {app.getPackageName()});			
			write_db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		}
		write_db.endTransaction();
		//write_db.close();	
	}
	
	public void updateApplicationStatus(List<Application> apps) {
		if (apps == null || apps.size() == 0) {
			if (GlobalConstants.CRASH_HAPPY) {
				throw new InvalidParameterException("DBInterface:updateApplicationStatus: apps must be provided");
			} else { 
				Log.e(GlobalConstants.LOG_TAG, "DBInterface:updateApplicationStatus: apps must be provided");
				return;
			}
		}

		if (dbhelper == null) {
			getDBHelper();
		}

		SQLiteDatabase write_db = dbhelper.getWritableDatabase();
		write_db.beginTransaction();

		ContentValues contentValues = new ContentValues();

		try {
			for (Application app : apps) {
				contentValues.put(ApplicationStatusTable.COLUMN_NAME_FLAGS, app.getStatusFlags());
				
					write_db.update(ApplicationStatusTable.TABLE_NAME, contentValues, ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_PACKAGENAME + " = ?", new String [] {app.getPackageName()});
			}
			write_db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			write_db.endTransaction();
		}
	}
	
	public void updateApplicationRecord(Application app) {
		if (dbhelper == null) {
			getDBHelper();
		}

		SQLiteDatabase write_db = dbhelper.getWritableDatabase();
		write_db.beginTransaction();

		try {
			int updatedFlags = app.getUpdatedFlags();
			
			if (0 != (updatedFlags & (
					Application.FLAG_APPLICATION_VALUES_CHANGED |
					Application.FLAG_ICON_CHANGED |
					Application.FLAG_PERMISSIONS_CHANGED))) {
				write_db.update(ApplicationTable.TABLE_NAME, ApplicationTable.getUpdateContentValues(app), ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " = ?", new String [] {app.getPackageName()});
			}
			
			if (0 != (updatedFlags & Application.FLAG_STATUSFLAGS_CHANGED)) {
				write_db.update(ApplicationStatusTable.TABLE_NAME, ApplicationStatusTable.getContentValues(app), ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_PACKAGENAME + " = ?", new String [] {app.getPackageName()});
			}
			
			if (0 != (updatedFlags & Application.FLAG_PERMISSIONS_CHANGED)) {
				write_db.delete(PermissionApplicationTable.TABLE_NAME, PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PACKAGENAME + " = ?", new String [] {app.getPackageName()});
				if (app.getPermissions() != null) {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "DBInterface.updateApplicationRecord: " + app.getPackageName() + " has permissions");
					InsertHelper permissionsInsertHelper = new InsertHelper(write_db, DBInterface.PermissionApplicationTable.TABLE_NAME);
					int [] permissionsTableColumnNumbers = new int[2];
					//I was thinking about using enums instead of static finals here, but apparently the performance in android for enums is not so good??
					permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PACKAGENAME] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PACKAGENAME);
					permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PERMISSION] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PERMISSION);
				
					for (String permission : app.getPermissions()) {
						permissionsInsertHelper.prepareForInsert();
						permissionsInsertHelper.bind(permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PACKAGENAME], app.getPackageName());
						permissionsInsertHelper.bind(permissionsTableColumnNumbers[PermissionApplicationTable.OFFSET_PERMISSION], permission);
						permissionsInsertHelper.execute();
					}
					permissionsInsertHelper.close();
				} else {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "DBInterface.updateApplicationRecord: " + app.getPackageName() + " has no permissions");
				}
			}
			
			write_db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		}
		write_db.endTransaction();
		//write_db.close();
		
	}

	
	private DBInterface(Context context) {
		this.context = context;
	}
	
	public class DBHelper extends SQLiteOpenHelper {
		public static final String DATABASE_NAME = "pdroidmgr.db";
		public static final int DATABASE_VERSION = 35;
		
		//private SQLiteDatabase db;
		
		private DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Executing DBInterface.DBHelper.onCreate");
			createTables(db, true);
			loadDefaultData(db);
		}
	
		
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Executing DBInterface.DBHelper.onUpgrade");
			// At version 1 - no upgrades yet!
			deleteTables(db, false);
			createTables(db, false);
			loadDefaultData(db);
		}

		public void deleteTables(SQLiteDatabase db, boolean includeLogTable) {
			if (includeLogTable) {
				db.execSQL(ApplicationLogTable.DROP_SQL);
			}
			db.execSQL(ApplicationStatusTable.DROP_SQL);
			db.execSQL(PermissionApplicationTable.DROP_SQL);
			db.execSQL(ApplicationTable.DROP_SQL);
			db.execSQL(PermissionSettingTable.DROP_SQL);
			db.execSQL(SettingTable.DROP_SQL);
		}

		public void createTables(SQLiteDatabase db, boolean includeLogTable) {
			if (includeLogTable) {
				db.execSQL(ApplicationLogTable.CREATE_SQL);
			}
			db.execSQL(ApplicationTable.CREATE_SQL);
			db.execSQL(ApplicationStatusTable.CREATE_SQL);
			db.execSQL(PermissionApplicationTable.CREATE_SQL);
			db.execSQL(SettingTable.CREATE_SQL);
			db.execSQL(PermissionSettingTable.CREATE_SQL);
		}

		
		public void loadDefaultData(SQLiteDatabase db) {
			Resources resources = context.getResources();
			String packageName = context.getPackageName();
			
			db.delete(SettingTable.TABLE_NAME, null, null);
			db.delete(PermissionSettingTable.TABLE_NAME, null, null);
			
			XmlResourceParser xrp = resources.getXml(R.xml.pdroid_settings);
			try {
				db.beginTransaction();
				InsertHelper settingInsertHelper = new InsertHelper(db, SettingTable.TABLE_NAME);
				int [] settingTableColumnNumbers = new int[SettingTable.COLUMN_COUNT];
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_ID] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_ID);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_NAME] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_NAME);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_SETTINGFUNCTIONNAME] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_SETTINGFUNCTIONNAME);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_VALUEFUNCTIONNAMESTUB] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_VALUEFUNCTIONNAMESTUB);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_TITLE] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_TITLE);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_GROUP_ID] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_GROUP_ID);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_GROUP_TITLE] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_GROUP_TITLE);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_OPTIONS] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_OPTIONS);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_TRUSTED_OPTION] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_TRUSTED_OPTION);
				settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_SORT] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_SORT);
				
				int eventType = xrp.next();
				while(!(eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) && eventType != XmlResourceParser.END_DOCUMENT) {
					eventType = xrp.next();
				}
				String id;
				while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) {
					settingInsertHelper.prepareForInsert();
					id = xrp.getIdAttribute();
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_ID], id);
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_NAME], 
							xrp.getAttributeValue(null, "name")
						);
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_SETTINGFUNCTIONNAME], 
							xrp.getAttributeValue(null, "settingfunctionname")
						);
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_VALUEFUNCTIONNAMESTUB], 
							xrp.getAttributeValue(null, "valuefunctionnamestub")
						);
		        	//I wish there were a nicer way to get this string. Maybe a pair of arrays - one with identifiers, one with labels?
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_TITLE],
							 resources.getString(resources.getIdentifier("SETTING_LABEL_" + id, "string", packageName))
						);

					id = xrp.getAttributeValue(null, "group"); 
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_GROUP_ID], id);
					//Because groups can be duplicated, it may be better to actually cache these in advance rather than repeatedly using reflection
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_GROUP_TITLE],
							resources.getString(resources.getIdentifier("SETTING_GROUP_LABEL_" + id, "string", packageName))
						);
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_SORT], 
							xrp.getAttributeValue(null, "sort")
						);
		        	eventType = xrp.next();
		 			while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
		 				eventType = xrp.next();
		 	 		}
			        LinkedList<String> options = new LinkedList<String>();
			        String trustedOption = null;
		        	while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("option")) {
		        		options.add(xrp.getAttributeValue(null, "name"));
		        		//Keep in mind that a 'boolean' value (I think) means 'true' if the attribute
		        		//is present, and false otherwise - but I'm not sure; worth checking
		        		if (xrp.getAttributeBooleanValue(null, "trustedoption", false)) {
		        			trustedOption = xrp.getAttributeValue(null, "name");
		        		}
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
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_OPTIONS],
							TextUtils.join(",",options)
						);
					settingInsertHelper.bind(settingTableColumnNumbers[SettingTable.COLUMN_NUMBER_OFFSET_TRUSTED_OPTION],
							trustedOption
						);
			        settingInsertHelper.execute();
				}
				settingTableColumnNumbers = null;
				settingInsertHelper.close();
				
				InsertHelper permissionInsertHelper = new InsertHelper(db, PermissionSettingTable.TABLE_NAME);
				int [] permissionSettingTableColumnNumbers = new int[2];
				permissionSettingTableColumnNumbers[PermissionSettingTable.COLUMN_NUMBER_OFFSET_PERMISSION] = permissionInsertHelper.getColumnIndex(PermissionSettingTable.COLUMN_NAME_PERMISSION);
				permissionSettingTableColumnNumbers[PermissionSettingTable.COLUMN_NUMBER_OFFSET_SETTING] = permissionInsertHelper.getColumnIndex(PermissionSettingTable.COLUMN_NAME_SETTING);

				xrp = resources.getXml(R.xml.permission_setting_map);
				eventType = xrp.next();
				while(!(eventType == XmlResourceParser.START_TAG && xrp.getName().equals("permission")) && eventType != XmlResourceParser.END_DOCUMENT) {
					eventType = xrp.next();
				}
				while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("permission")) {
					id = xrp.getIdAttribute();
					eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
					while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) {
						permissionInsertHelper.prepareForInsert();
						permissionInsertHelper.bind(permissionSettingTableColumnNumbers[PermissionSettingTable.COLUMN_NUMBER_OFFSET_PERMISSION],
								id
							);
						permissionInsertHelper.bind(permissionSettingTableColumnNumbers[PermissionSettingTable.COLUMN_NUMBER_OFFSET_SETTING],
								xrp.getIdAttribute()
							);
						permissionInsertHelper.execute();
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
					if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("permission")) {
						eventType = xrp.next();
						while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
							eventType = xrp.next();
						}
					} else {
						break;
					}
				}
				permissionInsertHelper.close();
				
				db.setTransactionSuccessful();
			} catch (XmlPullParserException e) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,e.getMessage());
				//TODO: Exception handling, mayhaps?
			} catch (IOException e) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,e.getMessage());
			} catch (NotFoundException e) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,e.getMessage());
			} finally {
				db.endTransaction();
			}
		}
	}
}

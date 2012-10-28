package net.digitalfeed.pdroidalternative;

import java.io.IOException;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class DBInterface {
	private static DBHelper dbhelper = null;
	private static DBInterface dbinterface = null;
	
	private static final int SETTING_TABLE_COLUMN_NUMBER_OFFSET_ID = 0;
	private static final int SETTING_TABLE_COLUMN_NUMBER_OFFSET_NAME = 1;
	private static final int SETTING_TABLE_COLUMN_NUMBER_OFFSET_TITLE = 2;
	private static final int SETTING_TABLE_COLUMN_NUMBER_OFFSET_GROUP_ID = 3;
	private static final int SETTING_TABLE_COLUMN_NUMBER_OFFSET_GROUP_TITLE = 4;
	private static final int SETTING_TABLE_COLUMN_NUMBER_OFFSET_OPTIONS = 5;

	private static final int PERMISSIONSETTING_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION = 0;
	private static final int PERMISSIONSETTING_TABLE_COLUMN_NUMBER_OFFSET_SETTING = 1;

	
	public static final class ApplicationTable {
		public ApplicationTable(){}
		
		public static final int COMPRESS_ICON_QUALITY = 100;
		
		public static final String TABLE_NAME = "application";
		public static final String COLUMN_NAME_LABEL = "label";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_UID = "uid";
		public static final String COLUMN_NAME_VERSIONCODE = "versionCode";
		public static final String COLUMN_NAME_PERMISSIONS = "permissions";
		public static final String COLUMN_NAME_ICON = "icon";
		public static final String COLUMN_NAME_FLAGS = "appFlags";
		
		public static final int FLAG_IS_SYSTEM_APP = 0x1;
		public static final int FLAG_HAS_INTERNET_ACCESS = 0x2;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_LABEL + " TEXT NOT NULL, " +
				COLUMN_NAME_PACKAGENAME + " TEXT NOT NULL, " + 
				COLUMN_NAME_UID + " INTEGER NOT NULL, " +
				COLUMN_NAME_VERSIONCODE + " INTEGER NOT NULL, " +
				COLUMN_NAME_PERMISSIONS + " TEXT, " + 
				COLUMN_NAME_ICON + " BLOB, " +
				COLUMN_NAME_FLAGS + "  INTEGER NOT NULL" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
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
	}
	
	public static final class ApplicationStatusTable {
		public ApplicationStatusTable(){}
		public static final String TABLE_NAME = "application_status";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_FLAGS = "statusFlags";

		public static final int FLAG_IS_UNTRUSTED = 0x1;
		public static final int FLAG_NOTIFY_ON_ACCESS = 0x2;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				COLUMN_NAME_PACKAGENAME + " TEXT, " + 
				COLUMN_NAME_FLAGS + " INTEGER, " + 
				"FOREIGN KEY(" + COLUMN_NAME_PACKAGENAME + ") REFERENCES " + ApplicationTable.TABLE_NAME + "(packageName)" + 
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
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
		
		public static final int FLAGS_ALLOWED = 0x1;
		
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
		
		public static final ContentValues getContentValues(String packageName, int uid, int versionCode,
				String operation, int flags) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_PACKAGENAME, packageName);
			contentValues.put(COLUMN_NAME_UID, uid);
			contentValues.put(COLUMN_NAME_VERSIONCODE, versionCode);
			contentValues.put(COLUMN_NAME_OPERATION, operation);
			contentValues.put(COLUMN_NAME_FLAGS, flags);
			return contentValues;
		}
	}
	
	public static final class SettingTable {
		public SettingTable(){}
		public static final String TABLE_NAME = "setting";
		public static final String COLUMN_NAME_ID = "id";
		public static final String COLUMN_NAME_NAME = "name";
		public static final String COLUMN_NAME_TITLE = "title"; //Used to store the 'friendly' title of the setting, which may be language specific.
																//If we start adding support for multiple languages, possibly we should be handling this better; maybe having another table with all the language text in it?
																//The point of this is to avoid using reflection to get the titles from resources all the time
		public static final String COLUMN_NAME_GROUP_ID = "groupId"; //Stored as a string, but maybe better in another table and linked?
		public static final String COLUMN_NAME_GROUP_TITLE = "groupTitle"; //As with the above 'group' column, may be better in a separate column, but then we need to be doing joins.
		public static final String COLUMN_NAME_OPTIONS = "options"; //Options are stored as a string array
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_ID + " TEXT NOT NULL, " + 
				COLUMN_NAME_NAME + " TEXT NOT NULL, " +
				COLUMN_NAME_TITLE + " TEXT, " +
				COLUMN_NAME_GROUP_ID + " TEXT NOT NULL, " +
				COLUMN_NAME_GROUP_TITLE + " TEXT, " +
				COLUMN_NAME_OPTIONS + " TEXT NOT NULL" +
				");";
		
		public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
		
		public static final ContentValues getContentValues(Setting setting) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_ID, setting.getId());
			contentValues.put(COLUMN_NAME_NAME, setting.getName());
			//contentValues.put(COLUMN_NAME_TITLE, setting.getTitle());
			contentValues.put(COLUMN_NAME_GROUP_ID, setting.getGroup());
			//contentValues.put(COLUMN_NAME_GROUP_TITLE, setting.getGroupTitle());
			contentValues.put(COLUMN_NAME_OPTIONS, TextUtils.join(",",setting.getOptions()));
			
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
		
		public static final ContentValues getContentValues(String permission, String settingId) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_NAME_PERMISSION, permission);
			contentValues.put(COLUMN_NAME_SETTING, settingId);		
			return contentValues;
		}
		
		public static final ContentValues getContentValues(String permission, Setting setting) {
			return getContentValues(permission, setting.getId());
		}

	}
	
	public static final class PermissionApplicationTable {
		public PermissionApplicationTable(){}
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
	
	protected static final String QUERYPART_COLUMNS_PACKAGENAME = 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME;
	
	protected static final String QUERYPART_COLUMNS_APP = 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL + ", " +  
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + ", " + 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_UID + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_VERSIONCODE + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_ICON + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_FLAGS + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PERMISSIONS; 
	
	protected static final String QUERYPART_COLUMNS_APP_WITH_STATUS = 
			QUERYPART_COLUMNS_APP + ", " + 
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
	
	protected static final String QUERYPART_FILTER_BY_TYPE = 
			" WHERE " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_FLAGS +
			" & " + ApplicationTable.FLAG_IS_SYSTEM_APP + " = CAST(? AS INTEGER)";
	
	public static final String QUERY_GET_ALL_APPS_WITH_STATUS = "SELECT " +
			QUERYPART_COLUMNS_APP_WITH_STATUS + 
			" FROM " + ApplicationTable.TABLE_NAME + 
			QUERYPART_JOIN_APP_WITH_STATUS;

	public static final String QUERY_GET_ALL_APPS_PACKAGENAME_ONLY = "SELECT " +
			QUERYPART_COLUMNS_PACKAGENAME +  
			" FROM " + ApplicationTable.TABLE_NAME;

	public static final String QUERY_GET_APPS_BY_PACKAGENAME_WITH_STATUS =
			QUERY_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_PACKAGENAME;
	
	public static final String QUERY_GET_APPS_BY_LABEL_WITH_STATUS =
			QUERY_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_LABEL_PACKAGENAME_ONLY =
			QUERY_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_FILTER_BY_LABEL;
	
	public static final String QUERY_GET_APPS_BY_PERMISSION_WITH_STATUS =
			QUERY_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_PERMISSION;
	
	public static final String QUERY_GET_APPS_BY_PERMISSION_PACKAGENAME_ONLY =
			QUERY_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_FILTER_BY_PERMISSION;

	public static final String QUERY_GET_APPS_BY_TYPE_WITH_STATUS =
			QUERY_GET_ALL_APPS_WITH_STATUS + QUERYPART_FILTER_BY_TYPE;
	
	public static final String QUERY_GET_APPS_BY_TYPE_PACKAGENAME_ONLY =
			QUERY_GET_ALL_APPS_PACKAGENAME_ONLY + QUERYPART_FILTER_BY_TYPE;

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
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_TITLE + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_ID + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_GROUP_TITLE + ", " +
			SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_OPTIONS + 
			" FROM " + PermissionApplicationTable.TABLE_NAME + 
			" INNER JOIN " + PermissionSettingTable.TABLE_NAME + 
			" ON (" + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_PERMISSION + 
			" = " + PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PERMISSION +
			" OR " + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_PERMISSION + " = 'any')" + 
			" INNER JOIN " + SettingTable.TABLE_NAME + 
			" ON " + SettingTable.TABLE_NAME + "." + SettingTable.COLUMN_NAME_ID + 
			" = " + PermissionSettingTable.TABLE_NAME + "." + PermissionSettingTable.COLUMN_NAME_SETTING +
			" WHERE " + PermissionApplicationTable.TABLE_NAME + "." + PermissionApplicationTable.COLUMN_NAME_PACKAGENAME + " = ?";	
	
	public Context context;
	
	public static DBInterface getInstance(Context context) {
		if (dbinterface == null) {
			dbinterface = new DBInterface(context.getApplicationContext());
		}
		return dbinterface;
	}
	
	public DBHelper getDBHelper() {
		if (dbhelper == null) {
			dbhelper = new DBHelper(this.context);
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
		write_db.close();
	}
		
	private DBInterface(Context context) {
		this.context = context;
	}
	
	public class DBHelper extends SQLiteOpenHelper {
		public static final String DATABASE_NAME = "pdroidmgr.db";
		public static final int DATABASE_VERSION = 23;
		
		//private SQLiteDatabase db;
		
		private DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d("PDroidAlternative", "Executing DBInterface.DBHelper.onCreate");
			createTables(db, true);
			loadDefaultData(db);
		}
	
		
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("PDroidAlternative", "Executing DBInterface.DBHelper.onUpgrade");
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

		
		private void loadDefaultData(SQLiteDatabase db) {
			Resources resources = context.getResources();
			String packageName = context.getPackageName();
			
			XmlResourceParser xrp = resources.getXml(R.xml.pdroid_settings);
			try {
				db.beginTransaction();
				Log.d("PDroidAlternative","Begin transaction");
				InsertHelper settingInsertHelper = new InsertHelper(db, DBInterface.SettingTable.TABLE_NAME);
				int [] settingTableColumnNumbers = new int[6];
				settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_ID] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_ID);
				settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_NAME] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_NAME);
				settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_TITLE] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_TITLE);
				settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_GROUP_ID] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_GROUP_ID);
				settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_GROUP_TITLE] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_GROUP_TITLE);
				settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_OPTIONS] = settingInsertHelper.getColumnIndex(SettingTable.COLUMN_NAME_OPTIONS);
				
				int eventType = xrp.next();
				while(!(eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) && eventType != XmlResourceParser.END_DOCUMENT) {
					eventType = xrp.next();
				}
				String id;
				while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) {
					settingInsertHelper.prepareForInsert();
					id = xrp.getIdAttribute();
					settingInsertHelper.bind(settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_ID], id);
					settingInsertHelper.bind(settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_NAME], 
							xrp.getAttributeValue(null, "name")
						);
		        	//I wish there were a nicer way to get this string. Maybe a pair of arrays - one with identifiers, one with labels?
					settingInsertHelper.bind(settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_TITLE],
							 resources.getString(resources.getIdentifier("SETTING_LABEL_" + id, "string", packageName))
						);

					id = xrp.getAttributeValue(null, "group"); 
					settingInsertHelper.bind(settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_GROUP_ID], id);
					//Because groups can be duplicated, it may be better to actually cache these in advance rather than repeatedly using reflection
					settingInsertHelper.bind(settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_GROUP_TITLE],
							resources.getString(resources.getIdentifier("SETTING_GROUP_LABEL_" + id, "string", packageName))
						);
					
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
					settingInsertHelper.bind(settingTableColumnNumbers[SETTING_TABLE_COLUMN_NUMBER_OFFSET_OPTIONS],
							TextUtils.join(",",options)
						);
			        settingInsertHelper.execute();
				}
				settingTableColumnNumbers = null;
				settingInsertHelper.close();
				
				InsertHelper permissionInsertHelper = new InsertHelper(db, DBInterface.PermissionSettingTable.TABLE_NAME);
				int [] permissionSettingTableColumnNumbers = new int[2];
				permissionSettingTableColumnNumbers[PERMISSIONSETTING_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION] = permissionInsertHelper.getColumnIndex(PermissionSettingTable.COLUMN_NAME_PERMISSION);
				permissionSettingTableColumnNumbers[PERMISSIONSETTING_TABLE_COLUMN_NUMBER_OFFSET_SETTING] = permissionInsertHelper.getColumnIndex(PermissionSettingTable.COLUMN_NAME_SETTING);

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
						permissionInsertHelper.bind(permissionSettingTableColumnNumbers[PERMISSIONSETTING_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION],
								id
							);
						permissionInsertHelper.bind(permissionSettingTableColumnNumbers[PERMISSIONSETTING_TABLE_COLUMN_NUMBER_OFFSET_SETTING],
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
				
				Log.d("PDroidAlternative","Set transaction successful");
				db.setTransactionSuccessful();
			} catch (XmlPullParserException e) {
				Log.d("PDroidAlternative",e.getMessage());
				//TODO: Exception handling, mayhaps?
			} catch (IOException e) {
				Log.d("PDroidAlternative",e.getMessage());
			} catch (NotFoundException e) {
				Log.d("PDroidAlternative",e.getMessage());
			} finally {
				db.endTransaction();
			}
		}
	}
}

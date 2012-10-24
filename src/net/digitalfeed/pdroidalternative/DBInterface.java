package net.digitalfeed.pdroidalternative;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

public class DBInterface {
	private static DBHelper dbhelper = null;
	private static DBInterface dbinterface = null;
	
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

	public static final String ApplicationListQuery = "SELECT " + 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL + ", " +  
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + ", " + 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_UID + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_VERSIONCODE + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_ICON + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_FLAGS + ", " +
			ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_FLAGS +   
			" FROM " + ApplicationTable.TABLE_NAME + 
			" LEFT OUTER JOIN " + ApplicationStatusTable.TABLE_NAME +
			" ON " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " =  " +
			ApplicationStatusTable.TABLE_NAME + "." + ApplicationStatusTable.COLUMN_NAME_PACKAGENAME;
	
	public static final String ApplicationListTitleSubsetQuery = ApplicationListQuery + 
			" WHERE " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL + " LIKE ?";
	
	public static final String ApplicationSingleQuery = "SELECT " + 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_LABEL + ", " +  
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + ", " + 
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_UID + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_VERSIONCODE + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_ICON + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_FLAGS + ", " +
			ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PERMISSIONS +   
			" FROM " + ApplicationTable.TABLE_NAME + 
			" WHERE " + ApplicationTable.TABLE_NAME + "." + ApplicationTable.COLUMN_NAME_PACKAGENAME + " = ?";
	
	
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
		public static final int DATABASE_VERSION = 1;
		
		//private SQLiteDatabase db;
		
		private DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			//this.db = db;  
			db.execSQL(ApplicationTable.CREATE_SQL);
			//db.execSQL("CREATE TABLE permission (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, label TEXT NOT NULL, description TEXT);");
			//db.execSQL("CREATE TABLE setting (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, label TEXT NOT NULL, permission_id INTEGER, options INTEGER, fake_type INTEGER, description TEXT, FOREIGN KEY(permission_id) REFERENCES permission(_id));");
			//db.execSQL("CREATE TABLE application_permission (application_id INTEGER, permission_id INTEGER, FOREIGN KEY(application_id) REFERENCES application(_id), FOREIGN KEY(permission_id) REFERENCES permission(_id));");
			db.execSQL(ApplicationStatusTable.CREATE_SQL);
			db.execSQL(ApplicationLogTable.CREATE_SQL);
	
			//loadDefaultData();
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// At version 1 - no upgrades yet!
		}
		
		//private void loadDefaultData() {
			/*
			 * The idea here is/was to have Settings and Permissions tables which declare the settings associated with each permission
			 * and then provide the settings (and relevant options) based on the table content. Maybe later.
			class Setting {
				public static final int OPTION_REAL = 1;
				public static final int OPTION_BLOCK = 2;
				public static final int OPTION_RANDOM = 3;
				public static final int OPTION_FAKE = 4;
				public static final int FAKE_TYPE_INTEGER = 0;
				public static final int FAKE_TYPE_STRING = 1;
				public static final int FAKE_TYPE_FLOAT = 2;
				public static final int FAKE_TYPE_COORDINATES = 3;
				public static 
				final String name;
				final String label;
				final int options;
				final String description;
				
				public Setting (String name, String label, int options, String description) {
					this.name = name;
					this.label = label;
					this.options = options;
					this.description = description;
				}
				
				public void writeToDatabase(long permission_id) {
					ContentValues values = new ContentValues();
					values.put("name", this.name);
					values.put("label", this.label);
					values.put("permission_id", permission_id);
					values.put("options", options);
					db.insert("permission", null, values);
				}
			}
			
			class Permission {
				long _id;
				final String name;
				final String label;
				final String description;
				Setting[] settings = null;
				
				public Permission (String name, String label, String description, Setting[] settings) {
					this.name = name;
					this.label = label;
					this.description = description;
					this.settings = settings;
				}
				
				public void writeToDatabase() {
					ContentValues values = new ContentValues();
					values.put("name", this.name);
					values.put("label", this.label);
					values.put("description", this.description);
					this._id = db.insert("permission", null, values);
					for (Setting setting : this.settings) {
						setting.writeToDatabase(this._id);
					}
				} 
			}
			*/
		//}
	}
}

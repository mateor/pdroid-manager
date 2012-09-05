package net.digitalfeed.pdroidalternative;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBInterface {
	private static DBHelper dbhelper = null;
	private static DBInterface dbinterface = null;
	
	public static final class ApplicationTable {
		public ApplicationTable(){}
		public static final String TABLE_NAME = "application";
		public static final String COLUMN_NAME_NAME = "name";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_UID = "uid";
		public static final String COLUMN_NAME_VERSION = "version";
		public static final String COLUMN_NAME_PERMISSIONS = "permissions";
		public static final String COLUMN_NAME_ICON = "icon";
		public static final String COLUMN_NAME_FLAGS = "flags";
		
		public static final int FLAG_IS_SYSTEM_APP = 0x1;
		public static final int HAS_INTERNET = 0x2;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_NAME + " TEXT NOT NULL, " +
				COLUMN_NAME_PACKAGENAME + " TEXT NOT NULL, " + 
				COLUMN_NAME_UID + " INTEGER NOT NULL, " +
				COLUMN_NAME_VERSION + " INTEGER NOT NULL, " +
				COLUMN_NAME_PERMISSIONS + " TEXT, " + 
				COLUMN_NAME_ICON + " BLOB, " +
				COLUMN_NAME_FLAGS + "  INTEGER NOT NULL" + 
				");";
	}
	
	public static final class ApplicationStatusTable {
		public ApplicationStatusTable(){}
		public static final String TABLE_NAME = "application_status";
		public static final String COLUMN_NAME_APPLICATION_ID = "application_id";
		public static final String COLUMN_NAME_FLAGS = "flags";

		public static final int FLAGS_UNTRUSTED = 0x1;
		
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				COLUMN_NAME_APPLICATION_ID + " INTEGER, " + 
				COLUMN_NAME_FLAGS + " INTEGER, " + 
				"FOREIGN KEY(" + COLUMN_NAME_APPLICATION_ID + ") REFERENCES " + ApplicationTable.TABLE_NAME + "(_id)" + 
				");";
	}
	
	public static final class ApplicationLogTable {
		public ApplicationLogTable(){}
		public static final String TABLE_NAME = "application_log";
		public static final String COLUMN_NAME_DATETIME = "datetime";
		public static final String COLUMN_NAME_PACKAGENAME = "packageName";
		public static final String COLUMN_NAME_OPERATION = "operation";
		public static final String COLUMN_NAME_FLAGS = "flags";
		public static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + "(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_NAME_DATETIME + " REAL, " + 
				COLUMN_NAME_PACKAGENAME + " TEXT, " + 
				COLUMN_NAME_OPERATION + " TEXT, " +
				COLUMN_NAME_FLAGS + " INTEGER" + 
				");";
	}
	
	public Context context;
	
	public static DBInterface getInstance(Context context) {
		if (dbinterface == null) {
			dbinterface = new DBInterface(context.getApplicationContext());
		}
		return dbinterface; 
	}
	
	private DBInterface(Context context) {
		this.context = context;
	}
	
	public getApplicationList
	
	class DBHelper extends SQLiteOpenHelper {
		public static final String DATABASE_NAME = "pdroidmgr.db";
		public static final int DATABASE_VERSION = 1;
		
		private SQLiteDatabase db;
		
		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			this.db = db;  
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
		
		private void loadDefaultData() {
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
		}
	}
}

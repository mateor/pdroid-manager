package net.digitalfeed.pdroidalternative;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.digitalfeed.pdroidalternative.PermissionSettingHelper.TrustState;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class AppListFragment extends Fragment {

	/*
	 * If you change these numbers, you also need to change Arrays.xml to update
	 * the sequence of text as well
	 */
	public static final int APP_TYPE_USER_OPTION_POSITION = 0;
	public static final int APP_TYPE_SYSTEM_OPTION_POSITION = 1;
	public static final int APP_TYPE_ALL_OPTION_POSITION = 2;
	
	private static final int LONGPRESS_MENU_UPDATE_ALL_SETTINGS = 1;
	private static final int LONGPRESS_MENU_DELETE_SETTINGS = 2;
	
	OnApplicationSelectedListener callback;
	
	public interface OnApplicationSelectedListener {
		public void onApplicationSelected(Application application);
	}
	
	View rootView; // view for the root view element of this listing 
	ListView listView; // view for the list of applications
	AppListAdapter appListAdapter; // array adapter for listView
	ProgressDialog progDialog; // used to store the progress dialog from start to end of AsyncTasks
	//ActionBar actionBar;
	
	Context context;
	
	DBInterface dbInterface;
	Preferences prefs;
	
	List<Application> appList; // array of applications to be displayed by the view (basically, an
						   // array of handles to application objects stored in the applicationObjects hashmap
	HashMap<String, Application> applicationObjects; // stores application objects for all the applications, including
													// those not being displayed. This is loaded in advance so no more objects
													// need to be created when the display filters are changed

	boolean readyForInput = false; // not fully implemented: the idea is to use this to block the interface
								   // from responding during background operations
	
	String currentAppType; // stores the currently displayed app type: Preferences.APPLIST_LAST_APP_TYPE_[USER|SYSTEM|ALL]
	String currentSettingGroup; // title of the current setting group filter
	String settingGroupAllOption; // string of the 'all' option for the setting groups
	List<String> settingGroups; // list of all the setting group titles, used for the setting group spinner
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		
        // Check the container activity implements the callback interface
		// Thank you Google for the example code: https://developer.android.com/training/basics/fragments/communicating.html
        try {
            callback = (OnApplicationSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnApplicationSelectedListener");
        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        //get handle to the application preferences
        prefs = new Preferences(context);
        
      //get the most recently selected option from the 'App Type' list, and set as current
        currentAppType = prefs.getLastAppListType();
        
        /*
         * If the database version has changed, we will need to rebuild the application cache 
         */
        if (prefs.getLastRunDatabaseVersion() != DBInterface.DBHelper.DATABASE_VERSION) {
        	Log.d("PDroidAlternative", "Defined database version has changed since last run; we need to rebuild the cache");
        	//this will force the application list to be rebuilt from the application package manager
        	prefs.setIsApplicationListCacheValid(false); 
        	prefs.setLastRunDatabaseVersion(DBInterface.DBHelper.DATABASE_VERSION);
        }
        
        setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		this.rootView = inflater.inflate(R.layout.activity_main, container);
		this.listView = (ListView)this.rootView.findViewById(R.id.application_list);
		return this.rootView;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onStart () {
		super.onStart();

		//Do we have an application list already? is it valid?
        if (appList == null || !prefs.getIsApplicationListCacheValid()) {
            //Either we don't have an app list, or it isn't valid
        	if (!prefs.getIsApplicationListCacheValid()) {
        		//The app list isn't valid, so we need to rebuild it
	            rebuildApplicationList();
        	} else {
        		loadApplicationObjects();
        		loadApplicationList();
        	}
        } else {
        	if (this.appListAdapter == null) {
        		this.appListAdapter = new AppListAdapter(context, R.layout.application_list_row, this.appList);
        	}
        	listView.setAdapter(appListAdapter);
        }
        
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				callback.onApplicationSelected(appList.get(position));
			}
        });
        
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
        	
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
					long id) {
				showPopupMenu(view, position);
				return true;
			}
        });
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause () {
		super.onPause();
	}

	@Override
	public void onStop () {
		super.onStop();
	}
	
	@Override
	public void onDestroyView () {
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy () {
		super.onDestroy();
	}
	
	@Override
	public void onDetach () {
		super.onDetach();
	}
	
	
	@Override
	public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_main, menu);
        
        /*
         * Get a handle to the application type list spinner, and load the list of options
         * from a resource file. The order of items in this resource file must match the constants
         * (e.g. APP_TYPE_USER_OPTION_POSITION) in this java file.
         */
        MenuItem mSpinner = menu.findItem(R.id.appListMenu_appTypeSpinner);
        final Spinner appTypeSpinner = (Spinner)mSpinner.getActionView();
        final SpinnerAdapter spinnerAdapter = (SpinnerAdapter) ArrayAdapter.createFromResource(context,
				R.array.app_type_selection_options,
				android.R.layout.simple_spinner_dropdown_item);
        appTypeSpinner.setAdapter(spinnerAdapter);
        /*
         * Set the app spinner to be the 'current' option
         */
        if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
        	appTypeSpinner.setSelection(APP_TYPE_USER_OPTION_POSITION);
        } else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
        	appTypeSpinner.setSelection(APP_TYPE_SYSTEM_OPTION_POSITION);
        } else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
        	appTypeSpinner.setSelection(APP_TYPE_ALL_OPTION_POSITION);
        }
        // Add the handler for the spinner option being changed
        appTypeSpinner.setOnItemSelectedListener(new AppTypeSpinnerListener());
		
        
        /*
         * Load the setting group titles and present them in a spinner for filtering
         */
        //TODO: move the query parts of this off into the DBInterface, where they really belong
        // It may be better to create a specific adapter for this, since we are relying on searching by the group titles
        mSpinner = menu.findItem(R.id.appListMenu_filterByGroupSpinner);
        final Spinner groupSpinner = (Spinner)mSpinner.getActionView();
        
        //get a list of all the setting groups from the database
        SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
        Cursor groupNamesCursor = db.rawQuery("SELECT DISTINCT " + DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE + " FROM " + DBInterface.SettingTable.TABLE_NAME + " ORDER BY " + DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE, null);
        if (groupNamesCursor == null || groupNamesCursor.getCount() < 1) {
        	throw new DatabaseUninitialisedException("AppListActivity: No settings groups found.");
        }
        
        //if there is already a list of settings groups (although will this ever happen??)
        //clear it; otherwise initialise an arraylist for it
        if (this.settingGroups == null) {
        	this.settingGroups = new ArrayList<String>(groupNamesCursor.getCount() + 1);
        } else {
        	this.settingGroups.clear();
        }
        this.settingGroupAllOption = context.getResources().getString(R.string.applist_setting_filter_spinner_all_option_title);
        this.settingGroups.add(this.settingGroupAllOption);
        if (groupNamesCursor.moveToFirst()) {
	    	int groupTitleColumnNum = groupNamesCursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
	    	do {
	    		settingGroups.add(groupNamesCursor.getString(groupTitleColumnNum));
	    	} while (groupNamesCursor.moveToNext());
    	}
        
        groupNamesCursor.close();
        //db.close();
        //Create an array adapter for the spinner from the values loaded from the database, and assign to the spinner
        final SpinnerAdapter groupSpinnerAdapter = (SpinnerAdapter) new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, settingGroups);
        groupSpinner.setAdapter(groupSpinnerAdapter);

        //Add handler for when the selected option changes
        groupSpinner.setOnItemSelectedListener(new GroupSpinnerListener());
    }
	
	
	
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	if (readyForInput) {
	    	switch (item.getItemId()) {
	    	case R.id.appListMenuRefresh:
	    		rebuildApplicationList();
	    		break;
	    	}
    	}
    	
	    return false;
    }
    
        
    /**
     * Handles the display of the long-press pop-up menu, which provides 'allow all',
     * 'deny all' options for an application.
     */
    private void showPopupMenu(View view, int position){
    	PopupMenu popupMenu = new PopupMenu(context, view);
    	popupMenu.getMenuInflater().inflate(R.menu.activity_applist_longpress_menu, popupMenu.getMenu());

    	//get the selected Application object from the app list
    	final Application targetApp = appList.get(position);
    	
    	//If the app is already trusted, disable the 'make trusted' option
    	if (targetApp.getHasSettings()) {
    		if (!targetApp.getIsUntrusted()) {
    			popupMenu.getMenu().findItem(R.id.applist_popupmenu_set_trusted_values).setEnabled(false);
    		}
    	} else {
    		//if there are no settings, then disable the 'delete settings' option
    		popupMenu.getMenu().findItem(R.id.applist_popupmenu_delete_settings).setEnabled(false);
    	}
   	
    	// Add handler for when a menu item is selected
    	popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
    		@Override
    		public boolean onMenuItemClick(MenuItem item) {    	    	      
    			TrustState newTrustState = null;
    			
    			int action = 0;
    			switch (item.getItemId()) {
    			case R.id.applist_popupmenu_set_trusted_values:
    				newTrustState = TrustState.TRUSTED;
        			action = LONGPRESS_MENU_UPDATE_ALL_SETTINGS;
    				break;
    			case R.id.applist_popupmenu_set_untrusted_values:
    				newTrustState = TrustState.UNTRUSTED;
        			action = LONGPRESS_MENU_UPDATE_ALL_SETTINGS;
    				break;
    			case R.id.applist_popupmenu_delete_settings:
        			action = LONGPRESS_MENU_DELETE_SETTINGS;
        			break;
    			default:
    				throw new InvalidParameterException();
    			}

    			//display a modal progress dialog: this prevents the user doing anything to the interface
    			//while the actual update is taking place

    			switch (action) {
    			case LONGPRESS_MENU_UPDATE_ALL_SETTINGS:
    				showDialog(null, getString(R.string.applist_dialogtext_updating_settings));

    				//use an asynctask to actually update the settings, so it doesn't interfere with the UI thread
	    			ApplicationsUpdateAllSettingsTask updateAllSettingsTask = new ApplicationsUpdateAllSettingsTask(context, newTrustState, new AppListUpdateAllSettingsCallback());
	    			updateAllSettingsTask.execute(targetApp);
	    			break;
    			case LONGPRESS_MENU_DELETE_SETTINGS:
    				showDialog(null, getString(R.string.applist_dialogtext_deleting_settings));

	    			//use an asynctask to delete settings, so it doesn't interfere with the UI thread
	    			ApplicationsDeleteSettingsTask deleteSettingsTask = new ApplicationsDeleteSettingsTask(context, new AppListUpdateAllSettingsCallback());
	    			deleteSettingsTask.execute(targetApp);
    			}
    			return true;
    		}
    	});

    	popupMenu.show();
	}

    /**
     * Handles callback from AsyncTask used to update all settings. Closes the progress dialog, and
     * triggers update of Listview contents (notifies adapter of change).
     */
    class AppListUpdateAllSettingsCallback implements IAsyncTaskCallback<Void>{
    	@Override
    	public void asyncTaskComplete(Void result) {
    		//TODO: might be worth adding a toast here to notify the settings have been updated?
    		if (progDialog != null) {
    			progDialog.dismiss();
    		}
    		appListAdapter.notifyDataSetChanged(); //notify adapter that the data has changed, so app will update the trusted state in the listview
    	}
    }
    
    /**
     * A generic function used to update a currently displayed progress dialog.
     * 
     * @param currentValue The current value of the progress bar 
     * @param maxValue The maximum value of the progress bar
     */
    private void updateProgressDialog(int currentValue, int maxValue) {
		if (progDialog != null) {
			if (progDialog.isShowing()) {
				progDialog.setProgress(currentValue);
			} else {
				progDialog.setMax(maxValue);
				progDialog.setProgress(currentValue);
				progDialog.show();
			}
		}
    }
    
    /**
     * Uses an asynctask to generate an hashmap of Application objects representing all applications
     * in the database. This hashmap can them be used to get application objects
     * representing specific applications in subsequent searches
     */
    private void loadApplicationObjects() {
    	ApplicationsObjectLoaderTask appListGeneratorTask = new ApplicationsObjectLoaderTask(context, new AppListAppGeneratorCallback());
    	appListGeneratorTask.execute();
    }
    
    /**
     * Callback handler for the AppListAppGenerator AsyncTask. Receives the generated hashmap
     * of (all) Application objects, then calls function to load the applications relevant to the filter.
     */
    class AppListAppGeneratorCallback implements IAsyncTaskCallback<HashMap<String, Application>> {
    	
    	/**
    	 * Updates this with HashMap from AsyncTask, then initiates loading of list of
    	 * applications matching current filter settings
    	 * 
    	 * @param result HashMap of <package name>,<Application object> for all applications
    	 * listed in the database 
    	 */
    	@Override
    	public void asyncTaskComplete(HashMap<String, Application> result) {
    		applicationObjects = result;
    		loadApplicationList();
    	}
    }
    
    
    /**
     * Sets up and executes an AsyncTask to load a list of packagenames from the database
     * which match the current filtering criteria set on the spinners 
     */
    private void loadApplicationList() {
    	
    	//Create query builder to pass the the AsyncTask with the relevant filtering settings
    	AppQueryBuilder queryBuilder = new AppQueryBuilder();
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_PACKAGENAME); //only need the package names to look up in the hashmap
		
		//if the current setting group == the all option, then don't add a filter by group title
		if (this.currentSettingGroup != null && this.currentSettingGroup != this.settingGroupAllOption) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_SETTING_GROUP_TITLE, currentSettingGroup);
		}
		
		if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_TYPE, AppQueryBuilder.APP_TYPE_SYSTEM);
		} else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_TYPE, AppQueryBuilder.APP_TYPE_USER);
		} else if (currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
			//This is just here for the moment for exclusion purposes
		} else {
			Log.d("PDroidAlternative","You shouldn't be here!");
		}

		//Set up and execute the AsyncTask
		ApplicationsDatabaseSearchTask appListLoaderTask = new ApplicationsDatabaseSearchTask(context, new AppListLoaderCallback());
    	appListLoaderTask.execute(queryBuilder);
    }
    
    /**
     * Handles callback on completion of loading the list of packagenames matching
     * the filtering criteria
     */
    class AppListLoaderCallback implements IAsyncTaskCallback<List<String>>{
    	
    	/**
    	 * Clears the list of currently displayed applications, and updates the list
    	 * for display by adding Application objects from the application object Map
    	 * by packageName
    	 * 
    	 * @param result  List of packagenames of applications matching the filtering criteria
    	 */
    	@Override
    	public void asyncTaskComplete(List<String> result) {
    		if (result != null) {
	    		//Clear the current list of applications
	    		if (appList == null) {
	    			appList = new ArrayList<Application>(result.size()); //if the appList is null, initialise it to be the right size
	    		} else {
	    			appList.clear();
	    		}
	    		
	    		//Grab the application object from the cache of application objects, and add it to the list
	    		//for display
	    		for (String packageName : result) {
	    			appList.add(applicationObjects.get(packageName));
	    		}
	    		
	    		//create an AppListAdapter for displaying apps in the list view if not already present,
	    		//otherwise tell the adapter the undelying data set has changed (so it will update the listview)
	        	if (appListAdapter == null) {
	        		appListAdapter = new AppListAdapter(context, R.layout.application_list_row, appList);
	        		listView.setAdapter(appListAdapter);
	    		} else {
	    			//listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, appList));
	    			appListAdapter.notifyDataSetChanged();
	    		}
    		} else {
    			//TODO: Handle the case of no matching apps better: maybe clear the list, or display a
    			//'no matching entries' message of some sort over the top?
    			Log.d("PDroidAlternative","No results from app list load");
    		}
    		readyForInput = true;
    	}
    }

    /**
     * Commence the regeneration of the application list held in the database from the OS
     */
    private void rebuildApplicationList() {
    	showDialog(null, getString(R.string.applist_dialogtext_generateapplist), ProgressDialog.STYLE_HORIZONTAL);

        // Start the AsyncTask to build the list of apps and write them to the database
    	ApplicationsDatabaseFillerTask appListGenerator = new ApplicationsDatabaseFillerTask(context, new AppListGeneratorCallback());
    	appListGenerator.execute();
    }
    
    /**
     * Handles completion of the app list generator
     * Receives the resulting HashMap (which is the Application object cache for use in the view)
     * and adds to the current object before triggering load of applications to display
     */
    class AppListGeneratorCallback implements IAsyncTaskCallbackWithProgress<HashMap<String, Application>>{
    	@Override
    	public void asyncTaskComplete(HashMap<String, Application> returnedAppList) {
    		if (progDialog != null) {
    			progDialog.dismiss();
    		}
    		
    		applicationObjects = returnedAppList; 
    		//set the application cache as valid, so the application data will not be regenerated
    		//on next start
    		prefs.setIsApplicationListCacheValid(true);
    		loadApplicationList(); //trigger loading of package names matching filters for the listview
    	}
    	
    	@Override
    	public void asyncTaskProgressUpdate(Integer... progress) {
    		updateProgressDialog(progress[0], progress[1]);
    	}
    }

    
    class AppTypeSpinnerListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int itemPosition,
				long id) {
			//need to be able to disable this, just in case there is a time when
			//it is possible to change the entry, but there is already an operation underway
			if (readyForInput) {
				
	 			switch (itemPosition) {
				case APP_TYPE_USER_OPTION_POSITION:
					if (!currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
	        			prefs.setLastAppListType(Preferences.APPLIST_LAST_APP_TYPE_USER);
						currentAppType = Preferences.APPLIST_LAST_APP_TYPE_USER;
						loadApplicationList();
					}
					break;
				case APP_TYPE_SYSTEM_OPTION_POSITION:
					if (!currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
						prefs.setLastAppListType(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM);
						currentAppType = Preferences.APPLIST_LAST_APP_TYPE_SYSTEM;
						loadApplicationList();
					}
					break;
				case APP_TYPE_ALL_OPTION_POSITION:
					if (!currentAppType.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
						prefs.setLastAppListType(Preferences.APPLIST_LAST_APP_TYPE_ALL);
						currentAppType = Preferences.APPLIST_LAST_APP_TYPE_ALL;
						loadApplicationList();
					}
					break;
				}
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> view) {
			// TODO Auto-generated method stub
			
		}
    };
    
    class GroupSpinnerListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int itemPosition,
				long id) {
			if (readyForInput) {        		
				currentSettingGroup = settingGroups.get(itemPosition);
				loadApplicationList();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> view) {
			// TODO Auto-generated method stub
		}
    };
    
    
    /**
     * Helper to show a non-cancellable spinner progress dialog
     * 
     * @param title  Title for the progress dialog (or null for none)
     * @param message  Message for the progress dialog (or null for none)
     * @param type  ProgressDialog.x for the type of dialog to be displayed
     */
	private void showDialog(String title, String message, int type) {
		if (this.progDialog != null && this.progDialog.isShowing()) {
			this.progDialog.dismiss();
		}
		this.progDialog = new ProgressDialog(context);
		this.progDialog.setProgressStyle(type);
		if (title != null) {
			progDialog.setTitle(title);
		}
		if (message != null) {
			progDialog.setMessage(message);
		}
    	progDialog.setCancelable(false);
    	progDialog.show();
	}
	
	private void showDialog(String title, String message) {
		showDialog(title, message, ProgressDialog.STYLE_SPINNER);
	}
	
}

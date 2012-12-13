package net.digitalfeed.pdroidalternative;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.digitalfeed.pdroidalternative.PermissionSettingHelper.TrustState;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

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
	private static final int LONGPRESS_MENU_MODIFY_ALLSETTINGS = 3;
	
	private static final int FILTER_APPTYPE = 1;
	private static final int FILTER_GROUP = 2;
	private static final int FILTER_STATUS = 4;
	
	// Used to specify which, if any, type of progress dialog should appear on start
	private static final int DIALOG_NONE = 0;
	private static final int DIALOG_LINEAR = 1;
	
	private static final String PARCEL_LISTPOSITION = "listPosition";
	
	OnApplicationSelectedListener callback;
	
	public interface OnApplicationSelectedListener {
		public void onApplicationSelected(Application application);
		public void onBatchCommence(String [] packageNames);
	}
	
	View rootView; // view for the root view element of this listing 
	ListView listView; // view for the list of applications
	Integer listPosition = null; // first visible position of the list 
	View filterView; // view for the layout containing the filters
	View filterSummaryView; // view for the 'filters in use' text layout
	TextView filterSummaryText; // text in the 'filter summary' pane
	
	Spinner appTypeSpinner;
	Spinner groupSpinner;
	
	AppListAdapter appListAdapter; // array adapter for listView
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
	
	int showDialogOnStart = DIALOG_NONE;
	
	String currentAppTypeFilter; // stores the currently displayed app type: Preferences.APPLIST_LAST_APP_TYPE_[USER|SYSTEM|ALL]
	String currentSettingGroup; // title of the current setting group filter
	String currentStatusFilter; // not used yet
	String settingGroupAllOption; // string of the 'all' option for the setting groups
	List<String> settingGroups; // list of all the setting group titles, used for the setting group spinner
	List<String> appTypeOptions; // list of all the app type options, for the app type spinner
	int currentFilterBits;
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnAttach");
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
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnCreate");
		
        //get handle to the application preferences
        prefs = new Preferences(context);
        
      //get the most recently selected option from the 'App Type' list, and set as current
        currentAppTypeFilter = prefs.getLastAppListTypeFilter();
        if (currentAppTypeFilter == null) {
        	currentAppTypeFilter = Preferences.APPLIST_LAST_APP_TYPE_ALL;
        } else if (!currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
        	currentFilterBits |= FILTER_APPTYPE;
        }
        
        // because setting group text varies between languages, it is
        // harder to detect the 'all' option
        currentSettingGroup = prefs.getLastSettingGroupFilter();
        if (currentSettingGroup == null) {
        	currentSettingGroup = context.getResources().getString(R.string.applist_setting_filter_spinner_all_option_title);
        } else if (!currentSettingGroup.equals(context.getResources().getString(R.string.applist_setting_filter_spinner_all_option_title))) {
        	currentFilterBits |= FILTER_GROUP;
        }
        
        /*
         * If the database version has changed, we will need to rebuild the application cache 
         */
        if (prefs.getLastRunDatabaseVersion() != DBInterface.DBHelper.DATABASE_VERSION) {
        	if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Defined database version has changed since last run; we need to rebuild the cache");
        	//this will force the application list to be rebuilt from the application package manager
        	prefs.setIsApplicationListCacheValid(false); 
        	prefs.setLastRunDatabaseVersion(DBInterface.DBHelper.DATABASE_VERSION);
        }
        
        setHasOptionsMenu(true);
        
		//Do we have an application list already? is it valid?
        if (appList == null || !prefs.getIsApplicationListCacheValid()) {
            //Either we don't have an app list, or it isn't valid
        	if (!prefs.getIsApplicationListCacheValid()) {
        		//The app list isn't valid, so we need to rebuild it
	            rebuildApplicationList();
        	} else {
        		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:appList == null: reloading application objects");
        		loadApplicationObjects();
        		loadApplicationList();
        	}
        }
        
        if (savedInstanceState != null) {
        	listPosition = savedInstanceState.getInt(PARCEL_LISTPOSITION);
        	if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Saved instance state exists in onCreate");
        }
	}
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnCreateView");
		this.rootView = inflater.inflate(R.layout.activity_main, container);
		this.listView = (ListView)this.rootView.findViewById(R.id.application_list);
		this.filterView = this.rootView.findViewById(R.id.application_list_filter);
		this.filterSummaryView = this.rootView.findViewById(R.id.application_list_filter_summary);
		this.filterSummaryText = (TextView)this.filterSummaryView.findViewById(R.id.application_list_filter_summary_text);
		ImageButton clearFilterButton = (ImageButton)this.filterSummaryView.findViewById(R.id.application_list_filter_clear);
		clearFilterButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				clearFilters();
				updateFilterSummary();
			}
		});
		
		return this.rootView;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnActivityCreated");
	}
	
	@Override
	public void onViewCreated (View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnViewCreated");
		loadSpinners();
		updateFilterSummary();
		
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				callback.onApplicationSelected(appList.get(position));
			}
        });
        
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new ModeCallback());

	}

	@Override
	public void onStart () {
		super.onStart();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnStart");

		/*
		case DIALOG_LINEAR:
			DialogHelper.showProgressDialog(context, null, getString(R.string.applist_dialogtext_generateapplist), ProgressDialog.STYLE_HORIZONTAL);
		case DIALOG_NONE:
			DialogHelper.dismissProgressDialog();
		}*/
		
		// if the application list is loaded, and the listView has been build (which it should have)
		// but the adapter has not been set, we should set it now.
		// (If this is the case, then the asynctask finished before onStart)
		if (listView != null && this.appList != null && listView.getAdapter() == null) {
    		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:setting appListAdapter in onStart");
    		listView.setAdapter(appListAdapter);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnResume");
	}
	
	@Override
	public void onPause () {
		super.onPause();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnPause");
	}

	@Override
	public void onStop () {
		super.onStop();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnStop");
	}
	
	@Override
	public void onDestroyView () {
		super.onDestroyView();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG,"onDestroyView called");
	}
	
	@Override
	public void onDestroy () {
		super.onDestroy();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnDestroy");
	}
	
	@Override
	public void onDetach () {
		super.onDetach();
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:OnDetach");
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
	    super.onSaveInstanceState(state);
	    if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:onSaveInstanceState");
	    listPosition = listView.getFirstVisiblePosition();
	    state.putInt(PARCEL_LISTPOSITION, listPosition);
	}
	
	
	@Override
	public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:onCreateOptionsMenu");
        inflater.inflate(R.menu.activity_main, menu);
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:onOptionsItemSelected");
    	super.onOptionsItemSelected(item);
    	if (readyForInput) {
	    	switch (item.getItemId()) {
	    	case R.id.appListMenuRefresh:
	    		rebuildApplicationList();
	    		return true;
	    	case R.id.appListMenuFilter:
	    		toggleFilterVisibility();
	    		return true;
	    	}
    	}
    	
	    return false;
    }
    

    /**
     * Handles callback from AsyncTask used to update all settings. Closes the progress dialog, and
     * triggers update of Listview contents (notifies adapter of change).
     */
    class UpdateAllSettingsCallback implements IAsyncTaskCallback<Void>{
    	@Override
    	public void asyncTaskComplete(Void result) {
    		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:AppListUpdateAllSettingsCallback:asyncTaskComplete");
    		//TODO: might be worth adding a toast here to notify the settings have been updated?
    		DialogHelper.dismissProgressDialog();
    		appListAdapter.notifyDataSetChanged(); //notify adapter that the data has changed, so app will update the trusted state in the listview
    		
    	}
    }
    
    
    /**
     * Uses an asynctask to generate an hashmap of Application objects representing all applications
     * in the database. This hashmap can them be used to get application objects
     * representing specific applications in subsequent searches
     */
    private void loadApplicationObjects() {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:loadApplicationObjects");
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
    		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListAppGeneratorCallback:asyncTaskComplete");
    		applicationObjects = result;
    		loadApplicationList();
    	}
    }
    
    
    /**
     * Sets up and executes an AsyncTask to load a list of packagenames from the database
     * which match the current filtering criteria set on the spinners 
     */
    private void loadApplicationList() {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:loadApplicationList");
    	//Create query builder to pass the the AsyncTask with the relevant filtering settings
    	AppQueryBuilder queryBuilder = new AppQueryBuilder();
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_PACKAGENAME); //only need the package names to look up in the hashmap
		
		//if the current setting group == the all option, then don't add a filter by group title
		if (this.currentSettingGroup != null && !this.currentSettingGroup.equals(this.settingGroupAllOption)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_SETTING_GROUP_TITLE, currentSettingGroup);
		}
		
		if (currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_TYPE, AppQueryBuilder.APP_TYPE_SYSTEM);
		} else if (currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
			queryBuilder.addFilter(AppQueryBuilder.FILTER_BY_TYPE, AppQueryBuilder.APP_TYPE_USER);
		} else if (currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
			//This is just here for the moment for exclusion purposes
		} else {
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"You shouldn't be here!");
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
    		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:AppListLoaderCallback:asyncTaskComplete");

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

    		} else {
    			//TODO: Handle the case of no matching apps better: maybe clear the list, or display a
    			//'no matching entries' message of some sort over the top?
    			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"No results from app list load");
    			if (appList == null) {
    				appList = new ArrayList<Application>(0);
    			} else {
    				appList.clear();
    			}
    		}
    		if (listPosition != null) {
    			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "List position is: " + listPosition.toString());
    		} else {
    			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "List position is null");
    		}
    		//create an AppListAdapter for displaying apps in the list view if not already present,
    		//otherwise tell the adapter the undelying data set has changed (so it will update the listview)	    		
    		if (appListAdapter == null) {
    			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "AppListAdapter == null");
        		appListAdapter = new AppListAdapter(context, R.layout.application_list_row, appList);
        		//if the listView is not null, then we can set the adapter; otherwise, need to delay this
	    		if (listView != null) {
	    			listView.setAdapter(appListAdapter);
	    			if (listPosition != null) {
	    				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "listPosition != null");
	    				if (listPosition < appList.size()) {
	    					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "listPosition < appList.size()");
	    					listView.setSelection(listPosition);
	    					listPosition = null;
	    				}
	    			}
	    		}
    		} else {
    			appListAdapter.notifyDataSetChanged();
    			if (listPosition != null) {
    				if (listPosition < appList.size()) {
    					listView.setSelection(listPosition);
    					listPosition = null;
    				}
    			}
    		}
    		
    		readyForInput = true;
    	}
    }

    /**
     * Commence the regeneration of the application list held in the database from the OS
     */
    private void rebuildApplicationList() {
    	if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:rebuildApplicationList");
    	DialogHelper.showProgressDialog(context, null, getString(R.string.applist_dialogtext_generateapplist), ProgressDialog.STYLE_HORIZONTAL);
    	//showDialogOnStart = DIALOG_LINEAR;

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
    		if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:AppListGeneratorCallback:asyncTaskComplete");
                DialogHelper.dismissProgressDialog();
    		applicationObjects = returnedAppList; 
    		//set the application cache as valid, so the application data will not be regenerated
    		//on next start
    		prefs.setIsApplicationListCacheValid(true);
    		loadApplicationList(); //trigger loading of package names matching filters for the listview
    	}
    	
    	@Override
    	public void asyncTaskProgressUpdate(Integer... progress) {
    		DialogHelper.updateProgressDialog(progress[0], progress[1]);
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
					if (!currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
	        			prefs.setLastAppListTypeFilter(Preferences.APPLIST_LAST_APP_TYPE_USER);
						currentAppTypeFilter = Preferences.APPLIST_LAST_APP_TYPE_USER;
						currentFilterBits |= FILTER_APPTYPE;
						loadApplicationList();
					}
					break;
				case APP_TYPE_SYSTEM_OPTION_POSITION:
					if (!currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
						prefs.setLastAppListTypeFilter(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM);
						currentAppTypeFilter = Preferences.APPLIST_LAST_APP_TYPE_SYSTEM;
						currentFilterBits |= FILTER_APPTYPE;
						loadApplicationList();
					}
					break;
				case APP_TYPE_ALL_OPTION_POSITION:
					if (!currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
						prefs.setLastAppListTypeFilter(Preferences.APPLIST_LAST_APP_TYPE_ALL);
						currentAppTypeFilter = Preferences.APPLIST_LAST_APP_TYPE_ALL;
						currentFilterBits &= ~FILTER_APPTYPE;
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
				prefs.setLastSettingGroupFilter(currentSettingGroup);
				if (currentSettingGroup == settingGroupAllOption) {
					currentFilterBits &= ~FILTER_GROUP;
				} else {
					currentFilterBits |= FILTER_GROUP;
				}
				loadApplicationList();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> view) {
			// TODO Auto-generated method stub
		}
    }
    
    private class ModeCallback implements ListView.MultiChoiceModeListener {
    	
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.application_list_multiselect_menu, menu); 
            mode.setTitle("Select apps");
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "AppListFragment:ModeCallback:onActionItemClicked");
			TrustState newTrustState = null;
			int action = 0;
			
            switch (item.getItemId()) {
            case R.id.application_list_multiselect_set_trusted:
				newTrustState = TrustState.TRUSTED;
    			action = LONGPRESS_MENU_UPDATE_ALL_SETTINGS;
                break;
            case R.id.application_list_multiselect_set_untrusted:
				newTrustState = TrustState.UNTRUSTED;
    			action = LONGPRESS_MENU_UPDATE_ALL_SETTINGS;
                break;
            case R.id.application_list_multiselect_set_delete_settings:
    			action = LONGPRESS_MENU_DELETE_SETTINGS;
                break;
            case R.id.application_list_multiselect_set_all_settings:
            	action = LONGPRESS_MENU_MODIFY_ALLSETTINGS;
            	break;
            case R.id.application_list_multiselect_select_all:
            	for ( int i=0; i< appListAdapter.getCount(); i++ ) {
            	        listView.setItemChecked(i, true);
            	}
            	return true;
            /*case R.id.application_list_multiselect_deselect_all:
            	for ( int i=0; i< appListAdapter.getCount(); i++ ) {
        	        listView.setItemChecked(i, false);
        	}
            	break;*/
            default:
                return false;
            }
            
            LinkedList<Application> checkedApps = new LinkedList<Application>();;
            SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
            if (checkedItems == null) {
            	return false;
            }
            for (int position = 0; position < checkedItems.size(); position++) {
                if (checkedItems.valueAt(position)) {
                	if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,
                			"AppListFragment:ModeCallback:onActionItemClicked: position is checked " + Integer.toString(checkedItems.keyAt(position)));

                    checkedApps.add(appList.get(checkedItems.keyAt(position)));
                }
            }
            if (checkedApps.size() == 0) {
            	return false;
            }
            
            Application [] checkedAppsArray;
            
			switch (action) {
			case LONGPRESS_MENU_UPDATE_ALL_SETTINGS:
				DialogHelper.showProgressDialog(context, null, getString(R.string.applist_dialogtext_updating_settings));

				checkedAppsArray = checkedApps.toArray(new Application [checkedApps.size()]);
				//use an asynctask to actually update the settings, so it doesn't interfere with the UI thread
    			ApplicationsUpdateAllSettingsTask updateAllSettingsTask = new ApplicationsUpdateAllSettingsTask(context, newTrustState, new UpdateAllSettingsCallback());
    			updateAllSettingsTask.execute(checkedAppsArray);
    			mode.finish();
    			break;
			case LONGPRESS_MENU_DELETE_SETTINGS:
				DialogHelper.showProgressDialog(context, null, getString(R.string.applist_dialogtext_deleting_settings));

				checkedAppsArray = checkedApps.toArray(new Application [checkedApps.size()]);
    			//use an asynctask to delete settings, so it doesn't interfere with the UI thread
    			ApplicationsDeleteSettingsTask deleteSettingsTask = new ApplicationsDeleteSettingsTask(context, new UpdateAllSettingsCallback());
    			deleteSettingsTask.execute(checkedAppsArray);
    			mode.finish();
    			break;
			case LONGPRESS_MENU_MODIFY_ALLSETTINGS:
				
				String [] packageNames = new String [checkedApps.size()];
				int packageNum = 0;
				for (Application app : checkedApps) {
					packageNames[packageNum] = app.getPackageName();
					packageNum++;
				}
				callback.onBatchCommence(packageNames);
			}
			return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }

        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
            final int checkedCount = listView.getCheckedItemCount();
            
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(getString(R.string.applist_actionbar_batchselect_none));
                    break;
                case 1:
                    mode.setSubtitle(getString(R.string.applist_actionbar_batchselect_singular));
                    break;
                default:
                    mode.setSubtitle(Integer.toString(checkedCount) + " " + getString(R.string.applist_actionbar_batchselect_plural));
                    break;
            }
        }
    }
    
    private void setFilterVisibility(int newVisibilityState) {
    	if (this.filterView != null) {
    		switch (newVisibilityState) {
    		case View.GONE:
    			this.filterView.setVisibility(View.GONE);
    			break;
    		case View.VISIBLE:
    			this.filterView.setVisibility(View.VISIBLE);
    			break;
    		default:
    			throw new InvalidParameterException("Invalid visibility state");
    		}
    	}
    }
    
    private void toggleFilterVisibility() {
    	if (this.filterView != null) {
			if (this.filterView.getVisibility() == View.GONE) {
				this.filterView.setVisibility(View.VISIBLE);
			} else {
				this.filterView.setVisibility(View.GONE);
			}
			updateFilterSummary();
    	}
    }
    
    private void loadSpinners() {
        /*
         * Get a handle to the application type list spinner, and load the list of options
         * from a resource file. The order of items in this resource file must match the constants
         * (e.g. APP_TYPE_USER_OPTION_POSITION) in this java file.
         */
        appTypeSpinner = (Spinner)rootView.findViewById(R.id.appListMenu_appTypeSpinner);
        appTypeOptions = Arrays.asList(getResources().getStringArray(R.array.app_type_selection_options));
        
        final SpinnerAdapter spinnerAdapter = (SpinnerAdapter) new ArrayAdapter<String>(context,
        		android.R.layout.simple_spinner_dropdown_item,
        		appTypeOptions);

        //final SpinnerAdapter spinnerAdapter = (SpinnerAdapter) ArrayAdapter.createFromResource(context,
//				getResources().getStringArray(R.array.app_type_selection_options),
//				android.R.layout.simple_spinner_dropdown_item);
        appTypeSpinner.setAdapter(spinnerAdapter);
        /*
         * Set the app spinner to be the 'current' option
         */
        if (currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_USER)) {
        	appTypeSpinner.setSelection(APP_TYPE_USER_OPTION_POSITION);
        } else if (currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_SYSTEM)) {
        	appTypeSpinner.setSelection(APP_TYPE_SYSTEM_OPTION_POSITION);
        } else if (currentAppTypeFilter.equals(Preferences.APPLIST_LAST_APP_TYPE_ALL)) {
        	appTypeSpinner.setSelection(APP_TYPE_ALL_OPTION_POSITION);
        }
        // Add the handler for the spinner option being changed
        appTypeSpinner.setOnItemSelectedListener(new AppTypeSpinnerListener());
		
        
        /*
         * Load the setting group titles and present them in a spinner for filtering
         */
        //TODO: move the query parts of this off into the DBInterface, where they really belong
        // It may be better to create a specific adapter for this, since we are relying on searching by the group titles
        groupSpinner = (Spinner)rootView.findViewById(R.id.appListMenu_filterByGroupSpinner);
        
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
        int selectedOptionPosition = 0;
        if (groupNamesCursor.moveToFirst()) {
	    	int groupTitleColumnNum = groupNamesCursor.getColumnIndex(DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
	    	do {
	    		settingGroups.add(groupNamesCursor.getString(groupTitleColumnNum));
	    		if (groupNamesCursor.getString(groupTitleColumnNum).equals(currentSettingGroup)) {
	    			selectedOptionPosition = groupNamesCursor.getPosition();
	    		}
	    	} while (groupNamesCursor.moveToNext());
    	}
        
        groupNamesCursor.close();
        //db.close();
        //Create an array adapter for the spinner from the values loaded from the database, and assign to the spinner
        final SpinnerAdapter groupSpinnerAdapter = (SpinnerAdapter) new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, settingGroups);
        groupSpinner.setAdapter(groupSpinnerAdapter);
        groupSpinner.setSelection(selectedOptionPosition);

        //Add handler for when the selected option changes
        groupSpinner.setOnItemSelectedListener(new GroupSpinnerListener());
    }
    
    private void updateFilterSummary() {
    	//if the actual filtering options are visible, the filter summary should not be 
    	if (filterView.getVisibility() == View.VISIBLE) {
    		filterSummaryView.setVisibility(View.GONE);
    		return;
    	}
    	List<String> filters = new LinkedList<String>();
    	if (0 != (this.currentFilterBits & FILTER_APPTYPE)) {
    		filters.add("app type = " + currentAppTypeFilter);
    	}
    	if (0 != (this.currentFilterBits & FILTER_GROUP)) {
    		filters.add("group = " + currentSettingGroup);
    	}
    	if (0 != (this.currentFilterBits & FILTER_STATUS)) {
    		filters.add("status = " + currentStatusFilter);
    	}
    	
    	if (filters.size() > 0) {
    		this.filterSummaryText.setText(TextUtils.join(getString(R.string.detail_custom_value_spacer), filters));
    	}
    	
    	if (this.currentFilterBits != 0) {
    		filterSummaryView.setVisibility(View.VISIBLE);
    	} else {
    		filterSummaryView.setVisibility(View.GONE);
    	}
    }
    
    private void clearFilters() {
    	currentFilterBits = 0;
    	currentSettingGroup = settingGroupAllOption;
    	prefs.setLastSettingGroupFilter(currentSettingGroup);
    	groupSpinner.setSelection(settingGroups.indexOf(settingGroupAllOption));
    	appTypeSpinner.setSelection(APP_TYPE_ALL_OPTION_POSITION);
    	currentAppTypeFilter = Preferences.APPLIST_LAST_APP_TYPE_ALL;
    	prefs.setLastAppListTypeFilter(Preferences.APPLIST_LAST_APP_TYPE_ALL);
    	loadApplicationList();
    }
}

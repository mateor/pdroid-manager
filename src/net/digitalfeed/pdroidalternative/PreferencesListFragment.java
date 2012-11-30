package net.digitalfeed.pdroidalternative;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class PreferencesListFragment extends ListFragment {

	private static final int ITEM_TYPE_HEADING = 0;
	private static final int ITEM_TYPE_CONTENT = 1;
	private static final int ITEM_TYPE_COUNT = 2;

	private static final String PDROIDMANAGER_XDA_THREAD_URL = "http://forum.xda-developers.com/showthread.php?p=34190204";
	
	Context context;
	Preferences prefs;
	PreferencesAdapter adapter;
	private final List<BasePreference> preferences = new ArrayList<BasePreference>(10);
	
	public interface ListItemClickListener {
		public void onListItemClick (ListView l, View v, int position, long id);
	}
	
	public PreferencesListFragment() {}
	
	private static class BasePreference {
		public String title;
		public BasePreference(String title) {
			this.title = title;
		}
	}
	private static class PreferenceHeader extends BasePreference {
		public PreferenceHeader(String title) {
			super(title);
		}}
	private static class Preference extends BasePreference {
		public String summary;
		public Drawable icon;
		public ListItemClickListener clickListener;
		
		public Preference(String title, String summary, Drawable icon, ListItemClickListener clickListener) {
			super(title);
			this.summary = summary;
			this.icon = icon;
			this.clickListener = clickListener;
		}
		
		public Preference(String title, String summary) {
			this(title, summary, null, null);
		}
	}
	
	/**
	 * Adapter for the 'preferences' list - i.e. the configuration options for the app.
	 * Note that many of those are not actually preferences as such - i.e. not
	 * shared preferences. The goal is to mimic that look for the settings screen, though.
	 * 
	 * @author smorgan

	 */
	public class PreferencesAdapter extends BaseAdapter {
		
		@Override
		public int getCount() {
			return preferences.size();
		}

		@Override
		public Object getItem(int position) {
			return preferences.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return ITEM_TYPE_COUNT;
		}

		@Override
		public int getItemViewType(int position) {
			return (preferences.get(position) instanceof PreferenceHeader) ? ITEM_TYPE_HEADING
					: ITEM_TYPE_CONTENT;
		}

		@Override
		public boolean isEnabled(int position) {
			// A separator cannot be clicked
			if (preferences.get(position) instanceof PreferenceHeader) {
				return false;
			}
			if (((Preference)preferences.get(position)).clickListener == null) {
				return false;
			}
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final int type = getItemViewType(position);

			// First, let's create a new convertView if needed. You can also
			// create a ViewHolder to speed up changes if you want ;)
			if (convertView == null) {
				convertView = LayoutInflater
						.from(context)
						.inflate(
								type == ITEM_TYPE_HEADING ? R.layout.preference_heading
										: R.layout.preference_item, parent,
								false);
			}

			// We can now fill the list item view with the appropriate data.
			if (type == ITEM_TYPE_HEADING) {
				final PreferenceHeader preference = (PreferenceHeader) getItem(position);
				((TextView) convertView.findViewById(R.id.title)).setText(preference.title);
			} else {
				final Preference preference = (Preference) getItem(position);
				((TextView) convertView.findViewById(R.id.title)).setText(preference.title);
				((TextView) convertView.findViewById(R.id.summary)).setText(preference.summary);
			}

			return convertView;
		}
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		if (preferences.get(position) instanceof Preference
				&& ((Preference)preferences.get(position)).clickListener != null) {
			((Preference)preferences.get(position)).clickListener.onListItemClick(l, v, position, id);
			return;
		}
	}
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		this.prefs = new Preferences(context);
		
		preferences.add(
				new PreferenceHeader(getString(R.string.preferences_heading_about))
				);
		preferences.add(
				new Preference(getString(R.string.preferences_about_title),
						getString(R.string.preferences_about_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showAboutDialog();
							}
						})
				);
		preferences.add(
				new Preference(getString(R.string.preferences_credits_title),
						getString(R.string.preferences_credits_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showCreditsDialog();
							}
						})
				);
		preferences.add(
				new Preference(
						getString(R.string.preferences_openxdathread_title),
						getString(R.string.preferences_openxdathread_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								openXDA();
							}
						})
				);
		preferences.add(
				new PreferenceHeader("Language")
				);
		preferences.add(
				new Preference(
						getString(R.string.preferences_switchlanguage_title),
						getString(R.string.preferences_switchlanguage_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showLanguageSelector();
							}
						})
				);
		//if (this.prefs.getForcedLanguage() != null) {
			preferences.add(
					new Preference(
							getString(R.string.preferences_usephonelocale_title),
							getString(R.string.preferences_usephonelocale_summary),
							null,
							new ListItemClickListener() {
								@Override
								public void onListItemClick (ListView l, View v, int position, long id) {
									prefs.clearForcedLanguage();
									LanguageHelper.updateLanguageIfRequired(context);
									// TODO: The option to 'clear forced language' should be removed
									// when this is selected, or at least disabled
								}
							})
					);

		//}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setHasOptionsMenu(false); // TODO: Set up the options menu!
        this.adapter = new PreferencesAdapter(); 
		setListAdapter(adapter);
    }
    
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.preferences_list_view, container);
	}

    @Override
    public void onStart() {
    	super.onStart();
    }
    
    
    /**
     * Open the PDroid Manager thread in XDA-developers
     */
    private void openXDA() {
    	final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(PDROIDMANAGER_XDA_THREAD_URL));
    	context.startActivity(intent);
    }
    
    /**
     * Displays a dialog with the credits for the application
     */
    private void showCreditsDialog() {
    	Log.d("PDroidAlternative","Should be showing the dialog right now!");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = InformationDialog.newInstance(
        		getString(R.string.credits_dialog_title),
        		getString(R.string.credits_dialog_body));
        
        newFragment.show(ft, "dialog");
    }
    
    /**
     * Displays a dialog with the credits for the application
     */
    private void showAboutDialog() {
    	Log.d("PDroidAlternative","Should be showing the dialog right now!");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = InformationDialog.newInstance(
        		getString(R.string.about_dialog_title),
        		getString(R.string.about_dialog_body));
        
        newFragment.show(ft, "dialog");
    }
    
    /**
     * Shows an alert dialog with a list of options to choose from
     */
    private void showLanguageSelector() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        AlertDialogFragment newFragment = AlertDialogFragment.newInstance();        
        newFragment.show(ft, "dialog");
    }
    
    
    public static class InformationDialog extends DialogFragment {
    	
        static InformationDialog newInstance(String title, String body) {
        	if (title == null || body == null) {
        		throw new InvalidParameterException("Title and body cannot be null");
        	}
            InformationDialog infoDialog = new InformationDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("body", body);
            infoDialog.setArguments(args);
            return infoDialog;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	getDialog().setTitle(getArguments().getString("title"));
        	getDialog().setCanceledOnTouchOutside(true); 
        	//getDialog().setTitle(getString(R.string.credits_dialog_title));
            View view = inflater.inflate(R.layout.preferences_credits_dialog, container, false);
            Button closeButton = (Button)view.findViewById(R.id.credits_dialog_close_button);
            closeButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					closeDialog();
				}
			});
            
            TextView body = (TextView)view.findViewById(R.id.credits_dialog_body);
            body.setText(Html.fromHtml(getArguments().getString("body")));
            //Thank you stackoverflow: need to do the setMovementMethod for links to work in TextView.
            //http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
            body.setMovementMethod(LinkMovementMethod.getInstance());

            return view;
        }
        
        
        private void closeDialog() {
        	this.dismiss();
        }
    }
    
    //String[] bases = getResources().getStringArray(R.array.image_export_list_preference);
    
    public static class AlertDialogFragment extends DialogFragment {

        public static AlertDialogFragment newInstance() {
            return new AlertDialogFragment();
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    //.setIcon(R.drawable.alert_dialog_icon)
                    //.setTitle(title)
        	
        	builder.setItems(R.array.language_titles, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							forceLanguage(which);
							LanguageHelper.updateLanguageIfRequired(getActivity());
							closeDialog();
						}
            		});
            return builder.create();
        }
        
        private void forceLanguage(int languageIndex) {
        	Preferences prefs = new Preferences(getActivity());
        	String [] languageCodes = getResources().getStringArray(R.array.language_codes);
        	prefs.setForcedLanguage(languageCodes[languageIndex]);
        }

        
        private void closeDialog() {
        	this.dismiss();
        }
    }    
}

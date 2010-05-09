/**
 *     Copyright (C) 2009 Anders Aagaard <aagaande@gmail.com>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neuron.trafikanten.views.realtime;

import java.util.ArrayList;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenDevi;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.hacks.StationIcons;
import com.neuron.trafikanten.notification.NotificationDialog;
import com.neuron.trafikanten.tasks.SelectDeviTask;
import com.neuron.trafikanten.tasks.ShowDeviTask;
import com.neuron.trafikanten.tasks.ShowRealtimeLineDetails;

public class RealtimeView extends ListActivity {
	private static final String TAG = "Trafikanten-RealtimeView";
	public static final String SETTING_HIDECA = "realtime_hideCaText";
	private static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_DEVILIST = "devilist";
	private static final String KEY_FINISHEDLOADING = "finishedLoading";
	private static final String KEY_TIMEDIFFERENCE = "timeDifference";
	/*
	 * Options menu:
	 */
	private static final int REFRESH_ID = Menu.FIRST;
	private static final int HIDECA_ID = Menu.FIRST + 1;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	private static final int DEVI_ID = Menu.FIRST + 1;
	
	/*
	 * Dialogs
	 */
	private static final int DIALOG_NOTIFICATION = 1;
	private int selectedId = 0;
	
	/*
	 * Saved instance data
	 */
	// HACK STATIONICONS, this does not need to be public static
	public static StationData station;
	private RealtimeAdapter realtimeList;
	private long lastUpdate;
	private ArrayList<DeviData> deviItems;
	private boolean finishedLoading = false; // we're finishedLoading when devi has loaded successfully.
	public long timeDifference = 0; // This is the time desync between system clock and trafikanten servers.
	
	/*
	 * UI
	 */
	private LinearLayout devi;
	private TextView infoText;
	private TextView caText;
	
	/*
	 * Data providers
	 */
	private TrafikantenRealtime realtimeProvider = null;
	private TrafikantenDevi deviProvider = null;
	
	/*
	 * Other
	 */
    public SharedPreferences settings;
    public Typeface departuresTypeface;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        /*
         * Setup view and adapter.
         */
        setContentView(R.layout.realtime);
        realtimeList = new RealtimeAdapter(this);
        devi = (LinearLayout) findViewById(R.id.devi);
		infoText = (TextView) findViewById(R.id.emptyText);
		caText = (TextView) findViewById(R.id.caInfoText);
		settings = getSharedPreferences("trafikanten", MODE_PRIVATE);
		departuresTypeface = Typeface.createFromAsset(getAssets(), "fonts/DejaVuSans.ttf");
        		
        /*
         * Load instance state
         */
        if (savedInstanceState == null) {
        	Bundle bundle = getIntent().getExtras();
        	/*
        	 * Most of the time we get a normal StationData.PARCELABLE, but shortcuts sends a simple bundle.
        	 */
        	if (bundle.containsKey(StationData.PARCELABLE)) {
        		station = getIntent().getParcelableExtra(StationData.PARCELABLE);
        	} else {
        		station = StationData.readSimpleBundle(bundle);
        	}
        	load();
        } else {
        	station = savedInstanceState.getParcelable(StationData.PARCELABLE);
        	lastUpdate = savedInstanceState.getLong(KEY_LAST_UPDATE);
        	deviItems = savedInstanceState.getParcelableArrayList(KEY_DEVILIST);
        	finishedLoading = savedInstanceState.getBoolean(KEY_FINISHEDLOADING);
        	timeDifference = savedInstanceState.getLong(KEY_TIMEDIFFERENCE);
        	
        	realtimeList.loadInstanceState(savedInstanceState);
        	realtimeList.notifyDataSetChanged();
        	infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
        	
        	if (!finishedLoading) {
    			load();
        	}
        }

        registerForContextMenu(getListView());
        setListAdapter(realtimeList);
        refreshTitle();
        refreshDevi();
    }
    
    private void stopProviders() {
    	if (realtimeProvider != null) {
    		realtimeProvider.kill();
    	}
    	if (deviProvider != null) {
    		deviProvider.kill();
    	}
    }
    
    final Handler autoRefreshHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			refresh();
			autoRefreshHandler.sendEmptyMessageDelayed(0, 10000);
			return true;
		}
	});
    
    /*
     * Refreshes the title
     */
    private void refreshTitle() {
    	long lastUpdateDiff = (System.currentTimeMillis() - lastUpdate) / HelperFunctions.SECOND;
    	if (lastUpdateDiff > 60) {
    		lastUpdateDiff = lastUpdateDiff / 60;
    		setTitle("Trafikanten - " + station.stopName + "   (" + lastUpdateDiff + " min " + getText(R.string.old) + ")");
    	} else {
    		setTitle("Trafikanten - " + station.stopName);
    	}
    }
    

    /*
     * Function for creating the default devi text, used both for line data and station data
     */
    public static TextView createDefaultDeviText(final RealtimeView context, final String title, final DeviData deviData, boolean station) {
    	TextView deviText = new TextView(context);
		deviText.setText(title);
		
		deviText.setSingleLine();
		deviText.setPadding(4, 4, 26, 2);
		if (station) {
			deviText.setTextColor(Color.BLACK);
			deviText.setBackgroundResource(R.drawable.skin_stasjonsdevi);
		} else {
			deviText.setTextColor(Color.rgb(250, 244, 0));
			deviText.setBackgroundResource(R.drawable.skin_sanntiddevi);
		}
		deviText.setTypeface(context.departuresTypeface);
		
		deviText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	new ShowDeviTask(context, deviData);
								
			}
        });
		return deviText;
    }
    
    /*
     * Refreshes station specific devi data.
     */
    private void refreshDevi() {
    	/*
    	 * Calculate a time difference it's easier to work with
    	 */
    	long timeDiff = timeDifference / 1000;
    	if (timeDiff < 0)
    		timeDiff = timeDiff * -1;
    	
    	/*
    	 * Render devi    	
    	 */
    	if ((deviItems == null || deviItems.size() == 0) && timeDiff < 60) {
    		/*
    		 * Nothing to display
    		 */
    		devi.setVisibility(View.GONE);
    	} else {
    		/*
    		 * Render devi information
    		 */
    		devi.setVisibility(View.VISIBLE);
    		devi.removeAllViews();
    		
    		if (timeDiff >= 60) {
    			DeviData deviData = new DeviData();
    			deviData.title = (String) getText(R.string.clockDiffTitle);
    			deviData.description = (String) getText(R.string.clockDiffDescription); 
    			deviData.body = (String) getText(R.string.clockDiffBodyHead);
    			if (timeDifference < 0) {
    				deviData.body = deviData.body + "\n\n" + getText(R.string.clockDiffBodyYourClock) + " " +  timeDiff + "s " + getText(R.string.clockDiffBodyBehind);   				
    			} else {
    				deviData.body = deviData.body + "\n\n" + getText(R.string.clockDiffBodyYourClock) + " " +  timeDiff + "s " + getText(R.string.clockDiffBodyAhead);
    			}
    			
    			deviData.validFrom = 0;
    			deviData.validTo = 0;
    			
    			devi.addView(createDefaultDeviText(this, deviData.title, deviData, true), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    		}
    		
    		for (final DeviData deviData : deviItems) {
				devi.addView(createDefaultDeviText(this, deviData.title, deviData, true), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    		}
  		
    	}
    }
    
    /*
     * Load data, variable used to prevent updating data set on every iteration.
     */
    private boolean caVisibilityChecked;
    private void load() {
        lastUpdate = System.currentTimeMillis();
    	stopProviders();
    	
    	finishedLoading = false;
		realtimeList.itemsAddedWithoutNotify = 0;
    	realtimeList.clear();
    	realtimeList.notifyDataSetChanged();
    	deviItems = new ArrayList<DeviData>();

		caVisibilityChecked = settings.getBoolean(RealtimeView.SETTING_HIDECA, false); // if hideca = true we skip the visibility check
				
		realtimeProvider = new TrafikantenRealtime(this, station.stationId, new IGenericProviderHandler<RealtimeData>() {
			@Override
			public void onExtra(int what, Object obj) {
				switch (what) {
				case TrafikantenRealtime.MSG_TIMEDATA:
					timeDifference = (Long) obj;
					break;
				}
			}
			
			@Override
			public void onData(RealtimeData realtimeData) {
				if (!caVisibilityChecked && !realtimeData.realtime) {
					/*
					 * check "ca info text" visibility
					 */
					caVisibilityChecked = true;
		        	caText.setVisibility(View.VISIBLE);
				}
				realtimeList.addItem(realtimeData);
				realtimeList.itemsAddedWithoutNotify++;
				if (realtimeList.itemsAddedWithoutNotify > 5) {
					realtimeList.itemsAddedWithoutNotify = 0;
					realtimeList.notifyDataSetChanged();
				}				
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				realtimeProvider = null;
				if (exception != null) {
					Log.w(TAG,"onException " + exception);
					infoText.setVisibility(View.VISIBLE);
					if (exception.getClass().getSimpleName().equals("ParseException")) {
						infoText.setText("" + getText(R.string.parseError) + ":" + "\n\n" + exception);
					} else {
						infoText.setText("" + getText(R.string.exception) + ":" + "\n\n" + exception);
					}
				} else {
					refreshTitle();
					/*
					 * Show info text if view is empty
					 */
					infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
					if (realtimeList.itemsAddedWithoutNotify > 0) {
						realtimeList.itemsAddedWithoutNotify = 0;
						realtimeList.notifyDataSetChanged();
					}
					loadDevi();	
				}
			}

			@Override
			public void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);				
			}
		});
    }
    
    /*
     * Load devi data
     */
    private void loadDevi() {
    	/*
    	 * Create list of lines - first create lineList
    	 */
    	ArrayList<String> lineList = new ArrayList<String>();
    	{
	    	final int count = realtimeList.getCount();
	    	for (int i = 0; i < count; i++) {
	    		final RealtimeData realtimeData = realtimeList.getItem(i);
	    		if (!lineList.contains(realtimeData.line)) {
	    			lineList.add(realtimeData.line);
	    		}
	    	}
    	}
    	/*
    	 * Create list of lines - then merge it into a comma seperated list
    	 */
    	StringBuffer deviLines = new StringBuffer();
    	{
    		final int count = lineList.size();
	    	for (int i = 0; i < count; i++) {
	    		if (i > 0) {
	    			deviLines.append(",");
	    		}
	    		deviLines.append(lineList.get(i));
	    	}
    	}
    	
    	deviProvider = new TrafikantenDevi(this, station.stationId, deviLines.toString(), new IGenericProviderHandler<DeviData>() {
    		@Override
    		public void onExtra(int what, Object obj) {
    			/* Class has no extra data */
    		}

			@Override
			public void onData(DeviData deviData) {
				if (deviData.lines.size() > 0) {
					/*
					 * Line specific data
					 */
					realtimeList.addDeviItem(deviData);
					realtimeList.itemsAddedWithoutNotify++;
					if (realtimeList.itemsAddedWithoutNotify > 5) {
						realtimeList.itemsAddedWithoutNotify = 0;
						realtimeList.notifyDataSetChanged();
					}
				} else {
					/*
					 * Station specific data
					 */
					deviItems.add(deviData);
				}				
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				finishedLoading = true;
				deviProvider = null;

				if (exception != null) {
					Log.w(TAG,"onException " + exception);
					Toast.makeText(RealtimeView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
		
				} else {
					refreshDevi();
					if (realtimeList.itemsAddedWithoutNotify > 0) {
						realtimeList.itemsAddedWithoutNotify = 0;
						realtimeList.notifyDataSetChanged();
					}
				}
			}

			@Override
			public void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);
			}
    	});
    }
    
	/*
	 * Options menu, visible on menu button.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem myLocation = menu.add(0, REFRESH_ID, 0, R.string.refresh);
		myLocation.setIcon(R.drawable.ic_menu_refresh);
        
        final MenuItem showHideCaText = menu.add(0, HIDECA_ID, 0, R.string.changeCaTextVisibility);
        showHideCaText.setIcon(android.R.drawable.ic_menu_info_details);

		return true;
	}

	/*O
	 * Options menu item selected, options menu visible on menu button.
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case REFRESH_ID:
        	load();
        	break;
        case HIDECA_ID:
        	boolean hideCaText = !(caText.getVisibility() == View.GONE);
        	SharedPreferences.Editor editor = settings.edit();
            if (hideCaText) {
            	caText.setVisibility(View.GONE);
            	editor.putBoolean(SETTING_HIDECA, true);
            } else {
            	caText.setVisibility(View.VISIBLE);
            	editor.putBoolean(SETTING_HIDECA, false);
            }
            editor.commit();
        	break;
        }
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Dialog creation
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_NOTIFICATION:
			/*
			 * notify dialog
			 */
			return NotificationDialog.getDialog(this);
		}
		return super.onCreateDialog(id);
	}

	/*
	 * Load data into dialog
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		final RealtimeData realtimeData = (RealtimeData) realtimeList.getItem(selectedId);
		final String notifyWith = realtimeData.line.equals(realtimeData.destination) 
			? realtimeData.line 
			: realtimeData.line + " " + realtimeData.destination;
		NotificationDialog.setRealtimeData(realtimeData, station, notifyWith);
		super.onPrepareDialog(id, dialog);
	}

	/*
	 * onCreate - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final RealtimeData realtimeData = realtimeList.getItem(info.position);
		
		menu.add(0, NOTIFY_ID, 0, R.string.alarm);
		if (realtimeData.devi.size() > 0)
			menu.add(0, DEVI_ID, 0, R.string.warnings);
	}
	
	/*
	 * onSelected - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        selectedId = info.position;
		
		switch(item.getItemId()) {
		case NOTIFY_ID:
			showDialog(DIALOG_NOTIFICATION);
			return true;
		case DEVI_ID:
			final RealtimeData realtimeData = (RealtimeData) realtimeList.getItem(selectedId);
			final ArrayList<DeviData> deviPopup = realtimeList.getDevi(realtimeData);
			new SelectDeviTask(this, deviPopup);

		}
		return super.onContextItemSelected(item);
	}
	
	private void refresh() {
		refreshTitle();
		realtimeList.notifyDataSetChanged(); // force refreshing times.
	}
	
	/*
	 * Functions for dealing with program state.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		autoRefreshHandler.removeMessages(0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refresh();
		autoRefreshHandler.sendEmptyMessageDelayed(0, 10000);
	}
	
	@Override
	protected void onStop() {
		/*
		 * make sure background threads is properly killed off.
		 */
		stopProviders();
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(StationData.PARCELABLE, station);
		outState.putLong(KEY_LAST_UPDATE, lastUpdate);
		outState.putParcelableArrayList(KEY_DEVILIST, deviItems);
		outState.putBoolean(KEY_FINISHEDLOADING, finishedLoading);
		outState.putLong(KEY_TIMEDIFFERENCE, timeDifference);

		realtimeList.saveInstanceState(outState);
	}
}

class RealtimePlatformList extends ArrayList<RealtimeData> implements Parcelable {
	private static final long serialVersionUID = -8158771022676013360L;
	public int platform;
	
	public RealtimePlatformList(int platform) {
		super();
		this.platform = platform;
	}

	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimePlatformList(Parcel in) {
		platform = in.readInt();
		in.readList(this, RealtimeData.class.getClassLoader());
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(platform);
		out.writeList(this);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RealtimePlatformList> CREATOR = new Parcelable.Creator<RealtimePlatformList>() {
		public RealtimePlatformList createFromParcel(Parcel in) {
		    return new RealtimePlatformList(in);
		}
		
		public RealtimePlatformList[] newArray(int size) {
		    return new RealtimePlatformList[size];
		}
	};
}

class RealtimeAdapter extends BaseAdapter {
	public static final String KEY_REALTIMELIST = "realtimelist";
	public static final String KEY_STATIONDEVILIST = "stationdevilist";
	public static final String KEY_ITEMSSIZE = "devilistsize";
	private LayoutInflater inflater;
	
	/*
	 * Structure:
	 * platform ->
	 *     line + destination ->
	 *         RealtimeData	
	 */
	private ArrayList<RealtimePlatformList> items = new ArrayList<RealtimePlatformList>();
	private int itemsSize = 0; // Cached for performance, this is len(items) + len(item[0]) + len(item[1]) ...
	public int itemsAddedWithoutNotify = 0; // List of items added during load without a .notifyDataUpdated
	
	/*
	 * Devi data:
	 */
	private ArrayList<DeviData> deviItems = new ArrayList<DeviData>();
	
	/*
	 * This variable is set by getItem, it indicates this station is the first of the current platform, so platform should be shown.
	 */
	private boolean renderPlatform = false;
	
	private RealtimeView parent;
	
	public RealtimeAdapter(RealtimeView parent) {
		inflater = LayoutInflater.from(parent);
		this.parent = parent;
	}
	
	/*
	 * Clearing the list 
	 */
	public void clear() {
		items.clear();
		deviItems.clear();
		itemsSize = 0;
		itemsAddedWithoutNotify = 0;
	}
	/*
	 * Saving instance state
	 */
	public void saveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(KEY_STATIONDEVILIST, deviItems);
		outState.putInt(KEY_ITEMSSIZE, itemsSize);
		outState.putParcelableArrayList(KEY_REALTIMELIST, items);
	}
	
	/*
	 * Loading instance state
	 */
	public void loadInstanceState(Bundle inState) {
		deviItems = inState.getParcelableArrayList(KEY_STATIONDEVILIST);
		itemsSize = inState.getInt(KEY_ITEMSSIZE);
		items = inState.getParcelableArrayList(KEY_REALTIMELIST);		
	}

	
	/*
	 * Function to add devi data
	 *  - This only cares about devi's linked to a line, station devi's are handled in main class.
	 */
	public void addDeviItem(DeviData item) {
		int pos = deviItems.size();
		boolean addDevi = false;

		/*
		 * Scan and add the devi position index to all realtime data
		 */
		for (RealtimePlatformList realtimePlatformList : items) {
			for (RealtimeData d : realtimePlatformList) {
				if (item.lines.contains(d.line)) {
					addDevi = true;
					d.devi.add(pos);
				}
			}
		}
		
		if (addDevi) {
			deviItems.add(item);
		}
	}
		
	/*
	 * Get a list of devi's assosiated with a line
	 */
	public ArrayList<DeviData> getDevi(RealtimeData realtimeData) {
		ArrayList<DeviData> result = new ArrayList<DeviData>();
		for (Integer i : realtimeData.devi) {
			result.add(deviItems.get(i));
		}
		return result;
	}
	
	/*
	 * Simple function that gets (or creates a new) platform in items
	 */
	public RealtimePlatformList getOrCreatePlatform(int platform) {
		/*
		 * If the platform already exists in the database just return it
		 */
		for (RealtimePlatformList realtimePlatformList : items) {
			if (realtimePlatformList.platform == platform) {
				return realtimePlatformList;
			}
		}
		
		/*
		 * No platform found, create new
		 */
		RealtimePlatformList realtimePlatformList = new RealtimePlatformList(platform);
		
		/*
		 * We make sure the platform list is sorted the same way every time.
		 */
		int pos = 0;
		for (; pos < items.size(); pos++) {
			if (platform < items.get(pos).platform) {
				break;
			}
		}
		
		/*
		 * Finally add it and return
		 */
		items.add(pos, realtimePlatformList);
		return realtimePlatformList;
	}
	
	/*
	 * Adding an item puts it in the platform category, and compressed duplicate data to one entry.
	 */
	public void addItem(RealtimeData item) {
		RealtimePlatformList realtimePlatformList = getOrCreatePlatform(item.departurePlatform);
		for (RealtimeData d : realtimePlatformList) {
			if (d.destination.equals(item.destination) && d.line.equals(item.line)) {
				/*
				 * Data already exists, we add it to the arrival list and return
				 */
				d.addDeparture(item.expectedDeparture, item.realtime, item.stopVisitNote);
				return;
			}
		}
		/*
		 * Data does not exist, add it
		 */
		realtimePlatformList.add(item);
		itemsSize++;
	}
	
	/*
	 * Standard android.widget.Adapter items, self explanatory.
	 */
	@Override
	public int getCount() {
		if (itemsAddedWithoutNotify > 0) {
			itemsAddedWithoutNotify = 0;
			notifyDataSetChanged(); // This is incase getCount is called between our data set updates, which triggers IllegalStateException, listView does a simple if (mItemCount != mAdapter.getCount()) {
		}
		return itemsSize; 
	}
	@Override
	public long getItemId(int pos) { return pos; }
	@Override
	public RealtimeData getItem(int pos) {
		for (RealtimePlatformList realtimePlatformList : items) {
			if (pos < realtimePlatformList.size()) {
				if (pos == 0) {
					renderPlatform = true;
				} else {
					renderPlatform = false;
				}
				return realtimePlatformList.get(pos);
			} else {
				pos = pos - realtimePlatformList.size();
			}
		}
		return null;
	}
	
	/*
	 * Setup the view
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int pos, View convertView, ViewGroup arg2) {
		final RealtimeData data = getItem(pos);
		/*
		 * Setup holder, for performance and readability.
		 */
		ViewHolder holder;
		if (convertView == null) {
			/*
			 * New view, inflate and setup holder.
			 */
			convertView = inflater.inflate(R.layout.realtime_list, null);
			
			holder = new ViewHolder();
			holder.platform = (TextView) convertView.findViewById(R.id.platform);
			holder.line = (TextView) convertView.findViewById(R.id.line);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.destination = (TextView) convertView.findViewById(R.id.destination);
			holder.departures = (TextView) convertView.findViewById(R.id.departures);
			holder.departures.setTypeface(parent.departuresTypeface);
			holder.departures.setMovementMethod(ScrollingMovementMethod.getInstance());
			holder.departures.setHorizontallyScrolling(true);
			holder.departureInfo = (LinearLayout) convertView.findViewById(R.id.departureInfo);
			
			convertView.setTag(holder);
		} else {
			/*
			 * Old view found, we can reuse that instead of inflating.
			 */
			holder = (ViewHolder) convertView.getTag();
		}
		
		/*
		 * Workaround for clickable bug, onListItemClick does not trigger at all if ScrollingMovementMethod is being used.
		 */
		{
			final TableLayout tableLayout = (TableLayout) convertView.findViewById(R.id.tablelayout);
			tableLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					new ShowRealtimeLineDetails(parent, System.currentTimeMillis() - parent.timeDifference, data);
				}
			});
			tableLayout.setLongClickable(true);
		}
		
		/*
		 * Render data to view.
		 */
		holder.departures.setText(data.renderDepartures(System.currentTimeMillis() - parent.timeDifference, parent));
		holder.destination.setText(data.destination);
		if (data.destination.equals(data.line)) {
			holder.line.setText("-");
		} else {
			holder.line.setText(data.line);
		}
		
		if (renderPlatform && data.departurePlatform != 0) {
			holder.platform.setText("Plattform " + data.departurePlatform);
			holder.platform.setVisibility(View.VISIBLE);
		} else {
			holder.platform.setVisibility(View.GONE);
		}
		
		holder.icon.setImageResource(StationIcons.hackGetLineIcon(RealtimeView.station, data.line));
		
		/*
		 * Setup devi
		 */
		if (data.devi.size() > 0) {
			holder.departureInfo.setVisibility(View.VISIBLE);
			holder.departureInfo.removeAllViews();
			
			for (Integer i : data.devi) {
				/*
				 * Add all devi items.
				 */
				final DeviData devi = deviItems.get(i);
				holder.departureInfo.addView(RealtimeView.createDefaultDeviText(parent, devi.title, devi, false), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			}
		} else {
			holder.departureInfo.setVisibility(View.GONE);
		}
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		TextView platform;
		
		TextView line;
		TextView destination;
		ImageView icon;
		TextView departures;
		
		LinearLayout departureInfo;
	}
};
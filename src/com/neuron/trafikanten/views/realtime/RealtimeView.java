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
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.dataProviders.IDeviProvider;
import com.neuron.trafikanten.dataProviders.IRealtimeProvider;
import com.neuron.trafikanten.dataProviders.IDeviProvider.DeviProviderHandler;
import com.neuron.trafikanten.dataProviders.IRealtimeProvider.RealtimeProviderHandler;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.notification.NotificationDialog;
import com.neuron.trafikanten.tasks.SelectDeviTask;

public class RealtimeView extends ListActivity {
	private static final String TAG = "Trafikanten-RealtimeView";
	private static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_DEVILIST = "devilist";
	
	/*
	 * Options menu:
	 */
	private static final int REFRESH_ID = Menu.FIRST;
	
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
	private StationData station;
	private RealtimeAdapter realtimeList;
	private long lastUpdate;
	private ArrayList<DeviData> deviItems;
	
	/*
	 * UI
	 */
	private TextView deviText;
	private ImageView deviIcon;
	
	/*
	 * Data providers
	 */
	private IRealtimeProvider realtimeProvider;
	private IDeviProvider deviProvider;
	
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
        deviText = (TextView) findViewById(R.id.deviText);
        deviIcon = (ImageView) findViewById(R.id.deviIcon);
        
        /*
         * Load instance state
         */
        if (savedInstanceState == null) {
        	station = getIntent().getParcelableExtra(StationData.PARCELABLE);
            load();
        } else {
        	station = savedInstanceState.getParcelable(StationData.PARCELABLE);
        	lastUpdate = savedInstanceState.getLong(KEY_LAST_UPDATE);
        	deviItems = savedInstanceState.getParcelableArrayList(KEY_DEVILIST);
        	
        	realtimeList.loadInstanceState(savedInstanceState);
        	realtimeList.notifyDataSetChanged();
        }
        
        /*
         * Setup onclick handler for devi text
         */
        deviText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new SelectDeviTask(RealtimeView.this, deviItems);												
			}
        	
        });

        registerForContextMenu(getListView());
        setListAdapter(realtimeList);
        refreshTitle();
        refreshDevi();
    }
    
    /*
     * Refreshes the title
     */
    private void refreshTitle() {
    	long lastUpdateDiff = (lastUpdate - System.currentTimeMillis()) / HelperFunctions.SECOND;
    	if (lastUpdateDiff > 60) {
    		lastUpdateDiff = lastUpdateDiff / 60;
    		setTitle("Trafikanten - " + station.stopName + " (" + lastUpdateDiff + "m " + getText(R.string.old) + ")");
    	} else {
    		setTitle("Trafikanten - " + station.stopName);
    	}
    }
    
    /*
     * Refreshes station specific devi data.
     */
    private void refreshDevi() {
    	if (deviItems == null || deviItems.size() == 0) {
    		/*
    		 * Nothing to display
    		 */
    		deviText.setVisibility(View.GONE);
    		deviIcon.setVisibility(View.GONE);
    	} else {
    		/*
    		 * Render devi information
    		 */
    		deviText.setVisibility(View.VISIBLE);
    		deviIcon.setVisibility(View.VISIBLE);
    		
    		String text = "";
    		for (DeviData deviData : deviItems) {
    			if (text.length() == 0) {
    				text = deviData.title;
    			} else {
    				text = text + "\n" + deviData.title;
    			}
    		}
    		deviText.setText(text);   		
    	}
    }
    
    /*
     * Load data, variable used to prevent updating data set on every iteration.
     */
    private int tmpDataUpdated;
    private void load() {
        lastUpdate = System.currentTimeMillis();
    	setProgressBarIndeterminateVisibility(true);
    	if (realtimeProvider != null)
    		realtimeProvider.Stop();
    	if (deviProvider != null)
    		deviProvider.Stop();
    	
    	realtimeList.clear();
    	realtimeList.notifyDataSetChanged();
    	
    	tmpDataUpdated = 0;
    	realtimeProvider = DataProviderFactory.getRealtimeProvider(new RealtimeProviderHandler() {
			@Override
			public void onData(RealtimeData realtimeData) {
				realtimeList.addItem(realtimeData);
				tmpDataUpdated++;
				if (tmpDataUpdated > 5) {
					realtimeList.notifyDataSetChanged();
					tmpDataUpdated = 0;
				}
			}

			@Override
			public void onError(Exception exception) {
				Log.w(TAG,"onException " + exception);
				Toast.makeText(RealtimeView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
				setProgressBarIndeterminateVisibility(false);
			}

			@Override
			public void onFinished() {
				refreshTitle();
				setProgressBarIndeterminateVisibility(false);
				/*
				 * Show info text if view is empty
				 */
				final TextView infoText = (TextView) findViewById(R.id.emptyText);
				infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
				if (tmpDataUpdated > 0) {
					realtimeList.notifyDataSetChanged();
				}
				realtimeProvider = null;
				loadDevi();
			}
    	});
    	realtimeProvider.Fetch(station.stationId);
    }
    
    /*
     * Load devi data
     */
    private void loadDevi() {
    	setProgressBarIndeterminateVisibility(true);
    	tmpDataUpdated = 0;
    	deviItems = new ArrayList<DeviData>();

    	deviProvider = DataProviderFactory.getDeviProvider(new DeviProviderHandler() {
			@Override
			public void onData(DeviData deviData) {
				if (deviData.lines.size() > 0) {
					/*
					 * Line specific data
					 */
					realtimeList.addDeviItem(deviData);
					tmpDataUpdated++;
					if (tmpDataUpdated > 5) {
						realtimeList.notifyDataSetChanged();
						tmpDataUpdated = 0;
					}
				} else {
					/*
					 * Station specific data
					 */
					deviItems.add(deviData);
				}
			}

			@Override
			public void onError(Exception exception) {
				Log.w(TAG,"onException " + exception);
				Toast.makeText(RealtimeView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
				setProgressBarIndeterminateVisibility(false);				
			}

			@Override
			public void onFinished() {
				refreshDevi();
				setProgressBarIndeterminateVisibility(false);
				deviProvider = null;
				if (tmpDataUpdated > 0) {
					realtimeList.notifyDataSetChanged();
				}				
			}
    		
    	});
    	deviProvider.Fetch(station.stationId);
    }
    
	/*
	 * Options menu, visible on menu button.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem myLocation = menu.add(0, REFRESH_ID, 0, R.string.refresh);
		myLocation.setIcon(R.drawable.ic_menu_refresh);
		return true;
	}

	/*
	 * Options menu item selected, options menu visible on menu button.
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case REFRESH_ID:
        	load();
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
	
	
	
	/*
	 * Click on a list item
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		RealtimeData data = (RealtimeData) realtimeList.getItem(position);
		final long minutesDelayed = (data.expectedDeparture - data.aimedDeparture) / (60 * 1000);
		
		String info = data.destination + "\n" +
			"  " + minutesDelayed + "m " + getText(R.string.late);
		info = info + "\n   - " + getText(R.string.hintRealtimeHoldButton);
		Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
	}

	/*
	 * Resume state, restart search.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		refreshTitle();
		realtimeList.notifyDataSetChanged(); // force refreshing times.
	}

	/*
	 * Make sure we kill off threads when freeing memory.
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		if (realtimeProvider != null) {
			realtimeProvider.Stop();
		}
		if (deviProvider != null) {
			deviProvider.Stop();
		}
		super.onDestroy();
	}

	/*
	 * saveInstanceState saves all variables needed for onCreate
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(StationData.PARCELABLE, station);
		outState.putLong(KEY_LAST_UPDATE, lastUpdate);
		outState.putParcelableArrayList(KEY_DEVILIST, deviItems);

		realtimeList.saveInstanceState(outState);
	}
}

class RealtimePlatformList extends ArrayList<RealtimeData> implements Parcelable {
	private static final long serialVersionUID = -8158771022676013360L;
	public String platform;
	
	public RealtimePlatformList(String platform) {
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
		platform = in.readString();
		in.readList(this, RealtimeData.class.getClassLoader());
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(platform);
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
	public static final String KEY_ITEMSSIZE = "devilist";
	private LayoutInflater inflater;
	
	
	/*
	 * Structure:
	 * platform ->
	 *     line + destination ->
	 *         RealtimeData	
	 */
	private ArrayList<RealtimePlatformList> items = new ArrayList<RealtimePlatformList>();
	private int itemsSize = 0; // Cached for performance, this is len(items) + len(item[0]) + len(item[1]) ...
	
	/*
	 * Devi data:
	 */
	private ArrayList<DeviData> deviItems = new ArrayList<DeviData>();
	
	/*
	 * This variable is set by getItem, it indicates this station is the first of the current platform, so platform should be shown.
	 */
	private boolean renderPlatform = false;
	
	
	private Context context;
	
	public RealtimeAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		this.context = context;
	}
	
	/*
	 * Clearing the list 
	 */
	public void clear() {
		items.clear();
		deviItems.clear();
		itemsSize = 0;
	}
	/*
	 * Saving instance state
	 */
	public void saveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(RealtimeView.KEY_DEVILIST, deviItems);
		outState.putInt(KEY_ITEMSSIZE, itemsSize);
		outState.putParcelableArrayList(KEY_REALTIMELIST, items);
	}
	
	/*
	 * Loading instance state
	 */
	public void loadInstanceState(Bundle inState) {
		deviItems = inState.getParcelableArrayList(RealtimeView.KEY_DEVILIST);
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
	public RealtimePlatformList getOrCreatePlatform(String platform) {
		/*
		 * We cant deal with null platforms
		 */
		if (platform == null)
			platform = "";
		
		/*
		 * If the platform already exists in the database just return it
		 */
		for (RealtimePlatformList realtimePlatformList : items) {
			if (realtimePlatformList.platform.equals(platform)) {
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
			if (platform.compareTo(items.get(pos).platform) < 0) {
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
				if (d.arrivalList.length() == 0) {
					d.arrivalList.append(HelperFunctions.renderTime(context, item.expectedDeparture));
				} else {
					d.arrivalList.append(",  ");
					d.arrivalList.append(HelperFunctions.renderTime(context, item.expectedDeparture));
				}
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
	public int getCount() { return itemsSize; }
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
			holder.destination = (TextView) convertView.findViewById(R.id.destination);
			holder.platform = (TextView) convertView.findViewById(R.id.platform);
			holder.line = (TextView) convertView.findViewById(R.id.line);
			holder.time = (TextView) convertView.findViewById(R.id.time);
			holder.nextDepartures = (TextView) convertView.findViewById(R.id.nextDepartures);
			holder.deviIcon = (ImageView) convertView.findViewById(R.id.deviIcon);
			
			holder.stopVisitIcon = (ImageView) convertView.findViewById(R.id.stopVisitIcon);
			holder.stopVisitNote = (TextView) convertView.findViewById(R.id.stopVisitNote);
			
			convertView.setTag(holder);
		} else {
			/*
			 * Old view found, we can reuse that instead of inflating.
			 */
			holder = (ViewHolder) convertView.getTag();
		}
		
		/*
		 * Render data to view.
		 */
		final RealtimeData data = getItem(pos);
		if (data.destination.equals(data.line)) {
			holder.destination.setText("");
			holder.line.setText(data.line);			
		} else {
			holder.destination.setText(data.destination);
			holder.line.setText(data.line);
		}
		
		/*
		 * Render the data
		 */
		if (!data.realtime) {
			holder.time.setText("" + context.getText(R.string.ca) + " " + HelperFunctions.renderTime(context, data.expectedDeparture));
		} else {
			holder.time.setText(HelperFunctions.renderTime(context, data.expectedDeparture));
		}
		
		if (renderPlatform && data.departurePlatform != null) {
			holder.platform.setText("Platform " + data.departurePlatform);
			holder.platform.setVisibility(View.VISIBLE);
		} else {
			holder.platform.setVisibility(View.GONE);
		}
		
		/*
		 * Render list of coming departures
		 */
		if (data.arrivalList.length() > 0) {
			holder.nextDepartures.setText(data.arrivalList);
			holder.nextDepartures.setVisibility(View.VISIBLE);
		} else {
			holder.nextDepartures.setVisibility(View.GONE);
		}
		
		/*
		 * Show devi icon if appliable
		 */
		if (data.devi.size() > 0) {
			holder.deviIcon.setVisibility(View.VISIBLE);
		} else {
			holder.deviIcon.setVisibility(View.GONE);
		}
		
		/*
		 * And render stopVisitNote
		 */
		if (data.stopVisitNote != null) {
			holder.stopVisitIcon.setVisibility(View.VISIBLE);
			holder.stopVisitNote.setVisibility(View.VISIBLE);
			holder.stopVisitNote.setText(data.stopVisitNote);
		} else {
			holder.stopVisitIcon.setVisibility(View.GONE);
			holder.stopVisitNote.setVisibility(View.GONE);
		}
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		TextView line;
		TextView platform;
		TextView destination;
		TextView time;
		TextView nextDepartures;
		
		ImageView deviIcon;
		
		ImageView stopVisitIcon;
		TextView stopVisitNote;
	}
};

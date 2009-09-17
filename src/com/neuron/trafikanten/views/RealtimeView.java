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

package com.neuron.trafikanten.views;

import java.util.ArrayList;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.MySettings;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProvider;
import com.neuron.trafikanten.dataProviders.IRealtimeProvider;
import com.neuron.trafikanten.dataProviders.ResultsProviderFactory;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.SearchStationData;
import com.neuron.trafikanten.notification.NotificationDialog;
import com.neuron.trafikanten.tasks.GenericTask;
import com.neuron.trafikanten.tasks.RealtimeDataTask;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class RealtimeView extends ListActivity {
	private static final String TAG = "RealtimeView";
	
	/*
	 * Options menu:
	 */
	private static final int REFRESH_ID = Menu.FIRST;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	
	/*
	 * Dialogs
	 */
	private static final int DIALOG_NOTIFICATION = 1;
	private int selectedId = 0;
	
	
	private boolean firstLoad = true;

	/*
	 * Saved instance data
	 */
	private SearchStationData station;
	private RealtimeAdapter realtimeList;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /*
         * Setup view and adapter.
         */
        setContentView(R.layout.realtime);
        realtimeList = new RealtimeAdapter(this);
        
        /*
         * Load instance state
         */
        if (savedInstanceState == null) {
        	station = getIntent().getParcelableExtra(SearchStationData.PARCELABLE);
            load();
        } else {
        	station = savedInstanceState.getParcelable(SearchStationData.PARCELABLE);
        	final ArrayList<RealtimeData> list = savedInstanceState.getParcelableArrayList(RealtimeAdapter.KEY_REALTIMELIST);
        	realtimeList.setList(list);
        	setListAdapter(realtimeList);
        }
        registerForContextMenu(getListView());
    }
    
    private void load() {
    	RealtimeDataTask.StartTask(this, station.stationId);
    }
    
	/*
	 * Handler for messages (both from Intent's and Handlers)
	 */
	public void onMessage(Message msg) {
    	switch(msg.what) {
    	case IRealtimeProvider.MESSAGE_DONE:
			/*
			 * If we haven't already set the most up to date list, set it.
			 */
    		firstLoad = false;
    		final ArrayList<RealtimeData> list = ResultsProviderFactory.getRealtimeResults();
    		if (list != null) {
    			realtimeList.setList(list);
    		}
			RealtimeView.this.setListAdapter(realtimeList);
			
			/*
			 * Show info text if view is empty
			 */
			final TextView infoText = (TextView) findViewById(R.id.emptyText);
			infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
    		break;
    	case IRealtimeProvider.MESSAGE_EXCEPTION:
    		final String exception = msg.getData().getString(IGenericProvider.KEY_EXCEPTION);
			Log.w(TAG,"onException " + exception);
			Toast.makeText(RealtimeView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
			break;
    	}
	}
	
	/*
	 * activityResult is always a task, and can always be passed to onMessage
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_CANCELED && requestCode == RealtimeDataTask.TASK_REALTIME && firstLoad) {
    		/*
    		 * Canceled while loading view, should return to select station view.
    		 */
    		finish();
    		return;
    	}
    	if (resultCode == RESULT_OK) {
			final Message msg = data.getParcelableExtra(GenericTask.KEY_MESSAGE);
			onMessage(msg);
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
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
		final String notifyWith = realtimeData.line == null ? null : realtimeData.line + " " + realtimeData.destination;
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
		menu.add(0, NOTIFY_ID, 0, R.string.alarm);
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
		Toast.makeText(this, data.destination + "\n" + minutesDelayed + "m " + getText(R.string.late), Toast.LENGTH_SHORT).show();		
	}

	/*
	 * onPause - stop anything needed.
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * Resume state, restart search.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		MySettings.refresh(this);
		RealtimeView.this.setListAdapter(realtimeList); // Re render times to avoid showing "+5m" when it's 2m left.
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(SearchStationData.PARCELABLE, station);
		outState.putParcelableArrayList(RealtimeAdapter.KEY_REALTIMELIST, realtimeList.getList());
	}
}

class RealtimeAdapter extends BaseAdapter {
	public static final String KEY_REALTIMELIST = "realtimelist";
	private LayoutInflater inflater;
	private ArrayList<RealtimeData> items = new ArrayList<RealtimeData>();
	private Context context;
	
	public RealtimeAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		this.context = context;
	}
	
	/*
	 * Simple functions dealing with adding/setting items. 
	 */
	public ArrayList<RealtimeData> getList() { return items; }
	public void setList(ArrayList<RealtimeData> items) { this.items = items; }
	//public void sort() { Collections.sort(items); }
	
	/*
	 * Standard android.widget.Adapter items, self explanatory.
	 */
	@Override
	public int getCount() {	return items.size(); }
	@Override
	public Object getItem(int pos) { return items.get(pos); }
	@Override
	public long getItemId(int pos) { return pos; }
	
	/*
	 * Function to render time to a text view, this includes rendering color coding.
	 */
	public void renderTimeToTextView(RealtimeData data, TextView out) {
		if (!data.realtime || data.expectedArrival == 0) {
			if (!data.realtime) {
				out.setText("" + context.getText(R.string.ca) + " " + HelperFunctions.renderTime(context, data.aimedArrival));
			} else {
				out.setText(HelperFunctions.renderTime(context, data.aimedArrival));
			}
		
			final int color = Color.rgb(255, 255, 255);
			out.setTextColor(color);
		} else {
			out.setText(HelperFunctions.renderTime(context, data.expectedArrival));
		
			// Color the time text
			final long diffMinutes = (data.expectedArrival - data.aimedArrival) / (60 * 1000);
			final int colorValue = (int)(255 * diffMinutes / 9); // Compute a rgb value from diffMinutes, "max" color range is 9min, so >9 is completely red.
			final int color = Color.rgb(colorValue, 255 - colorValue, 0);
			out.setTextColor(color);
		}
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
			holder.line = (TextView) convertView.findViewById(R.id.line);
			holder.time = (TextView) convertView.findViewById(R.id.time);
			holder.nextDepartures = (TextView) convertView.findViewById(R.id.nextDepartures);
			
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
		final RealtimeData data = items.get(pos);
		if (data.destination.equals(data.line)) {
			holder.destination.setText("");
			holder.line.setText(data.line);			
		} else {
			holder.destination.setText(data.destination);
			holder.line.setText(data.line);
		}
		
		renderTimeToTextView(data, holder.time);
		
		/*
		 * Render list of coming departures
		 */
		if (data.arrivalList.size() > 0) {
			int size = data.arrivalList.size() - 1;
			String nextDepartures = HelperFunctions.renderTime(context, data.arrivalList.get(0).aimedArrival);
			for (int i = 1; i < size; i++) {
				final RealtimeData nextData = data.arrivalList.get(i);
				nextDepartures = nextDepartures + ",  " + HelperFunctions.renderTime(context, nextData.aimedArrival);
			}
			holder.nextDepartures.setText(nextDepartures);
			holder.nextDepartures.setVisibility(View.VISIBLE);
		} else {
			holder.nextDepartures.setVisibility(View.GONE);
		}
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		TextView line;
		TextView destination;
		TextView time;
		TextView nextDepartures;
		
	}
};

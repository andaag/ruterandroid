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

package com.neuron.trafikanten.views.route;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.notification.NotificationDialog;

public class DetailedRouteView extends ListActivity {
	//private final static String TAG = "Trafikanten-DetailedRouteView";
	private RouteAdapter routeList;
	private ViewHolder viewHolder = new ViewHolder();
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	
	/*
	 * Dialogs
	 */
	private static final int DIALOG_NOTIFICATION = 1;
	private int selectedId = 0;
	
	/*
	 * Saved instance data
	 */
	private ArrayList<RouteProposal> routeProposalList;
	private int proposalPosition;

	/*
	 * this is a list of departure dates, it's used to store next/previous button clicks.
	 * See the button onClick listeners for how this works.
	 */
	public final static String KEY_PROPOSALPOSITION = "proposalPosition";

	public static void ShowRoute(Activity activity, ArrayList<RouteProposal> routeProposalList, int proposalPosition) {
		Intent intent = new Intent(activity, DetailedRouteView.class);
		intent.putExtra(RouteProposal.PARCELABLE, routeProposalList);
		intent.putExtra(KEY_PROPOSALPOSITION, proposalPosition);
		activity.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Setup the view
		 */
		setContentView(R.layout.route_detailed);
		routeList = new RouteAdapter(this);
		viewHolder.previousButton = (ImageButton) findViewById(R.id.prevButton);
		viewHolder.infoText = (TextView) findViewById(R.id.infoText);
		viewHolder.nextButton = (ImageButton) findViewById(R.id.nextButton);

		/*
		 * Setup onClick handler on previous button
		 */
		viewHolder.previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				 * Search for previous departure date.
				 */
				proposalPosition--;
				load();
			}
		});
		
		/*
		 * Setup onClick handler on next button
		 */
		viewHolder.nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				 * Simply set departureDate to our first arrival time + 1 minute, that way the search will skip that route and take the next one.
				 */
				proposalPosition++;
				load();
			}
		});

		/*
		 * Load instance state
		 */
		final Bundle bundle = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();
		routeProposalList = bundle.getParcelableArrayList(RouteProposal.PARCELABLE);
		proposalPosition = bundle.getInt(KEY_PROPOSALPOSITION);
		load();
		registerForContextMenu(getListView());
	}
	
	/*
	 * Refresh button.isEnabled
	 */
	private void refreshButtons() {
		viewHolder.previousButton.setEnabled(proposalPosition != 0 && routeProposalList.size() > 1);
		viewHolder.nextButton.setEnabled(proposalPosition < routeProposalList.size() - 1);
	}
	
	/*
	 * Load station data
	 */
	private void load() {
		final ArrayList<RouteData> list = routeProposalList.get(proposalPosition).travelStageList;
		routeList.setList(list);
		setListAdapter(routeList);
		refreshButtons();
	}
	
	/*
	 * We need to override setListAdapter to trigger refreshing info overlay.
	 * @see android.app.ListActivity#setListAdapter(android.widget.ListAdapter)
	 */
	@Override
	public void setListAdapter(ListAdapter adapter) {
		super.setListAdapter(adapter);
		final ArrayList<RouteData> list = routeList.getList();
		/*
		 * Render info box on the bottom
		 */
		if (list.size() > 0) {
			viewHolder.infoText.setVisibility(View.VISIBLE);
			
			/*
			 * Calculate total travel time, waittime and walktime. 
			 */
			final long departure = list.get(0).departure;
			long arrival = list.get(list.size() - 1).arrival;
			int waitTime = 0;
			int walkTime = 0;
			
			if (arrival < departure) {
				/*
				 * We're arriving before we're leaving, must be a days difference.
				 */
				arrival = arrival + (HelperFunctions.HOUR * 24);
			}
			
			for (RouteData data : list) {
				waitTime = waitTime + data.waitTime;
				if (data.transportType == IRouteProvider.TRANSPORT_WALK) {
					walkTime = walkTime + (int)((data.arrival - data.departure) / HelperFunctions.MINUTE);					
				}
			}
			
			/*
			 * Setup actual text
			 */
			viewHolder.infoText.setText("" + getText(R.string.travelTime) + " " + ((arrival - departure) / HelperFunctions.MINUTE) + "m\n" +
					getText(R.string.waitTime) + " " + waitTime + "m\n" +
					getText(R.string.walkTime) + " " + walkTime + "m");
		} else {
			viewHolder.infoText.setVisibility(View.INVISIBLE);
		}
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
		final RouteData notifyRouteData = (RouteData) routeList.getItem(selectedId);
		/*
		 * Departure is what we base our notification on, 10 minuts before departure
		 */
		final long notifyDeparture = notifyRouteData.departure;
		final String notifyWith = notifyRouteData.line.equals(notifyRouteData.destination) ? notifyRouteData.line : notifyRouteData.line + " " + notifyRouteData.destination;
		NotificationDialog.setRouteData(routeProposalList, proposalPosition, notifyDeparture, notifyWith);
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(RouteProposal.PARCELABLE, routeProposalList);
		outState.putInt(KEY_PROPOSALPOSITION, proposalPosition);
	}
	
	/*
	 * Class for holding the view.
	 */
	static class ViewHolder {
		ImageButton previousButton;
		TextView infoText;
		ImageButton nextButton;
	}
}


class RouteAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<RouteData> items = new ArrayList<RouteData>();
	private Context context;
	
	public RouteAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		this.context = context;
	}
	
	/*
	 * Simple functions dealing with adding/setting items. 
	 */
	public ArrayList<RouteData> getList() { return items; }
	public void setList(ArrayList<RouteData> items) { this.items = items; }
	public void clear() { items.clear(); }
	
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
			convertView = inflater.inflate(R.layout.route_detailed_list, null);
			
			holder = new ViewHolder();
			holder.symbol = (ImageView) convertView.findViewById(R.id.symbol);
			holder.line = (TextView) convertView.findViewById(R.id.line);
			holder.transportDestination = (TextView) convertView.findViewById(R.id.transportDestination);
			holder.from = (TextView) convertView.findViewById(R.id.from);
			holder.fromTime = (TextView) convertView.findViewById(R.id.fromTime);
			holder.to = (TextView) convertView.findViewById(R.id.to);
			holder.toTime = (TextView) convertView.findViewById(R.id.toTime);
			holder.waittime = (TextView) convertView.findViewById(R.id.waittime);
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
		final RouteData routeData = items.get(pos);
		
		if (routeData.transportType == IRouteProvider.TRANSPORT_WALK) {
			holder.transportDestination.setText(R.string.walk);
		} else {
			holder.transportDestination.setText(routeData.destination);
		}
		holder.line.setText(routeData.line);
				
		holder.from.setText(routeData.fromStation.stopName);
		holder.fromTime.setText(HelperFunctions.hourFormater.format(routeData.departure));
		
		holder.to.setText(routeData.toStation.stopName);
		holder.toTime.setText(HelperFunctions.hourFormater.format(routeData.arrival));
		
		
		/*
		 * Setup symbol.
		 */
		final int symbolImage = DataProviderFactory.getImageResource(routeData.transportType);
		if (symbolImage > 0) {
			holder.symbol.setVisibility(View.VISIBLE);
			holder.symbol.setImageResource(symbolImage);
		} else {
			holder.symbol.setVisibility(View.GONE);
		}
		
		/*
		 * Setup waittime
		 */
		if (routeData.waitTime > 0) {
			holder.waittime.setText("" + context.getText(R.string.waitTime) + " " +
					HelperFunctions.renderAccurate(routeData.waitTime * (HelperFunctions.MINUTE)));
			holder.waittime.setVisibility(View.VISIBLE);
		} else {
			holder.waittime.setVisibility(View.GONE);
		}
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		ImageView symbol;
		TextView line;
		TextView transportDestination;
		TextView from;
		TextView fromTime;
		TextView to;
		TextView toTime;
		TextView waittime;
	}
}

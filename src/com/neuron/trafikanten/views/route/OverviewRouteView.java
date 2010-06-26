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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRoute;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.RouteSearchData;
import com.neuron.trafikanten.notification.NotificationDialog;
import com.neuron.trafikanten.views.map.GenericMap;

/*
 * This class shows a route selector list, when multiple travelproposals are sent.
 */

public class OverviewRouteView extends ListActivity {
	private final static String TAG = "Trafikanten-OverviewRouteView";
	private OverviewRouteAdapter routeList;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	private static final int MAP_ID = Menu.FIRST + 1;
	
	/*
	 * Dialogs
	 */
	private static final int DIALOG_NOTIFICATION = 1;
	private int selectedId = 0;
	
	/*
	 * Wanted Route, this is used as a base for the search.
	 */
	private RouteSearchData routeSearch;
	private TrafikantenRoute routeProvider;
	
	/*
	 * UI
	 */
	private TextView infoText;
	private GoogleAnalyticsTracker tracker;
	
	public static void ShowRoute(Activity activity, RouteSearchData routeSearch) {
		Intent intent = new Intent(activity, OverviewRouteView.class);
		intent.putExtra(RouteSearchData.PARCELABLE, routeSearch);
		activity.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        /*
         * Analytics
         */
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-16690738-3", this);
		tracker.trackPageView("/overviewRouteView");
		
		/*
		 * Setup the view
		 */
		setContentView(R.layout.route_overview);
		routeList = new OverviewRouteAdapter(this);
		infoText = (TextView) findViewById(R.id.emptyText);

		/*
		 * Load instance state
		 */
		if (savedInstanceState == null) {
			routeSearch = getIntent().getParcelableExtra(RouteSearchData.PARCELABLE);
			load();
		} else {
			routeSearch = savedInstanceState.getParcelable(RouteSearchData.PARCELABLE);
			final ArrayList<RouteProposal> list = savedInstanceState.getParcelableArrayList(OverviewRouteAdapter.KEY_ROUTELIST);
			routeList.setList(list);
			infoText.setVisibility(routeList.getCount() > 0 ? View.GONE : View.VISIBLE);
		}
		registerForContextMenu(getListView());
		setListAdapter(routeList);
	}
	
	/*
	 * Load station data
	 */
	private void load() {
    	if (routeProvider != null)
    		routeProvider.kill();
    	
    	routeList.getList().clear();
    	routeList.notifyDataSetChanged();
    	
		tracker.trackEvent("Data", "Route", "Data", 0);
    	routeProvider = new TrafikantenRoute(this, routeSearch, new IGenericProviderHandler<RouteProposal>() {
    		@Override
    		public void onExtra(int what, Object obj) {
    			/* Class has no extra data */
    		}
    		
			@Override
			public void onData(RouteProposal data) {
				routeList.addItem(data);
				routeList.notifyDataSetChanged();
				infoText.setVisibility(routeList.getCount() > 0 ? View.GONE : View.VISIBLE);
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				routeProvider = null; 
				if (exception != null) {
			    	routeList.getList().clear();
			    	routeList.notifyDataSetChanged();
					Log.w(TAG,"onException " + exception);
					infoText.setVisibility(View.VISIBLE);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	infoText.setText(R.string.trafikantenErrorParse);
			        } else {
			        	infoText.setText(R.string.trafikantenErrorOther);
			        }
					setProgressBarIndeterminateVisibility(false);
				} else {
					infoText.setText(R.string.noRoutesFound);
				}
			}

			@Override
			public void onPreExecute() {
				/*
				 * Send dispatch along with soap request, as they take time anyway.
				 */
				tracker.dispatch();

		    	setProgressBarIndeterminateVisibility(true);
			}
    		
    	});
	}
	
	/*
	 * Click on a list item
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DetailedRouteView.ShowRoute(this, routeList.getList(), position);
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
			return NotificationDialog.getDialog(this, tracker, 0);
		}
		return super.onCreateDialog(id);
	}

	/*
	 * Load data into dialog
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// notifyRouteData here is the first route data.
		final RouteData notifyRouteData = routeList.getItem(selectedId).travelStageList.get(0);
		/*
		 * Departure is what we base our notification on, 10 minuts before departure
		 */
		
		final long notifyDeparture = notifyRouteData.departure;
		final String notifyWith = notifyRouteData.line.equals(notifyRouteData.destination) ? notifyRouteData.line : notifyRouteData.line + " " + notifyRouteData.destination;
		NotificationDialog.setRouteData(routeList.getList(), selectedId, notifyDeparture, notifyWith);
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
		
		menu.add(0, MAP_ID, 0, R.string.map);
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
		case MAP_ID:
			GenericMap.Show(this, routeList.getList().get(selectedId).travelStageList, true, 0);
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onStop() {
		if (routeProvider != null) {
			routeProvider.kill();
		}
		super.onStop();
	}

	/*
	 * saveInstanceState saves all variables needed for onCreate
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	 @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(RouteSearchData.PARCELABLE, routeSearch);
		outState.putParcelableArrayList(OverviewRouteAdapter.KEY_ROUTELIST, routeList.getList());
	}
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Stop the tracker when it is no longer needed.
		tracker.stop();
	}
}

class OverviewRouteAdapter extends BaseAdapter {
	public static final String KEY_ROUTELIST = "routelist";
	private LayoutInflater inflater;
	private ArrayList<RouteProposal> items = new ArrayList<RouteProposal>();
	//final private Activity context;
	
	public OverviewRouteAdapter(Activity context) {
		inflater = LayoutInflater.from(context);
		//this.context = context;
	}
	
	/*
	 * Simple functions dealing with adding/setting items. 
	 */
	public ArrayList<RouteProposal> getList() { return items; }
	public void setList(ArrayList<RouteProposal> items) { this.items = items; }
	public void clear() { items.clear(); }
	
	/*
	 * Standard android.widget.Adapter items, self explanatory.
	 */
	@Override
	public int getCount() {	return items.size(); }
	@Override
	public RouteProposal getItem(int pos) { return items.get(pos); }
	@Override
	public long getItemId(int pos) { return pos; }
	public void addItem(RouteProposal item) { items.add(item); }
	
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
			convertView = inflater.inflate(R.layout.route_overview_list, null);
			
			holder = new ViewHolder();
			holder.travelTypes = (LinearLayout) convertView.findViewById(R.id.travelTypes);
			holder.departureTime = (TextView) convertView.findViewById(R.id.departureTime);
			holder.arrivalTime = (TextView) convertView.findViewById(R.id.arrivalTime);
			holder.travelTime = (TextView) convertView.findViewById(R.id.travelTime);

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
		final RouteProposal routeProposal = items.get(pos);
		long departure = 0;
		long arrival = 0;
		
		holder.travelTypes.removeAllViews();
		for(RouteData routeData : routeProposal.travelStageList) {
			/*
			 * Grab the first departure and last arrival to calculate total time
			 */
			if (departure == 0) {
				departure = routeData.departure;
			}
			arrival = routeData.arrival;
			
			/*
			 * Add Icon to travelTypes
			 */
			{
				final int symbolImage = routeData.transportType;
				if (symbolImage > 0) {
					final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.route_overview_traveltype, null);
					final TextView line = (TextView) layout.findViewById(R.id.line);
					final ImageView icon = (ImageView) layout.findViewById(R.id.icon);

					icon.setImageResource(symbolImage);
					if (routeData.line.length() > 0) {
						line.setText(routeData.line);
						line.setVisibility(View.VISIBLE);
					} else {
						line.setVisibility(View.GONE);
					}
					
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					params.leftMargin = 1;
					params.rightMargin = 1;
					holder.travelTypes.addView(layout, params);
				}
			}
		}
		
		{
			/*
			 * Setup the basic data
			 */
			holder.departureTime.setText(HelperFunctions.hourFormater.format(departure));
			holder.arrivalTime.setText(HelperFunctions.hourFormater.format(arrival));
			holder.travelTime.setText("Traveltime : " + HelperFunctions.hourFormater.format(arrival - departure));
		
		}
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		LinearLayout travelTypes;
		TextView travelTime;
		TextView departureTime;
		TextView arrivalTime;
	}
}


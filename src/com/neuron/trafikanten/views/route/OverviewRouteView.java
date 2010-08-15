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
import android.widget.AdapterView;
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
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteDeviData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.RouteSearchData;
import com.neuron.trafikanten.tasks.NotificationTask;
import com.neuron.trafikanten.tasks.SelectDeviTask;
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
	private static final int DEVI_ID = Menu.FIRST + 2;
	
	/*
	 * Dialogs
	 */
	private int selectedId = 0;
	
	/*
	 * Wanted Route, this is used as a base for the search.
	 */
	private RouteSearchData routeSearch;
	private TrafikantenRoute routeProvider;
	private RouteDeviLoader routeDeviLoader;
	public RouteDeviData deviList = new RouteDeviData();
	
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
			deviList = savedInstanceState.getParcelable(RouteDeviData.PARCELABLE);
			routeList.setList(list);
			infoText.setVisibility(routeList.getCount() > 0 ? View.GONE : View.VISIBLE);
			loadDevi(); // continue loading devi incase we stopped in the middle of a load.
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
				loadDevi();
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
	 * Load devi
	 */
	private void loadDevi() {
		routeDeviLoader = new RouteDeviLoader(this, deviList, new IGenericProviderHandler<Void>() {

			@Override
			public void onData(Void data) {}

			@Override
			public void onExtra(int i, Object data) {}

			@Override
			public void onPostExecute(Exception e) {
	        	infoText.setText(R.string.trafikantenErrorOther);
	        	routeDeviLoader = null;
	        	routeList.notifyDataSetChanged();
	        	loadDevi();
			}

			@Override
			public void onPreExecute() {}
			
		});
		if (routeDeviLoader.load(routeList.getList())) {
			setProgressBarIndeterminateVisibility(true);
		} else {
			setProgressBarIndeterminateVisibility(false);
		}				
	}
	
	/*
	 * Click on a list item
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DetailedRouteView.ShowRoute(this, routeList.getList(), deviList, position);
	}
	
	/*
	 * onCreate - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, MAP_ID, 0, R.string.map);
		
		if (routeList.getDevi(((AdapterView.AdapterContextMenuInfo) menuInfo).position, true).size() > 0) {
			menu.add(0, DEVI_ID, 0, R.string.warnings);
		}
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
			final RouteData notifyRouteData = routeList.getItem(selectedId).travelStageList.get(0);
			final String notifyWith = notifyRouteData.line.equals(notifyRouteData.destination) ? notifyRouteData.line : notifyRouteData.line + " " + notifyRouteData.destination;
			new NotificationTask(this, tracker, routeList.getList(), selectedId, deviList, notifyRouteData.departure, notifyWith);
			return true;
		case DEVI_ID:
			new SelectDeviTask(this, tracker, routeList.getDevi(info.position, false));
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
		if (routeDeviLoader != null) {
			routeDeviLoader.kill();
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
		outState.putParcelable(RouteDeviData.PARCELABLE, deviList);
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
	final private OverviewRouteView parent;
	
	public OverviewRouteAdapter(OverviewRouteView parent) {
		inflater = LayoutInflater.from(parent);
		this.parent = parent;
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
	
	public ArrayList<DeviData> getDevi(int pos, boolean checkOnly) {
		final ArrayList<DeviData> retList = new ArrayList<DeviData>();
		for(RouteData routeData : items.get(pos).travelStageList) {
			/*
			 * TODO, come up with a better way of id'ing the different values, using a string for this is dumb.
			 *  - this is also in routeDeviLoader
			 */
			final String deviKey = parent.deviList.getDeviKey(routeData.fromStation.stationId, routeData.line);
			ArrayList<DeviData> deviList = parent.deviList.items.get(deviKey);
			if (deviList != null) {
				for(DeviData devi : deviList) {
					retList.add(devi);
					if (checkOnly)
						return retList;
				}
			}
		}
		return retList;
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
			convertView = inflater.inflate(R.layout.route_overview_list, null);
			
			holder = new ViewHolder();
			holder.travelTypes = (LinearLayout) convertView.findViewById(R.id.travelTypes);
			holder.departureTime = (TextView) convertView.findViewById(R.id.departureTime);
			holder.arrivalTime = (TextView) convertView.findViewById(R.id.arrivalTime);
			holder.travelTime = (TextView) convertView.findViewById(R.id.travelTime);
			holder.deviSymbol = (TextView) convertView.findViewById(R.id.deviSymbol);

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
		
		if (getDevi(pos, true).size() > 0) {
			holder.deviSymbol.setVisibility(View.VISIBLE);						
		} else {
			holder.deviSymbol.setVisibility(View.GONE);
		}
		
		{
			/*
			 * Setup the basic data
			 */
			holder.departureTime.setText(HelperFunctions.hourFormater.format(departure));
			holder.arrivalTime.setText(HelperFunctions.hourFormater.format(arrival));

			holder.travelTime.setText(parent.getText(R.string.travelTime) + " : " + HelperFunctions.renderAccurate(arrival - departure));
		
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
		TextView deviSymbol;
	}
}


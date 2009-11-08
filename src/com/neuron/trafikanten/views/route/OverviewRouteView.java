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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProvider;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataProviders.ResultsProviderFactory;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.tasks.GenericTask;
import com.neuron.trafikanten.tasks.SearchRouteTask;

/*
 * This class shows a route selector list, when multiple travelproposals are sent.
 */

public class OverviewRouteView extends ListActivity {
	private final static String TAG = "Trafikanten-OverviewRouteView";
	private OverviewRouteAdapter routeList;
	
	/*
	 * Wanted Route, this is used as a base for the search.
	 */
	private RouteData routeData;
	
	public static void ShowRoute(Activity activity, RouteData routeData) {
		Intent intent = new Intent(activity, OverviewRouteView.class);
		intent.putExtra(RouteData.PARCELABLE, routeData);
		activity.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Setup the view
		 */
		setContentView(R.layout.route_overview);
		routeList = new OverviewRouteAdapter(this);

		/*
		 * Load instance state
		 */
		if (savedInstanceState == null) {
			routeData = getIntent().getParcelableExtra(RouteData.PARCELABLE);
			load();
		} else {
			routeData = savedInstanceState.getParcelable(RouteData.PARCELABLE);
			final ArrayList<RouteProposal> list = savedInstanceState.getParcelableArrayList(OverviewRouteAdapter.KEY_ROUTELIST);
			routeList.setList(list);
			setListAdapter(routeList);
		}
		registerForContextMenu(getListView());
	}
	
	/*
	 * Load station data
	 */
	private void load() {
		SearchRouteTask.StartTask(this, routeData);
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
	 * Handler for messages (both from Intent's and Handlers)
	 */
	public void onMessage(Message msg) {
    	switch(msg.what) {
    	case IRouteProvider.MESSAGE_DONE:
    		routeList.clear();
			/*
			 * If we haven't already set the most up to date list, set it.
			 */
    		final ArrayList<RouteProposal> list = ResultsProviderFactory.getRouteResults();
    		if (list != null) {
    			routeList.setList(list);
    		}
    		OverviewRouteView.this.setListAdapter(routeList);
    		break;
    	case IRouteProvider.MESSAGE_EXCEPTION:
    		final String exception = msg.getData().getString(IGenericProvider.KEY_EXCEPTION);
			Log.w(TAG,"onException " + exception);
			Toast.makeText(OverviewRouteView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
    		break;
    	}
	}
	
	/*
	 * activityResult is always a task, and can always be passed to onMessage
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			final Message msg = data.getParcelableExtra(GenericTask.KEY_MESSAGE);
			onMessage(msg);
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
    
    // TODO : We need notification support here.
    // TODO : We need savedInstanceState support here.
}

class OverviewRouteAdapter extends BaseAdapter {
	public static final String KEY_ROUTELIST = "routelist";
	private LayoutInflater inflater;
	private ArrayList<RouteProposal> items = new ArrayList<RouteProposal>();
	private Context context;
	
	public OverviewRouteAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		this.context = context;
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
			convertView = inflater.inflate(R.layout.route_overview_list, null);
			
			holder = new ViewHolder();
			holder.header = (TextView) convertView.findViewById(R.id.header);
			holder.routeInfo = (LinearLayout) convertView.findViewById(R.id.routeInfo);
			holder.footer = (TextView) convertView.findViewById(R.id.footer);

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
		int switches = 0;
		
		holder.routeInfo.removeAllViews();
		for(RouteData routeData : routeProposal.travelStageList) {
			/*
			 * Grab the first departure and last arrival to calculate total time
			 */
			if (departure == 0) {
				departure = routeData.departure;
			}
			arrival = routeData.arrival;

			final RelativeLayout layout = new RelativeLayout(context);
			
			/*
			 * Add Departure
			 */
			{
				final TextView textView = new TextView(context);
				textView.setText(HelperFunctions.hourFormater.format(routeData.departure));
				textView.setId(1);
				layout.addView(textView);
			}
			/*
			 * Add Departure text
			 */
			{
				final TextView textView = new TextView(context);
				textView.setText(routeData.fromStation.stopName);
				
				final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.BELOW, 1);
				
				layout.addView(textView, params);
			}
			
			/*
			 * Add arrival
			 */
			{
				final TextView textView = new TextView(context);
				textView.setText(HelperFunctions.hourFormater.format(routeData.departure));
				
				final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				
				layout.addView(textView, params);
			}
			
			holder.routeInfo.addView(layout);
			
			

			/*final TextView textView = new TextView(context);
			if (routeData.transportType != IRouteProvider.TRANSPORT_WALK) {
				/*
				 * If we're not walking, show line number and increase the amount of switches we are doing.
				 *
				textView.setText(routeData.line + " " + 
					HelperFunctions.hourFormater.format(routeData.departure) + " " + 
					routeData.fromStation.stopName + " -> " + 
					routeData.toStation.stopName);
				switches++;
			} else {
				/*
				 * We're talking, render that in routeInfo
				 *
				textView.setText(context.getText(R.string.walk) + " " + 
						HelperFunctions.hourFormater.format(routeData.departure) + " " + 
						routeData.fromStation.stopName + " -> " + 
						routeData.toStation.stopName);
			}
			holder.routeInfo.addView(textView);*/
		}
		
		
		holder.header.setText("Route " + (pos + 1) + " = " + HelperFunctions.hourFormater.format(departure) + " -> " +
				HelperFunctions.hourFormater.format(arrival) + " total " + 
				HelperFunctions.hourFormater.format(arrival - departure));
		//holder.footer.setText("TODO : Waittime/Switches/Walktime");
		
		
		
		/*
		 * Setup waittime
		 */
		/*if (routeData.waitTime > 0) {
			holder.waittime.setText("" + context.getText(R.string.waitTime) + " " +
					HelperFunctions.renderAccurate(routeData.waitTime * (HelperFunctions.MINUTE)));
			holder.waittime.setVisibility(View.VISIBLE);
		} else {
			holder.waittime.setVisibility(View.GONE);
		}*/
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		TextView header;
		LinearLayout routeInfo;
		TextView footer;
	}
}

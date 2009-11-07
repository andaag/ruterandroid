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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.dataProviders.IGenericProvider;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataProviders.ResultsProviderFactory;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.tasks.GenericTask;
import com.neuron.trafikanten.tasks.SearchRouteTask;
import com.neuron.trafikanten.views.route.RouteAdapter.ViewHolder;

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
		
		if (routeData.transportType == IRouteProvider.TRANSPORT_WALK && routeData.destination == null) {
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
		holder.symbol.setVisibility(View.GONE);
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

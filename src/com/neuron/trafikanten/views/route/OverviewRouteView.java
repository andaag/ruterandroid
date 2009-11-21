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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataProviders.IRouteProvider.RouteProviderHandler;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;

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
	private IRouteProvider routeProvider;
	
	public static void ShowRoute(Activity activity, RouteData routeData) {
		Intent intent = new Intent(activity, OverviewRouteView.class);
		intent.putExtra(RouteData.PARCELABLE, routeData);
		activity.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
		setListAdapter(routeList);
	}
	
	/*
	 * Load station data
	 */
	private void load() {
    	setProgressBarIndeterminateVisibility(true);
    	if (routeProvider != null)
    		routeProvider.Stop();
    	
    	routeList.getList().clear();
    	routeList.notifyDataSetChanged();
    	
    	routeProvider = DataProviderFactory.getRouteProvider(getResources(), new RouteProviderHandler() {
			@Override
			public void onData(RouteProposal routeProposal) {
				routeList.addItem(routeProposal);
				routeList.notifyDataSetChanged();
			}

			@Override
			public void onError(Exception exception) {
				Log.w(TAG,"onException " + exception);
				Toast.makeText(OverviewRouteView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
				setProgressBarIndeterminateVisibility(false);
			}

			@Override
			public void onFinished() {
				setProgressBarIndeterminateVisibility(false);
				/*
				 * Show info text if view is empty
				 */
				/*final TextView infoText = (TextView) findViewById(R.id.emptyText);
				infoText.setVisibility(routeList.getCount() > 0 ? View.GONE : View.VISIBLE);*/
			}
    		
    	});
    	routeProvider.Search(routeData);
	}
	
	/*
	 * Click on a list item
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DetailedRouteView.ShowRoute(this, routeList.getList(), position);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(RouteData.PARCELABLE, routeData);
		outState.putParcelableArrayList(OverviewRouteAdapter.KEY_ROUTELIST, routeList.getList());
	}
	


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
		
		holder.routeInfo.removeAllViews();
		boolean first = true;
		for(RouteData routeData : routeProposal.travelStageList) {
			/*
			 * Grab the first departure and last arrival to calculate total time
			 */
			if (departure == 0) {
				departure = routeData.departure;
			}
			arrival = routeData.arrival;
			
			/*
			 * Add Icon
			 */
			/*{
				final int symbolImage = DataProviderFactory.getImageResource(routeData.transportType);
				if (symbolImage > 0) {
					final ImageView imageView = new ImageView(context);
					imageView.setImageResource(symbolImage);
					holder.routeInfo.addView(imageView);
				}
			}*/

			
			/*
			 * Add text line
			 */
			{
				final TextView textView = new TextView(context);
				final long minDiff = (routeData.arrival - routeData.departure) / HelperFunctions.MINUTE;
				final String line = routeData.transportType == IRouteProvider.TRANSPORT_WALK ? context.getText(R.string.walk).toString() : routeData.line;
				if (!first)
					textView.setText(", " + line + " (" + minDiff + "m)");
				else
					textView.setText(line + " (" + minDiff + "m)");
				
				textView.setSingleLine();
				holder.routeInfo.addView(textView);
			}
			first = false;
		}
		
		
		final long minDiff = (arrival - departure) / HelperFunctions.MINUTE;
		holder.footer.setText("Departure " + HelperFunctions.hourFormater.format(departure) + " arrival " + HelperFunctions.hourFormater.format(arrival) + " total " + minDiff + "m");
		
		
		
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
		LinearLayout routeInfo;
		TextView footer;
	}
}

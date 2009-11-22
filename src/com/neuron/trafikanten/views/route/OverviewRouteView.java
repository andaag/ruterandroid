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
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
import com.neuron.trafikanten.dataSets.RouteSearchData;

/*
 * This class shows a route selector list, when multiple travelproposals are sent.
 */

public class OverviewRouteView extends ListActivity {
	private final static String TAG = "Trafikanten-OverviewRouteView";
	private OverviewRouteAdapter routeList;
	
	/*
	 * Wanted Route, this is used as a base for the search.
	 */
	private RouteSearchData routeSearch;
	private IRouteProvider routeProvider;
	
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
		 * Setup the view
		 */
		setContentView(R.layout.route_overview);
		routeList = new OverviewRouteAdapter(this);

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
				routeProvider = null; 
				/*
				 * Show info text if view is empty
				 */
				/*final TextView infoText = (TextView) findViewById(R.id.emptyText);
				infoText.setVisibility(routeList.getCount() > 0 ? View.GONE : View.VISIBLE);*/
			}
    		
    	});
    	routeProvider.Search(routeSearch);
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
		outState.putParcelable(RouteSearchData.PARCELABLE, routeSearch);
		outState.putParcelableArrayList(OverviewRouteAdapter.KEY_ROUTELIST, routeList.getList());
	}
}

/*
 * this renders fancy looking text for our text overview
 */
class RenderOverviewText {
	/*
	 * Contains text to add + the style
	 */
	class SpannableSet {
		public String text;
		public StyleSpan style;
		public SpannableSet(String text, StyleSpan style) {
			this.text = text;
			this.style = style;
		}
	}
	ArrayList<SpannableSet> spannableSet = new ArrayList<SpannableSet>();
	
	public void addString(String text, StyleSpan style) {
		spannableSet.add(new SpannableSet(text, style));
	}
	
	public SpannableString toSpannableString() {
		StringBuffer fullString = new StringBuffer();
		/*
		 * Construct the text
		 */
		for (SpannableSet spanSet : spannableSet) {
			fullString.append(spanSet.text);
		}
		
		SpannableString s = new SpannableString(fullString.toString());
		int pos = 0;
		for (SpannableSet spanSet : spannableSet) {
			final int len = spanSet.text.length();
			if (spanSet.style != null)
				s.setSpan(spanSet.style, pos, len, 0); 
			pos = pos + len;
		}
		return s;
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
			holder.proposalIcons = (LinearLayout) convertView.findViewById(R.id.proposalIcons);
			holder.routeInfo = (TextView) convertView.findViewById(R.id.routeInfo);
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
		
		holder.proposalIcons.removeAllViews();
		String routeInfoText = null;
		for(RouteData routeData : routeProposal.travelStageList) {
			/*
			 * Grab the first departure and last arrival to calculate total time
			 */
			if (departure == 0) {
				departure = routeData.departure;
			}
			arrival = routeData.arrival;
			
			/*
			 * Add Icon to proposalIcons
			 */
			{
				final int symbolImage = DataProviderFactory.getImageResource(routeData.transportType);
				if (symbolImage > 0 && symbolImage != IRouteProvider.TRANSPORT_UNKNOWN) {
					final ImageView imageView = new ImageView(context);
					imageView.setImageResource(symbolImage);
					holder.proposalIcons.addView(imageView);
				}
			}

			
			/*
			 * Add text line
			 */
			{
				final long minDiff = (routeData.arrival - routeData.departure) / HelperFunctions.MINUTE;
				final String line = routeData.transportType == IRouteProvider.TRANSPORT_WALK ? context.getText(R.string.walk).toString() : routeData.line;
				if (routeInfoText == null) {
					routeInfoText = line + " (" + minDiff + "m)";
				} else {
					routeInfoText = routeInfoText + ", " + line + " (" + minDiff + "m)";
				}
			}
		}
		
		
		final long minDiff = (arrival - departure) / HelperFunctions.MINUTE;
		holder.routeInfo.setText(routeInfoText);
		holder.routeInfo.setSingleLine();
		
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
		LinearLayout proposalIcons;
		TextView routeInfo;
		TextView footer;
	}
}

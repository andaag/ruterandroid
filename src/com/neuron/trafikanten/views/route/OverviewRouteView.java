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
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataProviders.IRouteProvider.RouteProviderHandler;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.RouteSearchData;
import com.neuron.trafikanten.notification.NotificationDialog;

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
	
	/*
	 * Dialogs
	 */
	private static final int DIALOG_NOTIFICATION = 1;
	private int selectedId = 0;
	
	/*
	 * Wanted Route, this is used as a base for the search.
	 */
	private RouteSearchData routeSearch;
	private IRouteProvider routeProvider;
	
	/*
	 * UI
	 */
	private TextView infoText;
	
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
				infoText.setVisibility(routeList.getCount() > 0 ? View.GONE : View.VISIBLE);
			}

			@Override
			public void onError(Exception exception) {
				Log.w(TAG,"onException " + exception);
				infoText.setVisibility(View.VISIBLE);
				if (exception.getClass().getSimpleName().equals("ParseException")) {
					infoText.setText("" + getText(R.string.parseError) + ":" + "\n\n" + exception);
				} else {
					infoText.setText("" + getText(R.string.exception) + ":" + "\n\n" + exception);
				}
				setProgressBarIndeterminateVisibility(false);
			}

			@Override
			public void onFinished() {
				infoText.setText(R.string.noRoutesFound);
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
	 * Make sure we kill off threads when freeing memory.
	 */
	@Override
	public void finish() {
		if (routeProvider != null) {
			routeProvider.Stop();
		}
		super.finish();
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
}

/*
 * this renders fancy looking text for our text overview
 */
class RenderOverviewText {
	/*
	 * Contains text to add + the style
	 */
	private class SpannableSet {
		public Object style;
		public int start;
		public int end;
		public int flags;
		public SpannableSet(int start, int end, Object style, int flags) {
			this.style = style;
			this.start = start;
			this.end = end;
			this.flags = flags;
		}
	}
	private ArrayList<SpannableSet> spannableSet = new ArrayList<SpannableSet>();
	private StringBuffer textBuffer = new StringBuffer();
	
	public int length() {
		return textBuffer.length();
	}
	
	public void addString(String text, Object style, int flags) {
		if (style != null) {
			final int start = textBuffer.length();
			final int end = start + text.length();
			spannableSet.add(new SpannableSet(start, end, style, flags));
		}
		textBuffer.append(text);
	}
	
	public SpannableString toSpannableString() {
		SpannableString s = new SpannableString(textBuffer.toString());
		for (SpannableSet spanSet : spannableSet) {
			s.setSpan(spanSet.style, spanSet.start, spanSet.end, spanSet.flags); 
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
		RenderOverviewText routeInfoText = new RenderOverviewText();
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
				if (symbolImage > 0) {
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
				if (routeInfoText.length() == 0) {
					/*routeInfoText.addString(line + " (", null, 0);
					routeInfoText.addString(minDiff + "m", new ForegroundColorSpan(Color.YELLOW), Spanned.SPAN_COMPOSING);
					routeInfoText.addString(")", null, 0);*/
					routeInfoText.addString(line + " (" + minDiff + "m)", null, 0);
				} else {
					/*routeInfoText.addString(", " + line + " (", null, 0);
					routeInfoText.addString(minDiff + "m", new ForegroundColorSpan(Color.YELLOW), Spanned.SPAN_COMPOSING);
					routeInfoText.addString(")", null, 0);*/
					routeInfoText.addString(", " + line + " (" + minDiff + "m)", null, 0);
				}
			}
		}
		holder.routeInfo.setText(routeInfoText.toSpannableString());
		holder.routeInfo.setSingleLine();
		
		{
			/*
			 * Footer text
			 */
			final long minDiff = (arrival - departure) / HelperFunctions.MINUTE;
			RenderOverviewText footerText = new RenderOverviewText();
			footerText.addString("Departure ", null, 0);
			footerText.addString(HelperFunctions.hourFormater.format(departure),new ForegroundColorSpan(Color.YELLOW), Spanned.SPAN_COMPOSING);
			footerText.addString(" arrival ", null, 0);
			footerText.addString(HelperFunctions.hourFormater.format(arrival),new ForegroundColorSpan(Color.YELLOW), Spanned.SPAN_COMPOSING);
			footerText.addString(" total ", null, 0);
			footerText.addString(new Long(minDiff).toString() + "m",new ForegroundColorSpan(Color.YELLOW), Spanned.SPAN_COMPOSING);
			holder.footer.setText(footerText.toSpannableString());
			holder.footer.setSingleLine();
		}
		
		
		
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

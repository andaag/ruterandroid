package com.neuron.trafikanten.dataSets.listadapters;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.listadapters.GenericKeyList.RealtimeDataRendererData;
import com.neuron.trafikanten.hacks.StationIcons;
import com.neuron.trafikanten.views.GenericDeviCreator;

public abstract class GenericRealtimeAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private Activity activity;
	public GoogleAnalyticsTracker tracker;
	public static final String KEY_REALTIMELIST = "realtimelist";
	public int itemsAddedWithoutNotify = 0; // List of items added during load without a .notifyDataUpdated
	public GenericKeyList items;
	public abstract void addRealtimeData(RealtimeData data);
	public void addDevi(DeviData data) {
		items.addDevi(data);
	}
	
	public GenericRealtimeAdapter(Activity activity, GoogleAnalyticsTracker tracker) {
		this.activity = activity;
		this.tracker = tracker;
		inflater = LayoutInflater.from(activity);
		// NOTE, items are created by parent
	}
	
	/*
	 * Clearing the list 
	 */
	public void clear() {
		items.clear();
		itemsAddedWithoutNotify = 0;
	}
	/*
	 * Saving instance state
	 */
	public void saveInstanceState(Bundle outState) {
		outState.putParcelable(KEY_REALTIMELIST, items);
	}
	
	
	/*
	 * Loading instance state
	 */
	public void loadInstanceState(Bundle inState) {
		items = inState.getParcelable(KEY_REALTIMELIST);		
	}
	
	/*
	 * Standard android.widget.Adapter items, self explanatory.
	 */
	@Override
	public int getCount() {
		if (itemsAddedWithoutNotify > 0) {
			itemsAddedWithoutNotify = 0;
			notifyDataSetChanged(); // This is incase getCount is called between our data set updates, which triggers IllegalStateException, listView does a simple if (mItemCount != mAdapter.getCount()) {
		}
		return items.size();
	}
	

	@Override
	public long getItemId(int pos) { return pos; }
	@Override
	public RealtimeDataRendererData getItem(int pos) {
		return items.getRealtimeData(pos);
	}
	
	public abstract void triggerOnClick(RealtimeData data);
	
	/*
	 * Setup the view
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int pos, View convertView, ViewGroup arg2) {
		final RealtimeDataRendererData renderData = getItem(pos);
		final RealtimeData data = renderData.data; 
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
			//ASDASDASDADASD holder.platform = (TextView) convertView.findViewById(R.id.platform);
			holder.line = (TextView) convertView.findViewById(R.id.line);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.destination = (TextView) convertView.findViewById(R.id.destination);
			holder.departures = (TextView) convertView.findViewById(R.id.departures);
			holder.departures.setTypeface(GenericDeviCreator.getDeviTypeface(activity));
			holder.departures.setMovementMethod(ScrollingMovementMethod.getInstance());
			holder.departures.setHorizontallyScrolling(true);
			holder.departureInfo = (LinearLayout) convertView.findViewById(R.id.departureInfo);
			
			convertView.setTag(holder);
		} else {
			/*
			 * Old view found, we can reuse that instead of inflating.
			 */
			holder = (ViewHolder) convertView.getTag();
		}
		
		/*
		 * Workaround for clickable bug, onListItemClick does not trigger at all if ScrollingMovementMethod is being used.
		 */
		{
			final TableLayout tableLayout = (TableLayout) convertView.findViewById(R.id.tablelayout);
			tableLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					triggerOnClick(data);
				}
			});
			tableLayout.setLongClickable(true);
		}
		
		/*
		 * Render data to view.
		 */
		holder.departures.setText(data.renderDepartures(System.currentTimeMillis() - getTimeDifference(), activity));
		holder.destination.setText(data.destination);
		if (data.destination.equals(data.line)) {
			holder.line.setText("-");
		} else {
			holder.line.setText(data.line);
		}
		
		if (renderData.renderHeader && renderData.header != null && renderData.header.length() > 0) {
			holder.platform.setText(renderData.header);
			holder.platform.setVisibility(View.VISIBLE);
		} else {
			holder.platform.setVisibility(View.GONE);
		}
		
		holder.icon.setImageResource(StationIcons.hackGetLineIcon(data.line));
		
		/*
		 * Setup devi
		 */
		if (data.devi.size() > 0) {
			holder.departureInfo.setVisibility(View.VISIBLE);
			holder.departureInfo.removeAllViews();
			
			for (DeviData devi : data.devi) {
				/*
				 * Add all devi items.
				 */
				holder.departureInfo.addView(GenericDeviCreator.createDefaultDeviText(activity, tracker, devi.title, devi, false), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			}
		} else {
			holder.departureInfo.setVisibility(View.GONE);
		}
		
		return convertView;
	}
	
	public abstract long getTimeDifference();
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		TextView platform;
		
		TextView line;
		TextView destination;
		ImageView icon;
		TextView departures;
		
		LinearLayout departureInfo;
	}
}

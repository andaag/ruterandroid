package com.neuron.trafikanten.views.realtime;

import android.app.Activity;
import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;
import com.neuron.trafikanten.dataSets.realtime.renderers.GenericRealtimeRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.PlatformRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.RealtimeRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.StationRenderer;
import com.neuron.trafikanten.hacks.StationIcons;
import com.neuron.trafikanten.tasks.ShowRealtimeLineDetails;
import com.neuron.trafikanten.views.GenericDeviCreator;

/*
 * This list adapter is shared between favorites and realtime views
 */
public class GenericRealtimeListAdapter extends BaseAdapter {
	private GenericRealtimeView parent;
	private LayoutInflater inflater;
	public GenericRealtimeList items;
	private boolean dirty = false;
	
	private Activity activity;
	
	public GenericRealtimeListAdapter(GenericRealtimeView parent, Activity activity, int groupBy) {
		super();
		this.parent = parent;
		inflater = LayoutInflater.from(parent);
		this.activity = activity;
		items = new GenericRealtimeList(groupBy);
	}
	
	public boolean addData(DeviData deviData) {
		boolean added = items.addData(deviData);
		dirty = true;
		return added;
	}
	public void addData(RealtimeData data) {
		items.addData(data);
		dirty = true;
	}
	public void addData(RealtimeData data, StationData station) {
		items.addData(data, station);
		dirty = true;
	}

	public void clear() {
		items.clear();
    	notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if (dirty) {
			dirty = false;
			notifyDataSetChanged();
		}
		return items.size();
	}

	@Override
	public GenericRealtimeRenderer getItem(int pos) {
		return items.get(pos);
	}
	public RealtimeData getRealtimeItem(int pos) {
		final GenericRealtimeRenderer renderer = getItem(pos);
		if (renderer.renderType == GenericRealtimeList.RENDERER_REALTIME) {
			final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
			return realtimeRenderer.data;
		}
		return null;
	}
	
	/*
	 * This finds a station belonging to a realtime item position. It does it by looking up until it finds a station.
	 */
	public StationData getStationForRealtimeItem(int pos) {
		while (pos >= 0) {
			final GenericRealtimeRenderer renderer = getItem(pos);
			if (renderer.renderType == GenericRealtimeList.RENDERER_STATION) {
				return ((StationRenderer) renderer).station;
			}
			pos--;
		}
		return null;
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		dirty = false;
	}

	@Override
	public void notifyDataSetInvalidated() {
		super.notifyDataSetInvalidated();
		dirty = false;
	}

	@Override
	public long getItemId(int position) { return position; }

	
	@Override
	public int getItemViewType(int pos) {
		final GenericRealtimeRenderer renderer = getItem(pos);
		return renderer.renderType - 1; // view types are 0 indexed.
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}
	
	
	/*
	 * Workaround for clickable bug, onListItemClick does not trigger at all if ScrollingMovementMethod is being used.
	 * TODO : Check if this workaround is still needed, HACK
	 */
	public void tableLayoutOnclickHack(View convertView, final RealtimeRenderer realtimeRenderer) {
		final TableLayout tableLayout = (TableLayout) convertView.findViewById(R.id.tablelayout);
		tableLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new ShowRealtimeLineDetails(parent, System.currentTimeMillis() - parent.timeDifference, realtimeRenderer.data);
			}
		});
		tableLayout.setLongClickable(true);
	}
	
	/*
	 * Setup the view
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int pos, View convertView, ViewGroup arg2) {
		final GenericRealtimeRenderer renderer = getItem(pos);
		
		/*
		 * Setup holder, for performance and readability.
		 */
		
		if (convertView == null) {
			/*
			 * New view, inflate and setup holder.
			 */
			switch(renderer.renderType) {
			case GenericRealtimeList.RENDERER_REALTIME:
				final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
				final ViewHolderRealtime holderRealtime = new ViewHolderRealtime();
				convertView = inflater.inflate(R.layout.realtime_list, null);
				holderRealtime.line = (TextView) convertView.findViewById(R.id.line);
				holderRealtime.icon = (ImageView) convertView.findViewById(R.id.icon);
				holderRealtime.destination = (TextView) convertView.findViewById(R.id.destination);
				holderRealtime.departures = (TextView) convertView.findViewById(R.id.departures);
				holderRealtime.departures.setTypeface(GenericDeviCreator.getDeviTypeface(parent));
				holderRealtime.departures.setMovementMethod(ScrollingMovementMethod.getInstance());
				holderRealtime.departures.setHorizontallyScrolling(true);
				holderRealtime.departureInfo = (LinearLayout) convertView.findViewById(R.id.departureInfo);
				renderRealtimeView(holderRealtime, realtimeRenderer.data);
				convertView.setTag(R.layout.realtime_list, holderRealtime);
				tableLayoutOnclickHack(convertView, realtimeRenderer);
				
				return convertView;
			case GenericRealtimeList.RENDERER_PLATFORM:
				final ViewHolderPlatform holderPlatform = new ViewHolderPlatform();
				final PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
				
				convertView = inflater.inflate(R.layout.realtime_list_platform, null);
				holderPlatform.header = (TextView) convertView.findViewById(R.id.header);
				
				renderPlatformView(holderPlatform, platformRenderer.platform);
				convertView.setTag(R.layout.realtime_list_platform, holderPlatform);
				return convertView;
			case GenericRealtimeList.RENDERER_STATION:
				final ViewHolderStation holderStation = new ViewHolderStation();
				final StationRenderer stationRenderer = (StationRenderer) renderer;
				
				convertView = inflater.inflate(R.layout.realtime_list_station, null);
				holderStation.header = (TextView) convertView.findViewById(R.id.header);
				holderStation.devi = (LinearLayout) convertView.findViewById(R.id.devi);
				
				renderStationView(holderStation, stationRenderer.station);
				convertView.setTag(R.layout.realtime_list_station, holderStation);
				return convertView;
			}


		} else {
			switch(renderer.renderType) {
			case GenericRealtimeList.RENDERER_REALTIME:
				final ViewHolderRealtime holderRealtime = (ViewHolderRealtime) convertView.getTag(R.layout.realtime_list);
				final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
				renderRealtimeView(holderRealtime, realtimeRenderer.data);
				tableLayoutOnclickHack(convertView, realtimeRenderer);
				return convertView;
			case GenericRealtimeList.RENDERER_PLATFORM:
				final ViewHolderPlatform holderPlatform = (ViewHolderPlatform) convertView.getTag(R.layout.realtime_list_platform);
				final PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
				renderPlatformView(holderPlatform, platformRenderer.platform);
				return convertView;
				
			case GenericRealtimeList.RENDERER_STATION:
				final ViewHolderStation holderStation = (ViewHolderStation) convertView.getTag(R.layout.realtime_list_station);
				final StationRenderer stationRenderer = (StationRenderer) renderer;
				renderStationView(holderStation, stationRenderer.station);
				return convertView;				
			}
		}
		

		
		return convertView;
	}
	
	private void renderPlatformView(ViewHolderPlatform holder, final String platform) {
		if (platform != null) {
			holder.header.setText("Plattform " + platform);
			holder.header.setVisibility(View.VISIBLE);
		} else {
			holder.header.setVisibility(View.GONE);
		}
	}
	
	private void renderStationView(ViewHolderStation holder, StationData station) {
		holder.header.setText(station.stopName);
		
		holder.devi.removeAllViews();
		if (station.devi.size() > 0) {
			holder.devi.setVisibility(View.VISIBLE);
    		for (final DeviData deviData : station.devi) {
				holder.devi.addView(GenericDeviCreator.createDefaultDeviText(activity, deviData.title, deviData, true), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    		}
		} else {
			holder.devi.setVisibility(View.GONE);
			
		}

	}
	
	private void renderRealtimeView(ViewHolderRealtime holder, final RealtimeData data) {
		/*
		 * Render data to view.
		 */
		holder.departures.setText(data.renderDepartures(System.currentTimeMillis() - parent.timeDifference, parent));
		holder.destination.setText(data.destination);
		if (data.destination.equals(data.line)) {
			holder.line.setText("-");
		} else {
			holder.line.setText(data.line);
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
				holder.departureInfo.addView(GenericDeviCreator.createDefaultDeviText(parent, devi.title, devi, false), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			}
		} else {
			holder.departureInfo.setVisibility(View.GONE);
		}
	}
	

	
	/*
	 * Classes for caching the view.
	 */
	static class ViewHolderPlatform {
		TextView header;
	}
	
	static class ViewHolderStation {
		TextView header;
		LinearLayout devi;
	}
	
	static class ViewHolderRealtime {
		TextView line;
		TextView destination;
		ImageView icon;
		TextView departures;
		
		LinearLayout departureInfo;
	}
	
	public Parcelable getParcelable() {
		return items;
	}
	
	public void setItems(Parcelable items) {
		this.items = (GenericRealtimeList) items;
		dirty = true;
	}

	
}
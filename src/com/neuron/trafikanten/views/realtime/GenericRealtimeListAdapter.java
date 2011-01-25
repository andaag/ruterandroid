package com.neuron.trafikanten.views.realtime;

import android.os.Parcelable;
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

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;
import com.neuron.trafikanten.dataSets.realtime.renderers.GenericRealtimeRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.PlatformRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.RealtimeRenderer;
import com.neuron.trafikanten.hacks.StationIcons;
import com.neuron.trafikanten.tasks.ShowRealtimeLineDetails;
import com.neuron.trafikanten.views.GenericDeviCreator;

/*
 * This list adapter is shared between favorites and realtime views
 */
public class GenericRealtimeListAdapter extends BaseAdapter {
	private GenericRealtimeView parent;
	private LayoutInflater inflater;
	public GenericRealtimeList items = new GenericRealtimeList(GenericRealtimeList.RENDERER_PLATFORM);
	private boolean dirty = false;
	
	public GenericRealtimeListAdapter(GenericRealtimeView parent) {
		super();
		this.parent = parent;
		inflater = LayoutInflater.from(parent);
	}
	
	public void addData(DeviData deviData) {
		/*
		 * Yes, this is needed. Stupid getParcebleArrayList gets confused otherwise..
		 */
		items.addData(deviData);
		dirty = true;
	}
	public void addData(RealtimeData data) {
		/*
		 * Yes, this is needed. Stupid getParcebleArrayList gets confused otherwise..
		 */
		items.addData(data);
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
		GenericRealtimeRenderer renderer = getItem(pos);
		if (renderer.renderType == GenericRealtimeList.RENDERER_REALTIME) {
			final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
			return realtimeRenderer.data;
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
		return 2;
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
				
				
				/*
				 * Workaround for clickable bug, onListItemClick does not trigger at all if ScrollingMovementMethod is being used.
				 * TODO : Check if this workaround is still needed, HACK
				 */
				{
					final TableLayout tableLayout = (TableLayout) convertView.findViewById(R.id.tablelayout);
					tableLayout.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							new ShowRealtimeLineDetails(parent, parent.tracker, System.currentTimeMillis() - parent.timeDifference, realtimeRenderer.data);
						}
					});
					tableLayout.setLongClickable(true);
				}
				
				return convertView;
			case GenericRealtimeList.RENDERER_PLATFORM:
				final ViewHolderHeader holderHeader = new ViewHolderHeader();
				final PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
				
				convertView = inflater.inflate(R.layout.realtime_list_header, null);
				holderHeader.header = (TextView) convertView.findViewById(R.id.header);
				
				renderHeaderView(holderHeader, "Plattform " + platformRenderer.platform);
				convertView.setTag(R.layout.realtime_list_header, holderHeader);
				return convertView;
			}


		} else {
			switch(renderer.renderType) {
			case GenericRealtimeList.RENDERER_REALTIME:
				final ViewHolderRealtime holderRealtime = (ViewHolderRealtime) convertView.getTag(R.layout.realtime_list);
				final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
				renderRealtimeView(holderRealtime, realtimeRenderer.data);
				return convertView;
			case GenericRealtimeList.RENDERER_PLATFORM:
				final ViewHolderHeader holderHeader = (ViewHolderHeader) convertView.getTag(R.layout.realtime_list_header);
				final PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
				renderHeaderView(holderHeader, "Plattform " + platformRenderer.platform);
				return convertView;
			}
		}
		

		
		return convertView;
	}
	
	private ViewHolderRealtime renderRealtimeView(ViewHolderRealtime holder, final RealtimeData data) {

		
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
				holder.departureInfo.addView(GenericDeviCreator.createDefaultDeviText(parent, parent.tracker, devi.title, devi, false), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			}
		} else {
			holder.departureInfo.setVisibility(View.GONE);
		}
		return holder;
	}
	
	private ViewHolderHeader renderHeaderView(ViewHolderHeader holder, String title) {
		if (title != null && title.length() > 0) {
			holder.header.setText(title);
			holder.header.setVisibility(View.VISIBLE);
		} else {
			holder.header.setVisibility(View.GONE);
		}
		return holder;
	}
	
	/*
	 * Classes for caching the view.
	 */
	static class ViewHolderHeader {
		TextView header;
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
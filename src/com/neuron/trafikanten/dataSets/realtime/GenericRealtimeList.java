package com.neuron.trafikanten.dataSets.realtime;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.dataSets.realtime.renderers.GenericRealtimeRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.PlatformRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.RealtimeRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.StationRenderer;

/*
 * This is GenericRealtimeListAdapter's list. Aka GenericRealtimeListAdapter.items
 */
public class GenericRealtimeList implements Parcelable {
	private static final String TAG = "Trafikanten-GenericRealtimeListAdapter";
	private static final long serialVersionUID = 4587512040075849425L;
	public static final int RENDERER_REALTIME = 1;
	public static final int RENDERER_PLATFORM = 2;
	public static final int RENDERER_STATION = 3;
	private ArrayList<GenericRealtimeRenderer> items;
	private int groupBy;
	
	public GenericRealtimeList(int groupBy) {
		super();
		this.groupBy = groupBy;
		items = new ArrayList<GenericRealtimeRenderer>();
	}
	
	public void clear() { items.clear(); }
	public int size() { return items.size(); }
	public GenericRealtimeRenderer get(int pos) { return items.get(pos); }
	
	public void addData(RealtimeData data, StationData station) {
		int i = 0;
		final int size = items.size();
		if (groupBy != RENDERER_STATION) {
			Log.e(TAG, "Error, groupBy expected RENDERER_STATION");
		}
		/*
		 * This will convert a flat list of RealtimeData to:
		 * station ->
		 *   realtimeData line,departure ->
		 *     realtimeData.nextDepartures if line.departure matches previous find.
		 */
		for (;i < size;i++) {
			//Log.d("DEBUGTrafikanten", "List iteration " + i + " / " + size);
			final GenericRealtimeRenderer renderer = items.get(i);
			if (renderer.renderType == RENDERER_STATION) {
				StationRenderer stationRenderer = (StationRenderer) renderer;
				/*
				 * Found our station
				 */
				if (stationRenderer.station.stationId == station.stationId) {
					// we're in the right station, keep going until we find the end of it.
					i++;
					while (i < size) {
						final GenericRealtimeRenderer subRenderer = items.get(i);
						//Log.d("DEBUGTrafikanten", "sub list iteration " + i + " / " + size);
						if (subRenderer.renderType == RENDERER_STATION) {
							//Log.d("DEBUGTrafikanten", "Position " + i + " contains next station, must insert above");
							// ok we just ran into the next station, break and insert
							break;
						} else if (subRenderer.renderType == RENDERER_REALTIME) {
							/*
							 * This is the same departure as us, merge
							 */
							if (data != null) {
								final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) subRenderer;
								if (realtimeRenderer.data.destination.equals(data.destination) && realtimeRenderer.data.line.equals(data.line)) {
									realtimeRenderer.data.addDeparture(data.expectedDeparture, data.realtime, data.stopVisitNote);
									return;
								}
							}
						}
						i++;
					}
					// insert item here!
					//Log.d("DEBUGTrafikanten", "Inserting data at " + i);
					if (data != null) {
						items.add(i, new RealtimeRenderer(data));
					}
					return;
				}
				if (stationRenderer.station.stationId > station.stationId) {
					/*
					 * Should insert before this, and include the header
					 */
					//Log.d("DEBUGTrafikanten", "Inserting data at " + i + " due to station insertion");
					items.add(i, new StationRenderer(station));
					if (data != null) {
						items.add(i + 1, new RealtimeRenderer(data));
					}
					return;
				}
			}
		}
		/*
		 * We never found it, append to the end
		 */
		//Log.d("DEBUGTrafikanten", "Appending data at end of list" + size);
		items.add(size, new StationRenderer(station));
		if (data != null) {
			items.add(size + 1, new RealtimeRenderer(data));
		}
	}
	
	public void addData(RealtimeData data) {
		int i = 0;
		final int size = items.size();
		if (groupBy != RENDERER_PLATFORM) {
			Log.e(TAG, "Error, groupBy expected RENDER_PLATFORM");
		}
	
		/*
		 * This will convert a flat list of RealtimeData to:
		 * platform ->
		 *   realtimeData line,departure ->
		 *     realtimeData.nextDepartures if line.departure matches previous find.
		 */
		for (;i < size;i++) {
			//Log.d("DEBUGTrafikanten", "List iteration " + i + " / " + size);
			final GenericRealtimeRenderer renderer = items.get(i);
			if (renderer.renderType == RENDERER_PLATFORM) {
				PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
				/*
				 * Found our platform
				 */
				if (platformRenderer.platform.equals(data.departurePlatform)) {
					// we're in the right platform, keep going until we find the end of it.
					i++;
					while (i < size) {
						final GenericRealtimeRenderer subRenderer = items.get(i);
						//Log.d("DEBUGTrafikanten", "sub list iteration " + i + " / " + size);
						if (subRenderer.renderType == RENDERER_PLATFORM) {
							//Log.d("DEBUGTrafikanten", "Position " + i + " contains next platform, must insert above");
							// ok we just ran into the next platform, break and insert
							break;
						} else if (subRenderer.renderType == RENDERER_REALTIME) {
							/*
							 * This is the same departure as us, merge
							 */
							final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) subRenderer;
							if (realtimeRenderer.data.destination.equals(data.destination) && realtimeRenderer.data.line.equals(data.line)) {
								realtimeRenderer.data.addDeparture(data.expectedDeparture, data.realtime, data.stopVisitNote);
								return;
							}
						}
						i++;
					}
					// insert item here!
					//Log.d("DEBUGTrafikanten", "Inserting data at " + i);
					items.add(i, new RealtimeRenderer(data));
					return;
				}
				if (platformRenderer.platform.compareTo(data.departurePlatform) > 0) {
					/*
					 * Should insert before this, and include the header
					 */
					//Log.d("DEBUGTrafikanten", "Inserting data at " + i + " due to platform insertion");
					items.add(i, new PlatformRenderer(data.departurePlatform));
					items.add(i + 1, new RealtimeRenderer(data));
					return;
				}
			}
		}
		/*
		 * We never found it, append to the end
		 */
		//Log.d("DEBUGTrafikanten", "Appending data at end of list" + size);
		items.add(size, new PlatformRenderer(data.departurePlatform));
		items.add(size + 1, new RealtimeRenderer(data));
	}
	
	/*
	 * Add devi data to the tree.
	 */
	public boolean addData(DeviData deviData) {
		/*
		 * Add devi to tree, ignore station devi, that's handled elsewhere.
		 */
		boolean addedData = false;
		for(GenericRealtimeRenderer renderer : items) {
			switch(renderer.renderType) {
			case RENDERER_REALTIME:
				final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
				final RealtimeData realtimeData = realtimeRenderer.data;
				if (deviData.lines.contains(realtimeData.line)) {
					realtimeData.devi.add(deviData);					
				}
				addedData = true;
				break;
			case RENDERER_STATION:
				final StationRenderer stationRenderer = (StationRenderer) renderer;
				final StationData station = stationRenderer.station;
				if (deviData.stops.contains(station.stationId)) {
					station.devi.add(deviData);					
				}
				addedData = true;
				break;
			}
		}
		return addedData;
	}

	/**
	 * Parcel code 
	 */
	@Override
	public int describeContents() { return 0; }

	public GenericRealtimeList(Parcel in) {
		groupBy = in.readInt();
		int size = in.readInt();
		items = new ArrayList<GenericRealtimeRenderer>();
		while (size > 0) {
			size--;
			final int renderType = in.readInt();
			switch(renderType) {
			case GenericRealtimeList.RENDERER_REALTIME:
				items.add(new RealtimeRenderer(in));
				break;
			case GenericRealtimeList.RENDERER_PLATFORM:
				items.add(new PlatformRenderer(in));
				break;
			case GenericRealtimeList.RENDERER_STATION:
				items.add(new StationRenderer(in));
				break;
			default:
				Log.e(TAG,"Error, unmatched renderType when unpacking realtime list");
			}
		}
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(groupBy);
		dest.writeInt(items.size());
		
		for(GenericRealtimeRenderer item : items) {
			dest.writeInt(item.renderType);
			item.writeToParcel(dest, 0);
			/*switch(item.renderType) {
			case GenericRealtimeListAdapter.RENDERER_REALTIME:
				final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) item;
				dest.writeParcelable(realtimeRenderer, 0);
				break;
			case GenericRealtimeListAdapter.RENDERER_PLATFORM:
				final PlatformRenderer platformRenderer = (PlatformRenderer) item;
				dest.writeParcelable(platformRenderer, 0);
				break;
			}*/
		}
	}

	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<GenericRealtimeList> CREATOR = new Parcelable.Creator<GenericRealtimeList>() {
		public GenericRealtimeList createFromParcel(Parcel in) {
		    return new GenericRealtimeList(in);
		}
		
		public GenericRealtimeList[] newArray(int size) {
		    return new GenericRealtimeList[size];
		}
	};
}


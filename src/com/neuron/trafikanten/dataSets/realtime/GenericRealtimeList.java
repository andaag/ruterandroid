package com.neuron.trafikanten.dataSets.realtime;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.realtime.renderers.GenericRealtimeRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.PlatformRenderer;
import com.neuron.trafikanten.dataSets.realtime.renderers.RealtimeRenderer;

/*
 * This is GenericRealtimeListAdapter's list. Aka GenericRealtimeListAdapter.items
 */
public class GenericRealtimeList implements Parcelable {
	private static final String TAG = "Trafikanten-GenericRealtimeListAdapter";
	private static final long serialVersionUID = 4587512040075849425L;
	public static final int RENDERER_REALTIME = 1;
	public static final int RENDERER_PLATFORM = 2;
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
	
	public void addData(RealtimeData data) {
		if (groupBy == RENDERER_PLATFORM) {
			/*
			 * This will convert a flat list of RealtimeData to:
			 * platform ->
			 *   realtimeData line,departure ->
			 *     realtimeData.nextDepartures if line.departure matches previous find.
			 */
			int i = 0;
			final int size = items.size();
			for (;i < size;i++) {
				//Log.d("DEBUGTrafikanten", "List iteration " + i + " / " + size);
				final GenericRealtimeRenderer renderer = items.get(i);
				if (renderer.renderType == RENDERER_PLATFORM) {
					PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
					/*
					 * Found our platform
					 */
					if (platformRenderer.platform == data.departurePlatform) {
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
								if (realtimeRenderer.data.line.equals(data.line) && realtimeRenderer.data.destination.equals(data.destination)) {
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
					if (platformRenderer.platform > data.departurePlatform) {
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
		} else {
			//TODO : Render type station
		}
	}
	
	/*
	 * Add devi data to the tree.
	 */
	public void addData(DeviData deviData) {
		/*
		 * Add devi to tree, ignore station devi, that's handled elsewhere.
		 */
		for(GenericRealtimeRenderer renderer : items) {
			switch(renderer.renderType) {
			case RENDERER_REALTIME:
				final RealtimeRenderer realtimeRenderer = (RealtimeRenderer) renderer;
				final RealtimeData realtimeData = realtimeRenderer.data;
				if (deviData.lines.contains(realtimeData.line)) {
					realtimeData.devi.add(deviData);					
				}
			// TODO : RENDERER_STATION to add station devi
			}
		}
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


package com.neuron.trafikanten.dataSets.realtime;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;

public class GenericRealtimeListAdapter {
	private static final String KEY_LIST = "GRLA.items";
	private static final String KEY_GROUPBY = "GRLA.groupby";
	private static final long serialVersionUID = 4587512040075849425L;
	public static final int RENDERER_REALTIME = 1;
	public static final int RENDERER_PLATFORM = 2;
	private ArrayList<GenericRealtimeRenderer> items;
	private int groupBy;
	
	public GenericRealtimeListAdapter(int groupBy) {
		super();
		this.groupBy = groupBy;
		items = new ArrayList<GenericRealtimeRenderer>();
	}
	
	public void clear() { items.clear(); }
	public int size() { return items.size(); }
	public GenericRealtimeRenderer get(int pos) { return items.get(pos); }
	
	public GenericRealtimeListAdapter(Bundle bundle) {
		super();
		Log.i("TrafikantenDebug","Reading bundle #1");
		groupBy = bundle.getInt(KEY_GROUPBY);
		Log.i("TrafikantenDebug","Reading bundle #2");
		items = bundle.getParcelableArrayList(KEY_LIST);
	}
	
	public void saveToBundle(Bundle bundle) {
		bundle.putInt(KEY_GROUPBY, groupBy);
		bundle.putParcelableArrayList(KEY_LIST, items);
	}
	
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
	 * * * * * * * * * DATA TYPES START HERE * * * * * * * *
	 */
	public abstract class GenericRealtimeRenderer implements Parcelable {
		public int renderType = -1;
		
		public GenericRealtimeRenderer(int renderType) {
			super();
			this.renderType = renderType;
		}
		
		@Override
		public int describeContents() { return renderType; };

	}

	public class RealtimeRenderer extends GenericRealtimeRenderer {
		public RealtimeData data;
		public RealtimeRenderer(RealtimeData data) {
			super(GenericRealtimeListAdapter.RENDERER_REALTIME);
			this.data = data;
		}
		
		public RealtimeRenderer(Parcel in) {
			super(GenericRealtimeListAdapter.RENDERER_REALTIME);
			this.data = in.readParcelable(RealtimeData.class.getClassLoader());
		}
		
		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeParcelable(data, 0);
		}
		
		/*
		 * Used for bundle.getParcel 
		 */
	    public final Parcelable.Creator<RealtimeRenderer> CREATOR = new Parcelable.Creator<RealtimeRenderer>() {
			public RealtimeRenderer createFromParcel(Parcel in) {
			    return new RealtimeRenderer(in);
			}
			
			public RealtimeRenderer[] newArray(int size) {
			    return new RealtimeRenderer[size];
			}
		};
	}

	public class PlatformRenderer extends GenericRealtimeRenderer {
		public int platform = 0;
		
		public PlatformRenderer(int platform) {
			super(GenericRealtimeListAdapter.RENDERER_PLATFORM);
			this.platform = platform;
		}
		
		public PlatformRenderer(Parcel in) {
			super(GenericRealtimeListAdapter.RENDERER_PLATFORM);
			this.platform = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(platform);
		}
		
		/*
		 * Used for bundle.getParcel 
		 */
	    public final Parcelable.Creator<PlatformRenderer> CREATOR = new Parcelable.Creator<PlatformRenderer>() {
			public PlatformRenderer createFromParcel(Parcel in) {
			    return new PlatformRenderer(in);
			}
			
			public PlatformRenderer[] newArray(int size) {
			    return new PlatformRenderer[size];
			}
		};
	}

}


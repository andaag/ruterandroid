package com.neuron.trafikanten.dataSets.realtime;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeListAdapter.GenericRealtimeRenderer;

public class GenericRealtimeListAdapter extends ArrayList<GenericRealtimeRenderer> {
	private static final long serialVersionUID = 4587512040075849425L;
	public static final int RENDERER_REALTIME = 1;
	public static final int RENDERER_PLATFORM = 2;

	public void addData(RealtimeData data, int groupBy) {
		if (groupBy == RENDERER_PLATFORM) {
			/*
			 * Scan through everything to find platform types, append if found.
			 */
			int i = 0;
			final int size = size();
			for (;i < size;i++) {
				//Log.d("DEBUGTrafikanten", "List iteration " + i + " / " + size);
				final GenericRealtimeRenderer renderer = get(i);
				if (renderer.renderType == RENDERER_PLATFORM) {
					PlatformRenderer platformRenderer = (PlatformRenderer) renderer;
					/*
					 * Found our platform
					 */
					if (platformRenderer.platform == data.departurePlatform) {
						// we're in the right platform, keep going until we find the end of it.
						i++;
						while (i < size) {
							final GenericRealtimeRenderer subRenderer = get(i);
							//Log.d("DEBUGTrafikanten", "sub list iteration " + i + " / " + size);
							if (subRenderer.renderType == RENDERER_PLATFORM) {
								Log.d("DEBUGTrafikanten", "Position " + i + " contains next platform, must insert above");
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
						add(i, new RealtimeRenderer(data));
						return;
					}
					if (platformRenderer.platform > data.departurePlatform) {
						/*
						 * Should insert before this, and include the header
						 */
						//Log.d("DEBUGTrafikanten", "Inserting data at " + i + " due to platform insertion");
						add(i, new PlatformRenderer(data.departurePlatform));
						add(i + 1, new RealtimeRenderer(data));
						return;
					}
				}
			}
			/*
			 * We never found it, append to the end
			 */
			Log.d("DEBUGTrafikanten", "Appending data at end of list" + size);
			add(size, new PlatformRenderer(data.departurePlatform));
			add(size + 1, new RealtimeRenderer(data));
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
		for(GenericRealtimeRenderer renderer : this) {
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
		
		@Override
		public int describeContents() { return renderType; };

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(renderType);
		}
	}

	public class RealtimeRenderer extends GenericRealtimeRenderer {
		public RealtimeData data;
		public RealtimeRenderer(RealtimeData data) {
			super();
			this.data = data;
			renderType = GenericRealtimeListAdapter.RENDERER_REALTIME;
		}

	}

	public class PlatformRenderer extends GenericRealtimeRenderer {
		public int platform = 0;
		
		public PlatformRenderer(int platform) {
			super();
			renderType = GenericRealtimeListAdapter.RENDERER_PLATFORM;
			this.platform = platform;
		}

	}
}


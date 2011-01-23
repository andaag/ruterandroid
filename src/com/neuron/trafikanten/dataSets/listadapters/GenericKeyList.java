package com.neuron.trafikanten.dataSets.listadapters;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;

/*
 * Generic list shared between Realtime and Favorite screens, sorts based on platform/station
 */
public abstract class GenericKeyList<T> implements Parcelable {
	private int _size = 0; // Cached for performance, this is len(items) + len(item[0]) + len(item[1]) ...
	public ArrayList<RealtimeGenericListContent> items;
	public void clear() {
		_size = 0;
		items.clear();
	}
	
	@SuppressWarnings("unchecked")
	public GenericKeyList(Parcel in) {
		super();
		_size = in.readInt();
		items = (ArrayList<RealtimeGenericListContent>) new ArrayList<T>();
		//  NOTE, items is read by parent
	}
	
	@SuppressWarnings("unchecked")
	public GenericKeyList() {
		super();
		items = (ArrayList<RealtimeGenericListContent>) new ArrayList<T>();
	}
	
	public int size() { return _size; };
	
	/*
	 * Add devi to tree, ignore station devi, that's handled elsewhere.
	 */
	public void addDevi(DeviData deviData) {
		for (RealtimeGenericListContent list : items) {
			for (RealtimeData realtimeData : list) {
				if (deviData.lines.contains(realtimeData.line)) {
					realtimeData.devi.add(deviData);					
				}
			}
		}
	}
	
	public abstract T createGenericListContent(int id);

	/*
	 * Simple function that gets (or creates a new) platform/station in items
	 */
	public RealtimeGenericListContent getOrCreateHeader(int id) {
		/*
		 * If the platform already exists in the database just return it
		 */
		for (RealtimeGenericListContent list : items) {
			if (list.id == id) {
				return list;
			}
		}
		
		/*
		 * No platform found, create new
		 */
		RealtimeGenericListContent list = (RealtimeGenericListContent) createGenericListContent(id);
		
		/*
		 * We make sure the platform list is sorted the same way every time.
		 */
		int pos = 0;
		for (; pos < items.size(); pos++) {
			if (id < items.get(pos).id) {
				break;
			}
		}
		
		/*
		 * Finally add it and return
		 */
		items.add(pos, list);
		return list;
	}
	
	/*
	 * Gets realtime data, and returns whether or not platform/station header should be rendered.
	 */
	public RealtimeDataRendererData getRealtimeData(int pos) {
		boolean renderHeader = false;
		for (RealtimeGenericListContent list : items) {
			if (pos < list.size()) {
				if (pos == 0) {
					renderHeader = true;
				} else {
					renderHeader = false;
				}
				return new RealtimeDataRendererData(list.get(pos), renderHeader);
			} else {
				pos = pos - list.size();
			}
		}
		return null;
	}
	public class RealtimeDataRendererData {
		public RealtimeData data;
		public boolean renderHeader;
		public RealtimeDataRendererData(RealtimeData data,  boolean renderHeader) {
			this.data = data;
			this.renderHeader = renderHeader;
		}
		
	}
	
	/*
	 * Adding an item puts it in the platform category, and compressed duplicate data to one entry.
	 */
	public void addRealtimeData(RealtimeData item) {
		RealtimeGenericListContent list = getOrCreateHeader(item.departurePlatform);
		for (RealtimeData d : list) {
			if (d.destination.equals(item.destination) && d.line.equals(item.line)) {
				/*
				 * Data already exists, we add it to the arrival list and return
				 */
				d.addDeparture(item.expectedDeparture, item.realtime, item.stopVisitNote);
				return;
			}
		}
		/*
		 * Data does not exist, add it
		 */
		list.add(item);
		_size++;
	}
	
	/*
	 * Generic content
	 */
	public static abstract class RealtimeGenericListContent extends ArrayList<RealtimeData> implements Parcelable {
		private static final long serialVersionUID = -8158771022676013360L;
		public int id;
		
		public RealtimeGenericListContent(int id) {
			super();
			this.id = id;
		}
		public RealtimeGenericListContent(Parcel in) {
			id = in.readInt();
			in.readList(this, RealtimeData.class.getClassLoader());
		}

		
		/*
		 * Writing current data to parcel.
		 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
		 */
		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(id);
			out.writeList(this);
		}
		

		/*
		 * @see android.os.Parcelable
		 */
		@Override
		public int describeContents() { return 0; }
	}

	

	@Override
	public int describeContents() { return 0;	}


	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(_size);
		out.writeList(items);
	}
}

package com.neuron.trafikanten.dataSets.listadapters;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;

/*
 * This class handles the data
 * platform ->
 *   arraylist(realtimedata)
 */
public class RealtimePlatformList implements Parcelable {
	private int _size = 0; // Cached for performance, this is len(items) + len(item[0]) + len(item[1]) ...
	private ArrayList<RealtimePlatformListContent> items;
	
	public void clear() {
		_size = 0;
		items.clear();
	}
	
	public RealtimePlatformList() {
		items = new ArrayList<RealtimePlatformListContent>();
	}
	
	/*
	 * Add devi to tree, ignore station devi, that's handled elsewhere.
	 */
	public void addDevi(DeviData deviData) {
		for (RealtimePlatformListContent list : items) {
			for (RealtimeData realtimeData : list) {
				if (deviData.lines.contains(realtimeData.line)) {
					realtimeData.devi.add(deviData);					
				}
			}
		}
		
	}
	
	/*
	 * Simple function that gets (or creates a new) platform in items
	 */
	private RealtimePlatformListContent getOrCreatePlatform(int platform) {
		/*
		 * If the platform already exists in the database just return it
		 */
		for (RealtimePlatformListContent platformList : items) {
			if (platformList.platform == platform) {
				return platformList;
			}
		}
		
		/*
		 * No platform found, create new
		 */
		RealtimePlatformListContent platformList = new RealtimePlatformListContent(platform);
		
		/*
		 * We make sure the platform list is sorted the same way every time.
		 */
		int pos = 0;
		for (; pos < items.size(); pos++) {
			if (platform < items.get(pos).platform) {
				break;
			}
		}
		
		/*
		 * Finally add it and return
		 */
		items.add(pos, platformList);
		return platformList;
	}
	
	/*
	 * Adding an item puts it in the platform category, and compressed duplicate data to one entry.
	 */
	public void addRealtimeData(RealtimeData item) {
		RealtimePlatformListContent platformList = getOrCreatePlatform(item.departurePlatform);
		for (RealtimeData d : platformList) {
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
		platformList.add(item);
		_size++;
	}
	
	public int size() { return _size; };
	
	public RealtimeDataRendererData getRealtimeData(int pos) {
		boolean renderPlatform = false;
		for (RealtimePlatformListContent platformList : items) {
			if (pos < platformList.size()) {
				if (pos == 0) {
					renderPlatform = true;
				} else {
					renderPlatform = false;
				}
				return new RealtimeDataRendererData(platformList.get(pos), renderPlatform);
			} else {
				pos = pos - platformList.size();
			}
		}
		return null;
	}
	public class RealtimeDataRendererData {
		public RealtimeData data;
		public boolean renderPlatform;
		public RealtimeDataRendererData(RealtimeData data,  boolean renderPlatform) {
			this.data = data;
			this.renderPlatform = renderPlatform;
		}
		
	}
	@Override
	public int describeContents() { return 0; }

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(_size);
		out.writeList(items);
	}
	
	public RealtimePlatformList(Parcel in) {
		_size = in.readInt();
		items = new ArrayList<RealtimePlatformListContent>();
		in.readList(items, RealtimePlatformListContent.class.getClassLoader());
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RealtimePlatformList> CREATOR = new Parcelable.Creator<RealtimePlatformList>() {
		public RealtimePlatformList createFromParcel(Parcel in) {
		    return new RealtimePlatformList(in);
		}
		
		public RealtimePlatformList[] newArray(int size) {
		    return new RealtimePlatformList[size];
		}
	};
}


class RealtimePlatformListContent extends ArrayList<RealtimeData> implements Parcelable {
	private static final long serialVersionUID = -8158771022676013360L;
	public int platform;
	
	public RealtimePlatformListContent(int platform) {
		super();
		this.platform = platform;
	}

	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() { return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimePlatformListContent(Parcel in) {
		platform = in.readInt();
		in.readList(this, RealtimeData.class.getClassLoader());
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(platform);
		out.writeList(this);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RealtimePlatformListContent> CREATOR = new Parcelable.Creator<RealtimePlatformListContent>() {
		public RealtimePlatformListContent createFromParcel(Parcel in) {
		    return new RealtimePlatformListContent(in);
		}
		
		public RealtimePlatformListContent[] newArray(int size) {
		    return new RealtimePlatformListContent[size];
		}
	};
}
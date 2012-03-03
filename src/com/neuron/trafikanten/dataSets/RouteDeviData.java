package com.neuron.trafikanten.dataSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Parcelable arraylist for storing devi data.
 */
public class RouteDeviData implements Parcelable  {
	public HashMap<String, ArrayList<DeviData> > items = new HashMap<String, ArrayList<DeviData> >();
	public final static String PARCELABLE = "RouteDeviData";

	public RouteDeviData() {
	}
	
	public String getDeviKey(int stationId, int lineId) {
		/*
		 * TODO Performance, come up with a better way of id'ing the different values, using a string for this is dumb.
		 */
		return "" + lineId + "-" + stationId;
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RouteDeviData(Parcel in) {
		int size = in.readInt();
		while (size > 0) {
			RouteDeviDataItem item = in.readParcelable(RouteDeviDataItem.class.getClassLoader());
			items.put(item.deviKey, item.items);
			size--;
		}
	}

	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		Set<String> keys = items.keySet();
		out.writeInt(keys.size());
		for (String key : keys) {
			RouteDeviDataItem item = new RouteDeviDataItem(key, items.get(key));
			out.writeParcelable(item, 0);
		}
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
	public static final Parcelable.Creator<RouteDeviData> CREATOR = new Parcelable.Creator<RouteDeviData>() {
		public RouteDeviData createFromParcel(Parcel in) {
		    return new RouteDeviData(in);
		}
		
		public RouteDeviData[] newArray(int size) {
		    return new RouteDeviData[size];
		}
	};
	
}

class RouteDeviDataItem implements Parcelable {
	public String deviKey;
	public ArrayList<DeviData> items;
	
	public RouteDeviDataItem(String deviKey, ArrayList<DeviData> items) {
		this.deviKey = deviKey;
		this.items = items;
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RouteDeviDataItem(Parcel in) {
		deviKey = in.readString();
		items = new ArrayList<DeviData>();
		in.readList(items, DeviData.class.getClassLoader());
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(deviKey);
		out.writeList(items);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
	public static final Parcelable.Creator<RouteDeviDataItem> CREATOR = new Parcelable.Creator<RouteDeviDataItem>() {
		public RouteDeviDataItem createFromParcel(Parcel in) {
		    return new RouteDeviDataItem(in);
		}
		
		public RouteDeviDataItem[] newArray(int size) {
		    return new RouteDeviDataItem[size];
		}
	};
}

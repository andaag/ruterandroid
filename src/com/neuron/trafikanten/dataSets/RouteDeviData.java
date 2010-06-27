package com.neuron.trafikanten.dataSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Parcelable arraylist for storing devi data.
 */
public class RouteDeviData extends HashMap<String, ArrayList<DeviData> > implements Parcelable  {
	public final static String PARCELABLE = "RouteDeviData";
	private static final long serialVersionUID = 275828144368446465L;

	public RouteDeviData() {
		super();
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
		super();
		while (in.dataAvail() > 0) {
			RouteDeviDataItem item = in.readParcelable(RouteDeviDataItem.class.getClassLoader());
			put(item.deviKey, item.items);
		}
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		Set<String> keys = keySet();
		for (String key : keys) {
			RouteDeviDataItem item = new RouteDeviDataItem(key, get(key));
			out.writeParcelable(item, 0);
		}
		clear();
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

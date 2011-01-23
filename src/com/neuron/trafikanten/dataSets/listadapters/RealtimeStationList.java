package com.neuron.trafikanten.dataSets.listadapters;

import android.os.Parcel;
import android.os.Parcelable;

import com.neuron.trafikanten.dataSets.listadapters.GenericKeyList.RealtimeGenericListContent;

/*
 * Essentially the same as platform, but sorting by station stopname instead for favorites
 */
public class RealtimeStationList extends GenericKeyList<RealtimeStationListContent> {
	
	public RealtimeStationListContent createGenericListContent(String id) {
		return new RealtimeStationListContent(id);
	}
	
	public RealtimeStationList() {
		super();
	}
	
	public RealtimeStationList(Parcel in) {
		super(in);
		in.readList(items, RealtimeStationListContent.class.getClassLoader());
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RealtimeStationList> CREATOR = new Parcelable.Creator<RealtimeStationList>() {
		public RealtimeStationList createFromParcel(Parcel in) {
		    return new RealtimeStationList(in);
		}
		
		public RealtimeStationList[] newArray(int size) {
		    return new RealtimeStationList[size];
		}
	};
}


class RealtimeStationListContent extends RealtimeGenericListContent {
	private static final long serialVersionUID = 336123708018750977L;
	/*
	 * For this kind, id = platform
	 */

	public RealtimeStationListContent(String stopName) {
		super(stopName);
	}

	/*
	 * Function for reading the parcel
	 */
	public RealtimeStationListContent(Parcel in) {
		super(in);
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
package com.neuron.trafikanten.dataSets.listadapters;

import android.os.Parcel;
import android.os.Parcelable;

import com.neuron.trafikanten.dataSets.listadapters.GenericKeyList.RealtimeGenericListContent;

/*
 * This class handles the data
 * platform ->
 *   arraylist(realtimedata)
 */
public class RealtimePlatformList extends GenericKeyList<RealtimePlatformListContent> {
	
	public RealtimePlatformListContent createGenericListContent(String id) {
		return new RealtimePlatformListContent(id);
	}
	
	public RealtimePlatformList() {
		super();
	}
	
	public RealtimePlatformList(Parcel in) {
		super(in);
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


class RealtimePlatformListContent extends RealtimeGenericListContent {
	private static final long serialVersionUID = 336123708018750977L;
	/*
	 * For this kind, id = platform
	 */

	public RealtimePlatformListContent(String platform) {
		super(platform);
	}

	/*
	 * Function for reading the parcel
	 */
	public RealtimePlatformListContent(Parcel in) {
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
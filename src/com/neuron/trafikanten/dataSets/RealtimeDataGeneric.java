package com.neuron.trafikanten.dataSets;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * This is realtime data that is available for "all" realtime sets, aka available both for main view and for subdepartures
 */
//FIXME : should use higher quality images for hdpi, most of them are now upscaled
public class RealtimeDataGeneric  implements Parcelable {
	public long expectedDeparture;
	
	public boolean inCongestion = false;
	public boolean realtime;
	public boolean lowFloor = false; // kun trikk
	public int numberOfBlockParts = 0; // kun t-bane (3 = kort tog, 6 = langt tog).

	
	public RealtimeDataGeneric() {
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimeDataGeneric(Parcel in) {
		expectedDeparture = in.readLong();
		inCongestion = in.readInt() != 0;
		realtime = in.readInt() != 0;
		lowFloor = in.readInt() != 0;
		numberOfBlockParts = in.readInt();
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(expectedDeparture);
		out.writeInt(inCongestion ? 1 : 0);
		out.writeInt(realtime ? 1 : 0);
		out.writeInt(lowFloor ? 1 : 0);
		out.writeInt(numberOfBlockParts);
	}
	
	public void renderToContainer(StringBuffer output, Activity activity, long currentTime) {
		output.append("   ");
		if (lowFloor) {
			output.append("<img src='LF'/>");
		}
		if (numberOfBlockParts == 3) {
			output.append("<img src='TL1'/>");
		}
		else if (numberOfBlockParts == 6) {
			output.append("<img src='TL2'/>");	
		}
		
		if (inCongestion) {
			output.append(activity.getText(R.string.congestion));
			output.append(" ");
		}
		
		if (realtime) {
			output.append("<font color='#FAF400'>");
		} else {
			output.append("<font color='#ffffff'>");
		}
		HelperFunctions.renderTime(output, currentTime, activity, expectedDeparture);
		output.append("</font>");
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
	public static final Parcelable.Creator<RealtimeDataGeneric> CREATOR = new Parcelable.Creator<RealtimeDataGeneric>() {
		public RealtimeDataGeneric createFromParcel(Parcel in) {
		    return new RealtimeDataGeneric(in);
		}
		
		public RealtimeDataGeneric[] newArray(int size) {
		    return new RealtimeDataGeneric[size];
		}
	};
}

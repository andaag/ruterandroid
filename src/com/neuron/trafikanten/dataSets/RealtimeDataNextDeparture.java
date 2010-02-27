package com.neuron.trafikanten.dataSets;

import android.os.Parcel;
import android.os.Parcelable;


/*
 * Class for RealtimeData.nextDeparture
 * saves list of departures + stopvisitnotes.
 */
public class RealtimeDataNextDeparture implements Parcelable {
	public long expectedDeparture;
	public boolean realtime;
	public String stopVisitNote;
	public RealtimeDataNextDeparture(long expectedDeparture, boolean realtime, String stopVisitNote) {
		this.expectedDeparture = expectedDeparture;
		this.realtime = realtime;
		this.stopVisitNote = stopVisitNote;
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimeDataNextDeparture(Parcel in) {
		expectedDeparture = in.readLong();
		realtime = in.readInt() != 0;
		stopVisitNote = in.readString();
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(expectedDeparture);
		out.writeInt(realtime ? 1 : 0);
		out.writeString(stopVisitNote);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
	public static final Parcelable.Creator<RealtimeDataNextDeparture> CREATOR = new Parcelable.Creator<RealtimeDataNextDeparture>() {
		public RealtimeDataNextDeparture createFromParcel(Parcel in) {
		    return new RealtimeDataNextDeparture(in);
		}
		
		public RealtimeDataNextDeparture[] newArray(int size) {
		    return new RealtimeDataNextDeparture[size];
		}
	};
}

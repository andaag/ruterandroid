/**
 *     Copyright (C) 2009 Anders Aagaard <aagaande@gmail.com>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neuron.trafikanten.dataSets;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class RealtimeData implements Parcelable {
	public final static String PARCELABLE = "RealtimeData";
	public String line;
	public String destination;
	public boolean realtime;
	public String departurePlatform;
	public String extra;

	/*public long aimedArrival;
	public long expectedArrival;*/

	public long aimedDeparture;
	public long expectedDeparture;
	
	/*
	 * List of coming arrivals, this is used for the list under current station in RealtimeView
	 */
	public StringBuffer arrivalList;
	/*
	 * List of devi data, this is a int list, it links to RealtimeView.RealtimeAdapter.deviItems
	 */
	public ArrayList<Integer> devi;
	
	public RealtimeData() {
		arrivalList = new StringBuffer();
		devi = new ArrayList<Integer>();
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimeData(Parcel in) {
		line = in.readString();
		destination = in.readString();
		realtime = in.readInt() != 0;
		departurePlatform = in.readString();
		extra = in.readString();
		
		/*aimedArrival = in.readLong();
		expectedArrival = in.readLong();*/
		
		aimedDeparture = in.readLong();
		expectedDeparture = in.readLong();
		
		arrivalList = new StringBuffer(in.readString());
		
		devi = new ArrayList<Integer>();
		while (in.dataAvail() > 0) {
			devi.add(in.readInt());
		}
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(line);
		out.writeString(destination);
		out.writeInt(realtime ? 1 : 0);
		out.writeString(departurePlatform);
		out.writeString(extra);
		
		/*out.writeLong(aimedArrival);
		out.writeLong(expectedArrival);*/
		
		out.writeLong(aimedDeparture);
		out.writeLong(expectedDeparture);
		
		out.writeString(arrivalList.toString());
		
		for (Integer deviPos : devi) {
			out.writeInt(deviPos);
		}
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RealtimeData> CREATOR = new Parcelable.Creator<RealtimeData>() {
		public RealtimeData createFromParcel(Parcel in) {
		    return new RealtimeData(in);
		}
		
		public RealtimeData[] newArray(int size) {
		    return new RealtimeData[size];
		}
	};

}

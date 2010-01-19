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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

public class RealtimeData implements Parcelable {
	public final static String PARCELABLE = "RealtimeData";
	public String line;
	public String destination;
	public boolean realtime;
	public String departurePlatform;
	public String stopVisitNote;
	
	//public long aimedDeparture;
	public long expectedDeparture;
	
	/*
	 * Data set of coming departures
	 */
	public ArrayList<NextDeparture> nextDepartures;
	
	/*
	 * List of devi data, this is a int list, it links to RealtimeView.RealtimeAdapter.deviItems
	 */
	public ArrayList<Integer> devi;
	
	public RealtimeData() {
		nextDepartures = new ArrayList<NextDeparture>();
		devi = new ArrayList<Integer>();
	}
	
	public void addDeparture(long expectedDeparture, boolean realtime) {
		nextDepartures.add(new NextDeparture(expectedDeparture, realtime));
	}
	
	/*
	 * Renders all departures, expectedDeparture + nextDepartures
	 */
	public CharSequence renderDepartures(Context context) {
		StringBuffer departures = new StringBuffer();
		if (!realtime) {
			departures.append(context.getText(R.string.ca));
			departures.append(" ");
		}
		departures.append(HelperFunctions.renderTime(context, expectedDeparture));
		
		for (NextDeparture nextDeparture : nextDepartures) {
			departures.append(", ");
			if (!nextDeparture.realtime) {
				departures.append(context.getText(R.string.ca));
				departures.append(" ");
			}
			departures.append(HelperFunctions.renderTime(context, nextDeparture.expectedDeparture));
		}
		return departures;
		
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
		stopVisitNote = in.readString();

		expectedDeparture = in.readLong();
		
		nextDepartures = new ArrayList<NextDeparture>();
		in.readList(nextDepartures, NextDeparture.class.getClassLoader());
		
		devi = new ArrayList<Integer>();
		in.readList(devi, Integer.class.getClassLoader());
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
		out.writeString(stopVisitNote);
		
		out.writeLong(expectedDeparture);
		
		out.writeList(nextDepartures);
		out.writeList(devi);
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

class NextDeparture implements Parcelable {
	public long expectedDeparture;
	public boolean realtime;
	public NextDeparture(long expectedDeparture, boolean realtime) {
		this.expectedDeparture = expectedDeparture;
		this.realtime = realtime;
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public NextDeparture(Parcel in) {
		expectedDeparture = in.readLong();
		realtime = in.readInt() != 0;
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(expectedDeparture);
		out.writeInt(realtime ? 1 : 0);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<NextDeparture> CREATOR = new Parcelable.Creator<NextDeparture>() {
		public NextDeparture createFromParcel(Parcel in) {
		    return new NextDeparture(in);
		}
		
		public NextDeparture[] newArray(int size) {
		    return new NextDeparture[size];
		}
	};
}
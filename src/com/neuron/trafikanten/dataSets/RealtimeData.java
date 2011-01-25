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
	public boolean inCongestion;
	public boolean realtime;
	public int departurePlatform = 0;
	public String stopVisitNote;
	
	public long expectedDeparture;
	
	/*
	 * Data set of coming departures
	 */
	public ArrayList<RealtimeDataNextDeparture> nextDepartures;
	
	/*
	 * List of devi data, used by RealtimeView/FavoritesView
	 */
	public ArrayList<DeviData> devi;
	
	public RealtimeData() {
		nextDepartures = new ArrayList<RealtimeDataNextDeparture>();
		devi = new ArrayList<DeviData>();
	}
	
	public void addDeparture(long expectedDeparture, boolean realtime, String stopVisitNote) {
		nextDepartures.add(new RealtimeDataNextDeparture(expectedDeparture, realtime, stopVisitNote));
	}
	
	/*
	 * Renders all departures, expectedDeparture + nextDepartures
	 */
	public CharSequence renderDepartures(Long currentTime, Context context) {
		StringBuffer departures = new StringBuffer();
		if (!realtime) {
			departures.append(context.getText(R.string.ca));
			departures.append(" ");
		} else if (inCongestion) {
			departures.append(context.getText(R.string.congestion));
			departures.append(" ");
		}
		departures.append(HelperFunctions.renderTime(currentTime, context, expectedDeparture));
		
		for (RealtimeDataNextDeparture nextDeparture : nextDepartures) {
			departures.append("  ");
			if (!nextDeparture.realtime) {
				departures.append(context.getText(R.string.ca));
				departures.append(" ");
			} else if (inCongestion) {
				departures.append(context.getText(R.string.congestion));
				departures.append(" ");
			}
			departures.append(HelperFunctions.renderTime(currentTime, context, nextDeparture.expectedDeparture));
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
		inCongestion = in.readInt() != 0;
		realtime = in.readInt() != 0;
		departurePlatform = in.readInt();
		stopVisitNote = in.readString();

		expectedDeparture = in.readLong();
		
		nextDepartures = new ArrayList<RealtimeDataNextDeparture>();
		in.readList(nextDepartures, RealtimeDataNextDeparture.class.getClassLoader());
		
		devi = new ArrayList<DeviData>();
		in.readList(devi, DeviData.class.getClassLoader());
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(line);
		out.writeString(destination);
		out.writeInt(inCongestion ? 1 : 0);
		out.writeInt(realtime ? 1 : 0);
		out.writeInt(departurePlatform);
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



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

import com.neuron.trafikanten.R;

/*
 * This is route search data, used for searching for routes.
 */
public class RouteSearchData implements Parcelable {
	public final static String PARCELABLE = "RouteSearchData";
	
	public ArrayList<StationData> fromStation = new ArrayList<StationData>();
	public ArrayList<StationData> toStation = new ArrayList<StationData>();
	public long departure = 0;
	public long arrival = 0;
	public boolean advancedOptionsEnabled = false;
	
	/*
	 * Advanced options, if advancedOptionsEnabled == false, dont use them
	 */
	public int changePunish = 2; // in minutes
	public int changeMargin = 8; // in minutes
	public int proposals = 10; // asking for 10 results by default
	
	// ORDER : transportBus, transportTrain, transportTram, transportMetro, transportAirportBus, transportAirportTrain, transportBoat
	public Boolean[] transportTypes = {true, true, true, true, true, true, true};
	
	public RouteSearchData() {}
	
	/*
	 * Called when advancedOptionsEnabled == false, this resets advanced options to defaults.
	 */
	public void resetAdvancedOptions() {
		changePunish = 2;
		changeMargin = 2;
		proposals = 10;
		
		for (int i = 0; i < 7; i++) {
			transportTypes[i] = true;
		}
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RouteSearchData(Parcel in) {
		in.readList(fromStation, StationData.class.getClassLoader());
		in.readList(toStation, StationData.class.getClassLoader());
		departure = in.readLong();
		arrival = in.readLong();
		advancedOptionsEnabled = in.readInt() == 1;
		/*preferDirect = in.readInt() == 1;
		avoidWalking = in.readInt() == 1;*/
		changePunish = in.readInt();
		changeMargin = in.readInt();
		proposals = in.readInt();
		
		final Object[] entries = in.readArray(Boolean.class.getClassLoader());
		for (int i = 0; i < 7; i++) {
			transportTypes[i] = (Boolean) entries[i];
		}
	}

	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeList(fromStation);
		out.writeList(toStation);
		out.writeLong(departure);
		out.writeLong(arrival);
		out.writeInt(advancedOptionsEnabled ? 1 : 0);
		/*out.writeInt(preferDirect ? 1 : 0);
		out.writeInt(avoidWalking ? 1 : 0);*/
		out.writeInt(changePunish);
		out.writeInt(changeMargin);
		out.writeInt(proposals);
		out.writeArray(transportTypes);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RouteSearchData> CREATOR = new Parcelable.Creator<RouteSearchData>() {
		public RouteSearchData createFromParcel(Parcel in) {
		    return new RouteSearchData(in);
		}
		
		public RouteSearchData[] newArray(int size) {
		    return new RouteSearchData[size];
		}
	};
	
	public CharSequence[] getAPITransportArray(Context context) {
		final CharSequence[] transportItems ={"Bus", "Train", "Tram", "Metro", "AirportBus" ,"AirportTrain", "Boat"};
		return transportItems;
	}
	
	public CharSequence[] getTransportArray(Context context) {
		final CharSequence[] transportItems = {context.getText(R.string.transportBus), context.getText(R.string.transportTrain), 
				context.getText(R.string.transportTram), context.getText(R.string.transportMetro),  
				context.getText(R.string.transportAirportBus), context.getText(R.string.transportAirportTrain),
				context.getText(R.string.transportBoat)};
		return transportItems;
	}
	
	public CharSequence renderTransportTypesApi(CharSequence[] array) {
		StringBuilder ret = new StringBuilder();
		boolean first = true;
		
		for (int i = 0; i < 7; i++) {
			if (transportTypes[i]) {
				if (!first) {
					ret.append(",");
				} 
				ret.append(array[i]);
				first = false;
			}
		}
		
		return ret.toString();
	}

	public CharSequence renderTransportTypes(Context context, CharSequence[] array) {
		StringBuilder enabledTransports = new StringBuilder();
		StringBuilder disabledTransports = new StringBuilder();
		
		
		for (int i = 0; i < 7; i++) {
			if (transportTypes[i]) {
				if (enabledTransports.length() != 0) {
					enabledTransports.append(", ");
				}
				enabledTransports.append(array[i]);
			} else {
				if (disabledTransports.length() != 0) {
					disabledTransports.append(", ");
				}
				disabledTransports.append(array[i]);
			}
		}
		
		if (disabledTransports.length() == 0) {
			return context.getText(R.string.allTransportsEnabled);
		}
		if (enabledTransports.length() < disabledTransports.length()) {
			return "+ " + enabledTransports.toString();
		}
		return "- " + disabledTransports.toString();
	}
}

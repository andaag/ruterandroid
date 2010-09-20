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

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Class for route data, from to and when.
 * 
 * This class is shared between actual routes, and search routes, when search routes is active it only uses fromStation/toStation and departure for the search.
 */
public class RouteData implements Parcelable {
	public final static String PARCELABLE = "RouteData";
	
	public StationData fromStation;
	public long departure;
	
	public StationData toStation;
	public long arrival;
	
	public String line;
	public String destination; // line destination = end station
	public int tourID;
	public String extra;
	public int waitTime; // in minutes, waittime for THIS transport, not next transport.
	
	//This is used for showing realtime data connected to a route
	public RealtimeData realtimeData = null;
	public long timeDifference = 0;
	
	/*
	 * transportType can be train/tram/etc, it's up to the seperate providers to decide what goes in here.
	 */
	public int transportType;
	
	public RouteData() { }
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RouteData(Parcel in) {
		fromStation = in.readParcelable(StationData.class.getClassLoader());
		departure = in.readLong();
		
		toStation = in.readParcelable(StationData.class.getClassLoader());
		arrival = in.readLong();
		
		line = in.readString();
		destination = in.readString();
		tourID = in.readInt();
		extra = in.readString();
		waitTime = in.readInt();
		
		transportType = in.readInt();
		
		realtimeData = in.readParcelable(RealtimeData.class.getClassLoader());
		timeDifference = in.readLong();
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(fromStation, 0);
		out.writeLong(departure);
		
		out.writeParcelable(toStation, 0);
		out.writeLong(arrival);
		
		out.writeString(line);
		out.writeString(destination);
		out.writeInt(tourID);
		out.writeString(extra);
		out.writeInt(waitTime);
		
		out.writeInt(transportType);
		
		out.writeParcelable(realtimeData, 0);
		out.writeLong(timeDifference);
	}
	
	/*
	 * Returns true if route realtime capability should be enabled.
	 */
	public boolean canRealtime() {
		if (transportType != R.drawable.icon_line_walk && fromStation.realtimeStop && 
				System.currentTimeMillis() > departure - HelperFunctions.HOUR) {
			return true;
		}
		return false;
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RouteData> CREATOR = new Parcelable.Creator<RouteData>() {
		public RouteData createFromParcel(Parcel in) {
		    return new RouteData(in);
		}
		
		public RouteData[] newArray(int size) {
		    return new RouteData[size];
		}
	};
}

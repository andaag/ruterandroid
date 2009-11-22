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

import android.os.Parcel;
import android.os.Parcelable;

/*
 * This is route search data, used for searching for routes.
 */
public class RouteSearchData implements Parcelable {
	public final static String PARCELABLE = "RouteSearchData";
	
	/*
	 * of routeData a RouteSearch uses : 
	 * 		fromStation, toStation
	 * 		departure or arrival
	 * 					- if arrival > 0, we assume arriveBefore, otherwise travelAt.
	 * 					- if departure == 0 we use now.
	 */
	public RouteData routeData = new RouteData();
	public boolean advancedOptionsEnabled = false;
	
	/*
	 * Advanced options, if advancedOptionsEnabled == false, dont use them
	 */
	public boolean preferDirect = false;
	public boolean avoidWalking = false;
	public int changeMargin = 2; // in minutes
	public int proposals = 5;
	
	public RouteSearchData() {}
	
	/*
	 * Called when advancedOptionsEnabled == false, this resets advanced options to defaults.
	 */
	public void resetAdvancedOptions() {
		preferDirect = false;
		avoidWalking = false;
		changeMargin = 2;
		proposals = 5;
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
		routeData = in.readParcelable(RouteData.class.getClassLoader());
		advancedOptionsEnabled = in.readInt() == 1;
		preferDirect = in.readInt() == 1;
		avoidWalking = in.readInt() == 1;
		changeMargin = in.readInt();
		proposals = in.readInt();
	}

	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(routeData, 0);
		out.writeInt(advancedOptionsEnabled ? 1 : 0);
		out.writeInt(preferDirect ? 1 : 0);
		out.writeInt(avoidWalking ? 1 : 0);
		out.writeInt(changeMargin);
		out.writeInt(proposals);
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
}

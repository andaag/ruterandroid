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

public class RouteProposal implements Parcelable {
	public final static String PARCELABLE = "RouteProposal";
	public ArrayList<RouteData> travelStageList = new ArrayList<RouteData>();
	
	public RouteProposal() { }
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RouteProposal(Parcel in) {
		travelStageList = new ArrayList<RouteData>();
		in.readTypedList(travelStageList, RouteData.CREATOR);
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeTypedList(travelStageList);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<RouteProposal> CREATOR = new Parcelable.Creator<RouteProposal>() {
		public RouteProposal createFromParcel(Parcel in) {
		    return new RouteProposal(in);
		}
		
		public RouteProposal[] newArray(int size) {
		    return new RouteProposal[size];
		}
	};
}

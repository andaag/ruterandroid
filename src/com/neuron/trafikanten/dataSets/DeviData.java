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

public class DeviData implements Parcelable {
	public final static String PARCELABLE = "DeviData";
	
	public int id;
	public String title;
	public String description;
	public String body;
	public ArrayList<Integer> stops;
	public ArrayList<Integer> lines;
	public long validFrom;
	public long validTo;
	public boolean important;
	
	public DeviData() {
		stops = new ArrayList<Integer>();
		lines = new ArrayList<Integer>();
	}

	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public DeviData(Parcel in) {
		id = in.readInt();
		title = in.readString();
		description = in.readString();
		body = in.readString();

		stops = new ArrayList<Integer>();
		in.readArrayList(Integer.class.getClassLoader());
		//in.readStringList(stops);
		
		lines = new ArrayList<Integer>();
		in.readArrayList(Integer.class.getClassLoader());
		
		validFrom = in.readLong();
		validTo = in.readLong();
		important = in.readInt() == 1;
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(id);
		out.writeString(title);
		out.writeString(description);
		out.writeString(body);
		out.writeList(stops);
		out.writeList(lines);
		out.writeLong(validFrom);
		out.writeLong(validTo);
		out.writeInt(important ? 1 : 0);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<DeviData> CREATOR = new Parcelable.Creator<DeviData>() {
		public DeviData createFromParcel(Parcel in) {
		    return new DeviData(in);
		}
		
		public DeviData[] newArray(int size) {
		    return new DeviData[size];
		}
	};
}

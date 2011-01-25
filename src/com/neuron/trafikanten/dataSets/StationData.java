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

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class StationData implements Parcelable {
	public final static String PARCELABLE = "StationData";
	public String stopName;
	public String extra; // Extra is shown under station name, on a seperate line.
	public int stationId;
	public int walkingDistance = 0;
	public boolean realtimeStop;

	/*
	 * List of devi data, used by RealtimeView/FavoritesView
	 */
	public ArrayList<DeviData> devi = null;
	
	public boolean isFavorite; // This is used for rendering a star next to favorites, this is NOT stored.
	
	public int[] utmCoords = new int[] {0, 0}; // x,y
	public double[] latLongCoords = new double[] {0, 0}; // lat,long
	public double[] getLongLat() {
		if (latLongCoords[0] == 0 && utmCoords[0] > 0) {
			/*
			 * Convert from utm to latlong
			 */
			UTMRef utm = new UTMRef(utmCoords[0], utmCoords[1], 'V', 32);
			LatLng latLng = utm.toLatLng();
			latLongCoords[0] = latLng.getLat();
			latLongCoords[1] = latLng.getLng();
		}
		
		return latLongCoords;
		
	}
	
	public StationData() { }
	public StationData(String stopName, String extra, int stationId, boolean realtimeStop, int[] utmCoords) {
		this.stopName = stopName;
		this.extra = extra;
		this.realtimeStop = realtimeStop;
		this.stationId = stationId;
		this.utmCoords = utmCoords;
		devi = new ArrayList<DeviData>();
	}
	
	public StationData(String stopName, int stationId) {
		this.stopName = stopName;
		this.stationId = stationId;
		devi = new ArrayList<DeviData>();
	}
	
	/*
	 * Functions for writing to a bundle, this writes a "simple" set without coordinates, this is used for shortcuts
	 */
	private final static String BUNDLE_STOPNAME = "stationDataStopname";
	private final static String BUNDLE_STATIONID = "stationDataStationid";
	public void writeSimpleBundle(Bundle bundle) {
		bundle.putString(BUNDLE_STOPNAME, stopName);
		bundle.putInt(BUNDLE_STATIONID, stationId);
	}
	
	/*
	 * Load the bundle back into a station set, this is used for shortcuts
	 */
	public static StationData readSimpleBundle(Bundle bundle) {
		return new StationData(bundle.getString(BUNDLE_STOPNAME), bundle.getInt(BUNDLE_STATIONID));
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public StationData(Parcel in) {
		stopName = in.readString();
		extra = in.readString();
		
		stationId = in.readInt();
		realtimeStop = in.readInt() != 0;
		walkingDistance = in.readInt();
		
		utmCoords[0] = in.readInt();
		utmCoords[1] = in.readInt();
		
		latLongCoords[0] = in.readDouble();
		latLongCoords[1] = in.readDouble();
		
		devi = new ArrayList<DeviData>();
		in.readList(devi, DeviData.class.getClassLoader());

	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(stopName);
		out.writeString(extra);
		
		out.writeInt(stationId);
		out.writeInt(realtimeStop ? 1 : 0);
		out.writeInt(walkingDistance);
		
		out.writeInt(utmCoords[0]);
		out.writeInt(utmCoords[1]);
		
		out.writeDouble(latLongCoords[0]);
		out.writeDouble(latLongCoords[1]);
		
		out.writeList(devi);
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
    public static final Parcelable.Creator<StationData> CREATOR = new Parcelable.Creator<StationData>() {
		public StationData createFromParcel(Parcel in) {
		    return new StationData(in);
		}
		
		public StationData[] newArray(int size) {
		    return new StationData[size];
		}
	};
}

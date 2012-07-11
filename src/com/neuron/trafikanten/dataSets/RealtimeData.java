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

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.util.SparseArray;
import android.widget.TextView;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

public class RealtimeData extends RealtimeDataGeneric implements Parcelable {
    private final static String TAG = "Trafikanten-RealtimeData";
	public final static String PARCELABLE = "RealtimeData";
	public String lineName;
	public int lineId;
	public String destination;
	public String departurePlatform;
    public int vehicleMode;
	
	/*
	 * Data set of coming departures
	 */
	public ArrayList<RealtimeDataGeneric> nextDepartures;
	
	/*
	 * List of devi data, used by RealtimeView/FavoritesView
	 */
	public ArrayList<DeviData> devi;
	
	public RealtimeData() {
		nextDepartures = new ArrayList<RealtimeDataGeneric>();
		devi = new ArrayList<DeviData>();
	}
	
	public void addDeparture(RealtimeDataGeneric realtimeData) {
		nextDepartures.add(realtimeData);
	}
	
	public int _platformNumber = -1;
	public int getPlatformNumber() {
		if (_platformNumber == -1) {
			try {
				_platformNumber = Integer.parseInt(departurePlatform);
			} catch (NumberFormatException e) {
				_platformNumber = 0;
			}
		}
		return _platformNumber;
	}

    public int getImageResource() {
        switch(vehicleMode) {
            case 0:
                return R.drawable.icon_line_bus;
            case 1:
                return R.drawable.icon_line_boat;
            case 2:
                return R.drawable.icon_line_train;
            case 3:
                return R.drawable.icon_line_tram;
            case 4:
                return R.drawable.icon_line_underground;
            default:
                return R.drawable.icon_line_bus;
        }
    }
	
		
	/*
	 * Renders all departures, expectedDeparture + nextDepartures
	 */
	private Spanned _cachedSpanned = null;
	private int _cachednextDepartures = 0;
    private long _lastCacheUpdated = 0;
    private static final long CACHE_INVALIDATETIME = HelperFunctions.SECOND * 10;

	
	public void renderDepartures(TextView tv, Activity activity, Long currentTime) {
		if (_lastCacheUpdated != 0 && _cachednextDepartures == nextDepartures.size()) {
			if (expectedDeparture - System.currentTimeMillis() > (HelperFunctions.MINUTE * 9)) {
				// We're not rendering a countdown, so lets not re render the same data.
				//Log.d("DEBUG CODE", "Skipping rerender of > 10 minute data");
				tv.setText(_cachedSpanned);
				return;
			}
			if ((System.currentTimeMillis() - _lastCacheUpdated) < (CACHE_INVALIDATETIME)) {
				// We can use cached data
				//Log.d("DEBUG CODE", "Using cached data");
				tv.setText(_cachedSpanned);
				return;				
			}
		}

		//Log.d("DEBUG CODE", "Regenerating cache");
		// Regenerate cache.
		StringBuffer content = new StringBuffer(" ");
		renderToContainer(content, activity, currentTime);
		for (RealtimeDataGeneric nextDeparture : nextDepartures) {
			nextDeparture.renderToContainer(content, activity, currentTime);
		}
		_cachedSpanned = Html.fromHtml(content.toString(), new ImageGetter(activity), null);
		_cachednextDepartures = nextDepartures.size();
		
		tv.setText(_cachedSpanned);
        _lastCacheUpdated = System.currentTimeMillis();
	}
	
	private static class ImageGetter implements Html.ImageGetter {
		
		Resources mResources;
		static final SparseArray<Drawable> mHashMap = new  SparseArray<Drawable>();
		
		private Drawable getCachedDrawable(int id) {
			Object o = mHashMap.get(id);
			if (o != null) {
				return (Drawable) o;
			}
			
	        Drawable d = mResources.getDrawable(id);
	        d.setBounds(0,0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
	        mHashMap.put(id, d);
	        return d;
		}
		
		public ImageGetter(Activity activity) {
			super();
			mResources = activity.getResources();
			
		}
		
	    public Drawable getDrawable(String source) {
	        int id;
	        
	        if (source.equals("LF")) {
	            id = R.drawable.departure_icon_lowfloor;
	        }
	        else if (source.equals("TL1")) {
	            id = R.drawable.departure_icon_trainlength1;
	        }
	        else if (source.equals("TL2")) {
	            id = R.drawable.departure_icon_trainlength2;
	        }
	        else {
	            return null;
	        }

	        return getCachedDrawable(id);
	    }
	};
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimeData(Parcel in) {
		super(in);
		lineName = in.readString();
		lineId = in.readInt();
		destination = in.readString();
		departurePlatform = in.readString();
        vehicleMode = in.readInt();

		nextDepartures = new ArrayList<RealtimeDataGeneric>();
		in.readList(nextDepartures, RealtimeDataGeneric.class.getClassLoader());
		
		devi = new ArrayList<DeviData>();
		in.readList(devi, DeviData.class.getClassLoader());
		
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(lineName);
		out.writeInt(lineId);
		out.writeString(destination);
		out.writeString(departurePlatform);
        out.writeInt(vehicleMode);
		
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



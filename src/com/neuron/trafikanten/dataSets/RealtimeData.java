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
import java.util.HashMap;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neuron.trafikanten.R;

public class RealtimeData extends RealtimeDataGeneric implements Parcelable {
	public final static String PARCELABLE = "RealtimeData";
	public String lineName;
	public int lineId;
	public String destination;
	public String departurePlatform;
	
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
	
		
	/*
	 * Renders all departures, expectedDeparture + nextDepartures
	 */
	public void renderDepartures(LinearLayout container, Activity activity, Long currentTime) {
		//FIXME : should remove container and only use textview.
		container.removeAllViews();
		TextView tv = new TextView(activity);

		
		StringBuffer content = new StringBuffer("   ");
		renderToContainer(content, activity, currentTime);
		for (RealtimeDataGeneric nextDeparture : nextDepartures) {
			nextDeparture.renderToContainer(content, activity, currentTime);
		}
		
		tv.setText(Html.fromHtml(content.toString(), new ImageGetter(activity), null));
		container.addView(tv);
		
		
		/*
		
		TextView reusedTextView = renderToContainer(container, false, activity, currentTime, null);
		
		for (RealtimeDataGeneric nextDeparture : nextDepartures) {
			reusedTextView = nextDeparture.renderToContainer(container, true, activity, currentTime, reusedTextView);
		}*/
	}
	
	private static class ImageGetter implements Html.ImageGetter {
		
		Resources mResources;
		static final HashMap<Integer, Drawable> mHashMap = new  HashMap<Integer, Drawable>();
		
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
	        else if (source.equals("LF1")) {
	            id = R.drawable.departure_icon_trainlength1;
	        }
	        else if (source.equals("LF2")) {
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



package com.neuron.trafikanten.dataSets;

import junit.framework.Assert;
import android.app.Activity;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

/*
 * This is realtime data that is available for "all" realtime sets, aka available both for main view and for subdepartures
 */
public class RealtimeDataGeneric  implements Parcelable {
	public long expectedDeparture;
	
	public boolean inCongestion = false;
	public boolean realtime;
	public boolean lowFloor = false; // kun trikk
	public int numberOfBlockParts = 0; // kun t-bane (3 = kort tog, 6 = langt tog).

	
	public String stopVisitNote;
	public RealtimeDataGeneric() {
	}
	
	/*
	 * @see android.os.Parcelable
	 */
	@Override
	public int describeContents() {	return 0; }
	
	/*
	 * Function for reading the parcel
	 */
	public RealtimeDataGeneric(Parcel in) {
		expectedDeparture = in.readLong();
		inCongestion = in.readInt() != 0;
		realtime = in.readInt() != 0;
		stopVisitNote = in.readString();
		lowFloor = in.readInt() != 0;
		numberOfBlockParts = in.readInt();
	}
	
	/*
	 * Writing current data to parcel.
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(expectedDeparture);
		out.writeInt(inCongestion ? 1 : 0);
		out.writeInt(realtime ? 1 : 0);
		out.writeString(stopVisitNote);
		out.writeInt(lowFloor ? 1 : 0);
		out.writeInt(numberOfBlockParts);
	}
	
	//TODO : http://developer.android.com/training/improving-layouts/smooth-scrolling.html ?
	private TextView buildTextView(boolean marginLeft, Activity activity, long currentTime, TextView reusedTextview) {
		TextView tv;
		if (reusedTextview == null) {
			tv = new TextView(activity);
			tv.setTypeface(HelperFunctions.getTypeface(activity));
			tv.setSingleLine();
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			//if (realtime) {
				tv.setTextColor(-330752); 		//Color.parseColor("#FAF400"); // -330752
			/*} else {
				tv.setTextColor(Color.parseColor("#ecebea"));
			}*/ //TODO : add this, and dont reuse the textview if textcolor changes.
		} else {
			tv = reusedTextview; 
		}
		//Assert.assertTrue(marginLeft || reusedTextview == null);
		
		StringBuffer txt = new StringBuffer(tv.getText());
		if (marginLeft) {
			txt.append("   ");
		}
		if (!realtime) {
			txt.append(activity.getText(R.string.ca));
			txt.append(" ");
		} else if (inCongestion) {
			txt.append(activity.getText(R.string.congestion));
			txt.append(" ");
		}
		HelperFunctions.renderTime(txt, currentTime, activity, expectedDeparture);
		tv.setText(txt);
		return tv;
	}
	
	/*
	 * Returns TextView when it can be reused (aka if no icons are rendered)
	 */
	public TextView renderToContainer(LinearLayout container, boolean marginLeft, Activity activity, long currentTime, TextView reusedTextview) {
		TextView txt = buildTextView(marginLeft, activity, currentTime, reusedTextview);
		if (reusedTextview == null) {
			container.addView(txt);
		}
		
		
		//FIXME : check layout sizes on mdpi and ldpi, icons might not align!
		if (lowFloor) {
			txt = null;
			ImageView img = new ImageView(activity);
			img.setImageResource(R.drawable.departure_icon_lowfloor);
			container.addView(img);
		}
		if (numberOfBlockParts == 3) {
			txt = null;
			ImageView img = new ImageView(activity);
			img.setImageResource(R.drawable.departure_icon_trainlength1);
			container.addView(img);
		}
		else if (numberOfBlockParts == 6) {
			txt = null;
			ImageView img = new ImageView(activity);
			img.setImageResource(R.drawable.departure_icon_trainlength2);
			container.addView(img);			
		}
		return txt;
	}
	
	/*
	 * Used for bundle.getParcel 
	 */
	public static final Parcelable.Creator<RealtimeDataGeneric> CREATOR = new Parcelable.Creator<RealtimeDataGeneric>() {
		public RealtimeDataGeneric createFromParcel(Parcel in) {
		    return new RealtimeDataGeneric(in);
		}
		
		public RealtimeDataGeneric[] newArray(int size) {
		    return new RealtimeDataGeneric[size];
		}
	};
}

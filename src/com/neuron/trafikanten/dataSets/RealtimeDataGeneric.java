package com.neuron.trafikanten.dataSets;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * This is realtime data that is available for "all" realtime sets, aka available both for main view and for subdepartures
 */
//FIXME : should use higher quality images for hdpi, most of them are now upscaled
public class RealtimeDataGeneric  implements Parcelable {
	public long expectedDeparture;
	
	public boolean inCongestion = false;
	public boolean realtime;
	public boolean lowFloor = false; // kun trikk
	public int numberOfBlockParts = 0; // kun t-bane (3 = kort tog, 6 = langt tog).

	
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
		out.writeInt(lowFloor ? 1 : 0);
		out.writeInt(numberOfBlockParts);
	}
	
	/*
	 * //TODO : http://developer.android.com/training/improving-layouts/smooth-scrolling.html ?
	 *
	private TextView buildTextView(boolean marginLeft, Activity activity, long currentTime, TextView reusedTextview) {
		TextView tv;
		if (reusedTextview == null) {
			tv = new TextView(activity);
			tv.setTypeface(HelperFunctions.getTypeface(activity));
			tv.setSingleLine();
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			if (realtime) {
				tv.setTextColor(-330752); 		//Color.parseColor("#FAF400"); // -330752
			} else {
				//tv.setTextColor(Color.parseColor("#ecebea"));
				tv.setTextColor(0);
			}
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
	 *
	public TextView renderToContainer(LinearLayout container, boolean marginLeft, Activity activity, long currentTime, TextView reusedTextview) {
		TextView txt = buildTextView(marginLeft, activity, currentTime, reusedTextview);
		if (reusedTextview == null) {
			container.addView(txt);
		}
		
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
	}*/
	
	public void renderToContainer(StringBuffer output, Activity activity, long currentTime) {
		output.append("   ");
		if (lowFloor) {
			output.append("<img src='LF'/>");
		}
		if (numberOfBlockParts == 3) {
			output.append("<img src='TL1'/>");
		}
		else if (numberOfBlockParts == 6) {
			output.append("<img src='TL2'/>");	
		}
		
		if (inCongestion) {
			output.append(activity.getText(R.string.congestion));
			output.append(" ");
		}
		
		if (realtime) {
			output.append("<font color='#FAF400'>");
		} else {
			output.append("<font color='#ffffff'>");
		}
		HelperFunctions.renderTime(output, currentTime, activity, expectedDeparture);
		output.append("</font>");
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

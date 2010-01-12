package com.google.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Projection;
import com.neuron.trafikanten.R;

// http://community.developer.motorola.com/t5/Android-App-Development-for/Google-Maps/m-p/3422;jsessionid=75FB01FDA4F23318196FEC79A1ADFA11
// http://groups.google.com/group/android-developers/browse_thread/thread/43615742f462bbf1/8918ddfc92808c42

public class FixedMyLocationOverlay extends MyLocationOverlay {
	private final static String TAG = "Trafikanten-FixedMyLocationOverlay";
	private boolean bugged = false;

	private Paint accuracyPaint;
	private Point center;
	private Point left;
	private Drawable drawable;
	private int width;
	private int height;

	public FixedMyLocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
	}

	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLoc, long when) {
		if (!bugged) {
			try {
				super.drawMyLocation(canvas, mapView, lastFix, myLoc, when);
			} catch (Exception e) {
				bugged = true;
			}
		}

		if (bugged) {
			Log.e(TAG,"Bugged sdk version found, using workaround for map bug, see http://groups.google.com/group/android-developers/browse_thread/thread/43615742f462bbf1/8918ddfc92808c42");
			if (drawable == null) {
				accuracyPaint = new Paint();
				accuracyPaint.setAntiAlias(true);
				accuracyPaint.setStrokeWidth(2.0f);
				
				drawable = mapView.getContext().getResources().getDrawable(R.drawable.mylocation);
				width = drawable.getIntrinsicWidth();
				height = drawable.getIntrinsicHeight();
				center = new Point();
				left = new Point();
			}
			Projection projection = mapView.getProjection();
			
			double latitude = lastFix.getLatitude();
			double longitude = lastFix.getLongitude();
			float accuracy = lastFix.getAccuracy();
			
			float[] result = new float[1];

			Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
			float longitudeLineDistance = result[0];

			GeoPoint leftGeo = new GeoPoint((int)(latitude*1e6), (int)((longitude-accuracy/longitudeLineDistance)*1e6));
			projection.toPixels(leftGeo, left);
			projection.toPixels(myLoc, center);
			int radius = center.x - left.x;
			
			accuracyPaint.setColor(0xff6666ff);
			accuracyPaint.setStyle(Style.STROKE);
			canvas.drawCircle(center.x, center.y, radius, accuracyPaint);

			accuracyPaint.setColor(0x186666ff);
			accuracyPaint.setStyle(Style.FILL);
			canvas.drawCircle(center.x, center.y, radius, accuracyPaint);
						
			drawable.setBounds(center.x - width / 2, center.y - height / 2, center.x + width / 2, center.y + height / 2);
			drawable.draw(canvas);
		}
	}

}

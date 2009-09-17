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

package com.google.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class TransparentPanel extends RelativeLayout {
	private static Paint mBackgroundPaint;
	private static Paint mBorderPaint;
	
	private void init() {
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setARGB(225, 75, 75, 75);
		
		mBorderPaint = new Paint();
		mBorderPaint.setARGB(255, 255, 255, 255);
		mBorderPaint.setAntiAlias(true);
		mBorderPaint.setStyle(Style.STROKE);
		mBorderPaint.setStrokeWidth(2);
	}
	
	public TransparentPanel(Context context) {
		super(context);
		init();
	}
	
	public TransparentPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		RectF rect = new RectF();
		rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
		
		canvas.drawRoundRect(rect, 5, 5, mBackgroundPaint);
		canvas.drawRoundRect(rect, 5, 5, mBorderPaint);
		
		super.dispatchDraw(canvas);
	}
}

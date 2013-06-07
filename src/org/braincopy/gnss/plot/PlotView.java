/*
 
Copyright (c) 2013 braincopy.org

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished
to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
IN THE SOFTWARE.

 */

package org.braincopy.gnss.plot;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 
 * @author Hiroaki Tateshita
 * @version 0.51
 * 
 */
public class PlotView extends SurfaceView implements SurfaceHolder.Callback {
	/**
	 * background.
	 */
	private Bitmap background;

	/**
	 * image for points.
	 */
	private Drawable image;

	/**
	 * image rectangle for points.
	 */
	private Rect imageRect;

	/**
	 * latitude of center [degree].
	 */
	private double latCenter = Double.MAX_VALUE;

	/**
	 * Longitude of center [degree].
	 */
	private double lonCenter = Double.MAX_VALUE;

	/**
	 * matrix for the size adjust.
	 */
	private Matrix matrix;

	/**
	 * center of plot area.
	 */
	private float xCenter;

	/**
	 * center of plot area.
	 */
	private float yCenter;

	/**
	 * array of points (x, y) .
	 */
	private PointArray pointArray;

	/**
	 * this value shows a unit length for a dp. initially 100 dp is 1 m
	 */
	private double unitDP = 100;

	/**
	 * radius of earth. [m]
	 */
	private final double radiusOfEarth = 6378100;

	/**
	 * flag for fix or not fix the screen of plotting.
	 */
	private boolean fixTrackCenter = false;

	/**
	 * array of unit length for each scale.
	 */
	private final double[] unitLength = { 0.001, 0.002, 0.005, 0.01, 0.02,
			0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100 };

	/**
	 * index for current scale. initially 9. it means 1m.
	 */
	private int currentUnitLengthIndex = 9;

	/**
	 * 
	 * @param context
	 *            context
	 */
	public PlotView(final Context context) {
		super(context);
		getHolder().addCallback(this);
	}

	/**
	 * @param context
	 *            context
	 * @param attrs
	 *            attribute set
	 */
	public PlotView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
	}

	/**
	 * @param context
	 *            context
	 * @param attrs
	 *            attribute set
	 * @param defStyle
	 *            style
	 */
	public PlotView(final Context context, final AttributeSet attrs,
			final int defStyle) {
		super(context, attrs, defStyle);
		getHolder().addCallback(this);
	}

	/**
	 * 
	 * @param lat
	 *            latitude
	 * @param lon
	 *            longitude
	 * @param scale
	 *            scale index
	 */
	public final void drawPlotArea(final double lat, final double lon,
			final double scale) {
		Canvas canvas = null;
		SurfaceHolder holder = getHolder();
		try {
			canvas = holder.lockCanvas();
			synchronized (holder) {
				// zoom rate of display
				float fImageScale;

				// margin
				float fMarginX = 0.0f;
				float fMarginY = 0.0f;

				// get zoom rate

				int nImageWidth = background.getWidth();
				int nImageHeight = background.getHeight();
				int nViewWidth = getWidth();
				int nViewHeight = getHeight();

				// fit image
				if ((long) nImageWidth * nViewHeight > (long) nViewWidth
						* nImageHeight) {
					fImageScale = (float) nViewWidth / nImageWidth;
					fMarginY = (nViewHeight - fImageScale * nImageHeight) / 2;
				} else {
					fImageScale = (float) nViewHeight / nImageHeight;
					fMarginX = (nViewWidth - fImageScale * nImageWidth) / 2;
				}
				xCenter = nViewWidth / 2;
				yCenter = nViewHeight / 2;

				// zoom
				float fScale = fImageScale;

				// move margin
				float fMoveX = fMarginX;
				float fMoveY = fMarginY;
				if (matrix == null) {
					matrix = new Matrix();
				}
				// zoom
				matrix.preScale(fScale, fScale);

				// move
				matrix.postTranslate(fMoveX, fMoveY);
				canvas.drawBitmap(background, matrix, null);

			}
		} finally {
			if (canvas != null) {
				holder.unlockCanvasAndPost(canvas);
			}
		}
	}

	/**
	 * 
	 */
	public final void plot() {
		Canvas canvas = null;
		SurfaceHolder holder = getHolder();
		try {
			canvas = holder.lockCanvas();
			synchronized (holder) {
				canvas.drawBitmap(background, matrix, null);

				// for test
				/*
				 * for (int i = 0; i < 90; i++) { pointArray.addPoint(x_center +
				 * i, y_center - 2 * i); }
				 */

				// draw points
				for (int i = 0; i < pointArray.getSize(); i++) {
					imageRect.set((int) (pointArray.getX(i) - image
							.getIntrinsicWidth() / 2), (int) (pointArray
							.getY(i) - image.getIntrinsicWidth() / 2),
							(int) (pointArray.getX(i) + image
									.getIntrinsicWidth() / 2),
							(int) (pointArray.getY(i) + image
									.getIntrinsicHeight() / 2));
					image.setBounds(imageRect);
					image.draw(canvas);
				}

				// draw unit bar
				Paint paint = new Paint();
				paint.setColor(Color.BLACK);
				// culDP: currentUnitLengthDP
				double culDP = unitDP * unitLength[currentUnitLengthIndex];
				canvas.drawLine((float) (xCenter - culDP / 2),
						2 * yCenter - 50, (float) (xCenter + culDP / 2),
						2 * yCenter - 50, paint);
				canvas.drawLine((float) (xCenter - culDP / 2),
						2 * yCenter - 45, (float) (xCenter - culDP / 2),
						2 * yCenter - 55, paint);
				canvas.drawLine((float) (xCenter + culDP / 2),
						2 * yCenter - 45, (float) (xCenter + 0.5 * culDP),
						2 * yCenter - 55, paint);
				paint.setTextSize(20);
				canvas.drawText(unitString(), (float) xCenter,
						2 * yCenter - 60, paint);
			}
		} finally {
			if (canvas != null) {
				holder.unlockCanvasAndPost(canvas);
			}
		}
	}

	/**
	 * return string of appropriate length. private double unit_length[]
	 * ={0.001, 0.002, 0.005, 0.01,0.02,0.05 ,0.1,0.2,0.5,1,2,5,10,20,50,100};
	 * 
	 * @return string of appropriate length
	 */
	private String unitString() {
		String result = "1m";
		// zoom up
		while (unitDP * unitLength[currentUnitLengthIndex] > 200.0
				&& currentUnitLengthIndex > 0) {
			currentUnitLengthIndex--;
		}
		while (unitDP * unitLength[currentUnitLengthIndex] < 50.0
				&& currentUnitLengthIndex < (unitLength.length - 1)) {
			currentUnitLengthIndex++;
		}
		switch (currentUnitLengthIndex) {
		case 0:
			result = "1mm";
			break;
		case 1:
			result = "2mm";
			break;
		case 2:
			result = "5mm";
			break;
		case 3:
			result = "1cm";
			break;
		case 4:
			result = "2cm";
			break;
		case 5:
			result = "5cm";
			break;
		case 6:
			result = "10cm";
			break;
		case 7:
			result = "20cm";
			break;
		case 8:
			result = "50cm";
			break;
		case 9:
			result = "1m";
			break;
		case 10:
			result = "2m";
			break;
		case 11:
			result = "5m";
			break;
		case 12:
			result = "10m";
			break;
		case 13:
			result = "20m";
			break;
		case 14:
			result = "50m";
			break;
		case 15:
			result = "100m";
			break;
		default:
			break;
		}
		return result;
	}

	/**
	 * 
	 * @param context
	 *            context
	 */
	public final void loadImages(final Context context) {
		Resources r = context.getResources();
		background = BitmapFactory.decodeResource(r, R.drawable.background);
		image = r.getDrawable(R.drawable.item);
		imageRect = new Rect(0, 0, image.getIntrinsicWidth(),
				image.getIntrinsicHeight());

	}

	/**
	 * This method is to get Lat and Lon info from NMEA sentence and set in the
	 * pointArray with x-y frame.
	 * 
	 * @param message
	 *            it should be NMEA and include RMC
	 */
	public final void setPoint(final String message) {
		if (message.contains("RMC")) {
			String[] rmcMessages = message.split(",");
			if (rmcMessages[2].equals("A")) {
				int indexOfPeriod = rmcMessages[3].indexOf(".");
				Double latDeg = Double.parseDouble(rmcMessages[3].substring(0,
						indexOfPeriod - 2));
				Double latMin = Double.parseDouble(rmcMessages[3]
						.substring(indexOfPeriod - 2));
				Double lat = latDeg + latMin / 60.0;
				if (rmcMessages[4].equals("S")) {
					lat = lat * -1;
				}
				Double lonDeg = Double.parseDouble(rmcMessages[5].substring(0,
						indexOfPeriod - 2));
				Double lonMin = Double.parseDouble(rmcMessages[5]
						.substring(indexOfPeriod - 2));
				Double lon = lonDeg + lonMin / 60.0;
				if (rmcMessages[6].equals("E")) {
					lon = lon * -1;
				}
				if ((lonCenter == Double.MAX_VALUE)
						&& (latCenter == Double.MAX_VALUE)) {
					lonCenter = lon;
					latCenter = lat;
				}

				double deltaX = 2 * radiusOfEarth * Math.PI * (lonCenter - lon)
						/ 360.0 * Math.cos(lat * Math.PI / 180) * unitDP;
				double deltaY = 2 * radiusOfEarth * Math.PI * (latCenter - lat)
						/ 360.0 * unitDP;
				// fixView = false;
				if (fixTrackCenter) {
					pointArray.moveAll(-1 * deltaX, -1 * deltaY);
					pointArray.addPoint(xCenter, yCenter);
					lonCenter = lon;
					latCenter = lat;
				} else {
					pointArray.addPoint(xCenter + deltaX, yCenter + deltaY);
				}
			} else {
				Log.e("hiro", "not validated nmea: " + message);
			}
		}

	}

	@Override
	public void surfaceChanged(final SurfaceHolder arg0, final int arg1,
			final int arg2, final int arg3) {

	}

	@Override
	public final void surfaceCreated(final SurfaceHolder arg0) {
		matrix = new Matrix();
		drawPlotArea(-1, -1, -1);
		final int maxPointNumber = 1000;
		pointArray = new PointArray(maxPointNumber);
		// for test
		/*
		 * for (int i = 0; i < 100; i++) { pointArray.addPoint( x_center + 100 *
		 * Math.sin(i * 2 * Math.PI / 50.0), y_center + 100 * Math.cos(i *
		 * Math.PI / 50.0)); }
		 */
		// plot();
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder arg0) {

	}

	/**
	 * 
	 * @param d
	 *            > 1 : zoom up, < 1 : zoom down
	 */
	public final void setZoom(final double d) {
		unitDP = unitDP * d;
		pointArray.zoomAll(d, xCenter, yCenter);

	}

	/**
	 * 
	 * @param deltaX
	 *            move value of x
	 * @param deltaY
	 *            move value of y
	 */
	public final void moveAll(final double deltaX, final double deltaY) {
		this.pointArray.moveAll(deltaX, deltaY);
		double deltaLon = deltaX
				* 180
				/ (radiusOfEarth * Math.PI
						* Math.cos(latCenter * Math.PI / 180) * unitDP) * -1;
		double deltaLat = deltaY * 180 / (radiusOfEarth * Math.PI * unitDP)
				* -1;
		lonCenter = lonCenter - deltaLon;
		latCenter = latCenter - deltaLat;
	}

	/**
	 * 
	 */
	public final void clear() {
		this.pointArray.setSize(0);

	}

	/**
	 * 
	 * @param fixViewflag
	 *            flag for
	 */
	public final void setFixTrackCenter(final boolean fixViewflag) {
		this.fixTrackCenter = fixViewflag;

		// when false to true
		if (fixViewflag && pointArray.getSize() > 0) {
			double deltaX = xCenter - pointArray.getLatestX();
			double deltaY = yCenter - pointArray.getLatestY();
			moveAll(deltaX, deltaY);
		}
	}

	/**
	 * 
	 * @return fix view flag
	 */
	public final boolean isFixTrackCenter() {
		return this.fixTrackCenter;
	}

}

package com.androar;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

public class RenderRectanglesView extends ImageView {
	private Paint paint;
	private ArrayList<Rect> rectangles;
	private int width, height;

	
	public RenderRectanglesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public void init() {
		initPaint();
		initDimensions();
	}
	
	public void initPaint() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.YELLOW);
		paint.setStrokeWidth(3);
	}

	public void initDimensions() {
		WindowManager wm = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		width = display.getWidth();
		height = display.getHeight();
	}
	
	public void setPaintColor(int c) {
		paint.setColor(c);
	}
	
	public void setRects(ArrayList<Rect> rects) {
		rectangles = rects;
	}

	public void addRect(Rect rect) {
		rectangles.add(rect);
	}

	/* Redraws the rectangles array. If you want
	 * to draw something new, just change the rectangles array
	 * and call invalidate(). This should call onDraw and redraw
	 * rectangles on screen.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		for (Rect r : rectangles) {
			System.out.println("crt = " + r);
			canvas.drawRect(r, paint);
		}
	}

	public Rect LandscapeToPortrait(Rect r) {
		int left = height - r.bottom;
		int bottom = width - r.left;
		int right = height - r.top;
		int top = width - r.left;
		return new Rect(left, top, right, bottom);
	}
}

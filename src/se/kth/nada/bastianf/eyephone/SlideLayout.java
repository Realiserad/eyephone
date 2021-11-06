package se.kth.nada.bastianf.eyephone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * A relative layout handling horizontal slide events. An event is fired when
 * the user presses and holds the component while moving her finger. To handle
 * the event, set an OnSlideCompleteListener and override onSlideComplete.
 * Different logic may be implemented depending on whether the user has moved
 * her finger left or right. The default implementation does nothing.
 * 
 * Example:
 * {@code
 * SlideLayout slideLayout = new SlideLayout(this);
 * slideLayout.setOnSlideCompleteListener(new SlideLayout.OnSlideCompleteListener() {
 * 		@Override
 * 		public void onSlideComplete(SlideLayout.Direction dir) {
 * 			if (dir == SlideLayout.Direction.LEFT) {
 * 				// Left slide
 * 			} else if (dir == SlideLayout.Direction.RIGHT) {
 * 				// Right slide
 * 			} else if (dir == SlideLayout.Direction.NONE) {
 * 				// Corresponds to an onClick event
 * 			}
 * 		}
 * });
 * }
 * 
 * @author Bastian Fredriksson
 */
public class SlideLayout extends RelativeLayout {
	/** The value of this field is the same as the phone's dpi */
	private int DELTA;
	private float x;
	private Drawable defaultBackground;

	private OnSlideCompleteListener onSlideCompleteListener = new OnSlideCompleteListener() {
		@Override
		public void onSlideComplete(Direction dir) {
		}
	};

	public SlideLayout(Context context) {
		super(context);
		setDelta(context);
	}

	public SlideLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDelta(context);
	}

	public SlideLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setDelta(context);
	}
	
	private void setDelta(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		DELTA = dm.densityDpi;
	}

	public void setOnSlideCompleteListener(
			OnSlideCompleteListener onSlideCompleteListener) {
		this.onSlideCompleteListener = onSlideCompleteListener;
	}
	
	/**
	 * Set the background image of this view. This method keeps compatible with
	 * older APIs by checking the current build version.
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void setBackground(Drawable background) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
		    super.setBackgroundDrawable(background);
		} else {
		    super.setBackground(background);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			// Backup background
			defaultBackground = getBackground();
			x = e.getX();
			return true;
		}
		if (e.getAction() == MotionEvent.ACTION_MOVE) {
			float distance = e.getX() - x; // Will be < 0 if left
			if (distance > DELTA || distance < -DELTA) {
				return true;
			}
			double ratio = (e.getX() - x) / DELTA;
			if (ratio > 0) {
				setBackgroundColor(Color.rgb(255, 255,
						(int) (255 - 255 * ratio)));
			} else {
				int fade = (int) (255 - 255 * Math.abs(ratio));
				setBackgroundColor(Color.rgb(fade, 255, fade));
			}
			return true;
		}
		if (e.getAction() == MotionEvent.ACTION_CANCEL) {
			// Moving outside the bounds of the view, restore default color 
			setBackground(defaultBackground);
			return true;
		}
		if (e.getAction() == MotionEvent.ACTION_UP) {
			// Restore default background color
			setBackground(defaultBackground);
			if (e.getX() - x > DELTA) {
				onSlideCompleteListener.onSlideComplete(Direction.RIGHT);
			} else if (e.getX() - x < -DELTA) {
				onSlideCompleteListener.onSlideComplete(Direction.LEFT);
			} else {
				onSlideCompleteListener.onSlideComplete(Direction.NONE);
			}
			return true;
		}
		return super.onTouchEvent(e);
	}

	public interface OnSlideCompleteListener {
		void onSlideComplete(Direction dir);
	}

	enum Direction {
		LEFT, RIGHT, NONE;
	}
}
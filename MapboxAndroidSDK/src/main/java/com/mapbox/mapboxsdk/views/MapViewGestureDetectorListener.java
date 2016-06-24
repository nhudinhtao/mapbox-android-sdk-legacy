package com.mapbox.mapboxsdk.views;

import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.util.constants.UtilConstants;

import java.util.HashSet;

/**
 * A custom gesture detector that processes gesture events and dispatches them
 * to the map's overlay system.
 */
public class MapViewGestureDetectorListener extends SimpleOnGestureListener
{
	// Private Methods
	// =================================================================================================================================================================================================

    private final MapView mapView;

	private final HashSet<GestureDetector.OnGestureListener> mGestureListener;
	private final HashSet<GestureDetector.OnDoubleTapListener> mDoubleTapListener;

	// Constructors
	// =================================================================================================================================================================================================

    /**
     * Bind a new gesture detector to a map
     *
     * @param mv a map view
     */
    public MapViewGestureDetectorListener(final MapView mv)
	{
        this.mapView = mv;

		mGestureListener = new HashSet<>();
		mDoubleTapListener = new HashSet<>();
    }

	// Public Methods
	// =================================================================================================================================================================================================

	/**
	 *
	 * @param pListener
	 */
	public final void addOnGestureListener(final GestureDetector.OnGestureListener pListener)
	{
		mGestureListener.add(pListener);
	}

	public final void addOnDoubleTapListener(final GestureDetector.OnDoubleTapListener pListener)
	{
		mDoubleTapListener.add(pListener);
	}

	public final void removeOnGestureListener(final GestureDetector.OnGestureListener pListener)
	{
		mGestureListener.remove(pListener);
	}

	public final void removeOnDoubleTapListener(final GestureDetector.OnDoubleTapListener pListener)
	{
		mDoubleTapListener.remove(pListener);
	}

	// Implements SimpleOnGestureListener Methods
	// =================================================================================================================================================================================================

    @Override
    public boolean onDown(final MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnGestureListener aListener : mGestureListener)
			aListener.onDown(e);

        // Stop scrolling if we are in the middle of a fling!
        if (this.mapView.mIsFlinging)
		{
            this.mapView.mScroller.abortAnimation();
            this.mapView.mIsFlinging = false;
        }

        if (this.mapView.getOverlayManager().onDown(e, this.mapView))
		{
            return true;
        }

        return true;
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY)
	{
		// inform all MapView listener !
		for (GestureDetector.OnGestureListener aListener : mGestureListener)
			aListener.onFling(e1, e2, velocityX, velocityY);

        if (this.mapView.isAnimating() || this.mapView.getOverlayManager().onFling(e1, e2, velocityX, velocityY, this.mapView))
            return true;

        final int worldSize = this.mapView.getProjection().mapSize(this.mapView.getZoomLevel(false));
        this.mapView.mIsFlinging = true;
        this.mapView.mScroller.fling(this.mapView.getScrollX(), this.mapView.getScrollY(), (int) -velocityX, (int) -velocityY, -worldSize, worldSize, -worldSize, worldSize);
        return true;
    }

    @Override
    public void onLongPress(final MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnGestureListener aListener : mGestureListener)
			aListener.onLongPress(e);

        if (this.mapView.getOverlayManager().onLongPress(e, this.mapView))
            return;

        if (UtilConstants.DEBUGMODE)
		{
            final ILatLng center = this.mapView.getProjection().fromPixels(e.getX(), e.getY());
            this.mapView.zoomOutFixing(center, false);
        }
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY)
	{
		// inform all MapView listener !
		for (GestureDetector.OnGestureListener aListener : mGestureListener)
			aListener.onScroll(e1, e2, distanceX, distanceY);

        if (this.mapView.isAnimating() || this.mapView.getOverlayManager().onScroll(e1, e2, distanceX, distanceY, this.mapView))
		    return true;

		this.mapView.getController().panBy((int) distanceX, (int) distanceY, true);
        return true;
    }

    @Override
    public void onShowPress(final MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnGestureListener aListener : mGestureListener)
			aListener.onShowPress(e);

        this.mapView.getOverlayManager().onShowPress(e, this.mapView);
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnGestureListener aListener : mGestureListener)
			aListener.onSingleTapUp(e);

        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnDoubleTapListener aListener : mDoubleTapListener)
			aListener.onSingleTapConfirmed(e);

        return this.mapView.getOverlayManager().onSingleTapConfirmed(e, this.mapView);
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnDoubleTapListener aListener : mDoubleTapListener)
			aListener.onDoubleTap(e);

        if (this.mapView.getOverlayManager().onDoubleTap(e, this.mapView))
		    return true;

        final ILatLng position = this.mapView.getProjection().fromPixels(e.getX(), e.getY());
        this.mapView.onDoubleTap(position);
        return true;
    }

	@Override
	public boolean onDoubleTapEvent(MotionEvent e)
	{
		// inform all MapView listener !
		for (GestureDetector.OnDoubleTapListener aListener : mDoubleTapListener)
			aListener.onDoubleTapEvent(e);

		return false;
	}
}

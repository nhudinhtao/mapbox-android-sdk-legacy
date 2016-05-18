package com.mapbox.mapboxsdk.views;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;

public interface MapViewListener {
    void onShowMarker(final MapView pMapView, final Marker pMarker);

    void onHideMarker(final MapView pMapView, final Marker pMarker);

    void onTapMarker(final MapView pMapView, final Marker pMarker);

    void onLongPressMarker(final MapView pMapView, final Marker pMarker);

    void onTapMap(final MapView pMapView, final ILatLng pPosition);

    void onDoubleTapMap(final MapView pMapView, final ILatLng pPosition);

    void onLongPressMap(final MapView pMapView, final ILatLng pPosition);
}

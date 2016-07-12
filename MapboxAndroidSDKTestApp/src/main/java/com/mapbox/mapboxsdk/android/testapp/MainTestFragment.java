package com.mapbox.mapboxsdk.android.testapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.TilesLoadedListener;

public class MainTestFragment extends Fragment implements TabLayout.OnTabSelectedListener {

	private static final String TERR   = "Terr";
	private static final String SAT    = "Sat";
	private static final String STREET = "Street";

	private MapView mv;
	private String satellite    = "mapbox.satellite";
	private String street       = "mapbox.streets";
	private String terrain      = "mapbox.outdoors";
	private String currentLayer = "";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_maintest, container, false);

		mv = (MapView) view.findViewById(R.id.mapview);
		// Set Default Map Type
		replaceMapView(terrain);
		currentLayer = "terrain";
		mv.setUserLocationEnabled(true)
				.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
		// Set a reasonable user location zoom level
		mv.setUserLocationRequiredZoom(16);

        /*
        // Original GeoJSON Test that caus es crash when Hardware Acceleration when enabled in TestApp
        mv.loadFromGeoJSONURL("https://gist.githubusercontent.com/tmcw/4a6f5fa40ab9a6b2f163/raw/b1ee1e445225fc0a397e2605feda7da74c36161b/map.geojson");
        */
		// Smaller GeoJSON Test
		mv.loadFromGeoJSONURL("https://gist.githubusercontent.com/bleege/133920f60eb7a334430f/raw/5392bad4e09015d3995d6153db21869b02f34d27/map.geojson");
		Marker m = new Marker(mv, "Edinburgh", "Scotland", new LatLng(55.94629, -3.20777));
		m.setIcon(new Icon(getActivity(), Icon.Size.SMALL, "marker-stroked", "FF0000"));
		mv.addMarker(m);

		m = new Marker(mv, "Stockholm", "Sweden", new LatLng(59.32995, 18.06461));
		m.setIcon(new Icon(getActivity(), Icon.Size.MEDIUM, "city", "FFFF00"));
		mv.addMarker(m);

		m = new Marker(mv, "Prague", "Czech Republic", new LatLng(50.08734, 14.42112));
		m.setIcon(new Icon(getActivity(), Icon.Size.LARGE, "land-use", "00FFFF"));
		mv.addMarker(m);

		m = new Marker(mv, "Athens", "Greece", new LatLng(37.97885, 23.71399));
		mv.addMarker(m);

		m = new Marker(mv, "Milwaukee", "Wisconsin", new LatLng(43.04506, -87.92217));
		m.setIcon(new Icon(getActivity(), Icon.Size.LARGE, "city", "0000FF"));
		mv.addMarker(m);

		final TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tablayout);
		tabLayout.addTab(tabLayout.newTab().setText(TERR));
		tabLayout.addTab(tabLayout.newTab().setText(SAT));
		tabLayout.addTab(tabLayout.newTab().setText(STREET));
		tabLayout.setOnTabSelectedListener(this);

		FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				mv.setMapOrientation(mv.getMapOrientation() + 45f);
			}
		});

		mv.setOnTilesLoadedListener(new TilesLoadedListener() {
			@Override
			public boolean onTilesLoaded() {
				return false;
			}

			@Override
			public boolean onTilesLoadStarted() {
				// TODO Auto-generated method stub
				return false;
			}
		});
		mv.setVisibility(View.VISIBLE);

		PathOverlay equator = new PathOverlay();
		equator.addPoint(0, -89);
		equator.addPoint(0, 89);
		mv.getOverlays().add(equator);

		return view;
	}

	protected void replaceMapView(String layer) {
		ITileLayer source;
		BoundingBox box;
		if (layer.equalsIgnoreCase("OpenStreetMap")) {
			source = new WebSourceTileLayer("openstreetmap",
					"http://tile.openstreetmap.org/{z}/{x}/{y}.png").setName("OpenStreetMap")
					.setAttribution("© OpenStreetMap Contributors")
					.setMinimumZoomLevel(1)
					.setMaximumZoomLevel(18);
		} else if (layer.equalsIgnoreCase("OpenSeaMap")) {
			source = new WebSourceTileLayer("openstreetmap",
					"http://tile.openstreetmap.org/seamark/{z}/{x}/{y}.png").setName(
					"OpenStreetMap")
					.setAttribution("© OpenStreetMap Contributors")
					.setMinimumZoomLevel(1)
					.setMaximumZoomLevel(18);
		} else if (layer.equalsIgnoreCase("mapquest")) {
			source = new WebSourceTileLayer("mapquest",
					"http://otile1.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png").setName(
					"MapQuest Open Aerial")
					.setAttribution(
							"Tiles courtesy of MapQuest and OpenStreetMap contributors.")
					.setMinimumZoomLevel(1)
					.setMaximumZoomLevel(18);
		} else {
			source = new MapboxTileLayer(layer);
		}
		mv.setTileSource(source);
		box = source.getBoundingBox();
		mv.setScrollableAreaLimit(box);
		mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
		mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());
		mv.setCenter(mv.getTileProvider().getCenterCoordinate());
		mv.setZoom(1);
		Log.d("MainActivity", "zoomToBoundingBox " + box.toString());
	}

	private void setLayer(final String layer) {
		if (!currentLayer.equals(layer)) {
			replaceMapView(layer);
			currentLayer = layer;
		}
	}

	@Override
	public void onTabSelected(final TabLayout.Tab tab) {
		if (tab != null && tab.getText() != null) {
			final String text = tab.getText().toString();
			if (text.equals(SAT)) {
				setLayer(satellite);
			} else if (text.equals(TERR)) {
				setLayer(terrain);
			} else if (text.equals(STREET)) {
				setLayer(street);
			}
		}
	}

	@Override
	public void onTabUnselected(final TabLayout.Tab tab) {

	}

	@Override
	public void onTabReselected(final TabLayout.Tab tab) {

	}
}

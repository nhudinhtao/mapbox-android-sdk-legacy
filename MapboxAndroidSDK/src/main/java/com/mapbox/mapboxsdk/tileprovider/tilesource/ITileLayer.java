package com.mapbox.mapboxsdk.tileprovider.tilesource;

import android.graphics.drawable.Drawable;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileDownloader;

public interface ITileLayer {

    void detach();

    /**
     * Get a rendered Drawable from the specified Tile.
     *
     * @param downloader reference to the downloader asking for the tile
     * @param aTile the tile requested
     * @param hdpi is hdpi requested?
     * @return the rendered Drawable
     */
    Drawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile,
            boolean hdpi);

    /**
     * Set the current tile url template used in this layer
     *
     * @return the tile layer
     */
	TileLayer setURL(final String aUrl);

    /**
     * Get the minimum zoom level this tile source can provide.
     *
     * @return the minimum zoom level
     */
	float getMinimumZoomLevel();

    /**
     * Get the maximum zoom level this tile source can provide.
     *
     * @return the maximum zoom level
     */
	float getMaximumZoomLevel();

    /**
     * Get the tile size in pixels this tile source provides.
     *
     * @return the tile size in pixels
     */
	int getTileSizePixels();

    /**
     * Get the tile source bounding box.
     *
     * @return the tile source bounding box
     */
	BoundingBox getBoundingBox();

    /**
     * Get the tile source center.
     *
     * @return the tile source center
     */
	LatLng getCenterCoordinate();

    /**
     * Get the tile source suggested starting zoom.
     *
     * @return the tile suggested starting zoom
     */
	float getCenterZoom();

    /**
     * Get the tile source short name
     *
     * @return the short name
     */
	String getName();

    /**
     * Get the tile source description
     *
     * @return the short description
     */
	String getDescription();

    /**
     * Get the tile source attribution
     *
     * @return the short attribution
     */
	String getAttribution();

    /**
     * Get the tile source legend
     *
     * @return the legend
     */
	String getLegend();

    /**
     * Get the cache key used for the disk cache
     *
     * @return the cache key
     */
	String getCacheKey();
}

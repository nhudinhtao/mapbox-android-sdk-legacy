package com.mapbox.mapboxsdk.tileprovider.constants;

import com.mapbox.mapboxsdk.geometry.BoundingBox;

/**
 * This class contains constants used by the tile provider.
 *
 * @author Neil Boyd
 */
public interface TileLayerConstants {

    boolean DEBUG_TILE_PROVIDERS = false;

    BoundingBox WORLD_BOUNDING_BOX = new BoundingBox(90, 180, -90, -180);

    /**
     * Minimum Zoom Level
     */
	int MINIMUM_ZOOMLEVEL = 0;

    /**
     * Maximum Zoom Level - we use Integers to store zoom levels so overflow happens at 2^32 - 1,
     * but we also have a tile size that is typically 2^8, so (32-1)-8-1 = 22
     */
	int MAXIMUM_ZOOMLEVEL = 22;

    int DEFAULT_TILE_SIZE = 256;
    int RETINA_TILE_SIZE = 512;

    /**
     * Initial tile cache size. The size will be increased as required by calling {@link
     * LRUMapTileCache.ensureCapacity(int)} The tile cache will always be at least 3x3.
     */
	int CACHE_MAPTILECOUNT_DEFAULT = 9;

    int CACHE_MAPTILEDISKSIZE_DEFAULT = 100 * 1024 * 1024;
    /**
     * number of tile download threads, conforming to OSM policy:
     * http://wiki.openstreetmap.org/wiki/Tile_usage_policy
     */
	int NUMBER_OF_TILE_DOWNLOAD_THREADS = 4;

    int TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE = 40;
}

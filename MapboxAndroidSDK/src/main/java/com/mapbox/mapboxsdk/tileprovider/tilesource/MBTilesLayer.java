package com.mapbox.mapboxsdk.tileprovider.tilesource;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.modules.MBTilesFileArchive;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileDownloader;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

/**
 * A layer that pulls resources from an MBTiles file. Used for offline map tiles,
 * like those generated by TileMill.
 */
public class MBTilesLayer extends TileLayer implements MapViewConstants, MapboxConstants {

    private static final String TAG = "MBTilesLayer";
    MBTilesFileArchive mbTilesFileArchive;

    /**
     * Initialize a new tile layer, represented by a MBTiles file.
     *
     * @param url     path to a MBTiles file
     * @param context the graphics drawing context
     */
    public MBTilesLayer(final Context context, final String url) {
        super(url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')), url);
        initialize(url, context);
    }

    /**
     * Initialize a new tile layer, represented by a MBTiles file.
     * This constructor does not need a context but as a consequence won't look
     * for an asset mbtiles
     *
     * @param url path to a MBTiles file
     */
    public MBTilesLayer(final String url) {
        this(null, url);
    }

    /**
     * Initialize a new tile layer, represented by a MBTiles file.
     *
     * @param file a MBTiles file
     */
    public MBTilesLayer(final File file) {
        super(file.getName(), file.getAbsolutePath());
        initialize(file);
    }

    /**
     * Initialize a new tile layer, represented by a Database file.
     *
     * @param db a database used as the MBTiles source
     */
    public MBTilesLayer(final SQLiteDatabase db) {
        super(getFileName(db.getPath()), db.getPath());
        initialize(db);
    }

    /**
     * Get the filename of this layer based on the full path
     *
     * @param path
     * @return the filename of the backing mbtiles file
     */
    private static String getFileName(final String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
    }

    /**
     * Creates a file from an input stream by reading it byte by byte.
     * todo: same as MapViewFactory's createFileFromInputStream
     */
    private static File createFileFromInputStream(InputStream inputStream, String URL) {
        try {
            File f = new File(URL);
            OutputStream outputStream = new FileOutputStream(f);
            byte[] buffer = new byte[1024];
            int length = 0;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create file from input stream.", e);
        }
        return null;
    }

    /**
     * Reads and opens a MBTiles file and loads its tiles into this layer.
     *
     * @param file
     */
    private void initialize(File file) {
        if (file != null) {
            mbTilesFileArchive = MBTilesFileArchive.getDatabaseFileArchive(file);
        }

        if (mbTilesFileArchive != null) {
            mMaximumZoomLevel = mbTilesFileArchive.getMaxZoomLevel();
            mMinimumZoomLevel = mbTilesFileArchive.getMinZoomLevel();
            mName = mbTilesFileArchive.getName();
            mDescription = mbTilesFileArchive.getDescription();
            mAttribution = mbTilesFileArchive.getAttribution();
            mBoundingBox = mbTilesFileArchive.getBounds();
            mCenter = mbTilesFileArchive.getCenter();
        }
    }

    /**
     * Reads and opens a MBTiles file given by url and loads its tiles into this layer.
     */
    private void initialize(final SQLiteDatabase db) {
        if (db != null) {
            mbTilesFileArchive = new MBTilesFileArchive(db);
        }

        if (mbTilesFileArchive != null) {
            mMaximumZoomLevel = mbTilesFileArchive.getMaxZoomLevel();
            mMinimumZoomLevel = mbTilesFileArchive.getMinZoomLevel();
            mName = mbTilesFileArchive.getName();
            mDescription = mbTilesFileArchive.getDescription();
            mAttribution = mbTilesFileArchive.getAttribution();
            mBoundingBox = mbTilesFileArchive.getBounds();
            mCenter = mbTilesFileArchive.getCenter();
        }
    }

    /**
     * Reads and opens a MBTiles file given by url and loads its tiles into this layer.
     */
    private void initialize(String url, final Context context) {
        initialize(getFile(url, context));
    }

    private File getFile(String url, final Context context) {
        if (context != null) {
            //we assume asset here
            AssetManager am = context.getAssets();
            InputStream inputStream;
            try {
                inputStream = am.open(url);
                final File mbTilesDir;
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                        || (!Environment.isExternalStorageRemovable())) {
                    mbTilesDir = new File(context.getExternalFilesDir(null), url);
                } else {
                    mbTilesDir = new File(context.getFilesDir(), url);
                }
                return createFileFromInputStream(inputStream, mbTilesDir.getPath());
            } catch (IOException e) {
                Log.e(TAG, "MBTiles file not found in assets: " + e.toString());
                return null;
            }
        }
        try {
            return new File(url);
        } catch (Exception e) {
            Log.e(TAG, "can't load MBTiles: " + e.toString());
            return null;
        }
    }

    @Override
    public void detach()
	{
        if (mbTilesFileArchive != null)
		{
            mbTilesFileArchive.close();
            mbTilesFileArchive = null;
        }
    }

    @Override
    public CacheableBitmapDrawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile, boolean hdpi)
	{
        if (mbTilesFileArchive != null)
		{
            InputStream stream = mbTilesFileArchive.getInputStream(this, aTile);

			if (stream != null)
			{
                CacheableBitmapDrawable result = downloader.getCache().putTileStream(aTile, stream, null);
                if (result == null) {
                    Log.d(TAG, "error reading stream from mbtiles");
                }
                return result;
            }
        }
        return null;
    }
}

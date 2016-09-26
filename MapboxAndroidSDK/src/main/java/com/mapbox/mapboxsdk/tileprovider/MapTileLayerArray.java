package com.mapbox.mapboxsdk.tileprovider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileModuleLayerBase;
import com.mapbox.mapboxsdk.tileprovider.modules.NetworkAvailabilityCheck;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.util.BitmapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

/**
 * This top-level tile provider allows a consumer to provide an array of modular asynchronous tile
 * providers to be used to obtain map tiles. When a tile is requested, the
 * {@link MapTileLayerArray} first checks the {@link MapTileCache} (synchronously) and returns
 * the tile if available. If not, then the {@link MapTileLayerArray} returns null and sends the
 * tile request through the asynchronous tile request chain. Each asynchronous tile provider
 * returns
 * success/failure to the {@link MapTileLayerArray}. If successful, the
 * {@link MapTileLayerArray} passes the result to the base class. If failed, then the next
 * asynchronous tile provider is called in the chain. If there are no more asynchronous tile
 * providers in the chain, then the failure result is passed to the base class. The
 * {@link MapTileLayerArray} provides a mechanism so that only one unique tile-request can be in
 * the map tile request chain at a time.
 *
 * @author Marc Kurtz
 */
public class MapTileLayerArray extends MapTileLayerBase
{
	// Constants
	// =================================================================================================================================================================================================

	// Instance Vars
	// =================================================================================================================================================================================================

    protected final HashMap<MapTile, MapTileRequestState> mWorking;

    protected final List<MapTileModuleLayerBase> mTileProviderList;

    protected final List<MapTile> mUnaccessibleTiles;

    protected final NetworkAvailabilityCheck mNetworkAvailabilityCheck;

	private final ReentrantReadWriteLock mLockWorkingTiles = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock mLockUnaccessibleTiles = new ReentrantReadWriteLock();

	// Constructors
	// =================================================================================================================================================================================================

    /**
     * Creates an {@link MapTileLayerArray} with no tile providers.
     *
     * @param pRegisterReceiver a {@link IRegisterReceiver}
     */
    protected MapTileLayerArray(final Context context, final ITileLayer pTileSource, final IRegisterReceiver pRegisterReceiver)
	{
        this(context, pTileSource, pRegisterReceiver, null);
    }

    /**
     * Creates an {@link MapTileLayerArray} with the specified tile providers.
     *
     * @param aRegisterReceiver  a {@link IRegisterReceiver}
     * @param pTileProviderArray an array of {@link com.mapbox.mapboxsdk.tileprovider.modules.MapTileModuleLayerBase}
     */
    public MapTileLayerArray(final Context context, final ITileLayer pTileSource, final IRegisterReceiver aRegisterReceiver, final MapTileModuleLayerBase[] pTileProviderArray)
	{
        super(context, pTileSource);

        mWorking = new HashMap<>();
        mUnaccessibleTiles = new ArrayList<>();

        mNetworkAvailabilityCheck = new NetworkAvailabilityCheck(context);

        mTileProviderList = new ArrayList<>();

		if (pTileProviderArray != null)
		{
            mCacheKey = pTileProviderArray[0].getCacheKey();
            Collections.addAll(mTileProviderList, pTileProviderArray);
        }
    }

	// Public Methods
	// =================================================================================================================================================================================================

    @Override
    public final void detach()
	{
		super.detach();

        if (getTileSource() != null)
            getTileSource().detach();

        synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
                tileProvider.detach();
        }

		try
		{
			mLockWorkingTiles.writeLock().lock();

			mWorking.clear();
        }
		finally
		{
			mLockWorkingTiles.writeLock().unlock();
		}
	}

	/**
	 *
	 * @return
	 */
    private final boolean networkAvailable()
	{
        return mNetworkAvailabilityCheck == null || mNetworkAvailabilityCheck.getNetworkAvailable();
    }

    /**
     * Checks whether this tile is unavailable and the system is offline.
     *
     * @param pTile the tile in question
     * @return whether the tile is unavailable
     */
    private final boolean tileUnavailable(final MapTile pTile)
	{
		assert pTile != null;

		try
		{
			// Because of a possible write to mUnaccessibleTiles we need to use a write look.
			// Dont do the mistake to try to combine a read and a write lock.
			// Bad code 1: read.lock(), write.lock() will cause a Deadlock !
			// Java does not have a updateToWriteLock functionality.
			// Bad code 2: read.lock(), read.unlock(), write.lock() is not thread safe !
			mLockUnaccessibleTiles.writeLock().lock();

			if (mUnaccessibleTiles.size() > 0)
			{
				if (networkAvailable() || !useDataConnection())
				{
					mUnaccessibleTiles.clear();
				}
				else if (mUnaccessibleTiles.contains(pTile))
				{
					return true;
				}
			}

			return false;
		}
		finally
		{
			mLockUnaccessibleTiles.writeLock().unlock();
		}
    }

	@Nullable
    @Override
    public Drawable getMapTile(final MapTile pTile, final boolean allowRemote)
	{
        if (tileUnavailable(pTile))
		    return null;

        CacheableBitmapDrawable tileDrawable = mTileCache.getMapTileFromMemory(pTile);

        if (tileDrawable != null && tileDrawable.isBitmapValid() && !BitmapUtils.isCacheDrawableExpired(tileDrawable))
		{
            tileDrawable.setBeingUsed(true);
            return tileDrawable;
        }
		else if (allowRemote)
		{
            boolean alreadyInProgress = false;

			try
			{
				mLockWorkingTiles.readLock().lock();

                alreadyInProgress = mWorking.containsKey(pTile);
            }
			finally
			{
				mLockWorkingTiles.readLock().unlock();
			}

            if (!alreadyInProgress)
			{
                final MapTileRequestState state;

                synchronized (mTileProviderList)
				{
                    final MapTileModuleLayerBase[] providerArray = new MapTileModuleLayerBase[mTileProviderList.size()];
                    state = new MapTileRequestState(pTile, mTileProviderList.toArray(providerArray), this);
                }

				try
				{
					mLockWorkingTiles.readLock().lock();

                    // Check again
                    alreadyInProgress = mWorking.containsKey(pTile);

					if (alreadyInProgress)
                        return null;

				}
				finally
				{
					mLockWorkingTiles.readLock().unlock();
				}

				try
				{
					mLockWorkingTiles.writeLock().lock();

					mWorking.put(pTile, state);
				}
				finally
				{
					mLockWorkingTiles.writeLock().unlock();
				}

                final MapTileModuleLayerBase provider = findNextAppropriateProvider(state);

                if (provider != null)
				{
                    provider.loadMapTileAsync(state);
                }
				else
				{
                    mapTileRequestFailed(state);
                }
            }
            return tileDrawable;
        }

        return null;
    }

    @Override
    public void mapTileRequestCompleted(final MapTileRequestState aState, final Drawable aDrawable)
	{
        try
		{
			mLockWorkingTiles.writeLock().lock();

            mWorking.remove(aState.getMapTile());
        }
		finally
		{
			mLockWorkingTiles.writeLock().unlock();
		}

		super.mapTileRequestCompleted(aState, aDrawable);
    }

    @Override
    public void mapTileRequestFailed(final MapTileRequestState aState)
	{
        final MapTileModuleLayerBase nextProvider = findNextAppropriateProvider(aState);
        if (nextProvider != null)
		{
            nextProvider.loadMapTileAsync(aState);
        }
		else
		{
			try
			{
				mLockWorkingTiles.writeLock().lock();

				mWorking.remove(aState.getMapTile());
            }
			finally
			{
				mLockWorkingTiles.writeLock().unlock();
			}

			if (! networkAvailable())
			{
				try
				{
					mLockUnaccessibleTiles.writeLock().lock();

					mUnaccessibleTiles.add(aState.getMapTile());
				}
				finally
				{
					mLockUnaccessibleTiles.writeLock().unlock();
				}
			}

			super.mapTileRequestFailed(aState);
        }
    }

    @Override
    public void mapTileRequestExpiredTile(MapTileRequestState aState, CacheableBitmapDrawable aDrawable)
	{
        // Call through to the super first so aState.getCurrentProvider() still contains the proper
        // provider.
        super.mapTileRequestExpiredTile(aState, aDrawable);

        // Continue through the provider chain
        final MapTileModuleLayerBase nextProvider = findNextAppropriateProvider(aState);

        if (nextProvider != null)
		{
            nextProvider.loadMapTileAsync(aState);
        }
		else
		{
			try
			{
				mLockWorkingTiles.writeLock().lock();

				mWorking.remove(aState.getMapTile());
            }
			finally
			{
				mLockWorkingTiles.writeLock().unlock();
			}
		}
    }

    /**
     * We want to not use a provider that doesn't exist anymore in the chain, and we want to not
     * use
     * a provider that requires a data connection when one is not available.
     */
    protected MapTileModuleLayerBase findNextAppropriateProvider(final MapTileRequestState aState)
	{
        MapTileModuleLayerBase provider = null;
        boolean providerDoesntExist = false,
                providerCantGetDataConnection = false,
                providerCantServiceZoomlevel = false;
        // The logic of the while statement is
        // "Keep looping until you get null, or a provider that still exists
        // and has a data connection if it needs one and can service the zoom level,"
        do
		{
            provider = aState.getNextProvider();
            // Perform some checks to see if we can use this provider
            // If any of these are true, then that disqualifies the provider for this tile request.
            if (provider != null)
			{
                providerDoesntExist = !this.getProviderExists(provider);
                providerCantGetDataConnection = !useDataConnection() && provider.getUsesDataConnection();
                int zoomLevel = aState.getMapTile().getZ();
                providerCantServiceZoomlevel = zoomLevel > provider.getMaximumZoomLevel() || zoomLevel < provider.getMinimumZoomLevel();
            }
        }
		while ((provider != null) && (providerDoesntExist
                || providerCantGetDataConnection
                || providerCantServiceZoomlevel));
        return provider;
    }

	/**
	 *
	 * @param provider
	 * @return
	 */
    public boolean getProviderExists(final MapTileModuleLayerBase provider)
	{
        synchronized (mTileProviderList)
		{
            return mTileProviderList.contains(provider);
        }
    }

    @Override
    public float getMinimumZoomLevel()
	{
        float result = MINIMUM_ZOOMLEVEL;

		synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
                result = Math.max(result, tileProvider.getMinimumZoomLevel());
        }

        return result;
    }

    @Override
    public float getMaximumZoomLevel()
	{
        float result = MAXIMUM_ZOOMLEVEL;

        synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
                result = Math.min(result, tileProvider.getMaximumZoomLevel());
        }

        return result;
    }

    @Override
    public void setTileSource(final ITileLayer aTileSource)
	{
        super.setTileSource(aTileSource);

		try
		{
			mLockUnaccessibleTiles.writeLock().lock();

			mUnaccessibleTiles.clear();
		}
		finally
		{
			mLockUnaccessibleTiles.writeLock().unlock();
		}

		synchronized (mTileProviderList)
		{
            mTileProviderList.clear();
        }
    }

    @Override
    public boolean hasNoSource()
	{
        synchronized (mTileProviderList)
		{
            return mTileProviderList.size() == 0;
        }
    }

    @Override
    public BoundingBox getBoundingBox()
	{
        BoundingBox result = null;
        synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
			{
                BoundingBox providerBox = tileProvider.getBoundingBox();

                if (result == null)
				{
                    result = providerBox;
                }
				else
				{
                    result = result.union(providerBox);
                }
            }
        }
        return result;
    }

    @Override
    public LatLng getCenterCoordinate()
	{
        float latitude = 0;
        float longitude = 0;
        int nb = 0;

		synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
			{
                LatLng providerCenter = tileProvider.getCenterCoordinate();

                if (providerCenter != null)
				{
                    latitude += providerCenter.getLatitude();
                    longitude += providerCenter.getLongitude();
                    nb++;
                }
            }
        }

        if (nb > 0)
		{
            latitude /= nb;
            longitude /= nb;
            return new LatLng(latitude, longitude);
        }
        return null;
    }

    @Override
    public float getCenterZoom()
	{
        float centerZoom = 0;
        int nb = 0;
        synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
			{
                centerZoom += tileProvider.getCenterZoom();
                nb++;
            }
        }

        if (centerZoom > 0) {
            return centerZoom / nb;
        }

        return (getMaximumZoomLevel() + getMinimumZoomLevel()) / 2;
    }

    @Override
    public int getTileSizePixels()
	{
        int result = 0;
        synchronized (mTileProviderList)
		{
            for (final MapTileModuleLayerBase tileProvider : mTileProviderList)
			{
                result += tileProvider.getTileSizePixels();
                break;
            }
        }
        return result;
    }
}

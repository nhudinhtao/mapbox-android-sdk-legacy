/**
 * @author Brad Leege <bleege@gmail.com>
 * Created on 2/15/14 at 3:26 PM
 */

package com.mapbox.mapboxsdk.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.internal.huc.HttpURLConnectionImpl;
import okhttp3.internal.huc.HttpsURLConnectionImpl;

/**
 * @author Arne
 */
public class NetworkUtils
{
	/**
	 *
	 * @param pContext
	 * @return
	 */
    public static boolean isNetworkAvailable(final Context pContext)
	{
        ConnectivityManager connectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

	/**
	 *
	 * @param url
	 * @return
	 */
    public static HttpURLConnection getHttpURLConnection(final URL url)
	{
        return getHttpURLConnection(url, null, null);
    }

	/**
	 *
	 * @param url
	 * @param cache
	 * @return
	 */
    public static HttpURLConnection getHttpURLConnection(final URL url, final Cache cache)
	{
        return getHttpURLConnection(url, cache, null);
    }

	/**
	 *
	 * @param url
	 * @param cache
	 * @param sslSocketFactory
	 *
	 * @return
	 */
    public static HttpURLConnection getHttpURLConnection(final URL url, final Cache cache, final SSLSocketFactory sslSocketFactory)
	{
		final OkHttpClient.Builder builder = new OkHttpClient.Builder();

		if (cache != null)
			builder.cache(cache);

        if (sslSocketFactory != null)
            builder.sslSocketFactory(sslSocketFactory);

		final OkHttpClient client = builder.build();

        HttpURLConnection connection;

		if (url.getProtocol().equals("https"))
			connection = new HttpsURLConnectionImpl(url, client);
		else
			connection = new HttpURLConnectionImpl(url, client);

        connection.setRequestProperty("User-Agent", MapboxUtils.getUserAgent());
        return connection;
    }

	/**
	 *
	 * @param cacheDir
	 * @param maxSize
	 * @return
	 * @throws IOException
	 */
    public static Cache getCache(final File cacheDir, final int maxSize) throws IOException
	{
        return new Cache(cacheDir, maxSize);
    }
}

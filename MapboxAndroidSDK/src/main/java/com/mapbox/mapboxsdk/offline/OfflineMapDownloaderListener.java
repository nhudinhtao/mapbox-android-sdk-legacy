package com.mapbox.mapboxsdk.offline;

public interface OfflineMapDownloaderListener {

    void stateChanged(OfflineMapDownloader.MBXOfflineMapDownloaderState newState);
    void initialCountOfFiles(Integer numberOfFiles);
    void progressUpdate(Integer numberOfFilesWritten, Integer numberOfFilesExcepted);
    void networkConnectivityError(Throwable error);
    void sqlLiteError(Throwable error);
    void httpStatusError(Throwable error);
    void completionOfOfflineDatabaseMap(OfflineMapDatabase offlineMapDatabase);

}

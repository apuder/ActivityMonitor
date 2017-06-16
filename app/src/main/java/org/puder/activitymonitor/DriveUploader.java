package org.puder.activitymonitor;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.puder.activitymonitor.ann.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DriveUploader implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    final static public int REQUEST_CODE_RESOLUTION = 0;

    private Activity activity;
    private GoogleApiClient mGoogleApiClient;
    private DriveFolder driveFolder;
    private List<String> fileNames;
    private int currentUpload;


    public DriveUploader(Activity activity) {
        this.activity = activity;
        connect();
    }

    public void connect() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    private void uploadFailed() {
        UploaderEvent event = new UploaderEvent();
        event.message = "Upload failed";
        EventBus.getDefault().post(event);
    }

    private void uploadSucceeded() {
        UploaderEvent event = new UploaderEvent();
        event.message = "Upload succeeded";
        EventBus.getDefault().post(event);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Date date = new Date();
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(date);
        String folderName = Config.STORAGE_DIR + " (" + formattedDate + ")";
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(folderName)
                .build();
        Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                mGoogleApiClient, changeSet).setResultCallback(folderCreatedCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (!result.hasResolution()) {
            GoogleApiAvailability.getInstance().getErrorDialog(activity, result.getErrorCode(), 0).show();
            return;
        }
        try {
            result.startResolutionForResult(activity, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            uploadFailed();
        }
    }

    ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
            ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(DriveFolder.DriveFolderResult result) {
                    if (!result.getStatus().isSuccess()) {
                        uploadFailed();
                        return;
                    }
                    triggerUpload(result.getDriveFolder());
                }
            };

    public void triggerUpload(DriveFolder folder) {
        driveFolder = folder;
        fileNames = StorageUtil.getDirectoryListing();
        currentUpload = 0;
        triggerNextUpload();
    }

    private void triggerNextUpload() {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(driveContentsCallback);
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        uploadFailed();
                        return;
                    }
                    final DriveContents driveContents = result.getDriveContents();

                    new Thread() {
                        @Override
                        public void run() {
                            String file = fileNames.get(currentUpload);
                            OutputStream outputStream = driveContents.getOutputStream();
                            try {
                                FileInputStream in = new FileInputStream(StorageUtil.createFileName(file));
                                IOUtils.copy(in, outputStream);
                            } catch (IOException e) {
                                uploadFailed();
                            }

                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(file)
                                    .setMimeType("text/csv")
                                    .build();

                            driveFolder.createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(fileCallback);
                        }
                    }.start();
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        uploadFailed();
                        return;
                    }
                    currentUpload++;
                    if (currentUpload == fileNames.size()) {
                        uploadSucceeded();
                    } else {
                        triggerNextUpload();
                    }
                }
            };
}

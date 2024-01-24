package com.plugin.filters.plugin_filters.mlkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;

import com.google.android.gms.common.images.Size;
import com.google.common.base.Preconditions;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;

public class PreferenceUtils {
    @Nullable
    public static CameraSource.SizePair getCameraPreviewSizePair(Context context, int cameraId) {
        Preconditions.checkArgument(
                cameraId == CameraSource.CAMERA_FACING_BACK
                        || cameraId == CameraSource.CAMERA_FACING_FRONT);
        String previewSizePrefKey;
        String pictureSizePrefKey;
        if (cameraId == CameraSource.CAMERA_FACING_BACK) {
            previewSizePrefKey = "rear_camera_preview_size";
            pictureSizePrefKey = "rear_camera_picture_size";
        } else {
            previewSizePrefKey = "front_camera_preview_size";
            pictureSizePrefKey = "front_camera_picture_size";
        }

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            Log.d("duongnv", "setCameraPreviewSizePair:get  " + sharedPreferences.getString(previewSizePrefKey, null));

            return new CameraSource.SizePair(
                    Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
                    Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)));
        } catch (Exception e) {
            return null;
        }
    }

    public static void setCameraPreviewSizePair(Context context, int cameraId, CameraSource.SizePair sizePair) {
        String previewSizePrefKey;
        String pictureSizePrefKey;
        if (cameraId == CameraSource.CAMERA_FACING_BACK) {
            previewSizePrefKey = "rear_camera_preview_size";
            pictureSizePrefKey = "rear_camera_picture_size";
        } else {
            previewSizePrefKey = "front_camera_preview_size";
            pictureSizePrefKey = "front_camera_picture_size";
        }
        Log.d("duongnv", "setCameraPreviewSizePair:  " + sizePair.preview.toString());
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(previewSizePrefKey,sizePair.preview.toString()).apply();
        editor.putString(pictureSizePrefKey,sizePair.picture.toString()).apply();
    }

    public static boolean isCameraLiveViewportEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = "pref_key_camera_live_viewport";
        return sharedPreferences.getBoolean(prefKey, false);
    }

    public static boolean shouldHideDetectionInfo(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = "pref_key_info_hide";
        return sharedPreferences.getBoolean(prefKey, false);
    }

    public static boolean shouldSegmentationEnableRawSizeMask(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = "pref_key_segmentation_raw_size_mask";
        return sharedPreferences.getBoolean(prefKey, false);
    }

    @Nullable
    public static android.util.Size getCameraXTargetResolution(Context context, int lensfacing) {
        Preconditions.checkArgument(
                lensfacing == CameraSelector.LENS_FACING_BACK
                        || lensfacing == CameraSelector.LENS_FACING_FRONT);
        String prefKey = lensfacing == CameraSelector.LENS_FACING_BACK ? "pref_key_camerax_rear_camera_target_resolution" : "pref_key_camerax_front_camera_target_resolution";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return android.util.Size.parseSize(sharedPreferences.getString(prefKey, null));
        } catch (Exception e) {
            return null;
        }
    }

    public static int getFaceMeshUseCase(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = "pref_key_face_mesh_use_case";
        return Integer.parseInt(
                sharedPreferences.getString(prefKey, String.valueOf(FaceMeshDetectorOptions.FACE_MESH)));
    }
}

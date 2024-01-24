package com.plugin.filters.plugin_filters.util;

import static com.plugin.filters.plugin_filters.config.ConstantKt.ASSET_PREFIX;
import static com.plugin.filters.plugin_filters.config.ConstantKt.SAVED_IMAGE_FOLDER;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitmapUtils {
    private static final String TAG = BitmapUtils.class.getSimpleName();
    public static final String DRAWABLE_PREFIX = "drawable://";

    public static void loadImageWithGlide(Context context, ImageView imageView, String uri) {
        if (uri != null && uri.length() > 1) {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                Glide.with(context).load(uri).into(imageView);
            } else if (uri.startsWith(DRAWABLE_PREFIX)) {
                try {
                    int id = Integer.parseInt(uri.substring(DRAWABLE_PREFIX.length()));
                    Glide.with(context).load(id).into(imageView);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (uri.startsWith(ASSET_PREFIX)) {
                String file = uri.substring(ASSET_PREFIX.length());
                Glide.with(context).load(Uri.parse("file:///android_asset/".concat(file))).into(imageView);
            } else {
                Glide.with(context).load(new File(uri)).into(imageView);
            }
        }
    }

    public static Bitmap decodePNGImage(Context context, String uri) {
        if (uri.startsWith(DRAWABLE_PREFIX)) {
            try {
                int resId = Integer.parseInt(uri.substring(DRAWABLE_PREFIX.length()));
                return BitmapFactory.decodeResource(context.getResources(), resId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (uri.startsWith(ASSET_PREFIX)) {
            String path = uri.substring(ASSET_PREFIX.length());
            try {
                InputStream is = context.getAssets().open(path);
                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return BitmapFactory.decodeFile(uri);
        }

        return null;
    }

    /**
     * Getting bitmap from Assets folder
     *
     * @return
     */
    public static Bitmap getBitmapFromAssets(Context context, String filename, int width, int height) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            istr = assetManager.open(filename);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, width, height);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            Log.i("ReadBitmap", filename + " : " + "option: " + options);

            return BitmapFactory.decodeStream(istr, null, options);
        } catch (IOException e) {
            Log.e(TAG, "getBitmapFromAssets Exception: ", e);
        }

        return null;
    }

    /**
     * Getting bitmap from Gallery
     *
     * @return
     */
    public static Bitmap getBitmapFromGallery(Context context, Uri path, int width, int height) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(path, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(picturePath, options);
    }

    /**
     * Getting bitmap from file
     *
     * @return Bitmap
     */
    public static Bitmap getBitmapFromFile(File file, int width, int height) {
        InputStream istr;
        Bitmap bitmap = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            istr = new FileInputStream(file);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, width, height);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            Log.i("ReadBitmap", file.getPath() + " : " + "option: " + options);

            ExifInterface exif = new ExifInterface(file.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    break;
            }
            Bitmap bmOriginal = BitmapFactory.decodeStream(istr, null, options);
            return Bitmap.createBitmap(bmOriginal, 0, 0, bmOriginal.getWidth(), bmOriginal.getHeight(), matrix, true);
        } catch (IOException e) {
            Log.e(TAG, "getBitmapFromAssets Exception: ", e);
        }

        return null;
    }

//    private static int calculateInSampleSize(
//            BitmapFactory.Options options, int reqWidth, int reqHeight) {
//        // Raw height and width of image
//        final int height = options.outHeight;
//        final int width = options.outWidth;
//        int inSampleSize = 1;
//
//        if (height > reqHeight || width > reqWidth) {
//
//            final int halfHeight = height / 2;
//            final int halfWidth = width / 2;
//
//            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
//            // height and width larger than the requested height and width.
//            while ((halfHeight / inSampleSize) >= reqHeight
//                    && (halfWidth / inSampleSize) >= reqWidth) {
//                inSampleSize *= 2;
//            }
//        }
//
//        return inSampleSize;
//    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private static final void createFolder(Context c, String path) {
        String folder = c.getExternalFilesDir(null).getAbsolutePath() + "/" + path;
        File FPath = new File(folder);
        if (!FPath.exists()) {
            if (!FPath.mkdir()) {
                System.out.println("***Problem creating Image folder " + path);
            }
        }
    }

    private static Uri saveImage(Context context, Bitmap bitmap, @NonNull String folderName, @NonNull String fileName) throws IOException {
        OutputStream fos = null;
        File imageFile = null;
        Uri imageUri = null;


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + folderName);
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (imageUri == null)
                    throw new IOException("Failed to create new MediaStore record.");

                fos = resolver.openOutputStream(imageUri);
            } else {
//                context.getCacheDir()
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + folderName);

                if (!imagesDir.exists()) imagesDir.mkdir();

                imageFile = new File(imagesDir, fileName + ".png");
                fos = new FileOutputStream(imageFile);
            }


            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos))
                throw new IOException("Failed to save bitmap.");
            fos.flush();
        } finally {
            if (fos != null) fos.close();
        }

        if (imageFile != null) {//pre Q
            MediaScannerConnection.scanFile(context, new String[]{imageFile.toString()}, null, null);
            imageUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", imageFile);
        }
        return imageUri;
    }

    private static Uri saveImage(Context context, String imagePath, @NonNull String folderName, @NonNull String fileName) throws IOException {
        OutputStream fos = null;
        File imageFile = null;
        Uri imageUri = null;
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(imagePath);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (TextUtils.isEmpty(type)) type = "image/png";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, type);
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + folderName);
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri == null) throw new IOException("Failed to create new MediaStore record.");
            fos = resolver.openOutputStream(imageUri);
        } else {
            File imagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + folderName);
            if (!imagesDir.exists()) imagesDir.mkdir();
            imageFile = new File(imagesDir, new File(imagePath).getName());
            fos = new FileOutputStream(imageFile);
        }

        FileUtilsKt.copyFile(new File(imagePath), fos);

        if (imageFile != null) {
            MediaScannerConnection.scanFile(context, new String[]{imageFile.toString()}, null, null);
            imageUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", imageFile);
        }

        return imageUri;
    }

    private static Uri saveImageForShare(Context context, Bitmap bitmap, @NonNull String folderName, @NonNull String fileName) throws IOException {
        OutputStream fos = null;
        File imageFile;
        Uri imageUri;


        try {
            imageFile = new File(context.getExternalCacheDir(), fileName + ".png");
            fos = new FileOutputStream(imageFile);

            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos))
                throw new IOException("Failed to save bitmap.");
            fos.flush();
        } finally {
            if (fos != null) fos.close();
        }

        //pre Q
        MediaScannerConnection.scanFile(context, new String[]{imageFile.toString()}, null, null);
        //imageUri = Uri.fromFile(imageFile);
        imageUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", imageFile);

        return imageUri;
    }

    public static Uri saveImageUriPublic(Context c, Bitmap bm) {
        try {
            String fname = System.currentTimeMillis() + ""; // + ".jpg";
            Uri file = saveImageForShare(c, bm, "mangaverse", fname);

            Log.i("SaveIMAGE", "file: " + file);
            return file;
        } catch (Exception e) {
            Log.d("onBtnSavePng", e.toString());
        }

        return null;
    }

    public static Uri saveImage(Context c, String imagePath) {
        try {
            String name = new File(imagePath).getName();
            Uri file = saveImage(c, imagePath, SAVED_IMAGE_FOLDER, name);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Uri saveImage(Context c, Bitmap bm) {
        try {

            String name = System.currentTimeMillis() + "";
            Uri file = saveImage(c, bm, SAVED_IMAGE_FOLDER, name);

            Log.i("SaveIMAGE", "file: " + file.getPath());
            return file;
        } catch (Exception e) {
            Log.d("onBtnSavePng", e.toString());
        }

        return null;
    }

    public static final String saveImagePrivate(Context c, Bitmap bm) {
        try {
            // Get the external storage directory path
            String path = c.getExternalFilesDir(null).getAbsolutePath();
            // Create a file to save the image
            createFolder(c, "saved_images");

            File myDir = new File(path, "/saved_images");

            Log.i("SaveIMAGE", "folder: " + myDir.getAbsolutePath());

            myDir.mkdirs();
            String fname = System.currentTimeMillis() + ".jpg";
            File file = new File(myDir, fname);

            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Log.i("SaveIMAGE", "file: " + file.getAbsolutePath());

            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.d("onBtnSavePng", e.toString());
        }

        return "";
    }

    public static final String saveImageCache(Context c, Bitmap bm) {
        try {
            File cachePath = new File(c.getCacheDir(), "images");
            cachePath.mkdirs();
            String name = System.currentTimeMillis() + "_mangaverse.jpg";
            File file = new File(cachePath, name);

            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Log.i("SaveIMAGE", "file: " + file.getAbsolutePath());

            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.d("onBtnSavePng", e.toString());
        }

        return "";
    }

    /**
     * Storing image to device gallery
     *
     * @param cr
     * @param source
     * @param title
     * @param description
     * @return
     */
    public static final String insertImage(ContentResolver cr, Bitmap source, String title, String description) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta searchStickerObj to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri url = null;
        String stringUrl = null;    /* value to be returned */

        try {
            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (source != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                try {
                    source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                } finally {
                    imageOut.close();
                }

                long id = ContentUris.parseId(url);
                // Wait until MINI_KIND thumbnail is generated.
                Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                // This is for backward compatibility.
                storeThumbnail(cr, miniThumb, id, 50F, 50F, MediaStore.Images.Thumbnails.MICRO_KIND);
            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return stringUrl;
    }

    /**
     * A copy of the Android internals StoreThumbnail method, it used with the insertImage to
     * populate the android.provider.MediaStore.Images.Media#insertImage with all the correct
     * meta searchStickerObj. The StoreThumbnail method is private so it must be duplicated here.
     *
     * @see MediaStore.Images.Media (StoreThumbnail private method)
     */
    private static final Bitmap storeThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Thumbnails.KIND, kind);
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, (int) id);
        values.put(MediaStore.Images.Thumbnails.HEIGHT, thumb.getHeight());
        values.put(MediaStore.Images.Thumbnails.WIDTH, thumb.getWidth());

        Uri url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }


    /**
     * This method is responsible for solving the rotation issue if exist. Also scale the images to
     * 1024x1024 resolution
     *
     * @param context       The current context
     * @param selectedImage The Image URI
     * @return Bitmap image results
     * @throws IOException
     */
    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage) {
        int MAX_HEIGHT = 1024;//4096;
        int MAX_WIDTH = 1024;//4096;
        Bitmap img = null;
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
            BitmapFactory.decodeStream(imageStream, null, options);
            imageStream.close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            imageStream = context.getContentResolver().openInputStream(selectedImage);
            img = BitmapFactory.decodeStream(imageStream, null, options);

            img = rotateImageIfRequired(context, img, selectedImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            InputStream input = context.getContentResolver().openInputStream(selectedImage);
            ExifInterface ei;
            if (Build.VERSION.SDK_INT > 23) ei = new ExifInterface(input);
            else ei = new ExifInterface(selectedImage.getPath());

            orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    public static Bitmap scaleMaxSize(Bitmap image, int maxSize) {
        // Bitmap r = image.copy(Bitmap.Config.ARGB_8888, true);
        if (image == null) return null;
        int nW;
        int nH;

        if (image.getWidth() > image.getHeight()) {
            nW = maxSize;
            nH = (int) (1.0 * image.getHeight() * nW / image.getWidth());
        } else {
            nH = maxSize;
            nW = (int) (1.0 * image.getWidth() * nH / image.getHeight());
        }

        return Bitmap.createScaledBitmap(image, nW, nH, true);
    }

    public static Bitmap scaleMinSize(Bitmap image, int minSize) {
        // Bitmap r = image.copy(Bitmap.Config.ARGB_8888, true);

        int nW = image.getWidth();
        int nH = image.getHeight();

        if (image.getWidth() < image.getHeight()) {
            nW = minSize;
            nH = (int) (1.0 * image.getHeight() * nW / image.getWidth());
        } else {
            nH = minSize;
            nW = (int) (1.0 * image.getWidth() * nH / image.getHeight());
        }

        Bitmap r = Bitmap.createScaledBitmap(image, nW, nH, true);

        return r;
    }

}
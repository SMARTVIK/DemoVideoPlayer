package com.craterzone.quickvik.demovideoplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

  public class LaunchCamera extends Activity {
   ImageView imVCature_pic;
   Button btnCapture;
   private int reqHeight;
   private int reqWidth;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_launch_camera);
    initializeControls();
   }

   private void initializeControls() {
    imVCature_pic = (ImageView) findViewById(R.id.imVCature_pic);
    btnCapture = (Button) findViewById(R.id.btnCapture);
    btnCapture.setOnClickListener(new OnClickListener() {
     @Override
     public void onClick(View v) {
               /* create an instance of intent
                * pass action android.media.action.IMAGE_CAPTURE
                * as argument to launch camera
                */
      Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
      /*create instance of File with name img.jpg*/
      File file = new File(Environment.getExternalStorageDirectory() + File.separator + "img.jpg");
      /*put uri as extra in intent object*/
      intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
      /*start activity for result pass intent as argument and request code */
      startActivityForResult(intent, 1);
     }
    });

   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    //if request code is same we pass as argument in startActivityForResult
    if(requestCode==1){
     //create instance of File with same name we created before to get image from storage
     File file = new File(Environment.getExternalStorageDirectory()+File.separator + "img.jpg");
     //Crop the captured image using an other intent
     try {
      /*the user's device may not support cropping*/
      cropCapturedImage(Uri.fromFile(file));
     }
     catch(ActivityNotFoundException aNFE){
      //display an error message if user device doesn't support
      String errorMessage = "Sorry - your device doesn't support the crop action!";
      Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
      toast.show();
     }
    }
    if(requestCode==2){
     //Create an instance of bundle and get the returned data
     Bundle extras = data.getExtras();
     //get the cropped bitmap from extras
     Bitmap thePic = extras.getParcelable("data");
     //set image bitmap to image view
     imVCature_pic.setImageBitmap(thePic);
    }
   }

   //create helping method cropCapturedImage(Uri picUri)
   public void cropCapturedImage(Uri picUri) {
    //call the standard crop action intent
    Intent cropIntent = new Intent("com.android.camera.action.CROP");
    //indicate image type and Uri of image
    cropIntent.setDataAndType(picUri, "image/*");
    //set crop properties
    cropIntent.putExtra("crop", "true");
    //indicate aspect of desired crop
    cropIntent.putExtra("aspectX", 1);
    cropIntent.putExtra("aspectY", 1);
    //indicate output X and Y
    cropIntent.putExtra("outputX", 256);
    cropIntent.putExtra("outputY", 256);
    //retrieve data on return
    cropIntent.putExtra("return-data", true);
    //start the activity - we handle returning in onActivityResult
    startActivityForResult(cropIntent, 2);
   }

   public String compressImage(String imageUri) {

    String filePath = getRealPathFromURI(imageUri);
    Bitmap scaledBitmap = null;

    BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
    options.inJustDecodeBounds = true;
    Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

    int actualHeight = options.outHeight;
    int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

    float maxHeight = 816.0f;
    float maxWidth = 612.0f;
    float imgRatio = actualWidth / actualHeight;
    float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

    if (actualHeight > maxHeight || actualWidth > maxWidth) {
     if (imgRatio < maxRatio) {               imgRatio = maxHeight / actualHeight;                actualWidth = (int) (imgRatio * actualWidth);               actualHeight = (int) maxHeight;             } else if (imgRatio > maxRatio) {
      imgRatio = maxWidth / actualWidth;
      actualHeight = (int) (imgRatio * actualHeight);
      actualWidth = (int) maxWidth;
     } else {
      actualHeight = (int) maxHeight;
      actualWidth = (int) maxWidth;

     }
    }

//      setting inSampleSize value allows to load a scaled down version of the original image

    options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
    options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
    options.inPurgeable = true;
    options.inInputShareable = true;
    options.inTempStorage = new byte[16 * 1024];

    try {
//          load the bitmap from its path
     bmp = BitmapFactory.decodeFile(filePath, options);
    } catch (OutOfMemoryError exception) {
     exception.printStackTrace();

    }
    try {
     scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight,Bitmap.Config.ARGB_8888);
    } catch (OutOfMemoryError exception) {
     exception.printStackTrace();
    }

    float ratioX = actualWidth / (float) options.outWidth;
    float ratioY = actualHeight / (float) options.outHeight;
    float middleX = actualWidth / 2.0f;
    float middleY = actualHeight / 2.0f;

    Matrix scaleMatrix = new Matrix();
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

    Canvas canvas = new Canvas(scaledBitmap);
    canvas.setMatrix(scaleMatrix);
    canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
    ExifInterface exif;
    try {
     exif = new ExifInterface(filePath);

     int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
     Log.d("EXIF", "Exif: " + orientation);
     Matrix matrix = new Matrix();
     if (orientation == 6) {
      matrix.postRotate(90);
      Log.d("EXIF", "Exif: " + orientation);
     } else if (orientation == 3) {
      matrix.postRotate(180);
      Log.d("EXIF", "Exif: " + orientation);
     } else if (orientation == 8) {
      matrix.postRotate(270);
      Log.d("EXIF", "Exif: " + orientation);
     }
     scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
    } catch (IOException e) {
     e.printStackTrace();
    }

    FileOutputStream out = null;
    String filename = getFilename();
    try {
     out = new FileOutputStream(filename);

     // write the compressed bitmap at the destination specified by filename.
     scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

    } catch (FileNotFoundException e) {
     e.printStackTrace();
    }

    return filename;

   }

   private int calculateInSampleSize(BitmapFactory.Options options, int actualWidth, int actualHeight) {
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
     final int heightRatio = Math.round((float) height/ (float) reqHeight);
     final int widthRatio = Math.round((float) width / (float) reqWidth);
     inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;      }       final float totalPixels = width * height;       final float totalReqPixelsCap = reqWidth * reqHeight * 2;       while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
     inSampleSize++;
    }

    return inSampleSize;
   }

   private String getRealPathFromURI(String contentURI) {
    Uri contentUri = Uri.parse(contentURI);
    Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
    if (cursor == null) {
     return contentUri.getPath();
    } else {
     cursor.moveToFirst();
     int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
     return cursor.getString(index);
    }
   }

   private String getFilename() {
    File file = new File(Environment.getExternalStorageDirectory().getPath(), "MyFolder/Images");
    if (!file.exists()) {
     file.mkdirs();
    }
    String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
    return uriSting;
   }

  }
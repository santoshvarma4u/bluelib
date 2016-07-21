package blue.bluelib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("NewApi")
public class PhotoCaptureApi extends AppCompatActivity implements
		LocationListener, OnDismissListener {
	public String img_folder = "BlueLib";
	public boolean isGps = false;

	String GPS_TYPE = LocationManager.NETWORK_PROVIDER;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			Bundle extra = getIntent().getExtras();
			if (extra.containsKey("img_folder")) {
				img_folder = extra.getString("img_folder");
				if (extra.containsKey("isGps"))
					isGps = extra.getBoolean("isGps");

				if (isGps)
					searchGPS();
				else
					openCameraHere(REQUEST_IMAGE_CAPTURE);
			}
		}

	}

	static final int REQUEST_IMAGE_CAPTURE = 1;
	String IMAGE_PATH = "0";

	public void openCameraHere(final int request) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			IMAGE_PATH = CreateImageName();
			photoFile = new File(IMAGE_PATH);
			// Continue only if the File was successfully created
			if (photoFile != null) {
				Uri photoURI = Uri.fromFile(photoFile);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
				startActivityForResult(takePictureIntent, request);
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	public String CreateImageName() {
		String path = null;
		try {
			// folder name
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			String dateFileName = sdf.format(new Date());
			// image name
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss");
			String curentDateandTime = sdf1.format(new Date());

			File sdImageMainDirectory = new File(Environment
					.getExternalStorageDirectory().getPath()
					+ "/"
					+ img_folder
					+ "/" + dateFileName);
			if (!sdImageMainDirectory.exists()) {
				sdImageMainDirectory.mkdirs();
			}

			String PATH = Environment.getExternalStorageDirectory().getPath()
					+ "/" + img_folder + "/" + dateFileName + "/";

			path = PATH + curentDateandTime + ".jpg";
		} catch (Exception e) {

			e.printStackTrace();
		}
		return path;

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		if (outState != null) {
			outState.putString("IMAGE_PATH", IMAGE_PATH);
			outState.putString("img_folder", img_folder);
			outState.putBoolean("isGps", isGps);
			outState.putDouble("lat", lattide);
			outState.putDouble("lon", longitude);

		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);

		populateData(savedInstanceState);
	}

	public void populateData(Bundle savedInstanceState) {
		if (savedInstanceState != null) {

			IMAGE_PATH = savedInstanceState.getString("IMAGE_PATH");
			img_folder = savedInstanceState.getString("img_folder");
			isGps = savedInstanceState.getBoolean("isGps");
			lattide = savedInstanceState.getDouble("lat");
			longitude = savedInstanceState.getDouble("lon");
			if (isImageExists(IMAGE_PATH)) {
				sendResult(RESULT_OK);
			} else {
				sendResult(RESULT_CANCELED);
			}

		}
	}

	LocationManager locman;
	double lattide = 0.0, longitude = 0.0;
	ProgressDialog pd;

	public void searchGPS() {
		if (isGPSON()) {
			requestGPS();
		} else {

			confirmGPS();
		}

	}

	public void confirmGPS() {
		Toast.makeText(
				getApplicationContext(),
				"Please Select Use GPS satellites Option and then click Back button",
				Toast.LENGTH_LONG).show();
		Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivityForResult(intent, 123);
	}

	public boolean isGPSON() {
		boolean res = false;
		String provider = Settings.Secure.getString(getApplicationContext()
				.getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if (provider.contains("gps")) {
			res = true;
		}
		System.out.println("GPS status:" + res);
		return res;
	}

	public void requestGPS() {
		locman = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(GPS_TYPE, 0, 0, this);
		if (pd == null)
			pd = new ProgressDialog(this);
		pd.setMessage("searching for Location...");
		pd.setCancelable(false);
		pd.setOnDismissListener(this);
		pd.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {

		case REQUEST_IMAGE_CAPTURE:
			if (resultCode == RESULT_OK) {
				if (isImageExists(IMAGE_PATH)) {

					// CompressImageTask cit = new CompressImageTask();
					// cit.execute(IMAGE_PATH);

					if (compressImage(IMAGE_PATH))
						sendResult(RESULT_OK);
					else
						sendResult(RESULT_CANCELED);

				} else {
					sendResult(RESULT_CANCELED);
				}
			} else {
				sendResult(RESULT_CANCELED);
			}
			break;
		case 123:

			if (resultCode == RESULT_CANCELED) {

				if (isGPSON()) {
					requestGPS();
				} else {

					confirmGPS();
				}

			}

			break;

		}

	}

	class CompressImageTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... path) {
			// TODO Auto-generated method stub
			compressImage(path[0]);
			return "DONE";
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			sendResult(RESULT_OK);
		}

	}

	public boolean isImageExists(String path) {
		File file = new File(path);
		return file.exists();
	}

	public void sendResult(int resultCode) {
		Intent intent = new Intent();
		intent.putExtra("img_path", IMAGE_PATH);
		intent.putExtra("lattitude", lattide);
		intent.putExtra("longitude", longitude);
		setResult(resultCode, intent);
		finish();

	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		if (pd != null)
			pd.dismiss();
		lattide = location.getLatitude();
		longitude = location.getLongitude();

		locman.removeUpdates(this);
		locman = null;
		Toast.makeText(PhotoCaptureApi.this,
				"Location Found Successfully.\nPlease Capture Image Now",
				Toast.LENGTH_LONG).show();
		openCameraHere(REQUEST_IMAGE_CAPTURE);
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

	// compress
	final float MAX_WIDTH = 816.0f, MAX_HEIGHT = 612.0f;

	// 1024x768

	public boolean compressImage(String filePath) {

		// String filePath = getRealPathFromURI(imageUri);
		Bitmap scaledBitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();

		// by setting this field as true, the actual bitmap pixels are not
		// loaded in the memory. Just the bounds are loaded. If
		// you try the use the bitmap here, you will get null.
		options.inJustDecodeBounds = true;
		Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

		int actualHeight = options.outHeight;
		int actualWidth = options.outWidth;

		// max Height and width values of the compressed image is taken as
		// 816x612

		float maxHeight = MAX_HEIGHT;
		float maxWidth = MAX_WIDTH;
		float imgRatio = actualWidth / actualHeight;
		float maxRatio = maxWidth / maxHeight;

		// width and height values are set maintaining the aspect ratio of the
		// image

		if (actualHeight > maxHeight || actualWidth > maxWidth) {
			if (imgRatio < maxRatio) {
				imgRatio = maxHeight / actualHeight;
				actualWidth = (int) (imgRatio * actualWidth);
				actualHeight = (int) maxHeight;
			} else if (imgRatio > maxRatio) {
				imgRatio = maxWidth / actualWidth;
				actualHeight = (int) (imgRatio * actualHeight);
				actualWidth = (int) maxWidth;
			} else {
				actualHeight = (int) maxHeight;
				actualWidth = (int) maxWidth;

			}
		}

		// setting inSampleSize value allows to load a scaled down version of
		// the original image

		options.inSampleSize = calculateInSampleSize(options, actualWidth,
				actualHeight);

		// inJustDecodeBounds set to false to load the actual bitmap
		options.inJustDecodeBounds = false;

		// this options allow android to claim the bitmap memory if it runs low
		// on memory
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inTempStorage = new byte[16 * 1024];

		try {
			// load the bitmap from its path
			bmp = BitmapFactory.decodeFile(filePath, options);
		} catch (OutOfMemoryError exception) {
			exception.printStackTrace();

		}
		try {
			scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight,
					Bitmap.Config.ARGB_8888);
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
		canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2,
				middleY - bmp.getHeight() / 2, new Paint(
						Paint.FILTER_BITMAP_FLAG));

		// check the rotation of the image and display it properly
		ExifInterface exif;
		try {
			exif = new ExifInterface(filePath);

			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, 0);
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
			scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
					scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
					true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileOutputStream out = null;
		// String filename =
		// "/storage/sdcard0/NABARD/12-08-2014/20140812113841gggs.jpg";
		try {
			out = new FileOutputStream(filePath);

			// write the compressed bitmap at the destination specified by
			// filename.
			scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return false;

	}

	public int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		final float totalPixels = width * height;
		final float totalReqPixelsCap = reqWidth * reqHeight * 2;
		while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
			inSampleSize++;
		}

		return inSampleSize;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		// TODO Auto-generated method stub
		dialog.dismiss();
	}

}

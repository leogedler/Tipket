package com.tipket.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileHelper {

    public static int imageOrientation;

	public static final String TAG = FileHelper.class.getSimpleName();
	
	public static final int SHORT_SIDE_TARGET = 450;
	
	public static byte[] getByteArrayFromFile(Context context, Uri uri) {
		byte[] fileBytes = null;
        InputStream inStream = null;
        ByteArrayOutputStream outStream = null;


        if (uri.getScheme().equals("content")) {

        	try {
        		inStream = context.getContentResolver().openInputStream(uri);
        		outStream = new ByteArrayOutputStream();
            
        		byte[] bytesFromFile = new byte[1024*1024]; // buffer size (1 MB)
        		int bytesRead = inStream.read(bytesFromFile);
        		while (bytesRead != -1) {
        			outStream.write(bytesFromFile, 0, bytesRead);
        			bytesRead = inStream.read(bytesFromFile);
        		}
            
        		fileBytes = outStream.toByteArray();
        	}
	        catch (IOException e) {
	        	Log.e(TAG, e.getMessage());
	        }
	        finally {
	        	try {
	        		inStream.close();
	        		outStream.close();
	        	}
	        	catch (IOException e) { /*( Intentionally blank */  }
	        }
        }


        	try {
	        	File file = new File(uri.getPath());


	        	FileInputStream fileInput = new FileInputStream(file);
	        	fileBytes = IOUtils.toByteArray(fileInput);
        	}
        	catch (IOException e) {
        		Log.e(TAG, e.getMessage());
        	}

        return fileBytes;
	}


	public static byte[] reduceImageForUpload(byte[] imageData) {

		Bitmap bitmap = ImageResizer.resizeImageMaintainAspectRatio(imageData, SHORT_SIDE_TARGET);

            Matrix matrix = new Matrix();

        if (imageOrientation == 6) {
            matrix.postRotate(90);
        }

        else if (imageOrientation == 3) {
            matrix.postRotate(180);
        }

        else if (imageOrientation == 8) {
            matrix.postRotate(270);
        }

        else if (imageOrientation == 0){
            matrix.postRotate(0);
        }

            Bitmap bMapRotate = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bMapRotate.compress(Bitmap.CompressFormat.PNG, 100, outputStream);


		byte[] reducedData = outputStream.toByteArray();
		try {
			outputStream.close();
		}
		catch (IOException e) {
			// Intentionally blank
		}
		
		return reducedData;
	}


	public static String getFileName(Context context, Uri uri, String fileType) {
		String fileName = "uploaded_file.";
		
		if (fileType.equals(ParseConstants.TYPE_IMAGE)) {
			fileName += "png";
		}
		else {
			// For video, we want to get the actual file extension
			if (uri.getScheme().equals("content")) {
				// do it using the mime type
				String mimeType = context.getContentResolver().getType(uri);
				int slashIndex = mimeType.indexOf("/");
				String fileExtension = mimeType.substring(slashIndex + 1);
				fileName += fileExtension;
			}
			else {
				fileName = uri.getLastPathSegment();
			}
		}
		
		return fileName;
	}

    // Public method for retrieve the image orientation
    public static int getOrientation(int orientation) {

        imageOrientation = orientation;

        String orientationNumber = String.valueOf(orientation);

        Log.d(TAG, "Picture Orientation:" + " " + orientationNumber);

        return orientation;
    }

}
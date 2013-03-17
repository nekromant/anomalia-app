package org.ncrmnt.serverctl;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;

public class CamWindow extends Activity {
	private static final String TAG = "csact";
	byte[] image;
	Camera camera;
	Context cntx;
	public void post_camera_image(int cam, Context ctx) {
		image=null;
		cntx=ctx;
		Log.d(TAG, "Opening camera");
		camera = Camera.open(cam);
		Camera.Parameters parameters = camera.getParameters();
		Log.d(TAG, "Setting params");
		parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
		parameters.setPictureSize(640, 480);
		parameters.setPictureFormat(ImageFormat.JPEG);
		camera.setParameters(parameters);
		SurfaceView mview = (SurfaceView) findViewById(R.id.surface);
		try {
			camera.setPreviewDisplay(mview.getHolder());
			camera.startPreview();
			Log.d(TAG, "taking picture");
			camera.takePicture(shutCallback, null, null, photoCallback);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, "fuck");
		}
		
		/*
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Log.e(TAG,"interrupted");
		}
		Log.d(TAG,"releasing camera");
		camera.release();
		*/
		return;
	}


	Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
		
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "Got a picture!");
			image=data;
			send_data();
		}
	};

	Camera.ShutterCallback shutCallback = new Camera.ShutterCallback() {
		public void onShutter() {
			Log.d(TAG, "Shutter!");	
		}
	};
	
	public void send_data()
	{
		Intent i = new Intent();
		i.setAction("org.ncrmnt.serverctl.CService");
		startService(i);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_window);
    }
    
    @Override
    protected void onResume() {
    	Log.d(TAG,"onResume();");
    	send_data();
    	post_camera_image(0,this.getApplicationContext());
        super.onResume();
    }
   
    @Override
    protected void onPause() {
    	Log.d(TAG,"onPause();");
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_cam_window, menu);
        return true;
    }
}

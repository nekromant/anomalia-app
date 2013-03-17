package org.ncrmnt.serverctl;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = "sctl";
	private static final String CHROOT = "/data/debian";
	private static final String BROADCAST_ACTION = "org.ncrmnt.serverctl.log";
	static boolean active = false;
	Camera camera;
	Context cntx;

	public void post_camera_image(int cam, Context ctx) {
		cntx = ctx;
		Log.d(TAG, "Opening camera");
		camera = Camera.open(cam);
		Camera.Parameters parameters = camera.getParameters();
		Log.d(TAG, "Setting params");
		parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
		//parameters.setPreviewSize(640, 480);
		parameters.setRotation(180);
		parameters.setPictureFormat(ImageFormat.JPEG);
		parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
		parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
		parameters.setSceneMode(Parameters.SCENE_MODE_ACTION);
		parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
		camera.setParameters(parameters);
		SurfaceView mview = (SurfaceView) findViewById(R.id.surfaceView1);
		try {
			camera.setPreviewDisplay(mview.getHolder());
			camera.startPreview();
			Thread.sleep(500); /* let the camera auto-adjust */
			Log.d(TAG, "taking picture");
			camera.autoFocus(pCallback);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, "fuck");
		}

		/*
		 * try { Thread.sleep(5000); } catch (InterruptedException e) {
		 * Log.e(TAG,"interrupted"); } Log.d(TAG,"releasing camera");
		 * camera.release();
		 */
		return;
	}

	Camera.AutoFocusCallback pCallback = new Camera.AutoFocusCallback() {
		
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			camera.takePicture(shutCallback, null, null, photoCallback);
		}
	};
	
	Camera.PictureCallback photoCallback = new Camera.PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "Got a picture - " + data.length + " bytes");
			send_data(data);
			camera.release();
		}
	};

	Camera.ShutterCallback shutCallback = new Camera.ShutterCallback() {
		public void onShutter() {
			Log.d(TAG, "Shutter!");
		}
	};

	public void send_data(byte[] data) {
		Intent i = new Intent();
		i.setAction("org.ncrmnt.serverctl.CService");
		i.putExtra("picture", data);
		startService(i);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context c, Intent arg) {
			Log.d(TAG, arg.getAction());
			if (arg.hasExtra("log")) {
				String l = arg.getStringExtra("log");
				push_text(l);
			} else if (arg.hasExtra("camid")) {
				int cam = arg.getIntExtra("camid", 0);
				Log.d(TAG, "Snapping a pic from cam " + cam);
				push_text("capturing from cam " + cam);
				post_camera_image(cam, c);
			}
		}
	};

	public void RunAsRoot(String cmd) {
		Process p;
		try {
			p = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit\n");
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void push_text(String line) {
		TextView t = (TextView) findViewById(R.id.log);
		if (line.equals("clean")) {
			Log.d(TAG, "clean!");
			t.setText("");
			return;
		}
		t.append("\n" + line);
	}

	public void start_service() {
		Intent svc = new Intent();
		svc.setClassName("org.ncrmnt.serverctl",
				"org.ncrmnt.serverctl.CService");
		startService(svc);
		push_text("Starting camera service...");
	}

	public void stop_service() {
		Log.d(TAG, "Stopping service");
		Intent svc = new Intent();
		svc.setClassName("org.ncrmnt.serverctl",
				"org.ncrmnt.serverctl.CService");
		stopService(svc);
		push_text("Stopping camera service...");

	}

	public boolean is_chroot_mounted() {
		/* since android blocks off access to data - make it the hacky way! */
		Process p;
		try {
			p = Runtime.getRuntime().exec("su");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		DataOutputStream os = new DataOutputStream(p.getOutputStream());
		String cmd = "[ -f " + CHROOT + "/etc/hostname ] || exit 1";
		try {
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit 0\n");
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			int res = p.waitFor();
			if (res == 0)
				return true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		start_service();
		if (is_chroot_mounted()) {
			push_text("rootfs already mounted - skipping autostart");
		} else {
			push_text("autostarting...");
			//
			RunAsRoot("serverctl start");
		}
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume();");
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_ACTION);
		filter.addAction("org.ncrmnt.serverctl.capture");
		registerReceiver(receiver, filter);
		super.onResume();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}

	 @Override
     public void onStart() {
        super.onStart();
        active = true;
     } 

     @Override
     public void onStop() {
        super.onStop();
        active = false;
     }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Log.d(TAG, "onOptionsItemSelected()");

		switch (item.getItemId()) {
		case R.id.m_start:
			// start_service();
			RunAsRoot("serverctl start");
			return true;
		case R.id.m_stop:
			// stop_service();
			RunAsRoot("serverctl stop");
			return true;
		case R.id.m_term:
			// stop_service();
			RunAsRoot("serverctl terminate");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}

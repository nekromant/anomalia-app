package org.ncrmnt.serverctl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.ncrmnt.serverctl.CService.TCPServer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

public class CService extends Service {

	private static final String TAG = "csrv";
	private byte[] image;
	public class LocalBinder extends Binder {
		CService getService() {
			return CService.this;
		}
	}

	private final IBinder mBinder = (IBinder) new LocalBinder();

	private TCPServer srv = new TCPServer();
	private Thread t = new Thread(srv);
	boolean started = false;

	public void broadcast(String event, int camid) {
		Log.d(TAG, "BROADCAST: " + event);
		Intent i = new Intent();
		i.setAction(event);
		i.putExtra("camid", camid);
		getApplicationContext().sendBroadcast(i);
	}

	public class TCPServer implements Runnable {
		public int SERVERPORT = 8888;
		boolean running = true;
		public void run() {
			ServerSocket serverSocket = null;
			try {
				Log.d(TAG, "Server is up");
				serverSocket = new ServerSocket(8888);
				serverSocket.setSoTimeout(1000);
				Log.d(TAG, "Entering main loop");
			} catch (Exception e) {
				Log.e(TAG, "Got exception: ", e);
			}
			while (running) {
				Socket client = null;
				try {
					client = serverSocket.accept();
				} catch (Exception e) {
					// Timeout on accept
				}
				if (client == null)
					continue;

				try {
					Log.d(TAG, "Incoming connection.");
					BufferedReader in = new BufferedReader(
							new InputStreamReader(client.getInputStream()));
					/* Now, let's handle client */
					OutputStream ow = client.getOutputStream();
					String rq = in.readLine();
					Log.d(TAG, "rq " + rq);
					String[] req = rq.split("\\s+");
					if (!req[0].equalsIgnoreCase("GET")) {
						Log.e(TAG, "Not a GET request: '" + req[0] + "'");
						continue;
					}
					/*
					 * HTTP/1.x 200 OK Content-Type: application/x-iso9660
					 * Content-Length: 529784832
					 */

					/* Now, we need to get content length */
					Log.d(TAG, "We want: " + req[1]);

					if (req[1].equals("/back.jpg")) {
						/* Wait for a broadcast here */
						broadcast("org.ncrmnt.serverctl.capture", 0);
					} else if (req[1].equals("/front.jpg")) {
						broadcast("org.ncrmnt.serverctl.capture", 1);
					}

					/* Now, wait for data */
					boolean have_image = false;
					
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						have_image = true;
						Log.d(TAG, "Interrupted!");
						/* Interrupted == data arrived */
					}

						Log.d(TAG, "Sending image data");
						
						ow.write("HTTP/1.x 200 OK\r\n".getBytes());
						/* We need to disable caching if any */
						ow.write("Pragma: no-cache\r\n".getBytes());
						ow.write("Expires: Fri, 30 Oct 1998 14:19:41 GMT\r\n"
								.getBytes());
						ow.write("Cache-Control: no-cache, must-revalidate\r\n"
								.getBytes());

						ow.write("Content-Type: image/jpeg\r\n".getBytes());
						
						ow.write(("Content-Length: " + image.length + "\r\n\n")
								.getBytes());
						ow.write(image);

					ow.flush();
				} catch (Exception e) {
					Log.d(TAG, "Timeout on accept");
				} finally {
					try {
						
						client.close();
						Log.d(TAG, "Client connection closed");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		}

	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate();");
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy();");
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context c, Intent arg) {
			Log.d(TAG, arg.getAction());
			Log.d(TAG, "image data delivered!");
		}
	};

	@Override
	public void onStart(Intent intent, int startid) {
		Log.d(TAG, "onStart();");
		if (started) {
			Log.d(TAG, "already running");
			return;
		}
		Log.d(TAG, "Starting server thread...");
		t.start();
		// broadcast("org.ncrmnt.serverctl.capture");
		started = true;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCmd()");
		Log.d(TAG, "action: " + intent.getAction());
		if (intent.hasExtra("picture")) {
			Log.d(TAG, "Image data arrived");
			image=intent.getByteArrayExtra("picture");
			t.interrupt();
		}
		// if (intent.getAction().equals("org.ncrmnt.serverctl.CService.img")){
		//
		// }
		onStart(intent, startId);
		return START_STICKY;
	}

	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}

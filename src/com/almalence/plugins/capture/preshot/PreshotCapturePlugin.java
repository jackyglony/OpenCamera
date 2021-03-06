/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
 */

package com.almalence.plugins.capture.preshot;

import java.nio.ByteBuffer;
import java.util.Date;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.YuvImage;
/* <!-- +++
 import com.almalence.opencam_plus.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.ui.Switch.Switch;

/***
 * Implements back in time capture plugin
 * 
 * Starts capturing images immediately after start. Stops capturing when shutter
 * button pressed.
 ***/

public class PreshotCapturePlugin extends PluginCapture
{
	// preferences
	private static String		PreShotInterval;
	private static String		FPS;
	private static boolean		RefocusPreference;
	private static boolean		AutostartPreference;
	private static String		PauseBetweenShots;
	private int					preferenceFocusMode;

	private static boolean		isSlowMode			= false;

	private static boolean		isBuffering			= false;

	private static int			counter				= 0;
	private static final int	REFOCUS_INTERVAL	= 3;

	private Switch				modeSwitcher;

	private boolean				captureStarted		= false;

	public PreshotCapturePlugin()
	{
		super("com.almalence.plugins.preshotcapture", R.xml.preferences_capture_preshot,
				R.xml.preferences_capture_preshot, 0, null);
	}

	@Override
	public void onStart()
	{
		getPrefs();
	}

	@Override
	public void onResume()
	{
		preferenceFocusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).getInt(
				CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref,
				CameraParameters.AF_MODE_AUTO);
		MainScreen.getInstance().muteShutter(false);
		captureStarted = false;
	}

	@Override
	public void onPause()
	{
		StopBuffering();
		inCapture = false;
		PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit().putBoolean("UseFocus", true)
				.commit();
		PreferenceManager
				.getDefaultSharedPreferences(MainScreen.getMainContext())
				.edit()
				.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
						: MainScreen.sFrontFocusModePref, preferenceFocusMode).commit();

	}

	@Override
	public void onStop()
	{
		MainScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
	}

	@Override
	public void onDestroy()
	{
		PreShot.FreeBuffer();
	}

	@Override
	public void onGUICreate()
	{
		getPrefs();

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		modeSwitcher = (Switch) inflator.inflate(R.layout.plugin_capture_preshot_modeswitcher, null, false);

		modeSwitcher.setTextOn("Hi-Res");
		modeSwitcher.setTextOff("Hi-Speed");
		modeSwitcher.setChecked(isSlowMode);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				StopBuffering();
				inCapture = false;

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("modePrefPreShot", isChecked ? "1" : "0");
				editor.commit();

				Message msg = new Message();
				msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;
				MainScreen.getMessageHandler().sendMessage(msg);
			}
		});

		if (PluginManager.getInstance().getProcessingCounter() == 0)
			modeSwitcher.setEnabled(true);
		else
			modeSwitcher.setEnabled(false);

		MainScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).addView(this.modeSwitcher,
				params);

		this.modeSwitcher.setLayoutParams(params);
		this.modeSwitcher.requestLayout();

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).requestLayout();
	}

	private void getPrefs()
	{
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		RefocusPreference = prefs.getBoolean("refocusPrefPreShot", false);
		AutostartPreference = prefs.getBoolean("autostartPrefPreShot", false);
		PauseBetweenShots = prefs.getString("pauseBetweenShotsPrefPreShot", "500");
		PreShotInterval = prefs.getString("backInTimePrefPreShot", "5");

		if (1 == Integer.parseInt(prefs.getString("modePrefPreShot", "0")))
		{
			isSlowMode = true;
			FPS = "2";
		} else
		{
			FPS = prefs.getString("fpsPrefPreShot", "4");
			isSlowMode = false;
		}
	}

	@Override
	public void onCameraSetup()
	{
		if (AutostartPreference)
			if (PluginManager.getInstance().getProcessingCounter() == 0)
				StartBuffering();
	}

	@Override
	public void setupCameraParameters()
	{
		if (!isSlowMode)// fast mode
		{
			try
			{
				if (CameraController.isModeAvailable(CameraController.getInstance().getSupportedFocusModes(),
						CameraParameters.AF_MODE_CONTINUOUS_VIDEO))
				{
					CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
					PreferenceManager
							.getDefaultSharedPreferences(MainScreen.getMainContext())
							.edit()
							.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
									: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_CONTINUOUS_VIDEO)
							.commit();
				}
			} catch (Exception e)
			{
				Log.i("Preshot capture", "Exception fast:" + e.getMessage());
			}
		} else
		// slow mode
		{
			try
			{
				if (CameraController.isModeAvailable(CameraController.getInstance().getSupportedFocusModes(),
						CameraParameters.AF_MODE_CONTINUOUS_PICTURE))
				{
					CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
					PreferenceManager
							.getDefaultSharedPreferences(MainScreen.getMainContext())
							.edit()
							.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
									: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_CONTINUOUS_PICTURE)
							.commit();
				}
			} catch (Exception e)
			{
				Log.i("Preshot capture", "Exception slow:" + e.getMessage());

			}
		}

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
				PluginManager.MSG_FOCUS_CHANGED);
	}

	@Override
	public void onExportFinished()
	{
		if (modeSwitcher != null)
			modeSwitcher.setEnabled(true);
		if (AutostartPreference)
			StartBuffering();
	}

	@Override
	public void onShutterClick()
	{
		if (captureStarted || AutostartPreference)
		{
			if (0 == PreShot.GetImageCount())
			{
				Toast.makeText(MainScreen.getInstance(), "No images yet", Toast.LENGTH_SHORT).show();
				return;
			}
			captureStarted = false;
			StopBuffering();

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));
		} else
		{
			if (!AutostartPreference)
				modeSwitcher.setEnabled(false);
			captureStarted = true;
			StartBuffering();
		}
	}

	private static int	frmCnt		= 1;
	private static int	preview_fps	= 0;

	public static int	imW			= 0;
	public static int	imH			= 0;

	// starts buffering to native buffer
	void StartBuffering()
	{

		Date curDate = new Date();
		SessionID = curDate.getTime();

		MainScreen.getInstance().muteShutter(true);

		isBuffering = true;
		if (!isSlowMode)
		{
			MainScreen.getGUIManager().startContinuousCaptureIndication();
			preview_fps = CameraController.getInstance().getPreviewFrameRate();

			imW = MainScreen.getPreviewWidth();
			imH = MainScreen.getPreviewHeight();

			MainScreen.setSaveImageWidth(imW);
			MainScreen.setSaveImageHeight(imH);

			Log.i("Preshot capture", "StartBuffering trying to allocate!");

			int secondsAllocated = PreShot.AllocateBuffer(imW, imH, Integer.parseInt(FPS),
					Integer.parseInt(PreShotInterval), 0);
			if (secondsAllocated == 0)
			{
				Log.i("Preshot capture", "StartBuffering failed, can't allocate native buffer!");
				return;
			}
			PluginManager.getInstance().addToSharedMem("IsSlowMode" + SessionID, "false");
		} else
		{
			// full size code
			PreShot.FreeBuffer();
			imW = MainScreen.getImageWidth();
			imH = MainScreen.getImageHeight();

			MainScreen.setSaveImageWidth(imW);
			MainScreen.setSaveImageHeight(imH);

			int secondsAllocated = PreShot.AllocateBuffer(imW, imH, Integer.parseInt(FPS),
					Integer.parseInt(PreShotInterval), 1);
			if (secondsAllocated == 0)
			{
				Log.i("Preshot capture", "StartBuffering failed, can't allocate native buffer!");
				return;
			}
			PluginManager.getInstance().addToSharedMem("IsSlowMode" + SessionID, "true");

			StartCaptureSequence();
		}
	}

	void StopBuffering()
	{

		MainScreen.getGUIManager().stopCaptureIndication();

		MainScreen.getInstance().muteShutter(false);

		if (modeSwitcher != null)
			modeSwitcher.setEnabled(false);

		if (!isBuffering)
			return;
		else
			isBuffering = false;

		counter = 0;

		PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
				String.valueOf(PreShot.GetImageCount()));

		if (!isSlowMode)
		{
			frmCnt = 1;
		}
	}

	@Override
	public void onPreviewFrame(byte[] _data, Camera _camera)
	{
		if (isSlowMode || !isBuffering)
			return;

		if (0 == frmCnt % (preview_fps / Integer.parseInt(FPS)))
		{
			System.gc();

			if (frmCnt == 0)
				PluginManager.getInstance().addToSharedMem_ExifTagsFromCamera(SessionID);

			PreShot.InsertToBuffer(_data, MainScreen.getGUIManager().getDisplayOrientation());
		}
		frmCnt++;
	}

	@TargetApi(19)
	@Override
	public void onPreviewAvailable(Image im)
	{
		if (isSlowMode || !isBuffering)
			return;

		if (0 == frmCnt % (preview_fps / Integer.parseInt(FPS)))
		{
			System.gc();

			if (frmCnt == 0)
				PluginManager.getInstance().addToSharedMem_ExifTagsFromCamera(SessionID);

			ByteBuffer Y = im.getPlanes()[0].getBuffer();
			ByteBuffer U = im.getPlanes()[1].getBuffer();
			ByteBuffer V = im.getPlanes()[2].getBuffer();

			if ((!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect()))
			{
				Log.e("PreShotCapturePlugin", "Oops, YUV ByteBuffers isDirect failed");
				return;
			}

			int imageWidth = im.getWidth();
			int imageHeight = im.getHeight();
			// Note: android documentation guarantee that:
			// - Y pixel stride is always 1
			// - U and V strides are the same
			// So, passing all these parameters is a bit overkill
			int status = YuvImage.CreateYUVImage(Y, U, V, im.getPlanes()[0].getPixelStride(),
					im.getPlanes()[0].getRowStride(), im.getPlanes()[1].getPixelStride(),
					im.getPlanes()[1].getRowStride(), im.getPlanes()[2].getPixelStride(),
					im.getPlanes()[2].getRowStride(), imageWidth, imageHeight, 0);

			if (status != 0)
				Log.e("PreShotCapturePlugin", "Error while cropping: " + status);

			byte[] data = YuvImage.GetByteFrame(0);

			PreShot.InsertToBuffer(data, MainScreen.getGUIManager().getDisplayOrientation());
		}
		frmCnt++;
	}

	void StartCaptureSequence()
	{
		if (!inCapture)
		{
			inCapture = true;
			takingAlready = false;

			// start series
			try
			// some crappy models have autoFocus() = null
			{
				int focusMode = CameraController.getInstance().getFocusMode();
				if (!(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
						|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
						|| focusMode == CameraParameters.AF_MODE_INFINITY
						|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
						&& !MainScreen.getAutoFocusLock())
				{
					if (!CameraController.autoFocus())
					{
						this.CaptureFrame();
						takingAlready = true;
					}
				} else if (!takingAlready)
				{
					this.CaptureFrame();
					takingAlready = true;
				}
			} catch (NullPointerException e)
			{
				if (!takingAlready)
				{
					this.CaptureFrame();
					takingAlready = true;
				}
			}
		}
	}

	public void NotEnoughMemory()
	{
		Log.i("Preshot capture", "NotEnoughMemory!");
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		inCapture = false;

		if (0 == PreShot.GetImageCount())
			PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(paramArrayOfByte, SessionID, -1);

		PreShot.InsertToBuffer(paramArrayOfByte, MainScreen.getGUIManager().getDisplayOrientation());

		takingAlready = false;
		paramCamera.startPreview();

		try
		{
			if (isBuffering)
			{
				ProcessPauseBetweenShots();
			}
		} catch (RuntimeException e)
		{
			Log.i("Preshot capture", "StartPreview fail");
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
					PluginManager.MSG_NEXT_FRAME);
		}
	}

	@TargetApi(19)
	@Override
	public void onImageAvailable(Image im)
	{
		inCapture = false;

		ByteBuffer jpeg = im.getPlanes()[0].getBuffer();

		int frame_len = jpeg.limit();
		byte[] jpegByteArray = new byte[frame_len];
		jpeg.get(jpegByteArray, 0, frame_len);

		if (0 == PreShot.GetImageCount())
			PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(jpegByteArray, SessionID, -1);

		PreShot.InsertToBuffer(jpegByteArray, MainScreen.getGUIManager().getDisplayOrientation());

		takingAlready = false;
		CameraController.startCameraPreview();

		try
		{
			if (isBuffering)
			{
				ProcessPauseBetweenShots();
			}
		} catch (RuntimeException e)
		{
			Log.i("Preshot capture", "StartPreview fail");
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
					PluginManager.MSG_NEXT_FRAME);
		}
	}

	void ProcessPauseBetweenShots()
	{
		int interval = Integer.parseInt(PauseBetweenShots);
		if (interval == 0)
		{
			afterPause();
			return;
		}

		new CountDownTimer(interval, interval)
		{
			public void onFinish()
			{
				afterPause();
			}

			@Override
			public void onTick(long millisUntilFinished)
			{
			}
		}.start();
	}

	void afterPause()
	{
		if (isBuffering)
		{
			int focusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).getInt(
					CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref,
					-1);
			if (RefocusPreference
					|| (counter >= REFOCUS_INTERVAL)
					&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
							|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
							|| focusMode == CameraParameters.AF_MODE_INFINITY
							|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
					&& !MainScreen.getAutoFocusLock())
			{
				Log.v("", "1");
				counter = 0;
				if (!CameraController.autoFocus())
					CaptureFrame();
			} else
			{
				Log.v("", "2");
				CaptureFrame();
			}
		}
	}

	public void CaptureFrame()
	{
		// only requesting exposure change here
		try
		{
			if (isBuffering)
			{
				if (CameraController.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
					return;
				inCapture = true;

				MainScreen.getGUIManager().showCaptureIndication();
				MainScreen.getInstance().playShutter();

				requestID = CameraController.captureImage(1, CameraController.JPEG);
				counter++;
			}
		} catch (RuntimeException e)
		{
			Log.i("Preshot capture", "takePicture fail in CaptureFrame (called after release?)" + e.getMessage());
			CameraController.startCameraPreview();
		}
	}

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		// on motorola xt5 cm7 this function is called twice!
		// on motorola droid's onAutoFocus seem to be called at every
		// startPreview,
		// causing additional frame(s) taken after sequence is finished
		if (!takingAlready && isSlowMode)
		{
			CaptureFrame();
			takingAlready = true;
		}
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			try
			{
				CameraController.startCameraPreview();
				CaptureFrame();
			} catch (RuntimeException e)
			{
				Log.i("CameraTest", "RuntimeException in MSG_NEXT_FRAME");
				PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
						PluginManager.MSG_NEXT_FRAME);
			}
			return true;
		} else if (arg1 == PluginManager.MSG_STOP_CAPTURE)
		{
			StopBuffering();
			return true;
		} else if (arg1 == PluginManager.MSG_START_CAPTURE)
		{
			if (PluginManager.getInstance().getProcessingCounter() == 0)
				StartBuffering();
			return true;
		}
		return false;
	}
}

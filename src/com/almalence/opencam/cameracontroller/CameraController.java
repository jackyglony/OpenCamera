/*
	CameraController for OpenCamera project - interface to camera device
    Copyright (C) 2014  Almalence Inc.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam.cameracontroller;

//-+- -->

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginManagerInterface;
import com.almalence.opencam.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback,
		Camera.PreviewCallback, Camera.ShutterCallback
{
	private static final String						TAG								= "CameraController";

	public static final int							YUV								= 1;
	public static final int							JPEG							= 0;
	// Android camera parameters constants
	private static String							sceneAuto;
	private static String							sceneAction;
	private static String							scenePortrait;
	private static String							sceneLandscape;
	private static String							sceneNight;
	private static String							sceneNightPortrait;
	private static String							sceneTheatre;
	private static String							sceneBeach;
	private static String							sceneSnow;
	private static String							sceneSunset;
	private static String							sceneSteadyPhoto;
	private static String							sceneFireworks;
	private static String							sceneSports;
	private static String							sceneParty;
	private static String							sceneCandlelight;
	private static String							sceneBarcode;
	private static String							sceneHDR;
	private static String							sceneAR;

	private static String							wbAuto;
	private static String							wbIncandescent;
	private static String							wbFluorescent;
	private static String							wbWarmFluorescent;
	private static String							wbDaylight;
	private static String							wbCloudyDaylight;
	private static String							wbTwilight;
	private static String							wbShade;

	private static String							focusAuto;
	private static String							focusInfinity;
	private static String							focusNormal;
	private static String							focusMacro;
	private static String							focusFixed;
	private static String							focusEdof;
	private static String							focusContinuousVideo;
	private static String							focusContinuousPicture;
	private static String							focusAfLock;

	private static String							flashAuto;
	private static String							flashOn;
	private static String							flashOff;
	private static String							flashRedEye;
	private static String							flashTorch;

	private static String							isoAuto;
	private static String							iso50;
	private static String							iso100;
	private static String							iso200;
	private static String							iso400;
	private static String							iso800;
	private static String							iso1600;
	private static String							iso3200;

	private static String							isoAuto_2;
	private static String							iso50_2;
	private static String							iso100_2;
	private static String							iso200_2;
	private static String							iso400_2;
	private static String							iso800_2;
	private static String							iso1600_2;
	private static String							iso3200_2;

	private static String							meteringAuto;
	private static String							meteringMatrix;
	private static String							meteringCenter;
	private static String							meteringSpot;

	// List of localized names for camera parameters values
	private static Map<Integer, String>				mode_scene;
	private static Map<String, Integer>				key_scene;

	private static Map<Integer, String>				mode_wb;
	private static Map<String, Integer>				key_wb;

	private static Map<Integer, String>				mode_focus;
	private static Map<String, Integer>				key_focus;

	private static Map<Integer, String>				mode_flash;
	private static Map<String, Integer>				key_flash;

	private static List<Integer>					iso_values;
	private static List<String>						iso_default;
	private static Map<String, String>				iso_default_values;
	private static Map<Integer, String>				mode_iso;
	private static Map<Integer, String>				mode_iso2;
	private static Map<Integer, Integer>			mode_iso_HALv3;
	private static Map<String, Integer>				key_iso;
	private static Map<String, Integer>				key_iso2;

	private static CameraController					cameraController				= null;

	private PluginManagerInterface					pluginManager					= null;
	private ApplicationInterface					appInterface					= null;
	protected Context								mainContext						= null;

	// Old camera interface
	private static Camera							camera							= null;
	private byte[]									pviewBuffer;

	private static boolean							isHALv3							= false;
	private static boolean							isHALv3Supported				= false;

	protected String[]								cameraIdList					= { "" };

	// Flags to know which camera feature supported at current device
	private boolean									mEVSupported					= false;
	private boolean									mSceneModeSupported				= false;
	private boolean									mWBSupported					= false;
	private boolean									mFocusModeSupported				= false;
	private boolean									mFlashModeSupported				= false;
	private boolean									mISOSupported					= false;

	private int										minExpoCompensation				= 0;
	private int										maxExpoCompensation				= 0;
	private float									expoCompensationStep			= 0;

	protected boolean								mVideoStabilizationSupported	= false;

	private static byte[]							supportedSceneModes;
	private static byte[]							supportedWBModes;
	private static byte[]							supportedFocusModes;
	private static byte[]							supportedFlashModes;
	private static byte[]							supportedISOModes;

	private static int								maxRegionsSupported;

	protected static int							CameraIndex						= 0;
	protected static boolean						CameraMirrored					= false;

	// Image size index for capturing
	private static int								CapIdx;

	public static final int							MIN_MPIX_SUPPORTED				= 1280 * 960;

	// Lists of resolutions, their indexes and names (for capturing and preview)
	protected static List<Long>						ResolutionsMPixList;
	protected static List<CameraController.Size>	ResolutionsSizeList;
	protected static List<String>					ResolutionsIdxesList;
	protected static List<String>					ResolutionsNamesList;

	protected static List<Long>						ResolutionsMPixListIC;
	protected static List<String>					ResolutionsIdxesListIC;
	protected static List<String>					ResolutionsNamesListIC;

	protected static List<Long>						ResolutionsMPixListVF;
	protected static List<String>					ResolutionsIdxesListVF;
	protected static List<String>					ResolutionsNamesListVF;

	protected static final CharSequence[]			RATIO_STRINGS					= { " ", "4:3", "3:2", "16:9",
			"1:1"																	};

	// States of focus and capture
	public static final int							FOCUS_STATE_IDLE				= 0;
	public static final int							FOCUS_STATE_FOCUSED				= 1;
	public static final int							FOCUS_STATE_FAIL				= 3;
	public static final int							FOCUS_STATE_FOCUSING			= 4;

	public static final int							CAPTURE_STATE_IDLE				= 0;
	public static final int							CAPTURE_STATE_CAPTURING			= 1;

	private static int								mFocusState						= FOCUS_STATE_IDLE;
	private static int								mCaptureState					= CAPTURE_STATE_IDLE;

	protected static int							iCaptureID						= -1;
	protected static Surface						mPreviewSurface					= null;

	private static final Object						SYNC_OBJECT						= new Object();

	// Singleton access function
	public static CameraController getInstance()
	{
		if (cameraController == null)
		{
			cameraController = new CameraController();
		}
		return cameraController;
	}

	private CameraController()
	{

	}

	public void onCreate(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase)
	{
		pluginManager = pluginManagerBase;
		appInterface = app;
		mainContext = context;

		sceneAuto = mainContext.getResources().getString(R.string.sceneAutoSystem);
		sceneAction = mainContext.getResources().getString(R.string.sceneActionSystem);
		scenePortrait = mainContext.getResources().getString(R.string.scenePortraitSystem);
		sceneLandscape = mainContext.getResources().getString(R.string.sceneLandscapeSystem);
		sceneNight = mainContext.getResources().getString(R.string.sceneNightSystem);
		sceneNightPortrait = mainContext.getResources().getString(R.string.sceneNightPortraitSystem);
		sceneTheatre = mainContext.getResources().getString(R.string.sceneTheatreSystem);
		sceneBeach = mainContext.getResources().getString(R.string.sceneBeachSystem);
		sceneSnow = mainContext.getResources().getString(R.string.sceneSnowSystem);
		sceneSunset = mainContext.getResources().getString(R.string.sceneSunsetSystem);
		sceneSteadyPhoto = mainContext.getResources().getString(R.string.sceneSteadyPhotoSystem);
		sceneFireworks = mainContext.getResources().getString(R.string.sceneFireworksSystem);
		sceneSports = mainContext.getResources().getString(R.string.sceneSportsSystem);
		sceneParty = mainContext.getResources().getString(R.string.scenePartySystem);
		sceneCandlelight = mainContext.getResources().getString(R.string.sceneCandlelightSystem);
		sceneBarcode = mainContext.getResources().getString(R.string.sceneBarcodeSystem);
		sceneHDR = mainContext.getResources().getString(R.string.sceneHDRSystem);
		sceneAR = mainContext.getResources().getString(R.string.sceneARSystem);

		wbAuto = mainContext.getResources().getString(R.string.wbAutoSystem);
		wbIncandescent = mainContext.getResources().getString(R.string.wbIncandescentSystem);
		wbFluorescent = mainContext.getResources().getString(R.string.wbFluorescentSystem);
		wbWarmFluorescent = mainContext.getResources().getString(R.string.wbWarmFluorescentSystem);
		wbDaylight = mainContext.getResources().getString(R.string.wbDaylightSystem);
		wbCloudyDaylight = mainContext.getResources().getString(R.string.wbCloudyDaylightSystem);
		wbTwilight = mainContext.getResources().getString(R.string.wbTwilightSystem);
		wbShade = mainContext.getResources().getString(R.string.wbShadeSystem);

		focusAuto = mainContext.getResources().getString(R.string.focusAutoSystem);
		focusInfinity = mainContext.getResources().getString(R.string.focusInfinitySystem);
		focusNormal = mainContext.getResources().getString(R.string.focusNormalSystem);
		focusMacro = mainContext.getResources().getString(R.string.focusMacroSystem);
		focusFixed = mainContext.getResources().getString(R.string.focusFixedSystem);
		focusEdof = mainContext.getResources().getString(R.string.focusEdofSystem);
		focusContinuousVideo = mainContext.getResources().getString(R.string.focusContinuousVideoSystem);
		focusContinuousPicture = mainContext.getResources().getString(R.string.focusContinuousPictureSystem);
		focusAfLock = mainContext.getResources().getString(R.string.focusAfLockSystem);

		flashAuto = mainContext.getResources().getString(R.string.flashAutoSystem);
		flashOn = mainContext.getResources().getString(R.string.flashOnSystem);
		flashOff = mainContext.getResources().getString(R.string.flashOffSystem);
		flashRedEye = mainContext.getResources().getString(R.string.flashRedEyeSystem);
		flashTorch = mainContext.getResources().getString(R.string.flashTorchSystem);

		isoAuto = mainContext.getResources().getString(R.string.isoAutoSystem);
		iso50 = mainContext.getResources().getString(R.string.iso50System);
		iso100 = mainContext.getResources().getString(R.string.iso100System);
		iso200 = mainContext.getResources().getString(R.string.iso200System);
		iso400 = mainContext.getResources().getString(R.string.iso400System);
		iso800 = mainContext.getResources().getString(R.string.iso800System);
		iso1600 = mainContext.getResources().getString(R.string.iso1600System);
		iso3200 = mainContext.getResources().getString(R.string.iso3200System);

		isoAuto_2 = mainContext.getResources().getString(R.string.isoAutoDefaultSystem);
		iso50_2 = mainContext.getResources().getString(R.string.iso50DefaultSystem);
		iso100_2 = mainContext.getResources().getString(R.string.iso100DefaultSystem);
		iso200_2 = mainContext.getResources().getString(R.string.iso200DefaultSystem);
		iso400_2 = mainContext.getResources().getString(R.string.iso400DefaultSystem);
		iso800_2 = mainContext.getResources().getString(R.string.iso800DefaultSystem);
		iso1600_2 = mainContext.getResources().getString(R.string.iso1600DefaultSystem);
		iso3200_2 = mainContext.getResources().getString(R.string.iso3200DefaultSystem);

		meteringAuto = mainContext.getResources().getString(R.string.meteringAutoSystem);
		meteringMatrix = mainContext.getResources().getString(R.string.meteringMatrixSystem);
		meteringCenter = mainContext.getResources().getString(R.string.meteringCenterSystem);
		meteringSpot = mainContext.getResources().getString(R.string.meteringSpotSystem);

		// List of localized names for camera parameters values
		mode_scene = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.SCENE_MODE_AUTO, sceneAuto);
				put(CameraParameters.SCENE_MODE_ACTION, sceneAction);
				put(CameraParameters.SCENE_MODE_PORTRAIT, scenePortrait);
				put(CameraParameters.SCENE_MODE_LANDSCAPE, sceneLandscape);
				put(CameraParameters.SCENE_MODE_NIGHT, sceneNight);
				put(CameraParameters.SCENE_MODE_NIGHT_PORTRAIT, sceneNightPortrait);
				put(CameraParameters.SCENE_MODE_THEATRE, sceneTheatre);
				put(CameraParameters.SCENE_MODE_BEACH, sceneBeach);
				put(CameraParameters.SCENE_MODE_SNOW, sceneSnow);
				put(CameraParameters.SCENE_MODE_SUNSET, sceneSunset);
				put(CameraParameters.SCENE_MODE_STEADYPHOTO, sceneSteadyPhoto);
				put(CameraParameters.SCENE_MODE_FIREWORKS, sceneFireworks);
				put(CameraParameters.SCENE_MODE_SPORTS, sceneSports);
				put(CameraParameters.SCENE_MODE_PARTY, sceneParty);
				put(CameraParameters.SCENE_MODE_CANDLELIGHT, sceneCandlelight);
				put(CameraParameters.SCENE_MODE_BARCODE, sceneBarcode);
			}
		};

		key_scene = new HashMap<String, Integer>()
		{
			{
				put(sceneAuto, CameraParameters.SCENE_MODE_AUTO);
				put(sceneAction, CameraParameters.SCENE_MODE_ACTION);
				put(scenePortrait, CameraParameters.SCENE_MODE_PORTRAIT);
				put(sceneLandscape, CameraParameters.SCENE_MODE_LANDSCAPE);
				put(sceneNight, CameraParameters.SCENE_MODE_NIGHT);
				put(sceneNightPortrait, CameraParameters.SCENE_MODE_NIGHT_PORTRAIT);
				put(sceneTheatre, CameraParameters.SCENE_MODE_THEATRE);
				put(sceneBeach, CameraParameters.SCENE_MODE_BEACH);
				put(sceneSnow, CameraParameters.SCENE_MODE_SNOW);
				put(sceneSunset, CameraParameters.SCENE_MODE_SUNSET);
				put(sceneSteadyPhoto, CameraParameters.SCENE_MODE_STEADYPHOTO);
				put(sceneFireworks, CameraParameters.SCENE_MODE_FIREWORKS);
				put(sceneSports, CameraParameters.SCENE_MODE_SPORTS);
				put(sceneParty, CameraParameters.SCENE_MODE_PARTY);
				put(sceneCandlelight, CameraParameters.SCENE_MODE_CANDLELIGHT);
				put(sceneBarcode, CameraParameters.SCENE_MODE_BARCODE);
			}
		};

		mode_wb = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.WB_MODE_AUTO, wbAuto);
				put(CameraParameters.WB_MODE_INCANDESCENT, wbIncandescent);
				put(CameraParameters.WB_MODE_FLUORESCENT, wbFluorescent);
				put(CameraParameters.WB_MODE_WARM_FLUORESCENT, wbWarmFluorescent);
				put(CameraParameters.WB_MODE_DAYLIGHT, wbDaylight);
				put(CameraParameters.WB_MODE_CLOUDY_DAYLIGHT, wbCloudyDaylight);
				put(CameraParameters.WB_MODE_TWILIGHT, wbTwilight);
				put(CameraParameters.WB_MODE_SHADE, wbShade);
			}
		};

		key_wb = new HashMap<String, Integer>()
		{
			{
				put(wbAuto, CameraParameters.WB_MODE_AUTO);
				put(wbIncandescent, CameraParameters.WB_MODE_INCANDESCENT);
				put(wbFluorescent, CameraParameters.WB_MODE_FLUORESCENT);
				put(wbWarmFluorescent, CameraParameters.WB_MODE_WARM_FLUORESCENT);
				put(wbDaylight, CameraParameters.WB_MODE_DAYLIGHT);
				put(wbCloudyDaylight, CameraParameters.WB_MODE_CLOUDY_DAYLIGHT);
				put(wbTwilight, CameraParameters.WB_MODE_TWILIGHT);
				put(wbShade, CameraParameters.WB_MODE_SHADE);
			}
		};

		mode_focus = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.AF_MODE_AUTO, focusAuto);
				put(CameraParameters.AF_MODE_INFINITY, focusInfinity);
				put(CameraParameters.AF_MODE_NORMAL, focusNormal);
				put(CameraParameters.AF_MODE_MACRO, focusMacro);
				put(CameraParameters.AF_MODE_FIXED, focusFixed);
				put(CameraParameters.AF_MODE_EDOF, focusEdof);
				put(CameraParameters.AF_MODE_CONTINUOUS_VIDEO, focusContinuousVideo);
				put(CameraParameters.AF_MODE_CONTINUOUS_PICTURE, focusContinuousPicture);
			}
		};

		key_focus = new HashMap<String, Integer>()
		{
			{
				put(focusAuto, CameraParameters.AF_MODE_AUTO);
				put(focusInfinity, CameraParameters.AF_MODE_INFINITY);
				put(focusNormal, CameraParameters.AF_MODE_NORMAL);
				put(focusMacro, CameraParameters.AF_MODE_MACRO);
				put(focusFixed, CameraParameters.AF_MODE_FIXED);
				put(focusEdof, CameraParameters.AF_MODE_EDOF);
				put(focusContinuousVideo, CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
				put(focusContinuousPicture, CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
			}
		};

		mode_flash = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.FLASH_MODE_OFF, flashOff);
				put(CameraParameters.FLASH_MODE_AUTO, flashAuto);
				put(CameraParameters.FLASH_MODE_SINGLE, flashOn);
				put(CameraParameters.FLASH_MODE_REDEYE, flashRedEye);
				put(CameraParameters.FLASH_MODE_TORCH, flashTorch);
			}
		};

		key_flash = new HashMap<String, Integer>()
		{
			{
				put(flashOff, CameraParameters.FLASH_MODE_OFF);
				put(flashAuto, CameraParameters.FLASH_MODE_AUTO);
				put(flashOn, CameraParameters.FLASH_MODE_SINGLE);
				put(flashRedEye, CameraParameters.FLASH_MODE_REDEYE);
				put(flashTorch, CameraParameters.FLASH_MODE_TORCH);
			}
		};

		iso_values = new ArrayList<Integer>()
		{
			{
				add(CameraParameters.ISO_AUTO);
				add(CameraParameters.ISO_50);
				add(CameraParameters.ISO_100);
				add(CameraParameters.ISO_200);
				add(CameraParameters.ISO_400);
				add(CameraParameters.ISO_800);
				add(CameraParameters.ISO_1600);
				add(CameraParameters.ISO_3200);
			}
		};

		iso_default = new ArrayList<String>()
		{
			{
				add(isoAuto);
				add(iso100);
				add(iso200);
				add(iso400);
				add(iso800);
				add(iso1600);
			}
		};

		iso_default_values = new HashMap<String, String>()
		{
			{
				put(isoAuto, mainContext.getResources().getString(R.string.isoAutoDefaultSystem));
				put(iso100, mainContext.getResources().getString(R.string.iso100DefaultSystem));
				put(iso200, mainContext.getResources().getString(R.string.iso200DefaultSystem));
				put(iso400, mainContext.getResources().getString(R.string.iso400DefaultSystem));
				put(iso800, mainContext.getResources().getString(R.string.iso800DefaultSystem));
				put(iso1600, mainContext.getResources().getString(R.string.iso1600DefaultSystem));
			}
		};

		mode_iso = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.ISO_AUTO, isoAuto);
				put(CameraParameters.ISO_50, iso50);
				put(CameraParameters.ISO_100, iso100);
				put(CameraParameters.ISO_200, iso200);
				put(CameraParameters.ISO_400, iso400);
				put(CameraParameters.ISO_800, iso800);
				put(CameraParameters.ISO_1600, iso1600);
				put(CameraParameters.ISO_3200, iso3200);
			}
		};

		mode_iso2 = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.ISO_AUTO, isoAuto_2);
				put(CameraParameters.ISO_50, iso50_2);
				put(CameraParameters.ISO_100, iso100_2);
				put(CameraParameters.ISO_200, iso200_2);
				put(CameraParameters.ISO_400, iso400_2);
				put(CameraParameters.ISO_800, iso800_2);
				put(CameraParameters.ISO_1600, iso1600_2);
				put(CameraParameters.ISO_3200, iso3200_2);
			}
		};

		mode_iso_HALv3 = new HashMap<Integer, Integer>()
		{
			{
				put(CameraParameters.ISO_AUTO, 1);
				put(CameraParameters.ISO_50, 50);
				put(CameraParameters.ISO_100, 100);
				put(CameraParameters.ISO_200, 200);
				put(CameraParameters.ISO_400, 400);
				put(CameraParameters.ISO_800, 800);
				put(CameraParameters.ISO_1600, 1600);
				put(CameraParameters.ISO_3200, 3200);
			}
		};

		key_iso = new HashMap<String, Integer>()
		{
			{
				put(isoAuto, CameraParameters.ISO_AUTO);
				put(iso50, CameraParameters.ISO_50);
				put(iso100, CameraParameters.ISO_100);
				put(iso200, CameraParameters.ISO_200);
				put(iso400, CameraParameters.ISO_400);
				put(iso800, CameraParameters.ISO_800);
				put(iso1600, CameraParameters.ISO_1600);
				put(iso3200, CameraParameters.ISO_3200);
			}
		};

		key_iso2 = new HashMap<String, Integer>()
		{
			{
				put(isoAuto_2, CameraParameters.ISO_AUTO);
				put(iso50_2, CameraParameters.ISO_50);
				put(iso100_2, CameraParameters.ISO_100);
				put(iso200_2, CameraParameters.ISO_200);
				put(iso400_2, CameraParameters.ISO_400);
				put(iso800_2, CameraParameters.ISO_800);
				put(iso1600_2, CameraParameters.ISO_1600);
				put(iso3200_2, CameraParameters.ISO_3200);
			}
		};

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);

		isHALv3 = prefs.getBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false);
		if (null == mainContext.getSystemService("camera"))
		{
			isHALv3 = false;
			isHALv3Supported = false;
			prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false)
					.commit();
		} else
			isHALv3Supported = true;

		if (CameraController.isHALv3Supported)
			HALv3.onCreateHALv3();
	}

	public void createHALv3Manager()
	{
		if (CameraController.isHALv3Supported)
			HALv3.onCreateHALv3();
	}

	public void onStart()
	{
		// Does nothing yet
	}

	public void onResume()
	{
		// Does nothing yet
	}

	public void onPause()
	{
		// reset torch
		if (!CameraController.isHALv3)
		{
			try
			{
				Camera.Parameters p = cameraController.getCameraParameters();
				if (p != null && cameraController.isFlashModeSupported())
				{
					p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					cameraController.setCameraParameters(p);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			if (CameraController.camera != null)
			{
				CameraController.camera.setPreviewCallback(null);
				CameraController.camera.stopPreview();
				CameraController.camera.release();
				CameraController.camera = null;
			}
		} else
			HALv3.onPauseHALv3();
	}

	public void onStop()
	{
		// Does nothing yet
	}

	public void onDestroy()
	{
		// Does nothing yet
	}

	/* Get different list and maps of camera parameters */
	public static List<Integer> getIsoValuesList()
	{
		return iso_values;
	}

	public static List<String> getIsoDefaultList()
	{
		return iso_default;
	}

	public static Map<String, Integer> getIsoKey()
	{
		return key_iso;
	}

	public static Map<Integer, Integer> getIsoModeHALv3()
	{
		return mode_iso_HALv3;
	}

	/* ^^^ Get different list and maps of camera parameters */

	public void setPreviewSurface(Surface srf)
	{
		mPreviewSurface = srf;
	}

	/* Preview buffer methods */
	public void allocatePreviewBuffer(int size)
	{
		pviewBuffer = new byte[size];
	}

	public byte[] getPreviewBuffer()
	{
		return pviewBuffer;
	}

	/* ^^^ Preview buffer methods */

	public static void useHALv3(boolean useHALv3)
	{
		isHALv3 = useHALv3;
	}

	public static boolean isUseHALv3()
	{
		return isHALv3;
	}

	public static boolean isHALv3Supported()
	{
		return isHALv3Supported;
	}

	public void setupCamera(SurfaceHolder holder)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera == null)
			{
				try
				{
					if (Camera.getNumberOfCameras() > 0)
						camera = Camera.open(CameraController.CameraIndex);
					else
						camera = Camera.open();

					Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
					Camera.getCameraInfo(CameraIndex, cameraInfo);
					if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
						CameraMirrored = true;
					else
						CameraMirrored = false;

				} catch (RuntimeException e)
				{
					CameraController.camera = null;
				}

				if (CameraController.camera == null)
				{
					Toast.makeText(mainContext, "Unable to start camera", Toast.LENGTH_LONG).show();
					return;
				}
			}

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				cameraController.mVideoStabilizationSupported = getVideoStabilizationSupported();

			// screen rotation
			if (!pluginManager.shouldPreviewToGPU())
			{
				try
				{
					camera.setDisplayOrientation(90);
				} catch (RuntimeException e)
				{
					e.printStackTrace();
				}

				try
				{
					camera.setPreviewDisplay(holder);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.openCameraHALv3();

		pluginManager.selectDefaults();

		if (!CameraController.isHALv3)
		{
			// screen rotation
			try
			{
				CameraController.camera.setDisplayOrientation(90);
			} catch (RuntimeException e)
			{
				e.printStackTrace();
			}

			try
			{
				CameraController.camera.setPreviewDisplay(holder);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		if (CameraController.isHALv3)
			HALv3.populateCameraDimensionsHALv3();
		else
			populateCameraDimensions();

		CameraController.ResolutionsMPixListIC = CameraController.ResolutionsMPixList;
		CameraController.ResolutionsIdxesListIC = CameraController.ResolutionsIdxesList;
		CameraController.ResolutionsNamesListIC = CameraController.ResolutionsNamesList;

		pluginManager.selectImageDimension(); // updates SX, SY values

		if (CameraController.isHALv3)
			HALv3.setupImageReadersHALv3();

		if (!CameraController.isHALv3)
		{
			Message msg = new Message();
			msg.what = PluginManager.MSG_CAMERA_READY;
			MainScreen.getMessageHandler().sendMessage(msg);
		}
	}

	protected void openCameraFrontOrRear()
	{
		if (Camera.getNumberOfCameras() > 0)
		{
			CameraController.camera = Camera.open(CameraController.CameraIndex);
		}

		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(CameraController.CameraIndex, cameraInfo);

		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			CameraController.CameraMirrored = true;
		else
			CameraController.CameraMirrored = false;
	}

	public static boolean isCameraCreated()
	{
		if (!CameraController.isHALv3)
			return camera != null;
		else
			return isCameraCreatedHALv3();

	}

	@TargetApi(19)
	public static boolean isCameraCreatedHALv3()
	{
		return HALv3.getInstance().camDevice != null;
	}

	public void populateCameraDimensions()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();

		int minMPIX = CameraController.MIN_MPIX_SUPPORTED;
		Camera.Parameters cp = getCameraParameters();
		List<Camera.Size> cs = cp.getSupportedPictureSizes();

		if (cs == null)
			return;

		int iHighestIndex = 0;
		Camera.Size sHighest = cs.get(0);

		for (int ii = 0; ii < cs.size(); ++ii)
		{
			Camera.Size s = cs.get(ii);

			int currSizeWidth = s.width;
			int currSizeHeight = s.height;
			int highestSizeWidth = sHighest.width;
			int highestSizeHeight = sHighest.height;

			if ((long) currSizeWidth * currSizeHeight > (long) highestSizeWidth * highestSizeHeight)
			{
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) currSizeWidth * currSizeHeight < minMPIX)
				continue;

			fillResolutionsList(ii, currSizeWidth, currSizeHeight);
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			Camera.Size s = cs.get(iHighestIndex);

			int currSizeWidth = s.width;
			int currSizeHeight = s.height;

			fillResolutionsList(0, currSizeWidth, currSizeHeight);
		}

		return;
	}

	protected static void fillResolutionsList(int ii, int currSizeWidth, int currSizeHeight)
	{
		Long lmpix = (long) currSizeWidth * currSizeHeight;
		float mpix = (float) lmpix / 1000000.f;
		float ratio = (float) currSizeWidth / currSizeHeight;

		// find good location in a list
		int loc;
		for (loc = 0; loc < CameraController.ResolutionsMPixList.size(); ++loc)
			if (CameraController.ResolutionsMPixList.get(loc) < lmpix)
				break;

		int ri = 0;
		if (Math.abs(ratio - 4 / 3.f) < 0.1f)
			ri = 1;
		if (Math.abs(ratio - 3 / 2.f) < 0.12f)
			ri = 2;
		if (Math.abs(ratio - 16 / 9.f) < 0.15f)
			ri = 3;
		if (Math.abs(ratio - 1) == 0)
			ri = 4;

		CameraController.ResolutionsNamesList.add(loc, String.format("%3.1f Mpix  " + RATIO_STRINGS[ri], mpix));
		CameraController.ResolutionsIdxesList.add(loc, String.format("%d", ii));
		CameraController.ResolutionsMPixList.add(loc, lmpix);
		CameraController.ResolutionsSizeList.add(loc, CameraController.getInstance().new Size(currSizeWidth,
				currSizeHeight));
	}

	public List<CameraController.Size> getSupportedPreviewSizes()
	{
		List<CameraController.Size> previewSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isHALv3)
		{
			List<Camera.Size> sizes = CameraController.camera.getParameters().getSupportedPreviewSizes();
			for (Camera.Size sz : sizes)
				previewSizes.add(this.new Size(sz.width, sz.height));
		} else
			HALv3.fillPreviewSizeList(previewSizes);

		return previewSizes;
	}

	public List<CameraController.Size> getSupportedPictureSizes()
	{
		List<CameraController.Size> pictureSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				List<Camera.Size> sizes = CameraController.camera.getParameters().getSupportedPictureSizes();
				for (Camera.Size sz : sizes)
					pictureSizes.add(this.new Size(sz.width, sz.height));
			} else
			{
				Log.e(TAG, "cameraParameters == null");
			}
		} else
			HALv3.fillPictureSizeList(pictureSizes);

		return pictureSizes;
	}

	public static List<CameraController.Size> getResolutionsSizeList()
	{
		return CameraController.ResolutionsSizeList;
	}

	public static List<String> getResolutionsIdxesList()
	{
		return CameraController.ResolutionsIdxesList;
	}

	public static List<String> getResolutionsNamesList()
	{
		return CameraController.ResolutionsNamesList;
	}

	public static int getNumberOfCameras()
	{
		if (!CameraController.isHALv3)
			return Camera.getNumberOfCameras();
		else
			return CameraController.getInstance().cameraIdList.length;
	}

	public void updateCameraFeatures()
	{
		mEVSupported = getExposureCompensationSupported();
		mSceneModeSupported = getSceneModeSupported();
		mWBSupported = getWhiteBalanceSupported();
		mFocusModeSupported = getFocusModeSupported();
		mFlashModeSupported = getFlashModeSupported();
		mISOSupported = getISOSupported();

		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null && CameraController.camera.getParameters() != null)
			{
				minExpoCompensation = CameraController.camera.getParameters().getMinExposureCompensation();
				maxExpoCompensation = CameraController.camera.getParameters().getMaxExposureCompensation();
				expoCompensationStep = CameraController.camera.getParameters().getExposureCompensationStep();
			}
		} else
		{
			minExpoCompensation = HALv3.getMinExposureCompensationHALv3();
			maxExpoCompensation = HALv3.getMaxExposureCompensationHALv3();
			expoCompensationStep = HALv3.getExposureCompensationStepHALv3();
		}

		supportedSceneModes = getSupportedSceneModesInternal();
		supportedWBModes = getSupportedWhiteBalanceInternal();
		supportedFocusModes = getSupportedFocusModesInternal();
		supportedFlashModes = getSupportedFlashModesInternal();
		supportedISOModes = getSupportedISOInternal();

		maxRegionsSupported = CameraController.getInstance().getMaxNumFocusAreas();
	}

	@Override
	public void onError(int arg0, Camera arg1)
	{
		// Not used
	}

	// ------------ CAMERA PARAMETERS AND CAPABILITIES
	// SECTION-------------------------------------------
	public static boolean isAutoExposureLockSupported()
	{
		return false;
	}

	public static boolean isAutoWhiteBalanceLockSupported()
	{
		return false;
	}

	/*
	 * CAMERA parameters access functions
	 * 
	 * Camera.Parameters get/set Camera scene modes getSupported/set Camera
	 * white balance getSupported/set Camera focus modes getSupported/set Camera
	 * flash modes getSupported/set
	 * 
	 * For API14 Camera focus areas get/set Camera metering areas get/set
	 */
	public static boolean isFrontCamera()
	{
		return CameraMirrored;
	}

	public static Camera getCamera()
	{
		return camera;
	}

	public static void setCamera(Camera cam)
	{
		CameraController.camera = cam;
	}

	public Camera.Parameters getCameraParameters()
	{
		if (CameraController.camera != null && CameraController.camera.getParameters() != null)
			return CameraController.camera.getParameters();

		return null;
	}

	public boolean setCameraParameters(Camera.Parameters params)
	{
		if (params != null && CameraController.camera != null)
		{
			try
			{
				CameraController.camera.setParameters(params);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e(TAG, "setCameraParameters exception: " + e.getMessage());
				return false;
			}

			return true;
		}

		return false;
	}

	public static void startCameraPreview()
	{
		if (camera != null)
			camera.startPreview();
	}

	public static void stopCameraPreview()
	{
		if (camera != null)
			camera.stopPreview();
	}

	public static void lockCamera()
	{
		if (camera != null)
			camera.lock();
	}

	public static void unlockCamera()
	{
		if (camera != null)
			camera.unlock();
	}

	@TargetApi(15)
	public void setVideoStabilization(boolean stabilization)
	{
		if (CameraController.camera.getParameters() != null
				&& CameraController.camera.getParameters().isVideoStabilizationSupported())
		{
			CameraController.camera.getParameters().setVideoStabilization(stabilization);
			this.setCameraParameters(CameraController.camera.getParameters());
		}
	}

	@TargetApi(15)
	public boolean getVideoStabilizationSupported()
	{
		if (CameraController.camera.getParameters() != null)
			return CameraController.camera.getParameters().isVideoStabilizationSupported();

		return false;
	}

	public boolean isVideoStabilizationSupported()
	{
		return mVideoStabilizationSupported;
	}

	public boolean isExposureLockSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null)
				return false;

			return CameraController.camera.getParameters().isAutoExposureLockSupported();
		} else
			return true;
	}

	public boolean isWhiteBalanceLockSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null)
				return false;

			return CameraController.camera.getParameters().isAutoWhiteBalanceLockSupported();
		} else
			return true;
	}

	public boolean isZoomSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (null == camera)
				return false;

			return CameraController.camera.getParameters().isZoomSupported();
		} else
		{
			return HALv3.isZoomSupportedHALv3();
		}
	}

	public int getMaxZoom()
	{
		if (!CameraController.isHALv3)
		{
			if (null == camera)
				return 1;

			return CameraController.camera.getParameters().getMaxZoom();
		} else
		{
			float maxZoom = HALv3.getMaxZoomHALv3();
			return (int) (maxZoom - 10.0f);
		}
	}

	public void setZoom(int value)
	{
		if (!CameraController.isHALv3)
		{
			Camera.Parameters cp = this.getCameraParameters();
			if (cp != null)
			{
				cp.setZoom(value);
				this.setCameraParameters(cp);
			}
		} else
			HALv3.setZoom(value / 10.0f + 1f);

	}

	public boolean isLumaAdaptationSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (null == camera)
				return false;
			Camera.Parameters cp = CameraController.getInstance().getCameraParameters();

			String luma = cp.get("luma-adaptation");
			return luma != null;
		} else
		{
			return false;
		}
	}

	// Used to initialize internal variable
	private boolean getExposureCompensationSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null && CameraController.camera.getParameters() != null)
			{
				return CameraController.camera.getParameters().getMinExposureCompensation() != 0
						&& CameraController.camera.getParameters().getMaxExposureCompensation() != 0;
			} else
				return false;
		} else
			return HALv3.isExposureCompensationSupportedHALv3();
	}

	// Used by CameraController class users.
	public boolean isExposureCompensationSupported()
	{
		return mEVSupported;
	}

	public int getMinExposureCompensation()
	{
		return minExpoCompensation;
	}

	public int getMaxExposureCompensation()
	{
		return maxExpoCompensation;
	}

	public float getExposureCompensationStep()
	{
		return expoCompensationStep;
	}

	public float getExposureCompensation()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null && CameraController.camera.getParameters() != null)
				return CameraController.camera.getParameters().getExposureCompensation()
						* CameraController.camera.getParameters().getExposureCompensationStep();
			else
				return 0;
		} else
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
			return prefs.getInt(MainScreen.sEvPref, 0);
		}
	}

	public void resetExposureCompensation()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				if (!isExposureCompensationSupported())
					return;
				Camera.Parameters params = CameraController.camera.getParameters();
				params.setExposureCompensation(0);
				setCameraParameters(params);
			}
		} else
			HALv3.resetExposureCompensationHALv3();
	}

	private boolean getSceneModeSupported()
	{
		byte[] supported_scene = getSupportedSceneModesInternal();
		return supported_scene != null && supported_scene.length > 0
				&& supported_scene[0] != CameraParameters.SCENE_MODE_UNSUPPORTED;
	}

	public boolean isSceneModeSupported()
	{
		return mSceneModeSupported;
	}

	private byte[] getSupportedSceneModesInternal()
	{
		if (!CameraController.isHALv3)
		{
			List<String> sceneModes = CameraController.camera.getParameters().getSupportedSceneModes();
			if (CameraController.camera != null && sceneModes != null)
			{
				Set<String> known_scenes = CameraController.key_scene.keySet();
				sceneModes.retainAll(known_scenes);
				byte[] scenes = new byte[sceneModes.size()];
				for (int i = 0; i < sceneModes.size(); i++)
				{
					String mode = sceneModes.get(i);
					if (CameraController.key_scene.containsKey(mode))
						scenes[i] = CameraController.key_scene.get(mode).byteValue();
				}

				return scenes;
			}

			return new byte[0];
		} else
			return HALv3.getSupportedSceneModesHALv3();
	}

	public byte[] getSupportedSceneModes()
	{
		return supportedSceneModes;
	}

	private boolean getWhiteBalanceSupported()
	{
		byte[] supported_wb = getSupportedWhiteBalanceInternal();
		return supported_wb != null && supported_wb.length > 0;
	}

	public boolean isWhiteBalanceSupported()
	{
		return mWBSupported;
	}

	private byte[] getSupportedWhiteBalanceInternal()
	{
		if (!CameraController.isHALv3)
		{
			List<String> wbModes = CameraController.camera.getParameters().getSupportedWhiteBalance();
			if (CameraController.camera != null && wbModes != null)
			{
				Set<String> known_wb = CameraController.key_wb.keySet();
				wbModes.retainAll(known_wb);
				byte[] wb = new byte[wbModes.size()];
				for (int i = 0; i < wbModes.size(); i++)
				{
					String mode = wbModes.get(i);
					if (CameraController.key_wb.containsKey(mode))
						wb[i] = CameraController.key_wb.get(mode).byteValue();
				}
				return wb;
			}

			return new byte[0];
		} else
			return HALv3.getSupportedWhiteBalanceHALv3();
	}

	public byte[] getSupportedWhiteBalance()
	{
		return supportedWBModes;
	}

	private boolean getFocusModeSupported()
	{
		byte[] supported_focus = getSupportedFocusModesInternal();
		return supported_focus != null && supported_focus.length > 0;
	}

	public boolean isFocusModeSupported()
	{
		return mFocusModeSupported;
	}

	private byte[] getSupportedFocusModesInternal()
	{
		if (!CameraController.isHALv3)
		{
			List<String> focusModes = CameraController.camera.getParameters().getSupportedFocusModes();
			if (CameraController.camera != null && focusModes != null)
			{
				Set<String> known_focus = CameraController.key_focus.keySet();
				focusModes.retainAll(known_focus);
				byte[] focus = new byte[focusModes.size()];
				for (int i = 0; i < focusModes.size(); i++)
				{
					String mode = focusModes.get(i);
					if (CameraController.key_focus.containsKey(mode))
						focus[i] = CameraController.key_focus.get(mode).byteValue();
				}

				return focus;
			}

			return new byte[0];
		} else
			return HALv3.getSupportedFocusModesHALv3();
	}

	public byte[] getSupportedFocusModes()
	{
		return supportedFocusModes;
	}

	private boolean getFlashModeSupported()
	{
		if (CameraController.isHALv3)
			return HALv3.isFlashModeSupportedHALv3();
		else
		{
			byte[] supported_flash = getSupportedFlashModesInternal();
			return supported_flash != null && supported_flash.length > 0;
		}
	}

	public boolean isFlashModeSupported()
	{
		return mFlashModeSupported;
	}

	private byte[] getSupportedFlashModesInternal()
	{
		if (CameraController.isHALv3)
		{
			if (isFlashModeSupported())
			{
				byte[] flash = new byte[3];
				flash[0] = CameraParameters.FLASH_MODE_OFF;
				flash[1] = CameraParameters.FLASH_MODE_SINGLE;
				flash[2] = CameraParameters.FLASH_MODE_TORCH;
				return flash;
			}
		} else
		{
			List<String> flashModes = CameraController.camera.getParameters().getSupportedFlashModes();
			if (CameraController.camera != null && flashModes != null)
			{
				Set<String> known_flash = CameraController.key_flash.keySet();
				flashModes.retainAll(known_flash);
				byte[] flash = new byte[flashModes.size()];
				for (int i = 0; i < flashModes.size(); i++)
				{
					String mode = flashModes.get(i);
					if (CameraController.key_flash.containsKey(mode))
						flash[i] = CameraController.key_flash.get(flashModes.get(i)).byteValue();
				}

				return flash;
			}
		}

		return new byte[0];
	}

	public byte[] getSupportedFlashModes()
	{
		return supportedFlashModes;
	}

	private boolean getISOSupported()
	{
		if (!CameraController.isHALv3)
		{
			byte[] supported_iso = getSupportedISO();
			String isoSystem = CameraController.getInstance().getCameraParameters().get("iso");
			String isoSystem2 = CameraController.getInstance().getCameraParameters().get("iso-speed");
			return supported_iso != null || isoSystem != null || isoSystem2 != null;
		} else
			return HALv3.isISOModeSupportedHALv3();
	}

	public boolean isISOSupported()
	{
		return mISOSupported;
	}

	private byte[] getSupportedISOInternal()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				List<String> isoModes = null;
				Camera.Parameters camParams = CameraController.getInstance().getCameraParameters();
				String supportedIsoValues = camParams.get("iso-values");
				String supportedIsoValues2 = camParams.get("iso-speed-values");
				String supportedIsoValues3 = camParams.get("iso-mode-values");

				String delims = "[,]+";
				String[] isoList = null;

				if (supportedIsoValues != null && !supportedIsoValues.equals(""))
					isoList = supportedIsoValues.split(delims);
				else if (supportedIsoValues2 != null && !supportedIsoValues2.equals(""))
					isoList = supportedIsoValues2.split(delims);
				else if (supportedIsoValues3 != null && !supportedIsoValues3.equals(""))
					isoList = supportedIsoValues3.split(delims);

				if (isoList != null)
				{
					isoModes = new ArrayList<String>();
					for (int i = 0; i < isoList.length; i++)
						isoModes.add(isoList[i]);
				} else
					return new byte[0];

				byte[] iso = new byte[isoModes.size()];
				for (int i = 0; i < isoModes.size(); i++)
				{
					String mode = isoModes.get(i);
					if (CameraController.key_iso.containsKey(mode))
					{
						if (CameraController.key_iso.containsKey(mode))
							iso[i] = CameraController.key_iso.get(isoModes.get(i)).byteValue();
						else if (CameraController.key_iso2.containsKey(mode))
							iso[i] = CameraController.key_iso2.get(isoModes.get(i)).byteValue();
					}
				}

				return iso;
			}

			return new byte[0];
		} else
			return HALv3.getSupportedISOModesHALv3();
	}

	public byte[] getSupportedISO()
	{
		return supportedISOModes;
	}

	public int getMaxNumMeteringAreas()
	{
		if (CameraController.isHALv3)
			return HALv3.getMaxNumMeteringAreasHALv3();
		else if (CameraController.camera != null)
		{
			Camera.Parameters camParams = CameraController.camera.getParameters();
			return camParams.getMaxNumMeteringAreas();
		}

		return 0;
	}

	private int getMaxNumFocusAreas()
	{
		if (CameraController.isHALv3)
			return HALv3.getMaxNumFocusAreasHALv3();
		else if (CameraController.camera != null)
		{
			Camera.Parameters camParams = CameraController.camera.getParameters();
			return camParams.getMaxNumFocusAreas();
		}

		return 0;
	}

	public static int getMaxAreasSupported()
	{
		return maxRegionsSupported;
	}

	public static int getCameraIndex()
	{
		return CameraIndex;
	}

	public static void setCameraIndex(int index)
	{
		CameraIndex = index;
	}

	public static int getCameraImageSizeIndex()
	{
		return CapIdx;
	}

	public static void setCameraImageSizeIndex(int captureIndex)
	{
		CapIdx = captureIndex;
	}

	public static boolean isModeAvailable(byte[] modeList, int mode)
	{
		boolean isAvailable = false;
		for (int currMode : modeList)
		{
			if (currMode == mode)
			{
				isAvailable = true;
				break;
			}
		}
		return isAvailable;
	}

	public int getSceneMode()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				try
				{
					Camera.Parameters params = CameraController.camera.getParameters();
					if (params != null)
						return CameraController.key_scene.get(params.getSceneMode());
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e(TAG, "getSceneMode exception: " + e.getMessage());
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sSceneModePref, -1);

		return -1;
	}

	public int getWBMode()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				try
				{
					Camera.Parameters params = CameraController.camera.getParameters();
					if (params != null)
						return CameraController.key_wb.get(params.getWhiteBalance());
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e(TAG, "getWBMode exception: " + e.getMessage());
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sWBModePref, -1);

		return -1;
	}

	public int getFocusMode()
	{

		if (!CameraController.isHALv3)
		{
			try
			{
				if (CameraController.camera != null)
				{
					Camera.Parameters params = CameraController.camera.getParameters();
					if (params != null)
						return CameraController.key_focus.get(params.getFocusMode());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e(TAG, "getFocusMode exception: " + e.getMessage());
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(
					CameraMirrored ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, -1);

		return -1;
	}

	public int getFlashMode()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				try
				{
					Camera.Parameters params = CameraController.camera.getParameters();
					if (params != null)
						return CameraController.key_flash.get(params.getFlashMode());
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e(TAG, "getFlashMode exception: " + e.getMessage());
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sFlashModePref, -1);

		return -1;
	}

	public int getISOMode()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					String iso = null;
					iso = params.get("iso");
					if (iso == null)
						iso = params.get("iso-speed");

					return CameraController.key_iso.get(iso);
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sISOPref, -1);

		return -1;
	}

	public void setCameraSceneMode(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					params.setSceneMode(CameraController.mode_scene.get(mode));
					setCameraParameters(params);
				}
			}
		} else
			HALv3.setCameraSceneModeHALv3(mode);
	}

	public void setCameraWhiteBalance(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					params.setWhiteBalance(CameraController.mode_wb.get(mode));
					setCameraParameters(params);
				}
			}
		} else
			HALv3.setCameraWhiteBalanceHALv3(mode);
	}

	public void setCameraFocusMode(int mode)
	{
		Log.e(TAG, "SET CAMERA FOCUS MODE");
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					params.setFocusMode(CameraController.mode_focus.get(mode));
					setCameraParameters(params);
					MainScreen.setAutoFocusLock(false);
				}
			}
		} else
			HALv3.setCameraFocusModeHALv3(mode);
	}

	public void setCameraFlashMode(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					params.setFlashMode(CameraController.mode_flash.get(mode));
					setCameraParameters(params);
				}
			}
		} else
			HALv3.setCameraFlashModeHALv3(mode);
	}

	public void setCameraISO(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					if (params.get(CameraParameters.isoParam) != null)
						params.set(CameraParameters.isoParam, CameraController.mode_iso.get(mode));
					else if (params.get(CameraParameters.isoParam2) != null)
						params.set(CameraParameters.isoParam2, CameraController.mode_iso.get(mode));
					if (!this.setCameraParameters(params))
					{
						if (params.get(CameraParameters.isoParam) != null)
							params.set(CameraParameters.isoParam, CameraController.mode_iso2.get(mode));
						else if (params.get(CameraParameters.isoParam2) != null)
							params.set(CameraParameters.isoParam2, CameraController.mode_iso2.get(mode));
						this.setCameraParameters(params);
					}
				}
			}
		} else
			HALv3.setCameraISOModeHALv3(mode);
	}

	public void setLumaAdaptation(int iEv)
	{
		Camera.Parameters params = CameraController.getInstance().getCameraParameters();
		if (params != null)
		{
			params.set("luma-adaptation", iEv);
			setCameraParameters(params);
		}
	}

	public void setCameraExposureCompensation(int iEV)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.camera.getParameters();
				if (params != null)
				{
					params.setExposureCompensation(iEV);
					setCameraParameters(params);
				}
			}
		} else
			HALv3.setCameraExposureCompensationHALv3(iEV);
	}

	public void setCameraFocusAreas(List<Area> focusAreas)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				try
				{
					Camera.Parameters params = CameraController.getInstance().getCameraParameters();
					if (params != null)
					{
						params.setFocusAreas(focusAreas);
						cameraController.setCameraParameters(params);
					}
				} catch (RuntimeException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
		} else
			HALv3.setCameraFocusAreasHALv3(focusAreas);
	}

	public void setCameraMeteringAreas(List<Area> meteringAreas)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				try
				{
					Camera.Parameters params = CameraController.getInstance().getCameraParameters();
					if (params != null)
					{
						if (meteringAreas != null)
						{
							params.setMeteringAreas(null);
							cameraController.setCameraParameters(params);
						}
						params.setMeteringAreas(meteringAreas);
						cameraController.setCameraParameters(params);
					}
				} catch (RuntimeException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
		} else
			HALv3.setCameraMeteringAreasHALv3(meteringAreas);
	}

	public static void setFocusState(int state)
	{
		if (state != CameraController.FOCUS_STATE_IDLE && state != CameraController.FOCUS_STATE_FOCUSED
				&& state != CameraController.FOCUS_STATE_FAIL)
			return;

		mFocusState = state;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
				PluginManager.MSG_FOCUS_STATE_CHANGED);
	}

	public static int getFocusState()
	{
		return mFocusState;
	}

	public int getPreviewFrameRate()
	{
		if (!CameraController.isHALv3)
		{
			int[] range = { 0, 0 };
			CameraController.camera.getParameters().getPreviewFpsRange(range);
			return range[1] / 1000;
		} else
			return HALv3.getPreviewFrameRateHALv3();
	}

	public void setPictureSize(int width, int height)
	{
		final Camera.Parameters cp = getCameraParameters();
		if (cp == null)
		{
			return;
		}

		cp.setPictureSize(width, height);
		setCameraParameters(cp);
	}

	public void setJpegQuality(int quality)
	{
		final Camera.Parameters cp = getCameraParameters();
		if (cp == null)
		{
			return;
		}

		cp.setJpegQuality(quality);
		setCameraParameters(cp);
	}

	public float getHorizontalViewAngle()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
				return CameraController.camera.getParameters().getHorizontalViewAngle();
		} else if (Build.MODEL.contains("Nexus"))
			return 59.63f;

		return 55.4f;
	}

	public float getVerticalViewAngle()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
				return CameraController.camera.getParameters().getVerticalViewAngle();
		} else if (Build.MODEL.contains("Nexus"))
			return 46.66f;

		return 42.7f;
	}

	// ^^^^^^^^^^^ CAMERA PARAMETERS AND CAPABILITIES
	// SECTION---------------------------------------------

	// ------------ CAPTURE AND FOCUS FUNCTION ----------------------------

	public static int captureImage(int nFrames, int format)
	{
		// In old camera interface we can capture only JPEG images, so image
		// format parameter will be ignored.
		if (!CameraController.isHALv3)
		{
			synchronized (SYNC_OBJECT)
			{
				if (camera != null && CameraController.getFocusState() != CameraController.FOCUS_STATE_FOCUSING)
				{
					mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
					camera.setPreviewCallback(null);
					camera.takePicture(CameraController.getInstance(), null, null, CameraController.getInstance());
					return 0;
				}

				return -1;
			}
		} else
			return HALv3.captureImageHALv3(nFrames, format);
	}

	// Experimental code to take multiple images. Works only with HALv3
	// interface in API 19
	protected static int		pauseBetweenShots	= 0;

	protected static final int	MAX_HDR_FRAMES		= 4;
	protected static int[]		evValues			= new int[MAX_HDR_FRAMES];

	protected static int		total_frames;
	protected static int		frame_num;

	public static int captureImagesWithParams(int nFrames, int format, int pause, int[] evRequested)
	{
		if (!CameraController.isHALv3)
		{
			synchronized (SYNC_OBJECT)
			{
				if (camera != null && CameraController.getFocusState() != CameraController.FOCUS_STATE_FOCUSING)
				{
					mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
					camera.setPreviewCallback(null);
					camera.takePicture(CameraController.getInstance(), null, null, CameraController.getInstance());
					return 0;
				}

				return -1;
			}
		} else
		{
			pauseBetweenShots = pause;
			evValues = evRequested;

			total_frames = nFrames;
			frame_num = 0;

			return HALv3.captureImageWithParamsHALv3(nFrames, format, pause, evRequested);
		}
	}

	public static boolean autoFocus(Camera.AutoFocusCallback listener)
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isHALv3)
			{
				if (CameraController.getCamera() != null
						&& CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
				{
					CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
					try
					{
						CameraController.getCamera().autoFocus(listener);
					} catch (Exception e)
					{
						e.printStackTrace();
						Log.e(TAG, "autoFocus: " + e.getMessage());
						return false;
					}
					return true;
				}
			} else
				return HALv3.autoFocusHALv3();

			return false;
		}
	}

	public static boolean autoFocus()
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isHALv3)
			{
				if (CameraController.getCamera() != null)
				{
					if (CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
					{
						CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
						try
						{
							CameraController.getCamera().autoFocus(CameraController.getInstance());
						} catch (Exception e)
						{
							e.printStackTrace();
							Log.e(TAG, "autoFocus: " + e.getMessage());
							return false;
						}
						return true;
					}
				}
			} else
				return HALv3.autoFocusHALv3();

			return false;
		}
	}

	public static void cancelAutoFocus()
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
				try
				{
					camera.cancelAutoFocus();
				} catch (RuntimeException exp)
				{
					Log.e(TAG, "cancelAutoFocus failed. Message: " + exp.getMessage());
				}
			}
		} else
			HALv3.cancelAutoFocusHALv3();
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);

		pluginManager.onPictureTaken(paramArrayOfByte, paramCamera);
		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
	}

	@Override
	public void onAutoFocus(boolean focused, Camera paramCamera)
	{
		pluginManager.onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}

	public void onAutoFocus(boolean focused)
	{
		pluginManager.onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera)
	{
		pluginManager.onPreviewFrame(data, paramCamera);
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);
	}

	public static void setPreviewCallbackWithBuffer()
	{
		if (!CameraController.isHALv3)
		{
			CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
			CameraController.getCamera().addCallbackBuffer(CameraController.getInstance().pviewBuffer);
		}
	}

	// ^^^^^^^^^^^^^ CAPTURE AND FOCUS FUNCTION ----------------------------

	public class Size
	{
		private int	mWidth;
		private int	mHeight;

		public Size(int w, int h)
		{
			mWidth = w;
			mHeight = h;
		}

		public int getWidth()
		{
			return mWidth;
		}

		public int getHeight()
		{
			return mHeight;
		}

		public void setWidth(int width)
		{
			mWidth = width;
		}

		public void setHeight(int height)
		{
			mHeight = height;
		}
	}

	@Override
	public void onShutter()
	{
		// Not used
	}
}

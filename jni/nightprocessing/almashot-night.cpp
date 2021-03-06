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

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "almashot.h"
#include "blurless.h"
#include "superzoom.h"

#include "ImageConversionUtils.h"

// currently - no concurrent processing, using same instance for all processing types
static unsigned char *yuv[MAX_FRAMES] = {NULL};
static void *instance = NULL;
static int almashot_inited = 0;
static Uint8 *OutPic = NULL;



extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	char status[1024];
	int err=0;
	long mem_used, mem_free;

	if (almashot_inited == 0)
	{
		err = AlmaShot_Initialize(0);

		if (err == 0)
			almashot_inited = 1;
	}

	sprintf (status, "init status: %d\n", err);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Release
(
	JNIEnv* env,
	jobject thiz
)
{
	int i;

	if (almashot_inited == 1)
	{
		AlmaShot_Release();

		almashot_inited = 0;
	}

	return 0;
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_ConvertFromJpeg
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray in_len,
	jint nFrames,
	jint sx,
	jint sy
)
{
	int *jpeg_length;
	unsigned char * *jpeg;
	char status[1024];

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	DecodeAndRotateMultipleJpegs(yuv, jpeg, jpeg_length, sx, sy, nFrames, 0, 0, 0, true);

	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	sprintf (status, "frames total: %d\n", (int)nFrames);
	return env->NewStringUTF(status);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_NightAddYUVFrames
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jint nFrames,
	jint sx,
	jint sy
)
{
	int i;
	unsigned char * *yuvIn;
	char status[1024];

	Uint8 *inp[4];
	int x, y;
	int x0_out, y0_out, w_out, h_out;

	yuvIn = (unsigned char**)env->GetIntArrayElements(in, NULL);

//	for (int i=0; i<nFrames; ++i)
//	{
//		char str[256];
//		sprintf(str, "/sdcard/DCIM/nightin%02d.yuv", i);
//		FILE *f = fopen (str, "wb");
//		fwrite(yuvIn[i], sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
//		fclose(f);
//	}

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		yuv[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

		if (yuv[i]==NULL)
		{
			i--;
			for (;i>=0;--i)
			{
				free(yuv[i]);
				yuv[i] = NULL;
			}
			break;
		}

		//		yuv[i] = yuvIn[i];
		for (y=0; y<sy; y+=2)
		{
			// Y
			memcpy (&yuv[i][y*sx],     &yuvIn[i][y*sx],   sx);
			memcpy (&yuv[i][(y+1)*sx], &yuvIn[i][(y+1)*sx], sx);

			// UV - no direct memcpy as swap may be needed
			for (x=0; x<sx/2; ++x)
			{
				// U
				yuv[i][sx*sy+(y/2)*sx+x*2+1] = yuvIn[i][sx*sy+(y/2)*sx+x*2+1];

				// V
				yuv[i][sx*sy+(y/2)*sx+x*2]   = yuvIn[i][sx*sy+(y/2)*sx+x*2];
			}
		}
	}

	env->ReleaseIntArrayElements(in, (jint*)yuvIn, JNI_ABORT);

	//sprintf (status, "frames total: %d\nsize0: %d\nsize1: %d\nsize2: %d\n", (int)nFrames, jpeg_length[0], jpeg_length[1], jpeg_length[2]);
	sprintf (status, "frames total: %d\n", (int)nFrames);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_BlurLessPreview
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jint sensorGainPref,
	jint DeGhostPref,
	jint lumaEnh,
	jint chromaEnh,
	jint nImages
)
{
	int i;
	Uint8 *pview_rgb;
	Uint32 *pview;
	int nTable[3] = {2,4,6};
	int deghTable[3] = {256/2, 256, 3*256/2};

	//pview_rgb = (Uint8*)malloc((sx/4)*(sy/4)*3);

	//__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "BlurLessPreview 1");

//	FILE * pFile;
//	pFile = fopen ("/sdcard/DCIM/blurlessparams.txt","wb");
//    fprintf (pFile, "Blurless_Preview params:\ndeghTable[DeGhostPref] %d\nnImages %d\nSX %d\nSY %d\n64*nTable[sensorGainPref] %d\nlumaEnh %d\nchromaEnh %d",deghTable[DeGhostPref],nImages,sx,sy,64*nTable[sensorGainPref],lumaEnh,chromaEnh);
//    fclose (pFile);

//	for (int i=0; i<nImages; ++i)
//	{
//		char str[256];
//		sprintf(str, "/sdcard/DCIM/nightin%02d.yuv", i);
//		FILE *f = fopen (str, "wb");
//		fwrite(yuv[i], sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
//		fclose(f);
//	}

	BlurLess_Preview(&instance, yuv, NULL, NULL, NULL,
		0, // 256*3,
		deghTable[DeGhostPref], 1,
		2, nImages, sx, sy, 0, 64*nTable[sensorGainPref], 1, 0, lumaEnh, chromaEnh, 0);

	//__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "BlurLessPreview 3");

//	char s[1024];
//	sprintf(s, "/sdcard/DCIM/blurless_preview.bin");
//	FILE *f=fopen(s, "wb");
//	fwrite (pview_rgb, (sx/4)*(sy/4)*3, 1, f);
//	fclose(f);

	return env->NewStringUTF("ok");
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_BlurLessProcess
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jintArray jcrop,
	jboolean jrot,
	jboolean jmirror
)
{
	Uint8 *OutPic, *OutNV21;
	int *crop;

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	crop[0]=crop[1]=crop[2]=crop[3]=-1;
	BlurLess_Process(instance, &OutPic, &crop[0], &crop[1], &crop[2], &crop[3]);

//	char s[1024];
//
//	sprintf(s, "/sdcard/DCIM/night_result.bin");
//	FILE *f=fopen(s, "wb");
//	fwrite (OutPic, sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
//	fclose(f);

	OutNV21 = OutPic;
	if (jrot)
		OutNV21 = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	TransformNV21(OutPic, OutNV21, sx, sy, crop, jmirror&&jrot, jmirror&&jrot, jrot);

	if (jrot)
	{
		free(OutPic);
		OutPic = OutNV21;
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_ABORT);

	return (jint)OutPic;
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_SuperZoomPreview
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jint nFrames,
	jint sx,
	jint sy,
	jint sxo,
	jint syo,
	jint sensorGainPref,
	jint DeGhostPref,
	jint saturated,
	jint noSres
)
{
	int i;
	void * *frames;
	Uint8 *pview_yuv;
	Uint32 *pview;
	int nTable[3] = {2,4,6};
	int deghTable[3] = {256/2, 256, 3*256/2};

	frames = (void**)env->GetIntArrayElements(in, NULL);

	for (i=0; i<nFrames; ++i)
	{
		// not really sure if this copy is needed
		yuv[i] = (Uint8*)frames[i];
	}

	//__android_log_print(ANDROID_LOG_INFO, "CameraTest", "b: %d (%d %d %d %d)  %d   %dx%d", (int)yuv, (int)yuv[0], (int)yuv[1], (int)yuv[2], (int)yuv[3], sensorGainPref, sx, sy);
	//SuperZoom_Preview(&instance, yuv, pview_yuv, sx, sy, sxo, syo, -1, -1, nFrames,
	SuperZoom_Preview(&instance, yuv, NULL, NULL, sx, sy, sxo, syo, sxo/4, syo/4, nFrames,
		0, // 256*nTable[sensorGainPref],
		deghTable[DeGhostPref], 1,
		-1, 9+saturated*9*16+1,	// hack to get brightening (pass enh. level in kelvin2 parameter)
		1, 1, 64*nTable[sensorGainPref], 2, 1, NULL, 0, 0);

	//__android_log_print(ANDROID_LOG_INFO, "CameraTest", "Preview completed");

	env->ReleaseIntArrayElements(in, (jint*)frames, JNI_ABORT);

	return env->NewStringUTF("ok");
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_SuperZoomProcess
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jintArray jcrop,
	jboolean jrot,
	jboolean jmirror
)
{
	Uint8 *OutPic, *OutNV21;
	int *crop;

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	crop[0]=crop[1]=crop[2]=crop[3]=-1;

	SuperZoom_Process(instance, &OutPic, NULL, &crop[0], &crop[1], &crop[2], &crop[3]);

	OutNV21 = OutPic;
	if (jrot)
		OutNV21 = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	TransformNV21(OutPic, OutNV21, sx, sy, crop, jmirror, 0, jrot);

	if (jrot)
	{
		free(OutPic);
		OutPic = OutNV21;
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_ABORT);

	return (jint)OutPic;
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_convertPreview(
		JNIEnv *env, jclass clazz, jbyteArray ain, jbyteArray aout, jint width,	jint height, jint outWidth, jint outHeight)
{
	jbyte *cImageIn = env->GetByteArrayElements(ain, 0);
	jbyte *cImageOut = env->GetByteArrayElements(aout, 0);

	NV21_to_RGB_scaled_rotated((unsigned char*)cImageIn, width, height, 0, 0, width, height, outWidth, outHeight, 3, (unsigned char*)cImageOut);

	env->ReleaseByteArrayElements(ain, cImageIn, 0);
	env->ReleaseByteArrayElements(aout, cImageOut, 0);
}

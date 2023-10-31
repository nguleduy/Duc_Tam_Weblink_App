#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
//---
#include <assert.h>
#include <stdint.h>
//---
#include <libyuv/convert_from.h>
#include <libyuv/planar_functions.h>
#include <libyuv/convert_from_argb.h>
#include <lz4.h>


#define  LOG_TAG    "YUVDecoder.native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static bool getBitmapInfo(JNIEnv* env, jobject bitmap, AndroidBitmapInfo& info);
static jobject createBitmap(JNIEnv* env, int width, int height);
const static int FORMAT_I420 = 0;
const static int FORMAT_I422 = 1;
const static int FORMAT_I444 = 2;
const static int FORMAT_I411 = 3;

//////////////////////////////////////////////////////////////////////////////////////////////
/// Encodes a frame using I422+XOR+LZ4
///
/// @param[in] sourceFrame - The source image
/// @param[in] srcWidth    - The source image width
/// @param[in] srcHeight   - The source image height
/// @param[in] encWidth    - The encoded image width
/// @param[in] encHeight   - The encoded image height
/// @param[in] encodedFrame- The encoded frame
/// @param[in] format      - YUV format (420/422/444/411) as 0-3
///
/// @retval bool - true on success, false otherwise
//////////////////////////////////////////////////////////////////////////////////////////////
extern "C" JNIEXPORT jobject JNICALL Java_com_abaltatech_weblinkclient_framedecoding_FrameDecoder_1YUV_decodeFrame(
    JNIEnv*    env,
    jobject    classObj,
    jobject    frameBuffer,
    jbyteArray encFrame,
    jint       startPos,
    jint       size,
    jint       format
    )
{
  jobject  result     = NULL;
  jboolean isCopy     = false;
  jbyte*   source     = env->GetByteArrayElements(encFrame, &isCopy);

  if (source)
  {
    jbyte*  inputFrame = source + startPos;
    int     offset     = sizeof(uint32_t) + 2 * sizeof(uint16_t);
    int16_t encWidth   = ((int16_t*)inputFrame)[0];
    int16_t encHeight  = ((int16_t*)inputFrame)[1];
    int     bufferSize = *(uint32_t*)(inputFrame + 4);
    int     y_plane_size = encWidth * encHeight;
    int     u_plane_size = (encWidth * encHeight + 1) / 2;
    int     v_plane_size = (encWidth * encHeight + 1) / 2;
    AndroidBitmapInfo info;
    void*             pixels;

    assert(bufferSize == y_plane_size + u_plane_size + v_plane_size);
    if (!getBitmapInfo(env, frameBuffer, info) || info.width != encWidth || info.height != encHeight)
    {
      LOGI("New %dx%d bitmap created", encWidth, encHeight);
      result = createBitmap(env, encWidth, encHeight);
    }
    else
    {
      result  = frameBuffer;
    }
    if (result && AndroidBitmap_lockPixels(env, result, &pixels) >= 0)
    {
      uint8_t*     i422frame = new uint8_t[bufferSize];
      int          readSize  = LZ4_decompress_fast((char*)inputFrame + offset, (char*)i422frame, bufferSize);
      const uint8* src_y  = i422frame;
      const uint8* src_u  = i422frame + y_plane_size;
      const uint8* src_v  = i422frame + y_plane_size + u_plane_size;

      assert(readSize == size - offset);

      switch( format )
      {
      case FORMAT_I420:
          libyuv::I422ToABGR(
                          src_y, encWidth,
                          src_u, (encWidth + 1) / 2,
                          src_v, (encWidth + 1) / 2,
                          (uint8*)pixels, encWidth * 4,
                          encWidth, encHeight
                        );
          break;
      case FORMAT_I422:
          libyuv::I422ToABGR(
                          src_y, encWidth,
                          src_u, (encWidth + 1) / 2,
                          src_v, (encWidth + 1) / 2,
                          (uint8*)pixels, encWidth * 4,
                          encWidth, encHeight
                        );
          break;
      case FORMAT_I444:
          libyuv::I444ToARGB(
                          src_y, encWidth,
                          src_u, (encWidth + 1) / 2,
                          src_v, (encWidth + 1) / 2,
                          (uint8*)pixels, encWidth * 4,
                          encWidth, encHeight
                        );
          // convert ARGB to ABGR
          libyuv::ARGBToRGBA((const uint8*)pixels, encWidth * 4,
                         (uint8*)pixels, encWidth * 4,
                         encWidth, encHeight
                        );

          break;
      case FORMAT_I411:
          libyuv::I411ToARGB(
                          src_y, encWidth,
                          src_u, (encWidth + 1) / 2,
                          src_v, (encWidth + 1) / 2,
                          (uint8*)pixels, encWidth * 4,
                          encWidth, encHeight
                        );
          // convert ARGB to ABGR
          libyuv::ARGBToRGBA((const uint8*)pixels, encWidth * 4,
                         (uint8*)pixels, encWidth * 4,
                         encWidth, encHeight
                        );
          break;
      default:
          libyuv::I422ToABGR(
                          src_y, encWidth,
                          src_u, (encWidth + 1) / 2,
                          src_v, (encWidth + 1) / 2,
                          (uint8*)pixels, encWidth * 4,
                          encWidth, encHeight
                        );
          break;
      }

      AndroidBitmap_unlockPixels(env, result);
      delete[] i422frame;
    }
    env->ReleaseByteArrayElements(encFrame, source, JNI_ABORT);
  }
  return result;
}


static bool getBitmapInfo(JNIEnv* env, jobject bitmap, AndroidBitmapInfo& info)
{
  bool result = false;

  if (bitmap)
  {
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
    {
      LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
    }
    else
    {
      result = true;
    }
  }
  return result;
}

static jobject createBitmap(JNIEnv* env, int width, int height)
{
  jclass    bitmapConfig    = env->FindClass("android/graphics/Bitmap$Config");
  jfieldID  argb8888FieldID = env->GetStaticFieldID(bitmapConfig, "ARGB_8888",
                                  "Landroid/graphics/Bitmap$Config;");
  jobject   argb8888Obj     = env->GetStaticObjectField(bitmapConfig, argb8888FieldID);
  jclass    bitmapClass     = env->FindClass("android/graphics/Bitmap");
  jmethodID createBitmapMethodID =
                              env->GetStaticMethodID(bitmapClass,"createBitmap",
                                  "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
  jobject   bitmapObj       = env->CallStaticObjectMethod(bitmapClass,
                                  createBitmapMethodID, width, height, argb8888Obj);
  return bitmapObj;
}

LOCAL_PATH := $(call my-dir)

# libyuv, built statically
include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc 

LOCAL_SRC_FILES := \
    libyuv/source/compare.cc           \
    libyuv/source/compare_common.cc    \
    libyuv/source/compare_posix.cc     \
    libyuv/source/convert.cc           \
    libyuv/source/convert_argb.cc      \
    libyuv/source/convert_from.cc      \
    libyuv/source/convert_from_argb.cc \
    libyuv/source/cpu_id.cc            \
    libyuv/source/format_conversion.cc \
    libyuv/source/planar_functions.cc  \
    libyuv/source/rotate.cc            \
    libyuv/source/rotate_argb.cc       \
    libyuv/source/row_any.cc           \
    libyuv/source/row_common.cc        \
    libyuv/source/row_mips.cc          \
    libyuv/source/row_posix.cc         \
    libyuv/source/scale.cc             \
    libyuv/source/scale_argb.cc        \
    libyuv/source/scale_mips.cc        \
    libyuv/source/video_common.cc      \

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -DLIBYUV_NEON -D__ARM_NEON__
    LOCAL_CFLAGS += -mfpu=neon
    LOCAL_SRC_FILES += \
        libyuv/source/compare_neon.cc \
        libyuv/source/rotate_neon.cc \
        libyuv/source/row_neon.cc  \
        libyuv/source/scale_neon.cc \
        libyuv/source/scale_argb_neon.cc
endif

LOCAL_C_INCLUDES += $(LOCAL_PATH)/libyuv/include

LOCAL_MODULE    := libyuv
include $(BUILD_STATIC_LIBRARY)

# liblz4, built statically
include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  lz4/lz4.c
LOCAL_MODULE    := liblz4
include $(BUILD_STATIC_LIBRARY)


# the h264 decoder
include $(CLEAR_VARS)
LOCAL_MODULE    := h264decoder
LOCAL_SRC_FILES := ByteBuffer.cpp
LOCAL_CFLAGS += -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS

# the i420 decoder
include $(CLEAR_VARS)
LOCAL_MODULE    := i420decoder
LOCAL_SRC_FILES := FrameDecoder_I420.cpp
LOCAL_STATIC_LIBRARIES := libyuv liblz4
LOCAL_ALLOW_UNDEFINED_SYMBOLS=false
LOCAL_LDLIBS    += -llog -lz -lm -ljnigraphics
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_LDLIBS += -Wl,--no-warn-shared-textrel
endif
LOCAL_LDLIBS += -Wl
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libyuv/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/lz4
include $(BUILD_SHARED_LIBRARY)

# the i422 decoder
include $(CLEAR_VARS)
LOCAL_MODULE    := yuvdecoder
LOCAL_SRC_FILES := FrameDecoder_YUV.cpp
LOCAL_STATIC_LIBRARIES := libyuv liblz4
LOCAL_ALLOW_UNDEFINED_SYMBOLS=false
LOCAL_LDLIBS    += -llog -lz -lm -ljnigraphics
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_LDLIBS += -Wl,--no-warn-shared-textrel
endif
LOCAL_LDLIBS += -Wl
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libyuv/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/lz4
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
LOCAL_MODULE    := loader
LOCAL_SRC_FILES := loader/loader.cpp
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
 
include $(BUILD_SHARED_LIBRARY)

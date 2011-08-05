#include "decrypter.hpp"
#include <string.h>
#include <fstream>
#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>

using namespace std;

extern "C" {
  jbyteArray Java_thesis_sec_Decrypter_dec(JNIEnv* env, jobject obj, jbyteArray data) {
    
    char* jkey = NULL;
    jboolean iscopy;
    
    
    //get java methods reference
    jclass cls = env->GetObjectClass(obj);
    jmethodID throwExc = env->GetMethodID(cls, "throwException", "()V");
    jmethodID getUniqueIdentifier = env->GetMethodID(cls, "getUniqueIdentifier", "()Ljava/lang/String;");
    if ((getUniqueIdentifier == 0) || (throwExc == 0))
      return NULL;//raises exception
      
      
      // load the dec library
      void* dec = dlopen("/data/data/thesis.drmReader/libdec.so", RTLD_LAZY);
    if (!dec) {
      const char* msg = dlerror();
      __android_log_write(ANDROID_LOG_ERROR, "JNI", msg);
      return NULL;
    }
    
    // load the symbols
    create_t* create_dec = (create_t*) dlsym(dec, "create");
    destroy_t* destroy_dec = (destroy_t*) dlsym(dec, "destroy");
    if (!create_dec || !destroy_dec) {
      __android_log_write(ANDROID_LOG_ERROR, "JNI", "Cannot load symbols");
      return NULL;
    }
    
    // create an instance of the class
    decrypter* decrypter = create_dec();
    __android_log_write(ANDROID_LOG_ERROR, "JNI_dec", "Dec instance created");
    
    //get UniqueIdentifier
    jstring devK =  (jstring)env->CallObjectMethod(obj, getUniqueIdentifier);
    const char* deviceKey = env->GetStringUTFChars(devK , &iscopy);

    if(devK != NULL)
    {

      jsize dataSize = env->GetArrayLength( data );
      jbyte* dataBytes = env->GetByteArrayElements( data, JNI_FALSE );//we retrieve a reference to the java element.Not copy
      vector<unsigned char> resData;
      
      resData = decrypter->decrypt((unsigned char*)dataBytes,deviceKey, dataSize);
      
      if(resData.empty()){
	if(throwExc != 0){
	  env->CallVoidMethod(obj, throwExc);
	}
	
	destroy_dec(decrypter);
	// unload the library
	dlclose(dec);
	env->ReleaseStringUTFChars(devK , deviceKey); // release jstring
	//free(dataBytes);
	env->ReleaseByteArrayElements(data,dataBytes,JNI_ABORT); //commit changes back to java copy
	return NULL;
      }
      
      jbyteArray bArray = env->NewByteArray(resData.size());
      
      unsigned char arr[resData.size()];
      int i = 0;
      int resSize = resData.size();
      for(i=0;i<resSize;i++){
	arr[i] = resData[i];
      }

      env-> SetByteArrayRegion( bArray, 0, resData.size(), (jbyte*)arr);
      
      env->ReleaseByteArrayElements(data,dataBytes,JNI_ABORT); //commit changes back to java copy
      return bArray;
    }
    // destroy the class
    destroy_dec(decrypter);
    
    // unload the  library
    dlclose(dec);
    env->ReleaseStringUTFChars(devK , deviceKey); // release jstring
  }
}
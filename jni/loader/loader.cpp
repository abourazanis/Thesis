#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "decrypter.hpp"
#include <dlfcn.h>


extern "C" {
void Java_thesis_sec_Decrypter_dec(JNIEnv* env, jobject obj, jbyteArray data) {
  
  char* jkey = NULL;
  jboolean iscopy;

  
  //get java methods reference
  jclass cls = env->GetObjectClass(obj);
  jmethodID throwExc = env->GetMethodID(cls, "throwException", "()V");
  jmethodID getUniqueIdentifier = env->GetMethodID(cls, "getUniqueIdentifier", "()Ljava/lang/String;");
  if ((getUniqueIdentifier == 0) || (throwExc == 0))
    return;//raises exception
  

  // load the dec library
  void* dec = dlopen("/data/data/thesis.drmReader/libdec.so", RTLD_LAZY);
  if (!dec) {
    const char* msg = dlerror();
    __android_log_write(ANDROID_LOG_ERROR, "JNI", msg);
    return;
  }
  __android_log_write(ANDROID_LOG_ERROR, "JNI", "libdec loaded");

// load the symbols
  create_t* create_dec = (create_t*) dlsym(dec, "create");
  destroy_t* destroy_dec = (destroy_t*) dlsym(dec, "destroy");
  if (!create_dec || !destroy_dec) {
    __android_log_write(ANDROID_LOG_ERROR, "JNI", "Cannot load symbols");
    return;
  }
         
  // create an instance of the class
  decrypter* decrypter = create_dec();
  __android_log_write(ANDROID_LOG_ERROR, "JNI", "decrypter instance created");

  //get UniqueIdentifier
  jstring devK =  (jstring)env->CallObjectMethod(obj, getUniqueIdentifier);
  const char* deviceKey = env->GetStringUTFChars(devK , &iscopy);
  __android_log_write(ANDROID_LOG_ERROR, "JNI", "key call ");
  if(devK != NULL)
  {
	  __android_log_write(ANDROID_LOG_ERROR, "JNI", "Obtained key from java");
	  jbyte* dataBytes = env->GetByteArrayElements( data, JNI_FALSE );//we retrieve a reference to the java element.Not copy
	  char* resData;
	  __android_log_write(ANDROID_LOG_ERROR, "JNI", "before decrypter call Data:");
	  resData = decrypter->decrypt((char*)dataBytes,deviceKey);
	  __android_log_write(ANDROID_LOG_ERROR, "JNI", "After decrypter call Data:");
	  /*
	  int length = strlen(resData);
	  int i =0;
	  for(i=0;i<length;i++){
	    dataBytes[i] = resData[i];
	  }
	  */
	  if(resData == NULL){
	    if(throwExc != 0){
		  env->CallVoidMethod(obj, throwExc);
		}
		
		destroy_dec(decrypter);
		// unload the library
		dlclose(dec);
		env->ReleaseStringUTFChars(devK , deviceKey); // release jstring
		env->ReleaseByteArrayElements(data,dataBytes,JNI_COMMIT); //commit changes back to java copy
		return;
	  }
	  
	  env->ReleaseByteArrayElements(data,dataBytes,JNI_COMMIT); //commit changes back to java copy
	__android_log_write(ANDROID_LOG_ERROR, "JNI", "Finished");
	
  }
  
  __android_log_write(ANDROID_LOG_ERROR, "JNI", "After finished  -Release");
  // destroy the class
  destroy_dec(decrypter);

  // unload the triangle library
  dlclose(dec);
  env->ReleaseStringUTFChars(devK , deviceKey); // release jstring
}
}

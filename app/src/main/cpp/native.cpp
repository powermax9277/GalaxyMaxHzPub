//
// Created by tribalfs on 7 Nov 2022.
//
#include <jni.h>
#include <string>

_jclass *clz_ApplicationInfo;
_jfieldID *fId_ApplicationInfo_flags;
_jfieldID *fId_ApplicationInfo_FLAG_DEBUGGABLE;
_jclass *clz_Context;
_jmethodID *mId_Context_getApplicationInfo;

void initObjects(JNIEnv *env) {
    jclass local = env->FindClass("android/content/Context");
    clz_Context = reinterpret_cast<jclass>(env->NewGlobalRef(local));
    env->DeleteLocalRef(local);

    local = env->FindClass("android/content/pm/ApplicationInfo");
    clz_ApplicationInfo = reinterpret_cast<jclass>(env->NewGlobalRef(local));
    env->DeleteLocalRef(local);

    mId_Context_getApplicationInfo = env->GetMethodID( clz_Context,
                                                       "getApplicationInfo", "()Landroid/content/pm/ApplicationInfo;");


    fId_ApplicationInfo_flags = env->GetFieldID(clz_ApplicationInfo, "flags",
                                                "I");

    fId_ApplicationInfo_FLAG_DEBUGGABLE = env->GetStaticFieldID(
            clz_ApplicationInfo, "FLAG_DEBUGGABLE", "I");
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void) reserved; // Suppress the warning.
    JNIEnv * env;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    initObjects(env);

    return JNI_VERSION_1_6;
}


static void isDebuggable(JNIEnv *env, jobject *ctx) {
    jobject applicationInfoInstance = env->CallObjectMethod( *ctx, mId_Context_getApplicationInfo);
    int FLAG_DEBUGGABLE = env->GetStaticIntField( clz_ApplicationInfo,
                                                  fId_ApplicationInfo_FLAG_DEBUGGABLE);
    int flags = env->GetIntField( applicationInfoInstance,
                                  fId_ApplicationInfo_flags);

    env->DeleteLocalRef( applicationInfoInstance);

    if (0 != (flags & FLAG_DEBUGGABLE)){
        exit(0);
    }
}



extern "C" {
JNIEXPORT
jstring
JNICALL
Java_com_tribalfs_gmh_MyApplication_00024Companion_g03(JNIEnv *env, jobject  __unused GmhApp) {
    std::string str_baseURL = "https://script.google.com/macros/s/AKfycbz07eyttrJr0S9Q-IgIYWKoK6aMigcHG-lncrvNpyz6QKKuAjXyOOKvsSqDr5BPOn1awA/exec";
    return env->NewStringUTF(str_baseURL.c_str());
}

JNIEXPORT
void
JNICALL
Java_com_tribalfs_gmh_MyApplication_00024Companion_g02(JNIEnv *env, jobject  __unused GmhApp, jobject ctx) {
    isDebuggable(env, &ctx);
}

}
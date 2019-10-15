#include <jni.h>
#include <android/log.h>

#include <string.h>
#include <stdio.h>

int exists(const char *fname) {
    FILE *file;
    if ((file = fopen(fname, "r"))) {
        fclose(file);
        return 1;
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_forgerock_android_auth_detector_NativeDetector_exists(JNIEnv *env, jobject instance, jobjectArray pathArray) {

    int found = 0;

    int stringCount = (env)->GetArrayLength(pathArray);

    for (int i = 0; i < stringCount; i++) {
        jstring string = (jstring) (env)->GetObjectArrayElement(pathArray, i);
        const char *pathString = (env)->GetStringUTFChars(string, 0);

        found += exists(pathString);

        (env)->ReleaseStringUTFChars(string, pathString);
    }

    return found > 0;
}

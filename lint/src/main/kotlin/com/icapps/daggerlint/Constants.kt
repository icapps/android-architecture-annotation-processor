package com.icapps.daggerlint

object Constants {

    const val ANDROID_SUPPORT_ACTIVITY_CLASSNAME = "dagger.android.support.DaggerAppCompatActivity"
    const val ANDROID_SERVICE_CLASSNAME = "dagger.android.DaggerService"
    const val ANDROID_INTENT_SERVICE_CLASSNAME = "dagger.android.DaggerIntentService"
    const val ANDROID_DIALOG_COMPAT_FRAGMENT_CLASSNAME = "dagger.android.support.DaggerAppCompatDialogFragment"
    const val ANDROID_SUPPORT_APPLICATION_CLASSNAME = "dagger.android.support.DaggerApplication"
    const val DAGGER_SUPPORT_DIALOG_FRAGMENT_CLASSNAME = "dagger.android.support.DaggerDialogFragment"
    const val ANDROID_SUPPORT_FRAGMENT_CLASSNAME = "dagger.android.support.DaggerFragment"
    const val ANDROID_ACTIVITY_CLASSNAME = "dagger.android.DaggerActivity"
    const val ANDROID_APPLICATION_CLASSNAME = "dagger.android.DaggerApplication"
    const val ANDROID_BROADCAST_RECEIVER = "dagger.android.DaggerBroadcastReceiver"
    const val ANDROID_CONTENT_PROVIDERS = "dagger.android.DaggerContentProvider"

    val ALL_ANDROID_INJECTED = setOf(
        ANDROID_SUPPORT_ACTIVITY_CLASSNAME, ANDROID_SERVICE_CLASSNAME, ANDROID_INTENT_SERVICE_CLASSNAME, ANDROID_DIALOG_COMPAT_FRAGMENT_CLASSNAME,
        ANDROID_SUPPORT_APPLICATION_CLASSNAME, DAGGER_SUPPORT_DIALOG_FRAGMENT_CLASSNAME, ANDROID_SUPPORT_FRAGMENT_CLASSNAME,
        ANDROID_ACTIVITY_CLASSNAME, ANDROID_APPLICATION_CLASSNAME, ANDROID_BROADCAST_RECEIVER, ANDROID_CONTENT_PROVIDERS
    )

    const val GENERATE_INJECTION_CLASSNAME = "com.icapps.arch.annotation.AndroidInjected"
    const val GENERATE_INJECTION_CLASSNAME_SHORT = "AndroidInjected"
    const val JAVAX_INJECT_CLASSNAME = "javax.inject.Inject"
    const val JAVA_OBJECT_CLASSNAME = "java.lang.Object"

}
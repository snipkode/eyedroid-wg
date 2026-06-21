@file:Suppress("UnstableApiUsage")

val pkg: String = providers.gradleProperty("wireguardPackageName").get()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.legacy.kapt)
}

android {
    compileSdk = 36
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    namespace = pkg
    defaultConfig {
        applicationId = pkg
        minSdk = 24
        versionCode = providers.gradleProperty("wireguardVersionCode").get().toInt()
        versionName = providers.gradleProperty("wireguardVersionName").get()
        buildConfigField("int", "MIN_SDK_VERSION", minSdk.toString())
        buildConfigField("String", "TENANT_ID", "\"system\"")
    }
    flavorDimensions += "tenant"
    productFlavors {
        // Flavors injected dynamically by build_apks.sh via tenantFlavors property
        val flavors = providers.gradleProperty("tenantFlavors").orNull
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("system")
        flavors.forEach { tenantId ->
            create(tenantId.replace("-", "_")) {
                dimension = "tenant"
                buildConfigField("String", "TENANT_ID", "\"$tenantId\"")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    signingConfigs {
        create("release") {
            storeFile = file("../eyedroid-release.jks")
            storePassword = "eyedroid"
            keyAlias = "eyedroid"
            keyPassword = "eyedroid"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-android-optimize.txt")
            packaging {
                resources {
                    excludes += "DebugProbesKt.bin"
                    excludes += "kotlin-tooling-metadata.json"
                    excludes += "META-INF/*.version"
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        create("googleplay") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    lint {
        disable += "LongLogTag"
        disable += "MissingDefaultResource"
        warning += "MissingTranslation"
        warning += "ImpliedQuantity"
    }
}

dependencies {
    implementation(project(":tunnel"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.material)
    implementation(libs.zxing.android.embedded)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugarJdkLibs)
    // EyeDroid VPN additions
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // DataBinding runtime — needed by WireGuard model classes (BaseObservable, ObservableField)
    implementation("androidx.databinding:databinding-runtime:8.10.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}

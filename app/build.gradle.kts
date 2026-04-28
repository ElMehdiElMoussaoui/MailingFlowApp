plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.appmailing"

    // تم التحديث إلى 36 لحل تعارض مكتبة core-ktx:1.18.0
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.appmailing"

        // تم التثبيت على 26 لدعم مكتبة Apache POI ومنع خطأ MethodHandle
        minSdk = 26

        // التوافق مع سلوك أندرويد 15
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // تفعيل MultiDex لمنع أخطاء كثرة الدوال الناتجة عن مكتبة الإكسل
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // تفعيل Desugaring للسماح بميزات الجافا الحديثة على الإصدارات القديمة
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    // مكتبة فك التشفير الأساسية (إصدار مستقر وحديث)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // --- الربط مع ملف libs.versions.toml ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)

    // --- Excel Support (Apache POI) ---
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // --- Database (ROOM) ---
    // Updated Room to 2.7.0-alpha12 to support Kotlin 2.1+ metadata
    val room_version = "2.7.0-alpha12"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // --- Networking (Retrofit لـ SendGrid API) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // --- Lifecycle components ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // --- UI Components الإضافية ---
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // الاختبارات
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
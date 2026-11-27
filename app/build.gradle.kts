plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.practice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.practice"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // 每个产品变体编译自己的 MainActivity。不同应用 ID 让所有 APK 可以同时安装。
    flavorDimensions += "example"
    productFlavors {
        mapOf(
            "unit" to "单位换算",
            "score" to "比赛计分",
            "quiz" to "选择题测验",
            "shop" to "购物清单",
            "expense" to "开支记账",
            "water" to "饮水记录",
            "timer" to "倒计时",
            "photo" to "图片浏览",
            "notes" to "双语便签",
            "study" to "复习卡片",
            "bill" to "账单分摊",
            "color" to "RGB 调色板",
            "event" to "活动报名",
            "habit" to "每日打卡",
            "task" to "待办事项",
            "contact" to "本地通讯录",
            "books" to "离线图书目录",
            "weather" to "城市天气",
            "sensor" to "水平仪",
            "draw" to "涂鸦板"
        ).forEach { (flavor, label) ->
            create(flavor) {
                dimension = "example"
                applicationIdSuffix = ".$flavor"
                resValue("string", "app_name", label)
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}

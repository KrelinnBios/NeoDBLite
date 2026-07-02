// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 8.13.2 传递引入 bcprov-jdk18on 1.79(CVE-2025-14813,GOST CTR 密钥流重用),
        // 强制提升到修复版本;升级 AGP 后若其自带版本 >= 1.80.2 可移除此覆盖
        classpath("org.bouncycastle:bcprov-jdk18on:1.80.2")
    }
}
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

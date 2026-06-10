// Top-level build file — plugin versions declared here, applied in submodules
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

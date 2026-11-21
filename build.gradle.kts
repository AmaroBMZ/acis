// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Declara plugins comunes; apply false permite aplicarlos después en el módulo app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
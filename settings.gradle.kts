pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("https://rikka.dev/repository/release")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://rikka.dev/repository/release")
        }
    }
}


rootProject.name = "Performance Monitor"
include(":app")
include(":serialport")

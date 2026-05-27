pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Mapbox SDK 的私有 Maven 倉庫
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                // 從 local.properties 讀取 Mapbox Secret Token（這個檔案不會上傳 git）
                val localProps = java.util.Properties().also { props ->
                    file("local.properties").takeIf { it.exists() }
                        ?.inputStream()?.use { props.load(it) }
                }
                password = localProps.getProperty("MAPBOX_DOWNLOADS_TOKEN", "")
            }
        }
    }
}

rootProject.name = "GoForIt"
include(":app")

# Android-Performances-Monitor
## APK 源码结构

```plaintext
PerformanceMonitor （项目根目录）
├── .gradle （Gradle相关文件）
├── .idea （IntelliJ IDEA的项目配置文件）
├── app （应用主模块）
│   ├── build （构建输出目录）
│   │   └── release （发布版本的输出）
│   └── src （源代码目录）
│       └── main （主源代码目录）
│           ├── java
│           │   └── com.droidlogic.performancemonitor （Java包）
│           │       ├── BootReceiver （启动广播接收器类）
│           │       ├── FloatingService （悬浮窗服务类）
│           │       ├── MainActivity （主活动类）
│           │       ├── NetworkUtils （网络工具类）
│           │       ├── RestartServiceReceiver （服务重启接收器类）
│           │       ├── SftpUploader （SFTP上传工具类）
│           │       ├── SystemInfo （系统信息类）
│           │       └── Utils （工具类）
│           └── res （资源目录）
│               ├── layout （布局文件）
│               │   ├── floating_layout.xml （悬浮窗布局）
│               │   ├── identifier_layout.xml （标识符布局）
│               │   └── main.xml （主布局）
│               ├── mipmap-hdpi （不同密度的应用图标）
│               ├── mipmap-mdpi
│               ├── mipmap-xhdpi
│               ├── mipmap-xxhdpi
│               ├── mipmap-xxxhdpi
│               └── values （资源文件，如字符串、颜色等）
│                   └── xml （XML资源文件）
│   └── AndroidManifest.xml （应用程序的Manifest文件）
├── gradle （Gradle包装器相关文件）
│   └── wrapper
│       └── gradle-wrapper.properties （Gradle包装器属性文件）
├── .gitignore （Git忽略文件配置）
├── build.gradle.kts （Gradle构建脚本，使用Kotlin DSL）
└── proguard-rules.pro （ProGuard混淆规则）


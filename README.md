# Android 机考练习

十个可独立安装的示例按课堂练习主题设计。每个示例都有独立的应用 ID、入口和业务逻辑，复用 [Practice.kt](app/src/main/java/com/example/practice/Practice.kt) 中的原生控件样式。报名、随堂测验等题目换了场景时，仍可以复用表单校验、页面传值和保存状态的写法。

## 练习与验证

| App | 涉及的写法 | 一次自测 |
| --- | --- | --- |
| [单位换算](app/src/unit/java/com/example/practice/Main.kt) | Spinner、数字解析、单位转换、表单恢复 | 选择米和厘米，输入 2.5，得到 250 厘米。留空再提交，查看错误提示。 |
| [比赛计分](app/src/score/java/com/example/practice/Main.kt) | ViewModel、StateFlow、SavedStateHandle | 分别加分，旋转屏幕检查分数，再重置。 |
| [选择题测验](app/src/quiz/java/com/example/practice/Main.kt) | RadioGroup、答题状态、Intent 和 XML 结果页 | 完成三道题，查看成绩，返回重答。 |
| [购物清单](app/src/shop/java/com/example/practice/Main.kt) | SharedPreferences、JSON、列表增删和勾选 | 添加商品并勾选，重启时核对勾选状态，再删除。 |
| [开支记账](app/src/expense/java/com/example/practice/Main.kt) | SQLite 增删改查、事务、用整数分存金额 | 加入 12.30 元和 2.70 元的两笔记录，合计应为 15.00 元。编辑和删除后核对合计。 |
| [饮水记录](app/src/water/java/com/example/practice/Main.kt) | 日期主键、同日覆盖、后台查库、自定义折线图 | 保存不同日期，再给同一天保存两次，检查当天仅有最新数值。 |
| [倒计时](app/src/timer/java/com/example/practice/Main.kt) | 单调时钟、生命周期、暂停与恢复 | 设置 10 秒后开始，暂停检查时间保持，再继续并切后台。回到应用时应反映真实经过的时间。 |
| [图片浏览](app/src/photo/java/com/example/practice/Main.kt) | Activity Result、系统文件选择器、URI 读取授权 | 选一张本地图片，重新打开应用，检查已选预览。 |
| [双语便签](app/src/notes/java/com/example/practice/Main.kt) | Activity Result、表单编辑、搜索和中英文资源 | 新建便签，用正文里的词搜索。把系统语言切到简体中文，查看中文界面。 |
| [复习卡片](app/src/study/java/com/example/practice/Main.kt) | 列表与翻面状态、协程、可选 HTTPS/JSON 请求 | 离线时翻看内置卡片。联网生成时，在应用里输入自己的 DeepSeek API 密钥。 |

## 用指定版本打开

用 Android Studio **2025.1.3 或 2025.1.4** 打开本目录，安装 **Android SDK 36**，并把 **Gradle JDK** 设为 **17**。本工程固定 Kotlin **2.2.10**、JVM **17**、AGP **8.13.0** 和 Gradle **8.13**。最低支持 Android 7.0（API 24）。[Android Studio 兼容表](https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility)

在 **Build Variants** 面板为 `app` 选择对应的 Debug 变体，再按 **Run**。变体名和源码目录对应。例如 `waterDebug` 使用 `app/src/water/`。

```powershell
# 构建全部十个 APK。
.\gradlew.bat :app:assembleDebug

# 想只练一题时，只构建对应变体。
.\gradlew.bat :app:assembleWaterDebug
```

构建产物位于 `app/build/outputs/apk/<变体>/debug/`。若通过成品包安装，`APKs/` 里有十个调试包。第一次 Gradle 同步需要下载依赖，缓存齐全后可以用 `--offline` 构建。复习卡片的联网功能经用户点击后发送复习主题，其余练习流程可离线使用。

## 用 GitHub 保存源码

在 GitHub 上创建空仓库，仓库可见性按课程要求设置。将当前工程目录推送到新仓库即可。`.gitignore` 会排除 APK、构建目录、SDK 本地路径和 IDE 缓存。把 GitHub 显示的仓库地址填到下面的命令中。[GitHub 本地代码导入说明](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)

```powershell
# 在包含 settings.gradle.kts 的目录执行。
git init -b main
git add .
git update-index --chmod=+x gradlew
git status --short
git commit -m "Add ten annotated Android practice apps"
git remote add origin 'https://github.com/YOUR-ACCOUNT/AndroidPractice.git'
git push -u origin main
```

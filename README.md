# Android 机考练习

二十个可独立安装的示例按课堂练习主题设计。每个示例都有独立的应用 ID、入口和业务逻辑，复用 [Practice.kt](app/src/main/java/com/example/practice/Practice.kt) 中的原生控件样式。

## 第一批练习

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

## 第二批练习

| App | 涉及的写法 | 一次自测 |
| --- | --- | --- |
| [账单分摊](app/src/bill/java/com/example/practice/Main.kt) | 十进制金额校验、以分为单位分配余数 | ¥100.00 分给 3 人，其中 1 人付 ¥33.34，另外 2 人各付 ¥33.33。 |
| [RGB 调色板](app/src/color/java/com/example/practice/Main.kt) | SeekBar、颜色预览、剪贴板、旋转恢复 | 三个滑块都移到最大，复制得到 `#FFFFFF`。 |
| [活动报名](app/src/event/java/com/example/practice/Main.kt) | 表单校验、日期选择器、Intent 和确认页 | 选择日期和活动，填写报名信息，在确认页核对。 |
| [每日打卡](app/src/habit/java/com/example/practice/Main.kt) | SharedPreferences、本地日期、连续天数 | 勾选今天和昨天，退出再打开，核对两个日期和连续天数。 |
| [待办事项](app/src/task/java/com/example/practice/Main.kt) | 截止日期、任务状态和筛选、本地存储 | 新建待办并完成，切到已完成筛选，再重开应用确认状态。 |
| [本地通讯录](app/src/contact/java/com/example/practice/Main.kt) | 联系人增删改查、搜索、ACTION_DIAL | 保存测试联系人并搜索，点击拨号，核对系统拨号盘上填入的号码。 |
| [离线图书目录](app/src/books/java/com/example/practice/Main.kt) | assets JSON、列表过滤、排序与详情 | 搜索一本书，切换排序，然后打开详情核对书名与作者。 |
| [城市天气](app/src/weather/java/com/example/practice/Main.kt) | HTTPS GET、解析 Open-Meteo JSON、加载与错误状态 | 切换预设城市，读取离线样例或主动获取实时天气。[Open-Meteo API 文档](https://open-meteo.com/en/docs) |
| [水平仪](app/src/sensor/java/com/example/practice/Main.kt) | SensorManager、监听生命周期、加速度可视化 | 改变设备倾斜角度，观察气泡和读数，或核对无传感器提示。 |
| [涂鸦板](app/src/draw/java/com/example/practice/Main.kt) | Canvas、触摸事件、ViewModel 和系统文件保存 | 画两笔，撤销一笔，再用系统文件选择器导出 PNG。 |

## 用指定版本打开

用 Android Studio **2025.1.3 或 2025.1.4** 打开本目录，安装 **Android SDK 36**，并把 **Gradle JDK** 设为 **17**。本工程固定 Kotlin **2.2.10**、JVM **17**、AGP **8.13.0** 和 Gradle **8.13**。最低支持 Android 7.0（API 24）。[Android Studio 兼容表](https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility)

在 **Build Variants** 面板为 `app` 选择对应的 Debug 变体，再按 **Run**。变体名和源码目录对应。例如 `waterDebug` 使用 `app/src/water/`。

```powershell
# 构建全部二十个 APK。
.\gradlew.bat :app:assembleDebug

# 想只练一题时，只构建对应变体。
.\gradlew.bat :app:assembleWaterDebug
```

构建产物位于 `app/build/outputs/apk/<变体>/debug/`。通过成品包安装时，`APKs/` 里有二十个调试包。第一次 Gradle 同步需要下载依赖，缓存齐全后可以用 `--offline` 构建。城市天气和复习卡片各有离线示例，也可主动请求网络数据。

## 用 GitHub 保存源码

在 GitHub 上创建空仓库，仓库可见性按课程要求设置。将当前工程目录推送到新仓库即可。`.gitignore` 会排除 APK、构建目录、SDK 本地路径和 IDE 缓存。把 GitHub 显示的仓库地址填到下面的命令中。[GitHub 本地代码导入说明](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)

```powershell
# 在包含 settings.gradle.kts 的目录执行。
git init -b main
git add .
git update-index --chmod=+x gradlew
git status --short
git commit -m "Add annotated Android practice apps"
git remote add origin 'https://github.com/YOUR-ACCOUNT/AndroidPractice.git'
git push -u origin main
```

# 构建与测试指南

## 📋 目录

1. [环境准备](#-环境准备)
2. [模块依赖关系](#-模块依赖关系)
3. [构建项目](#-构建项目)
4. [运行测试](#-运行测试)
5. [测试覆盖说明](#-测试覆盖说明)
6. [常见问题](#-常见问题)

---

## 🔧 环境准备

### 必需软件

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| Android Studio | Hedgehog (2023.1.1) | 推荐 Ladybug (2024.x) 或更高 |
| JDK | 17 | 使用 Android Studio 自带的 JetBrains Runtime 即可 |
| Gradle | 8.5 | wrapper 已内置，无需手动安装 |
| Android SDK | 34 (compileSdk) | 通过 SDK Manager 安装 |
| Kotlin | 1.9.22 | 由 Gradle 插件管理 |
| Android Gradle Plugin | 8.2.2 | 在 `build.gradle.kts` 中声明 |

### 推荐 Android Studio 插件

- **Hilt** 代码导航支持（内置）
- **Kotlin** 插件（内置）
- **Android APK Support**（内置）

### 检查环境

```bash
# 确认 Java 版本
java -version
# 输出示例：openjdk version "17.0.9" ...

# 确认 Android SDK 路径（在 local.properties 中）
# 文件内容示例：
# sdk.dir=C:\\Users\\<用户名>\\AppData\\Local\\Android\\Sdk
```

---

## 🧩 模块依赖关系

```
app
├── core:common          (无依赖)
├── core:database        └── core:common
├── feature:scanner      ├── core:common
│                        └── core:database
├── feature:duplicate    └── core:common
├── feature:fileops      ├── core:common
│                        └── core:database
├── feature:appupdate    └── core:common
└── feature:settings     ├── core:common
                         └── feature:fileops
```

**依赖原则**：
- `core` 层不依赖 `feature` 层
- `feature` 层可依赖 `core` 层和其它 `feature` 模块
- `app` 模块依赖所有模块，负责组装和路由

---

## 🏗️ 构建项目

### 首次构建

```bash
# 1. 进入项目根目录
cd PhotoCleaner

# 2. （可选）生成 local.properties
# 在 Android Studio 中打开项目会自动生成
# 或手动创建：
echo "sdk.dir=C:\\Users\\<用户名>\\AppData\\Local\\Android\\Sdk" > local.properties

# 3. 完整调试构建
./gradlew assembleDebug

# 4. 安装到设备
./gradlew installDebug
```

### 构建类型

| 构建类型 | 命令 | 特性 |
|----------|------|------|
| Debug | `./gradlew assembleDebug` | 未混淆，日志全开，包名后缀 `.debug` |
| Release | `./gradlew assembleRelease` | R8 混淆 + 资源压缩，日志仅 Error |

### 构建产物位置

```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk          # 调试包
└── release/
    └── app-release.apk        # 发布包（需签名）
```

### 常用 Gradle 命令

```bash
# 清理构建缓存
./gradlew clean

# 检查依赖树
./gradlew app:dependencies

# Lint 检查
./gradlew lint

# 生成 BuildConfig
./gradlew generateBuildConfig
```

---

## 🧪 运行测试

### 测试框架

- **JUnit 4** — 单元测试框架
- **AndroidX Test** — Android 测试扩展库

### 运行所有测试

```bash
# 所有模块的单元测试
./gradlew test

# 注意：Android 库模块的 on-device 测试需要模拟器或真机
./gradlew connectedCheck
```

### 按模块运行

```bash
# Core 通用模块
./gradlew :core:common:test

# Core 数据库模块（需要 Room 编译）
./gradlew :core:database:test

# 去重算法模块（核心测试）
./gradlew :feature:duplicate:test

# 文件操作模块
./gradlew :feature:fileops:test

# App 模块
./gradlew :app:test
```

### 按测试类运行

```bash
# 单个测试类
./gradlew :feature:duplicate:test --tests "*HammingDistanceMatcherTest*"

# 单个测试方法
./gradlew :feature:duplicate:test --tests "*HammingDistanceMatcherTest.testExactMatch*"

# 按包名过滤
./gradlew test --tests "com.photocleaner.feature.duplicate.*"
```

### 在 Android Studio 中运行测试

1. 打开项目，等待 Gradle 同步完成
2. 在项目视图中找到目标测试文件（位于 `src/test/` 目录）
3. 右键点击测试类或方法
4. 选择 **Run** 或按 `Ctrl+Shift+F10`
5. 查看测试结果在 Run 面板中

### 查看测试报告

```bash
# 生成 HTML 测试报告
./gradlew test

# 报告位置
# 路径：<模块>/build/reports/tests/testDebugUnitTest/index.html
# 例如：
#   core/common/build/reports/tests/testDebugUnitTest/index.html
#   feature/duplicate/build/reports/tests/testDebugUnitTest/index.html

# 在浏览器中打开查看详细结果
start feature/duplicate/build/reports/tests/testDebugUnitTest/index.html
```

---

## 📊 测试覆盖说明

### 现有测试清单

| 模块 | 测试文件 | 覆盖内容 |
|------|----------|----------|
| core:common | `HashUtilsTest.kt` | MD5/SHA-256 哈希计算正确性 |
| core:common | `SizeUtilsTest.kt` | 字节格式化：B/KB/MB/GB |
| core:common | `DateUtilsTest.kt` | 时间格式化、同一天判断 |
| core:common | `DatabasePlaceholderTest.kt` | 数据库基础连通性 |
| feature:duplicate | `DHashCalculatorTest.kt` | dHash 计算一致性、64位输出 |
| feature:duplicate | `UnionFindTest.kt` | 并查集初始化/合并/路径压缩/分组 |
| feature:duplicate | `HammingDistanceMatcherTest.kt` | 汉明距离计算、边界条件、异常输入 |
| feature:duplicate | `LshClusterTest.kt` | LSH 聚类逻辑、空/单/重复/差异输入 |
| feature:fileops | `DeleteResultTest.kt` | 删除结果数据模型验证 |
| app | `ExampleUnitTest.kt` | 应用级占位测试 |

### 测试要点

- **算法测试**（dHash、汉明距离、并查集）不依赖 Android 框架，可在 JVM 上运行
- **Bitmap 测试** 通过 `Bitmap.createBitmap()` 构造测试图片，无需真实文件
- **DAO 测试** 需要 Room 编译（`kapt`/`ksp`），建议在 Android Studio 中运行
- **UI 测试** 需要 `androidx.compose.ui:ui-test-junit4`，建议使用模拟器

### 添加新测试

1. 在目标模块的 `src/test/java/` 下创建测试类
2. 使用 `@Test` 注解标记测试方法
3. 在模块的 `build.gradle.kts` 中添加测试依赖（如已包含 JUnit）
4. 运行验证

---

## ❓ 常见问题

### Q: Gradle 构建失败，提示 SDK 未找到

**原因**：`local.properties` 中未配置 SDK 路径。

**解决**：
1. 在 Android Studio 中打开项目，会自动生成
2. 或手动创建 `local.properties`：
   ```properties
   sdk.dir=C:\\Users\\用户名\\AppData\\Local\\Android\\Sdk
   ```

### Q: Room 编译错误（`kapt`/`ksp` 相关）

**原因**：注解处理器未正确配置。

**解决**：
```bash
# 清理并重新构建
./gradlew clean
./gradlew assembleDebug
```

### Q: 测试时出现 `Method ... not mocked`

**原因**：Android 框架方法在单元测试中不可用。

**解决**：在 `app/build.gradle.kts` 中添加：
```kotlin
android {
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}
```

### Q: Hilt 注入错误

**原因**：Hilt 组件未正确生成。

**解决**：
1. 确保 `@HiltAndroidApp` 注解在 `Application` 类上
2. 确保 `@AndroidEntryPoint` 注解在 `Activity` 上
3. 执行一次完整构建
4. 检查 `build/generated/source/ksp/` 目录下是否生成了 Hilt 组件

### Q: Windows PowerShell 中无法使用 `./gradlew`

**原因**：PowerShell 不识别 Unix 风格的路径。

**解决**：
```powershell
# 使用 PowerShell 执行
.\gradlew assembleDebug

# 或在 CMD 中执行
gradlew assembleDebug
```

### Q: 如何调试去重算法？

**建议**：
1. 在 `DetectDuplicateUseCase` 中设置断点
2. 使用少量测试图片（5-10 张）运行扫描
3. 在 Logcat 中过滤 `PhotoCleaner` 标签查看日志
4. 利用单元测试快速验证算法逻辑

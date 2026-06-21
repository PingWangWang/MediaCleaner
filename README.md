# 清图大师 PhotoCleaner

一键去重，释放存储空间。

> 纯本地、轻量、高精度的安卓图片文件去重工具。

---

## 项目简介

PhotoCleaner（清图大师）是一款完全离线的 Android 图片去重应用。它通过多层感知哈希算法（dHash → pHash → ORB）精准识别完全重复和高度相似的图片，帮助用户安全清理冗余文件，释放存储空间。

**核心价值**
- 🔒 **纯本地运算** — 所有扫描、对比、删除操作均在本地完成，不上传任何数据
- ⚡ **高速扫描** — 10000 张图片 ≤ 8 秒出首屏结果，50000 张 ≤ 90 秒
- 🎯 **高精度识别** — 完全重复 100%，相似图片 ≥ 98%
- 🛡️ **安全回收站** — 删除的文件进入回收站，30 天内可恢复
- 📱 **广泛适配** — Android 8.0 ~ 14+，分区存储完整支持

## 技术架构

### 模块结构

```
PhotoCleaner/
├── app/                          # 主应用模块（单 Activity + Compose UI）
├── core/
│   ├── common/                   # 通用基础：基类、扩展、工具、常量、模型
│   └── database/                 # Room 数据库：实体、DAO、转换器
└── feature/
    ├── scanner/                  # 图片扫描：MediaStore / SAF 双通道
    ├── duplicate/                # 去重算法核心（5 层流水线）
    ├── fileops/                  # 文件操作：删除、回收站、恢复
    ├── appupdate/                # 应用内升级：检测、下载、安装
    └── settings/                 # 设置：DataStore 偏好存储、UI
```

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 语言 | 100% Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture + 多模块 |
| 异步 | Kotlin Coroutines + Flow |
| DI | Hilt |
| 数据库 | Room 2.6+ |
| 图片加载 | Coil 2.5+ |
| 网络 | OkHttp 4.12+ |
| 任务调度 | WorkManager |
| 构建 | Gradle Kotlin DSL + KSP |

### 去重算法流水线

```
图片输入 → Layer 1: 大小/比例分桶预过滤
         → Layer 2: dHash + LSH 快速聚类 (O(n))
         → Layer 3: Union-Find 相似图（防链式误报）
         → Layer 4: pHash 边界检查（灰色地带重评估）
         → Layer 5: ORB 精细化匹配（三门控条件）
         → 结果输出 (DuplicateGroup Flow)
```

## 快速开始

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更新版本
- **JDK** 17+
- **Gradle** 8.5 (wrapper 已包含)
- **Android SDK** 34

### 构建

```bash
# 克隆项目
git clone <repo-url>
cd PhotoCleaner

# 调试构建
./gradlew assembleDebug

# 发布构建（含混淆和资源压缩）
./gradlew assembleRelease
```

### 运行测试

```bash
# 运行所有单元测试
./gradlew test

# 运行特定模块测试
./gradlew :core:common:test
./gradlew :feature:duplicate:test

# 运行特定测试类
./gradlew :feature:duplicate:test --tests "*HammingDistanceMatcherTest*"
```

## 详细文档

参见 [docs/](docs/) 目录：
- [构建与测试指南](docs/build-guide.md)
- 详细设计方案 `PhotoCleaner详细设计方案.md`

## 版本规划

| 版本 | 功能 | 状态 |
|------|------|------|
| V1.0 MVP | 完全重复检测、相似图片检测、回收站、基础设置 | 🚧 开发中 |
| V1.1 | 相似截图清理、模糊图片检测、微信/QQ 专项扫描 | 📅 规划中 |
| V1.2 | 大文件清理、截图智能分类、批量导出备份 | 📅 规划中 |

## 许可证

[MIT](LICENSE)

# PhotoMaster 📸

Android 图片管理 App — 基于系统 MediaStore，按规则自动分类每日图片。

## 功能

### 自动分类（每天最多 3 个文件夹）
| 文件夹名格式 | 内容 |
|------------|------|
| `拍摄_{yyyy-MM-dd}` | 相机拍摄照片 |
| `{yyyy-MM-dd}_截图` | 截图 |
| `视频_{yyyy-MM-dd}` | 相机录制视频 |

### 三方 App 图片
- `{AppName}_图片`：微信、微博等 App 的图片，按应用聚合

### 自建文件夹
- 自由命名，从任意文件夹选图移入或复制进来

### 操作
- 长按进入多选模式
- 支持：移动 / 复制 / 删除（直接删除手机上的图片）

## 技术栈

- **Kotlin** + Jetpack Compose
- **MVVM + Clean Architecture**
- **Hilt** DI, **Room** 本地数据库, **Coil** 图片加载
- **MediaStore API** 访问系统相册（API 26+）

## 构建

```bash
./gradlew assembleDebug
```

需要 Android Studio Hedgehog (2023.1.1) 或更高版本，JDK 17+。

## 注意

> 删除图片 = 从手机相册永久删除，非备份副本。App 内操作通过系统 MediaStore 执行，效果与系统相册一致。

## 权限
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`（API 33+）
- `READ_EXTERNAL_STORAGE`（API 26–32）
- 删除操作 API 30+ 需通过系统弹窗二次确认

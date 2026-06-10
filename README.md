<div align="center">

# 📸 PhotoMaster

**Android 本地图片管家 · 智能分类 · 局域网传图**

[![Latest Release](https://img.shields.io/github/v/release/magusLu/PhotoManager?style=flat-square&label=下载最新版)](https://github.com/magusLu/PhotoManager/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?style=flat-square&logo=android)](https://github.com/magusLu/PhotoManager)
[![License](https://img.shields.io/github/license/magusLu/PhotoManager?style=flat-square)](LICENSE)

</div>

---

## ✨ 功能一览

### 🗂️ 智能自动分类

打开 App 后，手机里的图片会自动按内容类型 + 日期分好组，无需手动整理。

| 文件夹 | 内容 | 示例 |
|--------|------|------|
| `拍摄_2024-06-10` | 相机拍摄的照片 | DCIM/Camera 下的图片 |
| `截图_2024-06-10` | 屏幕截图 | 系统截图目录 |
| `视频_2024-06-10` | 相机录制的视频 | DCIM/Camera 下的视频 |
| `图片_微信` | 微信保存的图片 | 按 App 聚合 |
| `图片_微博` | 微博保存的图片 | 按 App 聚合 |

> 每天自动生成，无需配置。

---

### 📁 自建文件夹

除自动分类外，你可以随意创建自己的文件夹，把任意图片归入其中。

- **创建**任意名称的文件夹
- **重命名** / **删除**文件夹
- 从任意分类文件夹中**移入**或**复制**图片进来

---

### ✅ 多选批量操作

长按任意图片，进入多选模式：

| 操作 | 说明 |
|------|------|
| 📦 移动 | 将选中图片移入另一个文件夹 |
| 📋 复制 | 复制一份到目标文件夹，原图保留 |
| 🗑️ 删除 | 从手机相册**永久删除**（等同系统相册删除） |

> ⚠️ 删除操作不可恢复。Android 11+ 会弹出系统二次确认弹窗。

---

### 📡 局域网传图到电脑

不需要数据线，不需要第三方软件，直接在浏览器下载。

1. 手机和电脑连同一个 Wi-Fi
2. 打开 App → 「传到电脑」
3. 点击「开启传输服务」，生成二维码和访问地址
4. 电脑浏览器扫码 / 输入地址，即可浏览并下载图片
5. 传完点「关闭服务」

---

## 📥 下载安装

前往 [Releases 页面](https://github.com/magusLu/PhotoManager/releases/latest) 下载最新 APK，直接安装即可。

> 需要允许「安装未知来源应用」权限。

---

## 🔐 权限说明

| 权限 | 用途 |
|------|------|
| 读取媒体文件（图片/视频） | 扫描手机相册 |
| 写入外部存储（Android 8） | 移动 / 复制图片 |
| 网络访问 | 局域网传输服务 |
| Wi-Fi 状态 | 获取本机 IP 地址用于生成传输链接 |

> App **不上传任何数据**到外部服务器，所有操作均在本地或局域网内完成。

---

## 📋 系统要求

- Android 8.0（API 26）及以上
- 需要授予相册读取权限


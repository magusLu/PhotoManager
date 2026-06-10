package com.photomaster.app.domain.model

enum class FolderType {
    /** 每日拍摄_yyyy-MM-dd */
    CAMERA,
    /** 每日截图 截图_yyyy-MM-dd */
    SCREENSHOT,
    /** 每日视频 视频_yyyy-MM-dd */
    VIDEO,
    /** 第三方应用图片 图片_{AppName} */
    THIRD_PARTY,
    /** 用户自建文件夹 */
    CUSTOM
}

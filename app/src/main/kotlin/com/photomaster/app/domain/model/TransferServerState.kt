package com.photomaster.app.domain.model

import android.graphics.Bitmap

/**
 * 局域网传输服务器运行状态。
 *
 * @param isRunning    服务器是否正在运行
 * @param ipAddress    手机局域网 IP（如 192.168.1.100）
 * @param port         监听端口（默认 8085）
 * @param itemCount    当前服务的图片/视频数量
 * @param qrBitmap     二维码位图（指向 http://ipAddress:port）
 */
data class TransferServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val port: Int = SERVER_PORT,
    val itemCount: Int = 0,
    val qrBitmap: Bitmap? = null,
    val error: String? = null,
) {
    val url: String get() = if (ipAddress.isNotBlank()) "http://$ipAddress:$port" else ""

    companion object {
        const val SERVER_PORT = 8085
    }
}

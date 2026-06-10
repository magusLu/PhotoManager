package com.photomaster.app.ui.transfer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.photomaster.app.data.transfer.LanTransferServer
import com.photomaster.app.domain.model.MediaItem
import com.photomaster.app.domain.model.TransferServerState
import com.photomaster.app.domain.usecase.ClassifyMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@HiltViewModel
class LanTransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classifyMediaUseCase: ClassifyMediaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferServerState())
    val state: StateFlow<TransferServerState> = _state.asStateFlow()

    private var server: LanTransferServer? = null
    private var cachedItems: List<MediaItem> = emptyList()

    // ── Public actions ────────────────────────────────────────────────────────

    fun startServer() {
        if (_state.value.isRunning) return
        viewModelScope.launch {
            try {
                // 1. 加载全部媒体
                val folders = classifyMediaUseCase()
                cachedItems = folders.flatMap { it.items }.distinctBy { it.id }

                // 2. 获取 WiFi IP
                val ip = getLocalWifiIp()
                if (ip.isBlank()) {
                    _state.update { it.copy(error = "未连接 WiFi，请先连接 WiFi") }
                    return@launch
                }

                // 3. 启动 NanoHTTPD
                val port = TransferServerState.SERVER_PORT
                server = LanTransferServer(port, context) { cachedItems }
                server!!.start()

                // 4. 生成二维码
                val url = "http://$ip:$port"
                val qr = generateQrBitmap(url)

                _state.update {
                    TransferServerState(
                        isRunning = true,
                        ipAddress = ip,
                        port = port,
                        itemCount = cachedItems.size,
                        qrBitmap = qr,
                    )
                }
            } catch (e: Exception) {
                stopServer()
                _state.update { it.copy(error = "启动失败：${e.message}") }
            }
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        _state.update { it.copy(isRunning = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        stopServer()
        super.onCleared()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * 获取当前 WiFi 网卡的 IPv4 地址。
     * 优先返回 wlan0 接口，次之返回任意非回环 IPv4。
     */
    private fun getLocalWifiIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            val candidates = mutableListOf<String>()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr.isLoopbackAddress) continue
                    if (addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        // 优先 wlan 接口
                        if (intf.name.startsWith("wlan", ignoreCase = true) ||
                            intf.name.startsWith("eth", ignoreCase = true)
                        ) return ip
                        candidates += ip
                    }
                }
            }
            return candidates.firstOrNull() ?: ""
        } catch (e: Exception) {
            return ""
        }
    }

    /** ZXing 生成 400×400 二维码位图 */
    private fun generateQrBitmap(text: String, size: Int = 400): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
        )
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}

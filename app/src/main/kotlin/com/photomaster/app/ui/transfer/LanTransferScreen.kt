package com.photomaster.app.ui.transfer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanTransferScreen(
    onBack: () -> Unit,
    viewModel: LanTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // 错误提示
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("传到电脑") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isRunning) viewModel.stopServer()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── 状态卡片 ──────────────────────────────────────────────────────
            StatusCard(
                isRunning = state.isRunning,
                itemCount = state.itemCount,
            )

            // ── 二维码 / 占位 ──────────────────────────────────────────────────
            QrSection(state = state)

            // ── URL + 复制按钮 ─────────────────────────────────────────────────
            if (state.isRunning && state.url.isNotBlank()) {
                UrlRow(
                    url = state.url,
                    onCopy = {
                        clipboard.setText(AnnotatedString(state.url))
                        Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── 操作按钮 ──────────────────────────────────────────────────────
            ActionButton(
                isRunning = state.isRunning,
                onStart = { viewModel.startServer() },
                onStop = { viewModel.stopServer() },
            )

            // ── 使用说明 ──────────────────────────────────────────────────────
            InstructionCard()
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun StatusCard(isRunning: Boolean, itemCount: Int) {
    val (bg, icon, label, sublabel) = if (isRunning) {
        arrayOf(
            Color(0xFF1B5E20),
            Icons.Filled.WifiTethering,
            "传输服务已开启",
            "共 $itemCount 个文件可下载",
        )
    } else {
        arrayOf(
            Color(0xFF1C1C2E),
            Icons.Outlined.WifiOff,
            "传输服务未开启",
            "点击下方按钮启动服务",
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg as Color,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(label as String, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    sublabel as String,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = .7f),
                )
            }
        }
    }
}

@Composable
private fun QrSection(state: com.photomaster.app.domain.model.TransferServerState) {
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isRunning && state.qrBitmap != null -> {
                Image(
                    bitmap = state.qrBitmap.asImageBitmap(),
                    contentDescription = "扫码在电脑浏览器打开",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp),
                        ),
                )
            }
            state.isRunning -> {
                CircularProgressIndicator()
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "启动服务后\n将显示二维码",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    if (state.isRunning) {
        Text(
            "用电脑摄像头扫码，或在浏览器输入下方地址",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UrlRow(url: String, onCopy: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                url,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "复制链接",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    if (isRunning) {
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.error
            ),
        ) {
            Text("关闭传输服务", fontSize = 15.sp)
        }
    } else {
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Icon(Icons.Filled.WifiTethering, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("开启传输服务", fontSize = 15.sp)
        }
    }
}

@Composable
private fun InstructionCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("使用步骤", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            listOf(
                "① 确保手机与电脑连接同一 WiFi",
                "② 点击「开启传输服务」",
                "③ 用电脑浏览器打开二维码中的地址",
                "④ 在网页中浏览并下载图片",
                "⑤ 传输完成后点击「关闭传输服务」",
            ).forEach { step ->
                Text(step, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

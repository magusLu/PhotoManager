package com.photomaster.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photomaster.app.data.local.entity.CleanRuleEntity
import com.photomaster.app.domain.model.FolderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanRuleScreen(
    onBack: () -> Unit,
    viewModel: CleanRuleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自动清理设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, "添加规则")
            }
        }
    ) { innerPadding ->
        if (uiState.rules.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无自动清理规则", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击右下角 + 添加规则",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "定时清理规则将在每天凌晨 2:00 自动执行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(uiState.rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onToggle = { viewModel.toggleRule(rule) },
                        onDelete = { viewModel.deleteRule(rule) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onConfirm = { type, schedule, weekDay, monthDay ->
                viewModel.addRule(type, schedule, weekDay, monthDay)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun RuleCard(
    rule: CleanRuleEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val folderTypeLabel = when (rule.folderType) {
        FolderType.CAMERA.name -> "📷 相机照片"
        FolderType.SCREENSHOT.name -> "🖼️ 截图"
        FolderType.VIDEO.name -> "🎬 视频"
        FolderType.THIRD_PARTY.name -> "📱 第三方 App 图片"
        FolderType.CUSTOM.name -> "📁 自建文件夹"
        else -> rule.folderType
    }
    val scheduleLabel = when (rule.scheduleType) {
        "DAILY" -> "每天"
        "WEEKLY" -> "每周${weekDayName(rule.weekDay)}"
        "MONTHLY" -> "每月 ${rule.monthDay} 号"
        else -> rule.scheduleType
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    folderTypeLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (rule.enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    scheduleLabel,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (rule.enabled) 1f else 0.4f
                    ),
                )
                if (rule.lastRunAt > 0) {
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    Text(
                        "上次执行：${fmt.format(rule.lastRunAt)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(start = 8.dp),
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除规则",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    onConfirm: (FolderType, ScheduleType, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFolderType by remember { mutableStateOf(FolderType.CAMERA) }
    var selectedSchedule by remember { mutableStateOf(ScheduleType.DAILY) }
    var selectedWeekDay by remember { mutableStateOf(1) }   // 1=周一
    var selectedMonthDay by remember { mutableStateOf(1) }  // 1号

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建清理规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // 文件夹类型选择
                Text("清理类型", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                val folderTypes = listOf(
                    FolderType.CAMERA to "📷 相机照片",
                    FolderType.SCREENSHOT to "🖼️ 截图",
                    FolderType.VIDEO to "🎬 视频",
                    FolderType.THIRD_PARTY to "📱 第三方 App 图片",
                    FolderType.CUSTOM to "📁 自建文件夹",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    folderTypes.chunked(3).forEach { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (type, label) ->
                                FilterChip(
                                    selected = selectedFolderType == type,
                                    onClick = { selectedFolderType = type },
                                    label = { Text(label, fontSize = 12.sp) },
                                )
                            }
                        }
                    }
                }

                Divider()

                // 调度类型
                Text("执行频率", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleType.entries.forEach { s ->
                        FilterChip(
                            selected = selectedSchedule == s,
                            onClick = { selectedSchedule = s },
                            label = { Text(s.label) },
                        )
                    }
                }

                // 周几 / 几号
                when (selectedSchedule) {
                    ScheduleType.WEEKLY -> {
                        Text("执行星期", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            val days = listOf("一", "二", "三", "四", "五", "六", "日")
                            days.forEachIndexed { idx, name ->
                                val day = idx + 1
                                FilterChip(
                                    selected = selectedWeekDay == day,
                                    onClick = { selectedWeekDay = day },
                                    label = { Text("周$name", fontSize = 11.sp) },
                                )
                            }
                        }
                    }

                    ScheduleType.MONTHLY -> {
                        Text("执行日期（每月几号）", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        val days = (1..28).toList()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.chunked(7).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { day ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (selectedMonthDay == day)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(8.dp),
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            TextButton(
                                                onClick = { selectedMonthDay = day },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.fillMaxSize(),
                                            ) {
                                                Text(
                                                    "$day",
                                                    fontSize = 12.sp,
                                                    color = if (selectedMonthDay == day)
                                                        MaterialTheme.colorScheme.onPrimary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                // 警告
                Text(
                    "⚠️ 执行时将永久删除匹配文件夹内所有图片，请谨慎操作",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    selectedFolderType,
                    selectedSchedule,
                    if (selectedSchedule == ScheduleType.WEEKLY) selectedWeekDay else 0,
                    if (selectedSchedule == ScheduleType.MONTHLY) selectedMonthDay else 0,
                )
            }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun weekDayName(day: Int) = listOf("", "一", "二", "三", "四", "五", "六", "日").getOrElse(day) { "$day" }

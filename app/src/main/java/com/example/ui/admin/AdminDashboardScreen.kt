package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserVipProfile
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.UserFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    currentUser: UserVipProfile,
    onBackToUserApp: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allUsers by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val filteredUsers by adminViewModel.filteredUsers.collectAsStateWithLifecycle()
    val searchQuery by adminViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by adminViewModel.selectedFilter.collectAsStateWithLifecycle()
    val actionMessage by adminViewModel.adminActionMessage.collectAsStateWithLifecycle()
    val actionIsError by adminViewModel.adminActionIsError.collectAsStateWithLifecycle()
    val isUploadingDataset by adminViewModel.isUploadingDataset.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showUploadDialog by remember { mutableStateOf(false) }

    val pendingCount = allUsers.count { !it.isVip }
    val vipCount = allUsers.count { it.isVip }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberPending),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = Color(0xFF78350F),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "لوحة تحكم المسؤول (Admin)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Firebase Cloud Firestore Live Management",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onBackToUserApp,
                        modifier = Modifier.testTag("admin_back_to_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "عرض واجهة المستخدم",
                            tint = EmeraldSuccess
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("admin_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = RoseError
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action Banner
            AnimatedVisibility(
                visible = actionMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                actionMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (actionIsError) RoseError.copy(alpha = 0.22f) else Color(0xFF065F46))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                color = if (actionIsError) Color(0xFFFFCDD2) else Color(0xFFA7F3D0),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { adminViewModel.clearActionMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Stats Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "إجمالي المسجلين",
                    value = allUsers.size.toString(),
                    color = IndigoSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "في الانتظار",
                    value = pendingCount.toString(),
                    color = AmberPending,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "VIP مفعّلون",
                    value = vipCount.toString(),
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs: User Management / Cloud Dataset Management
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المستخدمون والـ VIP (${allUsers.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("رفع قاعدة بيانات JSON", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // User Management View
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    // Search & Filters
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { adminViewModel.onSearchQueryChange(it) },
                        placeholder = { Text("بحث عن مستخدم بالبريد أو الاسم...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = IndigoSecondary,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == UserFilter.ALL,
                            onClick = { adminViewModel.setFilter(UserFilter.ALL) },
                            label = { Text("الكل (${allUsers.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoSecondary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == UserFilter.PENDING_ONLY,
                            onClick = { adminViewModel.setFilter(UserFilter.PENDING_ONLY) },
                            label = { Text("في الانتظار ($pendingCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPending,
                                selectedLabelColor = Color(0xFF78350F)
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == UserFilter.VIP_ONLY,
                            onClick = { adminViewModel.setFilter(UserFilter.VIP_ONLY) },
                            label = { Text("VIP فقط ($vipCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSuccess,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا يوجد مستخدمون مطابقون",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredUsers, key = { it.uid }) { user ->
                                UserAdminCard(
                                    user = user,
                                    onToggleVip = { enable ->
                                        adminViewModel.toggleUserVip(user, enable)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Cloud Dataset Management View
                AdminCloudDatasetView(
                    isUploading = isUploadingDataset,
                    onUploadJson = { jsonString ->
                        adminViewModel.uploadNewDatasetToCloud(jsonString)
                    }
                )
            }
        }
    }
}

@Composable
private fun UserAdminCard(
    user: UserVipProfile,
    onToggleVip: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val regDate = dateFormat.format(Date(user.registeredAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (user.isVip) EmeraldSuccess.copy(alpha = 0.2f)
                            else AmberPending.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (user.isVip) Icons.Default.Stars else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isVip) EmeraldSuccess else AmberPending,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName.ifBlank { user.email.substringBefore("@") },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(IndigoSecondary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Admin", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = if (user.isVip) EmeraldSuccess else AmberPending,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (user.isVip) "حساب VIP مفعّل" else "في الانتظار (Pending)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isVip) EmeraldSuccess else AmberPending
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $regDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive VIP Toggle Switch & Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (user.isVip) "VIP نشط" else "تفعيل VIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isVip) EmeraldSuccess else Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = user.isVip,
                    onCheckedChange = { onToggleVip(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldSuccess,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("toggle_vip_${user.uid.take(6)}")
                )
            }
        }
    }
}

@Composable
private fun AdminCloudDatasetView(
    isUploading: Boolean,
    onUploadJson: (String) -> Unit
) {
    var jsonText by remember {
        mutableStateOf(
            """{
  "_version": "3.2",
  "_fields": {
    "pois": ["name", "alternativeNames", "localizedNames", "coordinates", "address", "type"],
    "streets": ["name", "alternativeNames", "coordinates", "region"],
    "streetJunctions": ["streetRef", "coordinates"]
  },
  "pois": [
    [
      "محطة تونس المركزية الكبرى",
      ["Tunis Central Main Hub"],
      {"ar": "محطة تونس المركزية", "fr": "Gare Centrale de Tunis"},
      [10.180421, 36.795821],
      "تونس العاصمة",
      "محطة قطارات ومترو رئيسية"
    ],
    [
      "جامع الزيتونة المعمور - تونس",
      ["Al-Zaytuna Mosque"],
      {"ar": "جامع الزيتونة", "fr": "Grande Mosquée Zitouna"},
      [10.171111, 36.797778],
      "المدينة العتيقة",
      "معلم ديني وسياحي"
    ],
    [
      "شارع الحبيب بورقيبة المركزي",
      ["Avenue Habib Bourguiba"],
      {"ar": "شارع بورقيبة", "fr": "Av. Bourguiba"},
      [10.1815316, 36.800185],
      "وسط تونس",
      "شريان العاصمة"
    ]
  ],
  "streets": [
    [
      "شارع الحبيب بورقيبة",
      ["Habib Bourguiba"],
      [[10.17421, 36.79981], [10.18012, 36.80020], [10.18560, 36.80060], [10.19124, 36.80054]],
      "تونس الوسطى"
    ]
  ],
  "streetJunctions": [
    ["تقاطع شارع بورقيبة مع شارع محمد الخامس", [10.18560, 36.80060]]
  ]
}"""
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Cloud Upload",
                        tint = IndigoSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "رفع وتحديث قاعدة البيانات السحابية (Cloud Broadcast)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "سيتم نشر البيانات في Firestore ومزامنتها لجميع مستخدمي الـ VIP فوراً",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("محتوى الـ JSON المضغوط للشبكة") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .testTag("admin_json_editor"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFA7F3D0),
                        unfocusedTextColor = Color(0xFFA7F3D0),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = IndigoSecondary,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onUploadJson(jsonText) },
                    enabled = !isUploading && jsonText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_publish_dataset_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نشر في Firestore وتحديث هواتف جميع الـ VIP",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

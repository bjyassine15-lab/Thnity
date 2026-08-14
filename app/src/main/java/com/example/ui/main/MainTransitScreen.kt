package com.example.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.Poi
import com.example.data.model.Street
import com.example.data.model.StreetJunction
import com.example.data.model.UserVipProfile
import com.example.ui.components.TransitMapView
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.TransitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTransitScreen(
    transitViewModel: TransitViewModel,
    userProfile: UserVipProfile,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pois by transitViewModel.allPois.collectAsStateWithLifecycle()
    val filteredPois by transitViewModel.filteredPois.collectAsStateWithLifecycle()
    val streets by transitViewModel.allStreets.collectAsStateWithLifecycle()
    val junctions by transitViewModel.allJunctions.collectAsStateWithLifecycle()
    val selectedPoi by transitViewModel.selectedPoi.collectAsStateWithLifecycle()
    val searchQuery by transitViewModel.searchQuery.collectAsStateWithLifecycle()
    val isSyncing by transitViewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by transitViewModel.statusMessage.collectAsStateWithLifecycle()

    val poiCount by transitViewModel.poiCount.collectAsStateWithLifecycle()
    val streetCount by transitViewModel.streetCount.collectAsStateWithLifecycle()

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showPoiDetailsSheet by remember { mutableStateOf(false) }

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
                                .background(
                                    Brush.linearGradient(listOf(IndigoPrimary, IndigoSecondary))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "شبكة النقل التونسية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSuccess)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VIP نشط",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "مستخدم: ${userProfile.displayName.ifBlank { userProfile.email }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Cloud Sync Button
                    IconButton(
                        onClick = { transitViewModel.syncFromCloud() },
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("sync_cloud_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "مزامنة سحابية",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // If User is an Admin, show direct shortcut to Admin Panel
                    if (userProfile.isAdmin) {
                        IconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.testTag("open_admin_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "لوحة تحكم المشرف",
                                tint = AmberPending
                            )
                        }
                    }

                    // Sign Out Button
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("user_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = RoseError
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("الخريطة الحية") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndigoSecondary,
                        selectedTextColor = IndigoSecondary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = IndigoPrimary.copy(alpha = 0.3f)
                    )
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = { Icon(Icons.Default.Place, contentDescription = "POIs") },
                    label = { Text("المحطات ($poiCount)") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndigoSecondary,
                        selectedTextColor = IndigoSecondary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = IndigoPrimary.copy(alpha = 0.3f)
                    )
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 },
                    icon = { Icon(Icons.Default.Route, contentDescription = "Streets") },
                    label = { Text("المسارات ($streetCount)") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndigoSecondary,
                        selectedTextColor = IndigoSecondary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = IndigoPrimary.copy(alpha = 0.3f)
                    )
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 3,
                    onClick = { selectedNavIndex = 3 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Encrypted DB") },
                    label = { Text("التشفير والسحابة") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndigoSecondary,
                        selectedTextColor = IndigoSecondary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = IndigoPrimary.copy(alpha = 0.3f)
                    )
                )
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavIndex) {
                0 -> {
                    // Interactive Map Screen
                    TransitMapView(
                        pois = pois,
                        streets = streets,
                        selectedPoi = selectedPoi,
                        onPoiClick = { poi ->
                            transitViewModel.selectPoi(poi)
                            showPoiDetailsSheet = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // POIs Directory Screen
                    PoisDirectoryView(
                        pois = filteredPois,
                        searchQuery = searchQuery,
                        onSearchChange = { transitViewModel.onSearchQueryChange(it) },
                        onPoiSelect = { poi ->
                            transitViewModel.selectPoi(poi)
                            selectedNavIndex = 0 // jump to map
                        }
                    )
                }
                2 -> {
                    // Streets and Junctions Screen
                    StreetsDirectoryView(
                        streets = streets,
                        junctions = junctions
                    )
                }
                3 -> {
                    // SQLCipher Encrypted Storage & Cloud Sync View
                    EncryptedStorageView(
                        poiCount = poiCount,
                        streetCount = streetCount,
                        userProfile = userProfile,
                        onSyncCloud = { transitViewModel.syncFromCloud() },
                        onResetDefault = { transitViewModel.resetToDefaultData() }
                    )
                }
            }

            // Status message toast banner
            AnimatedVisibility(
                visible = statusMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                statusMessage?.let { msg ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { transitViewModel.clearStatusMessage() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }

    // POI Details Bottom Sheet
    if (showPoiDetailsSheet && selectedPoi != null) {
        val poi = selectedPoi!!
        ModalBottomSheet(
            onDismissRequest = { showPoiDetailsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = poi.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        poi.type?.let {
                            Text(text = it, style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                poi.address?.let {
                    Text(text = "العنوان: $it", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "الإحداثيات: [${poi.latitude}, ${poi.longitude}]",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                if (poi.localizedNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "الترجمات والأسماء البديلة:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    poi.localizedNames.forEach { (lang, text) ->
                        Text(text = "• $lang: $text", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showPoiDetailsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary)
                ) {
                    Text("إغلاق والعودة إلى الخريطة", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PoisDirectoryView(
    pois: List<Poi>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPoiSelect: (Poi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("بحث في المحطات ونقاط الاهتمام...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_poi_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IndigoSecondary,
                unfocusedBorderColor = Color(0xFF334155)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(pois) { poi ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPoiSelect(poi) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF818CF8))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = poi.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            poi.address?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Icon(Icons.Default.Map, contentDescription = "Show on map", tint = IndigoSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreetsDirectoryView(
    streets: List<Street>,
    junctions: List<StreetJunction>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "مسارات الطرق وشبكة السكك (${streets.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        items(streets) { street ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = IndigoSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = street.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    street.region?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "المنطقة: $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8))
                    }
                    Text(
                        text = "عدد الإحداثيات الجغرافية: ${street.coordinates.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "التقاطعات والمحاور الرئيسية (${junctions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        items(junctions) { junction ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = AmberPending)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = junction.streetRef,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Text(
                            text = "الإحداثيات: [${junction.latitude}, ${junction.longitude}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncryptedStorageView(
    poiCount: Int,
    streetCount: Int,
    userProfile: UserVipProfile,
    onSyncCloud: () -> Unit,
    onResetDefault: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "قاعدة البيانات المشفرة (SQLCipher 256-bit AES)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "تخزين محلي آمن ومحمي بالمفتاح السري",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "• المحطات المخزنة والمشفرة: $poiCount", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodyMedium)
                Text(text = "• المسارات المخزنة والمشفرة: $streetCount", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodyMedium)
                Text(text = "• معرّف المستخدم (UID): ${userProfile.uid}", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSyncCloud,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مزامنة فورية مع Cloud Firestore", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onResetDefault,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعادة تحميل البيانات النموذجية", color = Color.White)
                }
            }
        }
    }
}

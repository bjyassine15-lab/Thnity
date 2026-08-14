package com.example.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserVipProfile
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PendingVipScreen(
    userProfile: UserVipProfile,
    onSignOut: () -> Unit,
    onSimulateAdminApproval: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dateFormat = SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale("ar"))
    val regDate = dateFormat.format(Date(userProfile.registeredAt))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Pending Badge Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AmberPending, Color(0xFFF59E0B)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockPerson,
                        contentDescription = "Pending VIP",
                        tint = Color(0xFF78350F),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الحساب قيد مراجعة وتفعيل الأدمن",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = userProfile.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberPending,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Firestore Sync Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Live",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المراقب اللحظي نشط: سيفتح التطبيق فور تفعيل حسابك من الأدمن",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // User details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow(label = "الاسم المسجل:", value = userProfile.displayName)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "البريد الإلكتروني:", value = userProfile.email)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "معرف المستخدم (UID):", value = userProfile.uid.take(16) + "...")
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "تاريخ التسجيل:", value = regDate)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "حالة VIP:", value = "غير مفعّل (Pending)")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Demo Simulator Option (For quick testing without external dashboard)
                Text(
                    text = "أدوات الفحص السريع (Simulation):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSimulateAdminApproval,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("simulate_vip_approval_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "محاكاة قبول الأدمن الفوري (اختبار محلي)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sign out button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_pending_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoseError
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = RoseError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج والعودة", color = RoseError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}

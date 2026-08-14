package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val email by authViewModel.emailInput.collectAsStateWithLifecycle()
    val password by authViewModel.passwordInput.collectAsStateWithLifecycle()
    val displayName by authViewModel.displayNameInput.collectAsStateWithLifecycle()
    val isRegisterMode by authViewModel.isRegisterMode.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(IndigoPrimary, IndigoSecondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Transit Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "منظومة النقل الذكي",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = "Firebase Auth + Cloud Firestore + SQLCipher",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Firebase Status Banner
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF334155))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Firebase Connected",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "متصل بمشروع Firebase: thnity-e2d02",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tabs: Login / Register
                TabRow(
                    selectedTabIndex = if (isRegisterMode) 1 else 0,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = !isRegisterMode,
                        onClick = { authViewModel.setRegisterMode(false) },
                        modifier = Modifier.testTag("tab_login"),
                        text = {
                            Text(
                                text = "تسجيل الدخول",
                                fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = isRegisterMode,
                        onClick = { authViewModel.setRegisterMode(true) },
                        modifier = Modifier.testTag("tab_register"),
                        text = {
                            Text(
                                text = "حساب جديد",
                                fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Registration Name Field
                AnimatedVisibility(visible = isRegisterMode) {
                    Column {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { authViewModel.onDisplayNameChange(it) },
                            label = { Text("الاسم الكامل") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Name", tint = IndigoSecondary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_display_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IndigoSecondary,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { authViewModel.onEmailChange(it) },
                    label = { Text("البريد الإلكتروني") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = IndigoSecondary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IndigoSecondary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { authViewModel.onPasswordChange(it) },
                    label = { Text("كلمة المرور") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password", tint = IndigoSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { authViewModel.submitAuth() }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IndigoSecondary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RoseError.copy(alpha = 0.2f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFFECDD3),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = { authViewModel.submitAuth() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_auth_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoSecondary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "إنشاء حساب ومزامنة مع Firestore" else "تسجيل الدخول",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Admin & Demo Quick Fill Presets
                Text(
                    text = "حسابات تجريبية سريعة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            authViewModel.setRegisterMode(false)
                            authViewModel.onEmailChange("admin@transit.com")
                            authViewModel.onPasswordChange("admin123456")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AmberPending, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسؤول (Admin)", style = MaterialTheme.typography.labelSmall, color = AmberPending)
                    }

                    OutlinedButton(
                        onClick = {
                            authViewModel.setRegisterMode(false)
                            authViewModel.onEmailChange("user@transit.com")
                            authViewModel.onPasswordChange("password123")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مستخدم عادي", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8))
                    }
                }
            }
        }
    }
}

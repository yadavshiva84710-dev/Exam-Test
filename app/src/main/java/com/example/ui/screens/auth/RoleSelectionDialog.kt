package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.language.LanguageManager
import com.example.ui.theme.*

@Composable
fun RoleSelectionDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    onRegisterNewStudent: (name: String, phone: String, email: String, state: String, district: String, category: String) -> Unit
) {
    var showRegisterForm by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newState by remember { mutableStateOf("Uttar Pradesh") }
    var newDistrict by remember { mutableStateOf("Lucknow") }
    var newCategory by remember { mutableStateOf("UP_POLICE") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("role_dialog_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.text(
                            en = if (showRegisterForm) "Register New Student" else "User Account & Role",
                            hi = if (showRegisterForm) "नया छात्र पंजीकरण" else "उपयोगकर्ता खाता व भूमिका"
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!showRegisterForm) {
                    // Active Profile Card
                    if (currentUser != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = SoftIndigoBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentUser.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "ID: ${currentUser.studentId} • ${currentUser.role}",
                                            fontSize = 12.sp,
                                            color = ExamGray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${currentUser.phone} | ${currentUser.district}, ${currentUser.state}",
                                    fontSize = 11.sp,
                                    color = ExamGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = LanguageManager.text(
                            en = "Switch Platform Role:",
                            hi = "भूमिका (Role) बदलें:"
                        ),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Student Option
                    RoleOptionItem(
                        title = LanguageManager.text("Student Portal", "छात्र पोर्टल"),
                        subtitle = LanguageManager.text("Take live tests, practice, view rankings", "लाइव टेस्ट दें, अभ्यास करें, परिणाम व रैंक देखें"),
                        icon = Icons.Default.School,
                        isSelected = currentUser?.role == UserRole.STUDENT,
                        onClick = {
                            onRoleSelected(UserRole.STUDENT)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Exam Admin / Teacher Option
                    RoleOptionItem(
                        title = LanguageManager.text("Exam Admin / Teacher", "परीक्षा नियंत्रक / शिक्षक"),
                        subtitle = LanguageManager.text("Schedule exams, upload questions, live monitor", "परीक्षा बनाएं, प्रश्न बैंक प्रबंधन, लाइव निगरानी"),
                        icon = Icons.Default.EditNote,
                        isSelected = currentUser?.role == UserRole.EXAM_ADMIN,
                        onClick = {
                            onRoleSelected(UserRole.EXAM_ADMIN)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Super Admin Option
                    RoleOptionItem(
                        title = LanguageManager.text("Super Admin Control", "सुपर एडमिन कंट्रोल"),
                        subtitle = LanguageManager.text("Full platform analytics, audit logs & CSV export", "पूर्ण सिस्टम नियंत्रण, ऑडिट लॉग व डेटा निर्यात"),
                        icon = Icons.Default.AdminPanelSettings,
                        isSelected = currentUser?.role == UserRole.SUPER_ADMIN,
                        onClick = {
                            onRoleSelected(UserRole.SUPER_ADMIN)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showRegisterForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("show_register_form_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(LanguageManager.text("Register as New Student", "नया छात्र खाता बनाएं"))
                    }
                } else {
                    // Registration Form
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(LanguageManager.text("Full Name", "पूरा नाम")) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text(LanguageManager.text("Mobile Number", "मोबाइल नंबर")) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_phone_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text(LanguageManager.text("Email Address", "ईमेल पता")) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_email_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDistrict,
                        onValueChange = { newDistrict = it },
                        label = { Text(LanguageManager.text("District (जिला)", "जिला")) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_district_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRegisterForm = false }) {
                            Text(LanguageManager.text("Back", "पीछे"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onRegisterNewStudent(
                                        newName,
                                        if (newPhone.isBlank()) "+91 98765 00000" else newPhone,
                                        if (newEmail.isBlank()) "student@testbook.in" else newEmail,
                                        newState,
                                        newDistrict,
                                        newCategory
                                    )
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("submit_registration_button")
                        ) {
                            Text(LanguageManager.text("Complete Registration", "पंजीकरण पूर्ण करें"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleOptionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("role_option_${title.take(6).lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SoftIndigoBg else CleanBgLight,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) NavyPrimary else CardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NavyPrimary else ExamGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = ExamGray
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = NavyPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

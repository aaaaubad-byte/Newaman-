package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.ui.theme.AmanBgLight
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AmanViewModel

@Composable
fun AdminRolesScreen(viewModel: AmanViewModel, modifier: Modifier = Modifier) {
    val roles by viewModel.roleDetails.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    var selected by remember { mutableStateOf<Role?>(null) }
    Column(modifier.fillMaxSize().background(AmanBgLight).padding(16.dp)) {
        Text("الأدوار والصلاحيات", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(roles, key = { it.id }) { role ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(role.name, fontWeight = FontWeight.Bold)
                            Text(role.description ?: "بدون وصف", fontSize = 11.sp, color = TextSecondary)
                            Text("${role.permissionCodes.size} صلاحية — ${if (role.isActive) "نشط" else "معطل"}", fontSize = 11.sp, color = TextSecondary)
                        }
                        OutlinedButton(onClick = { selected = role }) { Text("تعديل الصلاحيات") }
                    }
                }
            }
        }
    }
    selected?.let { role ->
        var selectedCodes by remember(role.id, role.permissionCodes) { mutableStateOf(role.permissionCodes.toSet()) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("صلاحيات: ${role.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    permissions.forEach { permission ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = permission.code in selectedCodes, onCheckedChange = { checked ->
                                selectedCodes = if (checked) selectedCodes + permission.code else selectedCodes - permission.code
                            })
                            Column {
                                Text(permission.name.ifBlank { permission.code }, fontSize = 12.sp)
                                Text(permission.code, fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.replaceRolePermissions(role.id, selectedCodes.toList()); selected = null }) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("إلغاء") } }
        )
    }
}

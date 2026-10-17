package br.com.poliglota

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF10131C)
private val Panel = Color(0xFF1B2030)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFF9BA3B7)

data class InstalledApp(val label: String, val packageName: String, val icon: Drawable, val isGame: Boolean)

class MainActivity : ComponentActivity() {
    private var pendingPackage: String? = null
    private var onStatus: ((String) -> Unit)? = null

    private val exportApk = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri: Uri? ->
        val pkg = pendingPackage
        if (uri == null || pkg == null) {
            onStatus?.invoke("Exportação cancelada.")
            return@registerForActivityResult
        }
        Thread {
            val result = ApkTranslationPipeline.exportInstalledApk(this, pkg, uri)
            runOnUiThread {
                onStatus?.invoke(if (result) "APK base exportado. Atenção: jogos com APKs divididos (split APKs) podem exigir arquivos adicionais." else "Não foi possível exportar o APK deste aplicativo.")
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Violet, background = Ink, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
                PoliglotaApp(
                    onExport = { app, status ->
                        pendingPackage = app.packageName
                        onStatus = status
                        exportApk.launch(app.label.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".apk")
                    }
                )
            }
        }
    }
}

@Composable
private fun PoliglotaApp(onExport: (InstalledApp, (String) -> Unit) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var status by remember { mutableStateOf("") }
    var inspecting by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ApkTranslationPipeline.ApkReport?>(null) }
    val pickApk = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            inspecting = true
            status = "Analisando estrutura do APK…"
            report = null
            LaunchedEffect(uri) { }
            // Work off the UI thread; APK contents are treated as untrusted input.
            (context as? ComponentActivity)?.lifecycleScope
            kotlinx.coroutines.MainScope().launch {
                val result = withContext(Dispatchers.IO) { ApkTranslationPipeline.inspectApk(context, uri) }
                report = result
                inspecting = false
                status = if (result == null) "Não foi possível abrir esse arquivo como APK." else "Análise estrutural concluída. O APK ainda não foi traduzido nem reconstruído."
            }
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL).mapNotNull { item ->
                val info = item.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (info.packageName == context.packageName) return@mapNotNull null
                InstalledApp(item.loadLabel(pm).toString(), info.packageName, item.loadIcon(pm),
                    (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0 || info.category == ApplicationInfo.CATEGORY_GAME)
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        }
    }
    val visible = apps.filter { (selectedTab == 0 || it.isGame) && (it.label.contains(query, true) || it.packageName.contains(query, true)) }

    Scaffold(containerColor = Ink, bottomBar = {
        NavigationBar(containerColor = Panel) {
            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Apps, null) }, label = { Text("Aplicativos") })
            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Gamepad, null) }, label = { Text("Jogos") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Violet), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Translate, null, tint = Ink, modifier = Modifier.size(27.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Poliglota", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Modificação e tradução de APKs", fontSize = 13.sp, color = Muted)
                }
            }
            if (selectedApp == null) {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Pesquisar jogos e aplicativos…") })
                Spacer(Modifier.height(14.dp))
                Button(onClick = { pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Selecionar APK para analisar") }
                if (status.isNotBlank()) Text(status, color = Muted, modifier = Modifier.padding(vertical = 8.dp))
                report?.let { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Relatório do APK", fontWeight = FontWeight.Bold)
                            Text("Tamanho: ${r.sizeBytes / 1024} KB", color = Muted)
                            Text("Arquivos de recursos XML detectados: ${r.resourceFiles.size}", color = Muted)
                            Text("Pasta assets: ${if (r.hasAssets) "sim" else "não"} · Bibliotecas nativas: ${if (r.hasNativeLibraries) "sim" else "não"}", color = Muted)
                            Text("A inspeção não decodifica recursos binários nem modifica o pacote.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(if (selectedTab == 0) "Aplicativos instalados" else "Jogos instalados", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("${visible.size} encontrados", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                if (visible.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum resultado encontrado.", color = Muted) }
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(visible, key = { it.packageName }) { app -> AppRow(app) { selectedApp = app; status = ""; report = null } } }
            } else {
                val app = selectedApp!!
                Text("‹ Voltar", color = Violet, modifier = Modifier.clickable { selectedApp = null }.padding(vertical = 8.dp))
                Text(app.label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Pacote: ${app.packageName}", color = Muted, fontSize = 12.sp)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Preparar APK", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Exporte o APK base instalado para uma pasta escolhida. Alguns jogos usam APKs divididos e podem precisar de arquivos adicionais.", color = Muted)
                        Button(onClick = { onExport(app) { status = it } }, modifier = Modifier.fillMaxWidth()) { Text("Exportar APK base") }
                        if (status.isNotBlank()) Text(status, color = Muted)
                    }
                }
                Text("O que falta para a tradução final", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                Text("Para gerar um APK traduzido de verdade, é necessário decodificar recursos compilados com uma ferramenta compatível com a versão do jogo, traduzir strings, reconstruir e assinar o pacote. Esta versão ainda não realiza essas etapas e não deve ser usada como se já tivesse traduzido o jogo.", color = Muted)
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AndroidView(factory = { c -> ImageView(c).apply { layoutParams = ViewGroup.LayoutParams((48 * c.resources.displayMetrics.density).toInt(), (48 * c.resources.displayMetrics.density).toInt()); scaleType = ImageView.ScaleType.FIT_CENTER; setImageDrawable(app.icon) }, update = { it.setImageDrawable(app.icon) }, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(if (app.isGame) "Jogo" else "Aplicativo", color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Violet, fontSize = 28.sp)
        }
    }
}

package br.com.poliglota

import android.content.Intent
import android.provider.Settings
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import android.view.ViewGroup
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF10131C)
private val Panel = Color(0xFF1B2030)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFF9BA3B7)

data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isGame: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Violet,
                background = Ink,
                surface = Panel,
                onBackground = Color.White,
                onSurface = Color.White
            )) {
                PoliglotaApp()
            }
        }
    }
}

@Composable
private fun PoliglotaApp() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val launchables = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        apps = launchables.mapNotNull { resolveInfo ->
            val info = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
            if (info.packageName == context.packageName) return@mapNotNull null
            val isGame = (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                (info.category == ApplicationInfo.CATEGORY_GAME)
            InstalledApp(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = info.packageName,
                icon = resolveInfo.loadIcon(pm),
                isGame = isGame
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    val visibleApps = apps.filter {
        (selectedTab == 0 || it.isGame) &&
            (it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true))
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Todos os aplicativos") },
                    label = { Text("Aplicativos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Gamepad, contentDescription = "Jogos") },
                    label = { Text("Jogos") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(Violet),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = Ink, modifier = Modifier.size(27.dp))
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("Poliglota", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Seus apps em português", fontSize = 13.sp, color = Muted)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CircleShape,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                placeholder = { Text("Pesquisar aplicativos ou jogos…", color = Muted) }
            )

            Spacer(Modifier.height(22.dp))
            Text(
                if (selectedTab == 0) "Seus aplicativos" else "Seus jogos",
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${visibleApps.size} encontrados · Tradução para pt-BR",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            if (selectedApp == null) {
                if (visibleApps.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (apps.isEmpty()) "Nenhum aplicativo compatível foi encontrado." else "Nenhum resultado para sua pesquisa.",
                            color = Muted
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppRow(app = app, onClick = { selectedApp = app })
                        }
                    }
                }
            } else {
                AppDetails(
                    app = selectedApp!!,
                    onBack = { selectedApp = null },
                    onEnableTranslation = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                factory = { viewContext ->
                    ImageView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            (48 * viewContext.resources.displayMetrics.density).toInt(),
                            (48 * viewContext.resources.displayMetrics.density).toInt()
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(app.icon)
                    }
                },
                update = { imageView -> imageView.setImageDrawable(app.icon) },
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                Text(app.label, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(if (app.isGame) "Jogo" else "Aplicativo", color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Violet, fontSize = 28.sp)
        }
    }
}

@Composable
private fun AppDetails(
    app: InstalledApp,
    onBack: () -> Unit,
    onEnableTranslation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text("‹ Voltar", color = Violet, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        Text(app.label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Pacote: ${app.packageName}", color = Muted, fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tradução durante o jogo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Ative o serviço do Poliglota uma vez. Depois, abra o jogo normalmente: os textos que o Android conseguir identificar serão traduzidos e mostrados em uma faixa sobre a tela.", color = Muted)
                Button(onClick = onEnableTranslation, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Ativar tradutor")
                }
            }
        }
        Text("Como funciona", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("O Poliglota usa leitura de acessibilidade e um serviço de tradução online (inglês → português brasileiro). A tradução aparece sobre o jogo; o aplicativo original não é alterado.", color = Muted)
        Text("O serviço precisa ser ativado nas configurações de Acessibilidade do Android. Alguns jogos desenham todo o texto dentro da imagem e não expõem os textos ao Android; nesses casos, esta versão não consegue detectá-los.", color = Muted)
    }
}

package net.matsudamper.normalize_share_image.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import net.matsudamper.normalize_share_image.MainActivity
import net.matsudamper.normalize_share_image.R
import net.matsudamper.normalize_share_image.core.ConvertedImage
import net.matsudamper.normalize_share_image.core.ImageConverter
import net.matsudamper.normalize_share_image.core.ImageFormat
import net.matsudamper.normalize_share_image.core.ImageQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConverterScreen(
    onLaunch: () -> Unit = {},
    onImageSelected: (List<Uri>) -> Unit = {},
    onShareImages: (List<ConvertedImage>) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 選択された元画像URI
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    // 変換設定
    var selectedFormat by remember { mutableStateOf(ImageFormat.PNG) }
    var selectedQuality by remember { mutableStateOf(ImageQuality.VERY_HIGH) }
    // 変換結果
    var convertedImages by remember { mutableStateOf<List<ConvertedImage>>(emptyList()) }
    // 変換中フラグ
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableStateOf(0 to 0) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            convertedImages = emptyList()
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            showPermissionDialog = true
        }
    }
    
    LaunchedEffect(Unit) {
        onLaunch()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "画像変換共有",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (convertedImages.isNotEmpty()) {
                // 変換済みの場合は送信ボタン
                ExtendedFloatingActionButton(
                    onClick = {
                        onShareImages(convertedImages)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = "送信",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "送信",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 画像未選択の場合は選択ボタン
                ExtendedFloatingActionButton(
                    onClick = {
                        if ((context as MainActivity).checkPermissions()) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_photo),
                        contentDescription = "画像を選択",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "画像を選択",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { paddingValues ->
        if (selectedUris.isEmpty()) {
            ImageConverterContent(
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            ImagePreviewContent(
                modifier = Modifier.padding(paddingValues),
                selectedUris = selectedUris,
                selectedFormat = selectedFormat,
                selectedQuality = selectedQuality,
                convertedImages = convertedImages,
                isConverting = isConverting,
                conversionProgress = conversionProgress,
                onFormatChanged = { 
                    selectedFormat = it
                    convertedImages = emptyList()
                },
                onQualityChanged = { 
                    selectedQuality = it
                    convertedImages = emptyList()
                },
                onConvert = {
                    scope.launch {
                        isConverting = true
                        val converter = ImageConverter(context)
                        val results = converter.convertImagesWithOptions(
                            uris = selectedUris,
                            contentResolver = context.contentResolver,
                            format = selectedFormat,
                            quality = selectedQuality,
                            onProgress = { current, total ->
                                conversionProgress = current to total
                            }
                        )
                        convertedImages = results
                        isConverting = false
                    }
                },
                onSelectNewImages = {
                    if ((context as MainActivity).checkPermissions()) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            )
        }
    }
    
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    selectedUris: List<Uri>,
    selectedFormat: ImageFormat,
    selectedQuality: ImageQuality,
    convertedImages: List<ConvertedImage>,
    isConverting: Boolean,
    conversionProgress: Pair<Int, Int>,
    onFormatChanged: (ImageFormat) -> Unit,
    onQualityChanged: (ImageQuality) -> Unit,
    onConvert: () -> Unit,
    onSelectNewImages: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // サムネイルプレビュー
        Text(
            text = "選択された画像 (${selectedUris.size}枚)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedUris) { uri ->
                Card(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "選択された画像",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 別の画像を選択
        TextButton(onClick = onSelectNewImages) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_photo),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("別の画像を選択")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // フォーマット選択
        Text(
            text = "出力形式",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var formatExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = formatExpanded,
            onExpandedChange = { formatExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedFormat.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = formatExpanded,
                onDismissRequest = { formatExpanded = false }
            ) {
                ImageFormat.entries.forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.displayName) },
                        onClick = {
                            onFormatChanged(format)
                            formatExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // クオリティ選択
        Text(
            text = "画質",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImageQuality.entries.forEach { quality ->
                FilterChip(
                    selected = selectedQuality == quality,
                    onClick = { onQualityChanged(quality) },
                    label = { Text(quality.displayName) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 変換ボタン
        Button(
            onClick = onConvert,
            enabled = !isConverting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isConverting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("変換中... (${conversionProgress.first}/${conversionProgress.second})")
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_transform),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("変換")
            }
        }
        
        // 変換結果表示
        if (convertedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "変換結果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val totalSize = convertedImages.sumOf { it.fileSize }
                    Text(
                        text = "形式: ${selectedFormat.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "画質: ${selectedQuality.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "ファイル数: ${convertedImages.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "合計サイズ: ${formatFileSize(totalSize)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    if (convertedImages.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        convertedImages.forEachIndexed { index, image ->
                            Text(
                                text = "  画像${index + 1}: ${formatFileSize(image.fileSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "送信ボタンを押してこの画像を共有できます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // FABとの重なりを避けるための余白
        Spacer(modifier = Modifier.height(80.dp))
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

@Composable
private fun ImageConverterContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "画像変換アプリ",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "• PNG/WebP形式に変換\n• クオリティを選択可能\n• サムネイルプレビュー表示\n• 複数選択対応",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "画像を選択して変換・共有を開始",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("権限が必要です") },
        text = { Text("画像にアクセスするために権限が必要です。設定から権限を有効にしてください。") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
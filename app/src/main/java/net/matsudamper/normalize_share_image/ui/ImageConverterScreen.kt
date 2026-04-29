package net.matsudamper.normalize_share_image.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import net.matsudamper.normalize_share_image.core.PerImageOption

/** 一括 or 個別 */
private enum class ApplyMode(val label: String) {
    BATCH("一括適用"),
    INDIVIDUAL("個別適用");
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageConverterScreen(
    externalSelectedUris: List<Uri> = emptyList(),
    onExternalUrisConsumed: () -> Unit = {},
    onRequestSelectImages: (() -> Unit)? = null,
    onShareImages: (List<ConvertedImage>) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var applyMode by remember { mutableStateOf(ApplyMode.BATCH) }
    var batchFormat by remember { mutableStateOf(ImageFormat.PNG) }
    var batchQuality by remember { mutableStateOf(ImageQuality.VERY_HIGH) }
    var perImageOptions by remember { mutableStateOf<List<PerImageOption>>(emptyList()) }
    var convertedImages by remember { mutableStateOf<List<ConvertedImage>>(emptyList()) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableStateOf(0 to 0) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedImageIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // onRequestSelectImages 経由で「画像を追加」を呼んだ場合、次の外部URI通知を追加扱いにする
    var externalAddMode by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = selectedUris + uris
            convertedImages = emptyList()
            perImageOptions = perImageOptions + uris.map { PerImageOption() }
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

    fun openImagePicker() {
        onRequestSelectImages?.let {
            externalAddMode = selectedUris.isNotEmpty()
            it()
            return
        }
        if ((context as MainActivity).checkPermissions()) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    LaunchedEffect(externalSelectedUris) {
        if (externalSelectedUris.isNotEmpty()) {
            if (externalAddMode) {
                selectedUris = selectedUris + externalSelectedUris
                perImageOptions = perImageOptions + externalSelectedUris.map { PerImageOption() }
            } else {
                selectedUris = externalSelectedUris
                perImageOptions = externalSelectedUris.map { PerImageOption() }
                selectedImageIndex = 0
            }
            externalAddMode = false
            convertedImages = emptyList()
            selectionMode = false
            selectedImageIndices = emptySet()
            onExternalUrisConsumed()
        }
    }

    fun deleteSelectedImages() {
        val indicesToRemove = selectedImageIndices.sortedDescending()
        val newUris = selectedUris.toMutableList()
        val newOptions = perImageOptions.toMutableList()
        indicesToRemove.forEach { idx ->
            if (idx < newUris.size) {
                newUris.removeAt(idx)
                newOptions.removeAt(idx)
            }
        }
        selectedUris = newUris
        perImageOptions = newOptions
        convertedImages = emptyList()
        val removedBeforeSelected = selectedImageIndices.count { it < selectedImageIndex }
        selectedImageIndex = (selectedImageIndex - removedBeforeSelected)
            .coerceIn(0, (newUris.size - 1).coerceAtLeast(0))
        selectionMode = false
        selectedImageIndices = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionMode) "${selectedImageIndices.size}枚選択中" else "画像変換共有",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (selectionMode && selectedImageIndices.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "削除"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (convertedImages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onShareImages(convertedImages) },
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
                ExtendedFloatingActionButton(
                    onClick = { openImagePicker() },
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
            ImageConverterContent(modifier = Modifier.padding(paddingValues))
        } else {
            ImagePreviewContent(
                modifier = Modifier.padding(paddingValues),
                selectedUris = selectedUris,
                applyMode = applyMode,
                batchFormat = batchFormat,
                batchQuality = batchQuality,
                perImageOptions = perImageOptions,
                selectedImageIndex = selectedImageIndex,
                convertedImages = convertedImages,
                isConverting = isConverting,
                conversionProgress = conversionProgress,
                selectionMode = selectionMode,
                selectedImageIndices = selectedImageIndices,
                onApplyModeChanged = {
                    applyMode = it
                    convertedImages = emptyList()
                },
                onBatchFormatChanged = { format ->
                    batchFormat = format
                    if (format == ImageFormat.JPEG && batchQuality == ImageQuality.LOSSLESS) {
                        batchQuality = ImageQuality.VERY_HIGH
                    }
                    convertedImages = emptyList()
                },
                onBatchQualityChanged = {
                    batchQuality = it
                    convertedImages = emptyList()
                },
                onPerImageFormatChanged = { index, format ->
                    perImageOptions = perImageOptions.toMutableList().also {
                        val current = it[index]
                        val newQuality = if (format == ImageFormat.JPEG && current.quality == ImageQuality.LOSSLESS) {
                            ImageQuality.VERY_HIGH
                        } else {
                            current.quality
                        }
                        it[index] = current.copy(format = format, quality = newQuality)
                    }
                    convertedImages = emptyList()
                },
                onPerImageQualityChanged = { index, quality ->
                    perImageOptions = perImageOptions.toMutableList().also {
                        it[index] = it[index].copy(quality = quality)
                    }
                    convertedImages = emptyList()
                },
                onSelectedImageIndexChanged = { selectedImageIndex = it },
                onEnterSelectionMode = { index ->
                    selectionMode = true
                    selectedImageIndices = setOf(index)
                },
                onToggleImageSelection = { index ->
                    val newIndices = if (index in selectedImageIndices) {
                        selectedImageIndices - index
                    } else {
                        selectedImageIndices + index
                    }
                    selectedImageIndices = newIndices
                    if (newIndices.isEmpty()) {
                        selectionMode = false
                    }
                },
                onConvert = {
                    scope.launch {
                        isConverting = true
                        val converter = ImageConverter(context)
                        val options = when (applyMode) {
                            ApplyMode.BATCH -> selectedUris.map {
                                PerImageOption(batchFormat, batchQuality)
                            }
                            ApplyMode.INDIVIDUAL -> perImageOptions
                        }
                        val results = converter.convertImagesWithPerImageOptions(
                            uris = selectedUris,
                            contentResolver = context.contentResolver,
                            options = options,
                            onProgress = { current, total ->
                                conversionProgress = current to total
                            }
                        )
                        convertedImages = results
                        isConverting = false
                    }
                },
                onAddImages = { openImagePicker() }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("画像を削除") },
            text = { Text("選択した${selectedImageIndices.size}枚の画像を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deleteSelectedImages()
                }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showPermissionDialog) {
        PermissionDialog(onDismiss = { showPermissionDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    selectedUris: List<Uri>,
    applyMode: ApplyMode,
    batchFormat: ImageFormat,
    batchQuality: ImageQuality,
    perImageOptions: List<PerImageOption>,
    selectedImageIndex: Int,
    convertedImages: List<ConvertedImage>,
    isConverting: Boolean,
    conversionProgress: Pair<Int, Int>,
    selectionMode: Boolean,
    selectedImageIndices: Set<Int>,
    onApplyModeChanged: (ApplyMode) -> Unit,
    onBatchFormatChanged: (ImageFormat) -> Unit,
    onBatchQualityChanged: (ImageQuality) -> Unit,
    onPerImageFormatChanged: (Int, ImageFormat) -> Unit,
    onPerImageQualityChanged: (Int, ImageQuality) -> Unit,
    onSelectedImageIndexChanged: (Int) -> Unit,
    onEnterSelectionMode: (Int) -> Unit,
    onToggleImageSelection: (Int) -> Unit,
    onConvert: () -> Unit,
    onAddImages: () -> Unit
) {
    val thumbnailShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "選択された画像 (${selectedUris.size}枚)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(selectedUris) { index, uri ->
                val isIndividualSelected = !selectionMode && applyMode == ApplyMode.INDIVIDUAL && index == selectedImageIndex
                val isChecked = index in selectedImageIndices

                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(thumbnailShape)
                        .then(
                            if (isIndividualSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = thumbnailShape
                                )
                            } else Modifier
                        )
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    onToggleImageSelection(index)
                                } else if (applyMode == ApplyMode.INDIVIDUAL) {
                                    onSelectedImageIndexChanged(index)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    onEnterSelectionMode(index)
                                }
                            }
                        ),
                    shape = thumbnailShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = "画像 ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (selectionMode) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onAddImages) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_photo),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("画像を追加")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedUris.size > 1) {
            Text(
                text = "適用モード",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ApplyMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ApplyMode.entries.size
                        ),
                        onClick = { onApplyModeChanged(mode) },
                        selected = applyMode == mode
                    ) {
                        Text(mode.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        when (applyMode) {
            ApplyMode.BATCH -> {
                BatchSettingsSection(
                    selectedFormat = batchFormat,
                    selectedQuality = batchQuality,
                    onFormatChanged = onBatchFormatChanged,
                    onQualityChanged = onBatchQualityChanged
                )
            }
            ApplyMode.INDIVIDUAL -> {
                IndividualSettingsSection(
                    selectedUris = selectedUris,
                    perImageOptions = perImageOptions,
                    selectedImageIndex = selectedImageIndex,
                    onFormatChanged = { format ->
                        onPerImageFormatChanged(selectedImageIndex, format)
                    },
                    onQualityChanged = { quality ->
                        onPerImageQualityChanged(selectedImageIndex, quality)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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

        if (convertedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ConversionResultCard(convertedImages = convertedImages)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BatchSettingsSection(
    selectedFormat: ImageFormat,
    selectedQuality: ImageQuality,
    onFormatChanged: (ImageFormat) -> Unit,
    onQualityChanged: (ImageQuality) -> Unit
) {
    FormatSelector(
        label = "出力形式",
        selectedFormat = selectedFormat,
        onFormatChanged = onFormatChanged
    )

    Spacer(modifier = Modifier.height(16.dp))

    QualitySelector(
        label = "画質",
        selectedQuality = selectedQuality,
        format = selectedFormat,
        enabled = selectedFormat != ImageFormat.PNG,
        onQualityChanged = onQualityChanged
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IndividualSettingsSection(
    selectedUris: List<Uri>,
    perImageOptions: List<PerImageOption>,
    selectedImageIndex: Int,
    onFormatChanged: (ImageFormat) -> Unit,
    onQualityChanged: (ImageQuality) -> Unit
) {
    val currentOption = perImageOptions.getOrElse(selectedImageIndex) { PerImageOption() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = selectedUris.getOrNull(selectedImageIndex)
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "画像 ${selectedImageIndex + 1} / ${selectedUris.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            FormatSelector(
                label = "出力形式",
                selectedFormat = currentOption.format,
                onFormatChanged = onFormatChanged
            )

            Spacer(modifier = Modifier.height(12.dp))

            QualitySelector(
                label = "画質",
                selectedQuality = currentOption.quality,
                format = currentOption.format,
                enabled = currentOption.format != ImageFormat.PNG,
                onQualityChanged = onQualityChanged
            )

            if (perImageOptions.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "各画像の設定一覧",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                perImageOptions.forEachIndexed { i, opt ->
                    val prefix = if (i == selectedImageIndex) "▶ " else "   "
                    val qualityPart = if (opt.format != ImageFormat.PNG) " / ${opt.quality.displayName}" else ""
                    Text(
                        text = "${prefix}画像${i + 1}: ${opt.format.displayName}$qualityPart",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (i == selectedImageIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == selectedImageIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSelector(
    label: String,
    selectedFormat: ImageFormat,
    onFormatChanged: (ImageFormat) -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(4.dp))

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedFormat.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ImageFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.displayName) },
                    onClick = {
                        onFormatChanged(format)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualitySelector(
    label: String,
    selectedQuality: ImageQuality,
    format: ImageFormat,
    enabled: Boolean = true,
    onQualityChanged: (ImageQuality) -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )

    if (!enabled) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "PNG形式では画質設定は適用されません",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Spacer(modifier = Modifier.height(4.dp))

    val qualities = if (format == ImageFormat.JPEG) {
        ImageQuality.entries.filter { it != ImageQuality.LOSSLESS }
    } else {
        ImageQuality.entries
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        qualities.forEach { quality ->
            FilterChip(
                selected = selectedQuality == quality,
                onClick = { onQualityChanged(quality) },
                label = { Text(quality.displayName) }
            )
        }
    }
}

@Composable
private fun ConversionResultCard(
    convertedImages: List<ConvertedImage>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "変換結果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            val totalSize = convertedImages.sumOf { it.fileSize }
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

            Spacer(modifier = Modifier.height(8.dp))

            convertedImages.forEachIndexed { index, image ->
                val qualityPart = if (image.format != ImageFormat.PNG) " / ${image.quality.displayName}" else ""
                Text(
                    text = "画像${index + 1}: ${image.format.displayName}$qualityPart — ${formatFileSize(image.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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
                    text = "• PNG/WebP形式に変換\n• クオリティを選択可能\n• 一括適用 / 個別適用を選択\n• 複数選択対応",
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

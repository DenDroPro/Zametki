package com.zametki.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.text.InputType
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.FileProvider
import com.zametki.R
import com.zametki.data.*
import com.zametki.ui.NoteViewModel
import com.zametki.ui.components.Accent
import com.zametki.ui.components.BrownHeader
import com.zametki.ui.components.DarkBg
import com.zametki.ui.components.DarkSurface
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CharFormat(val bold: Boolean = false, val italic: Boolean = false, val underline: Boolean = false)

fun serializeFormats(formats: List<CharFormat>): String {
    if (formats.isEmpty() || formats.all { it == CharFormat() }) return ""
    val arr = JSONArray()
    val d = CharFormat()
    var i = 0
    while (i < formats.size) {
        val f = formats[i]; val s = i
        while (i < formats.size && formats[i] == f) i++
        if (f != d) {
            val o = JSONObject(); o.put("s", s); o.put("e", i)
            if (f.bold) o.put("b", true); if (f.italic) o.put("i", true); if (f.underline) o.put("u", true)
            arr.put(o)
        }
    }
    return if (arr.length() == 0) "" else arr.toString()
}

fun deserializeFormats(json: String, len: Int): List<CharFormat> {
    val r = MutableList(len) { CharFormat() }
    if (json.isBlank()) return r
    try {
        val arr = JSONArray(json)
        for (idx in 0 until arr.length()) {
            val o = arr.getJSONObject(idx)
            val s = o.getInt("s"); val e = o.getInt("e").coerceAtMost(len)
            val f = CharFormat(o.optBoolean("b"), o.optBoolean("i"), o.optBoolean("u"))
            for (j in s until e) { if (j < len) r[j] = f }
        }
    } catch (_: Exception) {}
    return r
}

data class UndoSnap(val text: String, val sel: TextRange, val formats: List<CharFormat>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NoteViewModel,
    noteId: Long,
    onNavigateBack: () -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val note by viewModel.currentNote.collectAsState()
    val context = LocalContext.current
    val screenH = LocalConfiguration.current.screenHeightDp

    var titleText by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var sheetColor by remember { mutableStateOf(SheetColor.WHITE) }
    var fontSize by remember { mutableIntStateOf(16) }
    var lineOpacity by remember { mutableFloatStateOf(0.15f) }
    var initialized by remember { mutableStateOf(false) }

    val charFormats = remember { mutableListOf<CharFormat>() }
    var formatVersion by remember { mutableIntStateOf(0) }
    var activeFormat by remember { mutableStateOf(CharFormat()) }

    val undoStack = remember { mutableListOf<UndoSnap>() }
    val redoStack = remember { mutableListOf<UndoSnap>() }

    fun pushUndo() {
        undoStack.add(UndoSnap(contentValue.text, contentValue.selection, charFormats.toList()))
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
    }
    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(UndoSnap(contentValue.text, contentValue.selection, charFormats.toList()))
        val s = undoStack.removeAt(undoStack.lastIndex)
        contentValue = TextFieldValue(s.text, s.sel); charFormats.clear(); charFormats.addAll(s.formats); formatVersion++
    }
    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(UndoSnap(contentValue.text, contentValue.selection, charFormats.toList()))
        val s = redoStack.removeAt(redoStack.lastIndex)
        contentValue = TextFieldValue(s.text, s.sel); charFormats.clear(); charFormats.addAll(s.formats); formatVersion++
    }

    // Load
    LaunchedEffect(noteId) {
        initialized = false; viewModel.loadNote(noteId)
        val n = viewModel.currentNote.first { it != null && it.id == noteId }
        if (n != null && !initialized) {
            titleText = n.title; contentValue = TextFieldValue(n.content, TextRange(n.content.length))
            charFormats.clear(); charFormats.addAll(deserializeFormats(n.formatting, n.content.length))
            sheetColor = n.sheetColor; fontSize = n.fontSize; lineOpacity = n.lineOpacity
            formatVersion++; initialized = true
        }
    }

    // Save — only save if note.id matches the noteId we're editing
    LaunchedEffect(titleText, contentValue.text, sheetColor, fontSize, lineOpacity, formatVersion) {
        if (initialized) note?.let {
            if (it.id == noteId) {
                viewModel.saveNote(it.copy(title = titleText, content = contentValue.text, preview = contentValue.text.take(100),
                    formatting = serializeFormats(charFormats), sheetColor = sheetColor, fontSize = fontSize, lineOpacity = lineOpacity))
            }
        }
    }

    var editTextRef by remember { mutableStateOf<EditText?>(null) }
    var isUpdatingFromCompose by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isBold = run {
        val s = contentValue.selection
        if (!s.collapsed && s.min < charFormats.size) (s.min until s.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.bold == true } else activeFormat.bold
    }
    val isItalic = run {
        val s = contentValue.selection
        if (!s.collapsed && s.min < charFormats.size) (s.min until s.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.italic == true } else activeFormat.italic
    }
    val isUnderline = run {
        val s = contentValue.selection
        if (!s.collapsed && s.min < charFormats.size) (s.min until s.max.coerceAtMost(charFormats.size)).all { charFormats.getOrNull(it)?.underline == true } else activeFormat.underline
    }

    fun toggleFmt(get: (CharFormat) -> Boolean, set: (CharFormat, Boolean) -> CharFormat) {
        val s = contentValue.selection
        if (!s.collapsed && s.min < charFormats.size) {
            val e = s.max.coerceAtMost(charFormats.size); val all = (s.min until e).all { get(charFormats[it]) }
            for (i in s.min until e) charFormats[i] = set(charFormats[i], !all); formatVersion++
        } else activeFormat = set(activeFormat, !get(activeFormat))
    }

    val sheetBg = sheetColor.color
    val textColorArgb = sheetColor.textColor.toArgb()
    val lineColorArgb = sheetColor.lineColor.toArgb()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF4A3728)) } },
                actions = {
                    IconButton(onClick = { undo() }) { Icon(Icons.Default.Undo, null, tint = Color(0xFF4A3728).copy(alpha = if (undoStack.isNotEmpty()) 1f else 0.3f)) }
                    IconButton(onClick = { redo() }) { Icon(Icons.Default.Redo, null, tint = Color(0xFF4A3728).copy(alpha = if (redoStack.isNotEmpty()) 1f else 0.3f)) }
                    // YD button — same code as HomeScreen selection mode
                    IconButton(onClick = {
                        try {
                            val name = titleText.ifBlank { "Без названия" }.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                            val cacheDir = File(context.cacheDir, "shared_notes")
                            cacheDir.mkdirs()
                            val file = File(cacheDir, "$name.txt")
                            file.writeText(contentValue.text)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val uris = ArrayList<Uri>()
                            uris.add(uri)
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "text/plain"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                setPackage("ru.yandex.disk")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val name = titleText.ifBlank { "Без названия" }.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                                val cacheDir = File(context.cacheDir, "shared_notes")
                                cacheDir.mkdirs()
                                val file = File(cacheDir, "$name.txt")
                                file.writeText(contentValue.text)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val uris = ArrayList<Uri>()
                                uris.add(uri)
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "text/plain"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Поделиться"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Не удалось поделиться", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Icon(painterResource(R.drawable.ic_yandex_disk), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, titleText); putExtra(Intent.EXTRA_TEXT, contentValue.text) }
                        context.startActivity(Intent.createChooser(intent, "Поделиться"))
                    }) { Icon(Icons.Default.Share, null, tint = Color(0xFF4A3728)) }
                    // Delete note button
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownHeader)
            )
        },
        bottomBar = {
            Surface(color = DarkSurface, tonalElevation = 8.dp) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { toggleFmt({ it.bold }, { f, v -> f.copy(bold = v) }) }) {
                            Text("Ж", fontWeight = if (isBold) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.Normal,
                                fontSize = 18.sp, color = if (isBold) Accent else Color(0xFFB0A396))
                        }
                        IconButton(onClick = { toggleFmt({ it.italic }, { f, v -> f.copy(italic = v) }) }) {
                            Text("К", fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                fontSize = 18.sp, color = if (isItalic) Accent else Color(0xFFB0A396),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        IconButton(onClick = { toggleFmt({ it.underline }, { f, v -> f.copy(underline = v) }) }) {
                            Text("Ч", fontSize = 18.sp, color = if (isUnderline) Accent else Color(0xFFB0A396),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        }
                        Spacer(Modifier.width(4.dp))
                        // Font size
                        IconButton(onClick = { if (fontSize > 10) { fontSize--; formatVersion++ } }) {
                            Text("A-", fontSize = 16.sp, color = Color(0xFF8B7B6E))
                        }
                        Text("${fontSize}", fontSize = 14.sp, color = Color(0xFF4A3728))
                        IconButton(onClick = { if (fontSize < 30) { fontSize++; formatVersion++ } }) {
                            Text("A+", fontSize = 16.sp, color = Color(0xFF8B7B6E))
                        }
                        Spacer(Modifier.weight(1f))
                        // Sheet color
                        IconButton(onClick = { showColorSheet = !showColorSheet }) {
                            Icon(Icons.Default.Palette, null, tint = Color(0xFF8B7B6E))
                        }
                    }
                    // Navigation prev/next
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = onNavigatePrev) { Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFF8B7B6E)) }
                        IconButton(onClick = onNavigateNext) { Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8B7B6E)) }
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(sheetBg).verticalScroll(scrollState)) {
            // Title
            val titleColor = sheetColor.textColor
            androidx.compose.foundation.text.BasicTextField(
                value = titleText, onValueChange = { titleText = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = titleColor
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (titleText.isEmpty()) Text("Заголовок", fontSize = 20.sp, color = titleColor.copy(alpha = 0.3f),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    inner()
                }
            )
            HorizontalDivider(color = sheetColor.lineColor.copy(alpha = 0.6f), thickness = 1.5.dp, modifier = Modifier.padding(horizontal = 15.dp))

            // Content — Native EditText
            val curFontSize = fontSize
            val curFormatVersion = formatVersion
            val curFormats = charFormats
            val curLineOpacity = lineOpacity

            AndroidView(
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density
                    val pad = (15 * density).toInt()
                    val padV = (8 * density).toInt()

                    object : EditText(ctx) {
                        var gridOpacity = 0.15f
                        var gridLineH = 0f
                        var gridLineColorArgb = 0

                        private val gridPaint = android.graphics.Paint().apply { isAntiAlias = true }

                        override fun onDraw(canvas: Canvas) {
                            // Draw lined paper
                            val lh = gridLineH
                            if (lh > 0f && gridOpacity > 0f) {
                                gridPaint.color = gridLineColorArgb
                                gridPaint.strokeWidth = density * 0.8f
                                gridPaint.alpha = (gridOpacity * 255).toInt()
                                val padTop = compoundPaddingTop.toFloat()
                                val w = width.toFloat()
                                val h = height.toFloat()
                                val offset = lh / 6f
                                var y = padTop + lh - offset
                                while (y < h + scrollY) {
                                    canvas.drawLine(0f, y, w, y, gridPaint)
                                    y += lh
                                }
                            }
                            super.onDraw(canvas)
                        }

                        override fun onSelectionChanged(selStart: Int, selEnd: Int) {
                            super.onSelectionChanged(selStart, selEnd)
                            if (!isUpdatingFromCompose) {
                                val t = text?.toString() ?: ""
                                contentValue = TextFieldValue(t, TextRange(selStart.coerceIn(0, t.length), selEnd.coerceIn(0, t.length)))
                            }
                            // Request scroll to cursor position — use getLocationInWindow for accuracy
                            post {
                                val layout = layout ?: return@post
                                val len = text?.length ?: 0
                                val safeSel = selStart.coerceIn(0, len)
                                val line = layout.getLineForOffset(safeSel)
                                val lineBottom = layout.getLineBottom(line)
                                // Get EditText position on screen
                                val loc = IntArray(2)
                                getLocationInWindow(loc)
                                val editTextWindowY = loc[1]
                                val cursorWindowY = editTextWindowY + lineBottom
                                // Get actual visible area (keyboard-aware)
                                val visibleRect = Rect()
                                rootView.getWindowVisibleDisplayFrame(visibleRect)
                                val visibleBottom = visibleRect.bottom
                                // Bottom bar (formatting + arrows) is ~110dp
                                val bottomBarPx = (110 * resources.displayMetrics.density).toInt()
                                val marginPx = (40 * resources.displayMetrics.density).toInt()
                                val threshold = visibleBottom - bottomBarPx - marginPx
                                if (cursorWindowY > threshold) {
                                    val scrollBy = cursorWindowY - threshold
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.value + scrollBy)
                                    }
                                }
                            }
                        }
                    }.apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setPadding(pad, padV, pad, padV)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, curFontSize.toFloat())
                        setTextColor(textColorArgb)
                        setLineSpacing(0f, 1.5f)
                        if (android.os.Build.VERSION.SDK_INT >= 28) isFallbackLineSpacing = false
                        gravity = Gravity.TOP or Gravity.START
                        minHeight = ctx.resources.displayMetrics.heightPixels
                        isSingleLine = false
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        includeFontPadding = false
                        isVerticalScrollBarEnabled = false
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER
                        hint = "Начните писать..."
                        setHintTextColor(android.graphics.Color.parseColor("#60888888"))

                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                if (isUpdatingFromCompose || s == null) return
                                val newT = s.toString(); val oldT = contentValue.text
                                if (newT != oldT) {
                                    pushUndo()
                                    if (newT.length > oldT.length) {
                                        val ins = newT.length - oldT.length; val pos = (selectionStart - ins).coerceIn(0, charFormats.size)
                                        repeat(ins) { charFormats.add(pos, activeFormat.copy()) }
                                    } else if (newT.length < oldT.length) {
                                        val del = oldT.length - newT.length; val pos = selectionStart.coerceIn(0, charFormats.size)
                                        repeat(del) { if (pos < charFormats.size) charFormats.removeAt(pos) }
                                    }
                                    contentValue = TextFieldValue(newT, TextRange(selectionStart.coerceIn(0, newT.length)))
                                    formatVersion++
                                }
                            }
                        })
                        editTextRef = this
                    }
                },
                update = { et ->
                    val ct = contentValue.text
                    if (et.text.toString() != ct) {
                        isUpdatingFromCompose = true
                        et.setText(ct)
                        val sel = contentValue.selection.start.coerceIn(0, ct.length)
                        if (et.text.length >= sel) et.setSelection(sel)
                        isUpdatingFromCompose = false
                    }
                    et.setTextSize(TypedValue.COMPLEX_UNIT_SP, curFontSize.toFloat())
                    et.setTextColor(textColorArgb)
                    et.setLineSpacing(0f, 1.5f)

                    // Apply formatting spans
                    val ed = et.text ?: return@AndroidView
                    ed.getSpans(0, ed.length, StyleSpan::class.java).forEach { ed.removeSpan(it) }
                    ed.getSpans(0, ed.length, UnderlineSpan::class.java).forEach { ed.removeSpan(it) }

                    val fv = curFormatVersion
                    val def = CharFormat()
                    var ci = 0
                    while (ci < ed.length && ci < curFormats.size) {
                        val f = curFormats[ci]; val st = ci
                        while (ci < ed.length && ci < curFormats.size && curFormats[ci] == f) ci++
                        if (f != def) {
                            val fl = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            if (f.bold && f.italic) ed.setSpan(StyleSpan(Typeface.BOLD_ITALIC), st, ci, fl)
                            else if (f.bold) ed.setSpan(StyleSpan(Typeface.BOLD), st, ci, fl)
                            else if (f.italic) ed.setSpan(StyleSpan(Typeface.ITALIC), st, ci, fl)
                            if (f.underline) ed.setSpan(UnderlineSpan(), st, ci, fl)
                        }
                    }

                    // Update grid
                    try {
                        val cls = et.javaClass
                        cls.getDeclaredField("gridOpacity").apply { isAccessible = true; setFloat(et, curLineOpacity) }
                        cls.getDeclaredField("gridLineH").apply { isAccessible = true; setFloat(et, et.lineHeight.toFloat()) }
                        cls.getDeclaredField("gridLineColorArgb").apply { isAccessible = true; setInt(et, lineColorArgb) }
                    } catch (_: Exception) {}
                    et.invalidate()
                },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = screenH.dp)
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFFF5F5F5),
            title = { Text("Удалить заметку?", color = Color(0xFF333333)) },
            text = { Text("Заметка будет перемещена в корзину.", color = Color(0xFF666666)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.softDelete(noteId)
                    onNavigateBack()
                }) { Text("Удалить", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = Color(0xFF666666)) }
            }
        )
    }

    // Sheet color picker
    if (showColorSheet) {
        AlertDialog(
            onDismissRequest = { showColorSheet = false },
            title = { Text("Цвет листа") },
            text = {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetColor.entries.forEach { c ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(c.color)
                                .then(if (c == sheetColor) Modifier.border(2.dp, Accent, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { sheetColor = c; showColorSheet = false }) {
                                if (c == sheetColor) Icon(Icons.Default.Check, null, tint = c.textColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorSheet = false }) { Text("Закрыть") } }
        )
    }
}

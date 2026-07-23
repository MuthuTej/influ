package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import np.com.bimalkafle.firebaseauthdemoapp.network.ChatEntity
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Dimens
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.LocalAppColors
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.AiChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.AiChatViewModel
import java.text.SimpleDateFormat
import java.util.*

private val BOLD_MARKDOWN_REGEX = Regex("\\*\\*(.+?)\\*\\*")
private val BULLET_LINE_REGEX = Regex("^\\s*[*-]\\s+(.*)$")
private const val ENTITY_ANNOTATION_TAG = "entity"

private data class TextRange(val start: Int, val end: Int)

// The AI's replies are Gemini-generated markdown (**bold**, "* " bullet
// lists) but the chat bubble was rendering that raw — so users saw literal
// asterisks instead of formatting. This is a small, dependency-free
// markdown-lite renderer covering just the two constructs Gemini actually
// produces here, not a general markdown parser. It also turns any mention of
// a suggested entity's name (influencer, campaign, collaboration) into a tap
// target, so results link out from their own name in the reply instead of a
// separate redirect chip below the message.
private fun formatChatMarkdown(raw: String, entities: List<ChatEntity>, linkColor: Color): AnnotatedString =
    buildAnnotatedString {
        val lines = raw.split("\n")
        lines.forEachIndexed { index, line ->
            val bulletMatch = BULLET_LINE_REGEX.find(line)
            val content = if (bulletMatch != null) {
                append("•  ")
                bulletMatch.groupValues[1]
            } else {
                line
            }
            appendWithBoldAndEntitySpans(content, entities, linkColor)
            if (index != lines.lastIndex) append("\n")
        }
    }

private fun AnnotatedString.Builder.appendWithBoldAndEntitySpans(
    text: String,
    entities: List<ChatEntity>,
    linkColor: Color
) {
    // Strip **bold** markers first, remembering the bold ranges in the
    // resulting plain text so entity-name matching runs against clean text.
    val clean = StringBuilder()
    val boldRanges = mutableListOf<TextRange>()
    var lastIndex = 0
    for (match in BOLD_MARKDOWN_REGEX.findAll(text)) {
        clean.append(text, lastIndex, match.range.first)
        val start = clean.length
        clean.append(match.groupValues[1])
        boldRanges.add(TextRange(start, clean.length))
        lastIndex = match.range.last + 1
    }
    clean.append(text, lastIndex, text.length)
    val plain = clean.toString()

    // Locate where each suggested entity is mentioned in this line so it can
    // become a tap target. The model isn't consistent about whether it names
    // an influencer by display name or @handle, so every known alias is
    // tried IN PRIORITY ORDER — the backend puts the display name before the
    // @handle, so the name wins whenever both happen to appear; a raw
    // Firestore ID is never a candidate at all (aliases only ever holds
    // human-readable name/handle/title values), so a link can never anchor
    // on an ID. The first candidate actually found in the text wins (first
    // non-overlapping mention only).
    data class EntityMention(val range: TextRange, val entityIndex: Int)
    val mentions = mutableListOf<EntityMention>()
    entities.forEachIndexed { entityIndex, entity ->
        val candidates = entity.aliases.filter { it.isNotBlank() }.distinct()
        val match = candidates.firstNotNullOfOrNull { candidate ->
            Regex(Regex.escape(candidate), RegexOption.IGNORE_CASE).find(plain)
        } ?: return@forEachIndexed
        val range = TextRange(match.range.first, match.range.last + 1)
        if (mentions.none { it.range.start < range.end && range.start < it.range.end }) {
            mentions.add(EntityMention(range, entityIndex))
        }
    }
    mentions.sortBy { it.range.start }

    fun appendPlainRange(from: Int, to: Int) {
        var pos = from
        boldRanges.filter { it.end > from && it.start < to }.sortedBy { it.start }.forEach { bold ->
            val segStart = maxOf(bold.start, pos)
            val segEnd = minOf(bold.end, to)
            if (segStart > pos) append(plain.substring(pos, segStart))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(plain.substring(segStart, segEnd)) }
            pos = segEnd
        }
        if (pos < to) append(plain.substring(pos, to))
    }

    var cursor = 0
    mentions.forEach { mention ->
        appendPlainRange(cursor, mention.range.start)
        pushStringAnnotation(tag = ENTITY_ANNOTATION_TAG, annotation = mention.entityIndex.toString())
        withStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = linkColor,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(plain.substring(mention.range.start, mention.range.end))
        }
        pop()
        cursor = mention.range.end
    }
    appendPlainRange(cursor, plain.length)
}

private fun routeForEntity(entity: ChatEntity): String? = when (entity.type) {
    "influencer"    -> "brand_influencer_detail/${entity.id}"
    "campaign"      -> "brand_campaign_detail/${entity.id}"
    "collaboration" -> "collaboration_analytics/${entity.id}"
    else            -> null
}

private val suggestions = listOf(
    Triple(Icons.Default.Campaign,     "Active campaigns",     "What are my active campaigns?"),
    Triple(Icons.Default.People,       "Top influencers",      "Who are the top influencers right now?"),
    Triple(Icons.Default.BarChart,     "Analytics summary",    "Summarize my collaboration analytics"),
    Triple(Icons.Default.Lightbulb,    "Get a recommendation", "Recommend influencers for a fashion brand")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    navController: NavHostController,
    aiChatViewModel: AiChatViewModel = viewModel()
) {
    val appColors    = LocalAppColors.current
    val messages     by aiChatViewModel.messages.collectAsState()
    val isLoading    by aiChatViewModel.isLoading.collectAsState()
    val error        by aiChatViewModel.error.collectAsState()
    val listState    = rememberLazyListState()
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty())
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
    }

    Box(modifier = Modifier.fillMaxSize().background(appColors.brandPrimary)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.space16, vertical = Dimens.space16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(Dimens.minTouchTarget)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(Dimens.space12))

                // AI avatar in header
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(Dimens.space12))

                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Color(0xFF4CD964), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("Online · Ready to help", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                }

                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Clear chat", tint = Color.White)
                    }
                }
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title   = { Text("Clear conversation?") },
                    text    = { Text("Your conversation with the AI assistant will be deleted from this device. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = { aiChatViewModel.clearChat(); showClearConfirm = false }) {
                            Text("Clear", color = appColors.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                    }
                )
            }

            // ── Body ────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFFF5F6FA))
                    .imePadding()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        messages.isEmpty() -> AiEmptyState(onSuggestionClick = { aiChatViewModel.sendMessage(it) })
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp)
                        ) {
                            items(messages) { message ->
                                AiMessageBubble(
                                    message    = message,
                                    brandColor = appColors.brandPrimary,
                                    onEntityClick = { entity ->
                                        routeForEntity(entity)?.let { navController.navigate(it) }
                                    }
                                )
                            }
                            if (isLoading) {
                                item { AiTypingBubble(brandColor = appColors.brandPrimary) }
                            }
                        }
                    }
                }

                if (error != null) {
                    Surface(
                        color = appColors.error.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.space16, vertical = Dimens.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = appColors.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Dimens.space8))
                            Text(error ?: "", color = appColors.error, fontSize = 13.sp)
                        }
                    }
                }

                AiInputBar(
                    enabled = !isLoading,
                    brandColor = appColors.brandPrimary,
                    onSend   = { aiChatViewModel.sendMessage(it) }
                )
            }
        }
    }
}

// ── Empty state with suggestion chips ───────────────────────────────────────

@Composable
private fun AiEmptyState(onSuggestionClick: (String) -> Unit) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glow circle + icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(appColors.brandPrimary.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(appColors.brandPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    tint   = appColors.brandPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "What can I help you with?",
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp,
            color      = appColors.textPrimary,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Ask me about your campaigns, influencers, or collaboration analytics.",
            fontSize  = 14.sp,
            color     = appColors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(32.dp))

        // 2-column suggestion grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (icon, label, query) ->
                        SuggestionChip(
                            icon    = icon,
                            label   = label,
                            onClick = { onSuggestionClick(query) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // pad if odd number
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Surface(
        modifier = modifier.clickable { onClick() },
        shape  = RoundedCornerShape(14.dp),
        color  = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(appColors.brandPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = appColors.brandPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = appColors.textPrimary,
                lineHeight = 17.sp
            )
        }
    }
}

// ── AI avatar ───────────────────────────────────────────────────────────────

@Composable
private fun AiAvatar(brandColor: Color, size: Int = 32) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(brandColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            null,
            tint     = brandColor,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

// ── Message bubble ──────────────────────────────────────────────────────────

@Composable
private fun AiMessageBubble(
    message: AiChatMessage,
    brandColor: Color,
    onEntityClick: (ChatEntity) -> Unit
) {
    val isUser = message.isUser
    val time   = remember(message) {
        try { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) } catch (e: Exception) { "" }
    }
    val annotatedText = remember(message, brandColor) {
        if (isUser) AnnotatedString(message.text) else formatChatMarkdown(message.text, message.entities, brandColor)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start  = if (isUser) 48.dp else Dimens.space16,
                end    = if (isUser) Dimens.space16 else 48.dp,
                top    = 4.dp,
                bottom = 4.dp
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment   = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                AiAvatar(brandColor = brandColor)
                Spacer(Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart    = if (isUser) 18.dp else 4.dp,
                    topEnd      = if (isUser) 4.dp  else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd   = 18.dp
                ),
                color = if (isUser) brandColor else Color.White,
                shadowElevation = if (isUser) 0.dp else 2.dp
            ) {
                ClickableText(
                    text  = annotatedText,
                    style = TextStyle(
                        color      = if (isUser) Color.White else Color(0xFF1C1C1E),
                        fontSize   = 15.sp,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    onClick  = { offset ->
                        if (!isUser) {
                            annotatedText.getStringAnnotations(ENTITY_ANNOTATION_TAG, offset, offset)
                                .firstOrNull()
                                ?.item?.toIntOrNull()
                                ?.let { message.entities.getOrNull(it) }
                                ?.let(onEntityClick)
                        }
                    }
                )
            }
        }

        // Timestamp
        Spacer(Modifier.height(3.dp))
        Text(
            text     = time,
            fontSize = 10.sp,
            color    = Color(0xFF8E8E93),
            modifier = Modifier.padding(
                start = if (!isUser) (32 + 8).dp else 0.dp
            )
        )
    }
}

// ── Animated 3-dot typing indicator ─────────────────────────────────────────

@Composable
private fun AiTypingBubble(brandColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    @Composable
    fun dot(delayMs: Int): Float {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue  = 1f,
            animationSpec = infiniteRepeatable(
                animation   = tween(400, easing = FastOutSlowInEasing),
                repeatMode  = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMs)
            ),
            label = "dot$delayMs"
        )
        return scale
    }

    val s1 = dot(0)
    val s2 = dot(160)
    val s3 = dot(320)

    Row(
        modifier = Modifier.padding(start = Dimens.space16, end = 48.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        AiAvatar(brandColor = brandColor)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(s1, s2, s3).forEach { scale ->
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(8.dp)
                            .background(brandColor.copy(alpha = 0.7f), CircleShape)
                    )
                }
            }
        }
    }
}

// ── Input bar ────────────────────────────────────────────────────────────────

@Composable
private fun AiInputBar(
    enabled: Boolean,
    brandColor: Color,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) text = spoken
        }
    }

    fun launchVoiceSearch() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask anything…")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Speech recognition isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchVoiceSearch()
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice search", Toast.LENGTH_SHORT).show()
        }
    }

    fun onMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) launchVoiceSearch() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Surface(
        shadowElevation = 12.dp,
        color  = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sparkle icon hint
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint     = brandColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))

            TextField(
                value           = text,
                onValueChange   = { text = it },
                placeholder     = { Text("Ask anything…", color = Color(0xFFAAAAAA), fontSize = 15.sp) },
                textStyle       = androidx.compose.ui.text.TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp),
                modifier        = Modifier.weight(1f).heightIn(min = 48.dp),
                shape           = RoundedCornerShape(24.dp),
                colors          = TextFieldDefaults.colors(
                    focusedContainerColor   = Color(0xFFF2F2F7),
                    unfocusedContainerColor = Color(0xFFF2F2F7),
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = brandColor
                ),
                maxLines = 4,
                enabled  = enabled
            )

            Spacer(Modifier.width(10.dp))

            val canSend = text.isNotBlank() && enabled
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (canSend) brandColor else Color(0xFFF2F2F7), CircleShape)
                    .then(
                        when {
                            canSend -> Modifier.clickable { onSend(text); text = "" }
                            enabled -> Modifier.clickable { onMicClick() }
                            else    -> Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (canSend) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Send",
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Mic,
                        "Voice search",
                        tint     = brandColor.copy(alpha = if (enabled) 1f else 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ui/components/MerrySheet.kt
package com.hambaksanta.merrycri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.ui.theme.MerryColors
import com.hambaksanta.merrycri.ui.theme.MerryTheme
import kotlinx.coroutines.launch

/**
 * 화면 아래에서 올라오는 공통 시트. 담당: C
 * 편지 쓰기, 내 선물함, 설정, 간식 고르기 등에 쓴다.
 *
 * 사용 예:
 *   var open by remember { mutableStateOf(false) }
 *   if (open) MerrySheet(title = "편지 쓰기", onDismiss = { open = false }) { ... }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerrySheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 닫기 버튼: 내려가는 애니메이션이 끝난 뒤 onDismiss
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MerryColors.Card,
        contentColor = MerryColors.Snow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MerryColors.Muted) },
    ) {
        MerrySheetContent(title = title, onClose = close, content = content)
    }
}

/** 시트 안쪽 내용. Preview 에서도 쓸 수 있게 따로 뺐다. */
@Composable
fun MerrySheetContent(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MerryColors.Snow,
                modifier = Modifier.weight(1f),
            )
            // 아이콘 라이브러리를 따로 추가하지 않으려고 글자 버튼으로 둔다
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close), color = MerryColors.Muted, fontSize = 15.sp)
            }
        }
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF24365C)
@Composable
private fun MerrySheetPreview() {
    MerryTheme {
        Column(Modifier.background(MerryColors.Card, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))) {
            MerrySheetContent(title = "편지 쓰기", onClose = {}) {
                Text("받는 사람, 편지지, 본문이 여기에 들어가요.", color = MerryColors.Muted, fontSize = 15.sp)
                MerryButton("포장하기", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

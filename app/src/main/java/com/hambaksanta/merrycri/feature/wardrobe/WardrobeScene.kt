// feature/wardrobe/WardrobeScene.kt
package com.hambaksanta.merrycri.feature.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.character.CharacterView
import com.hambaksanta.merrycri.core.character.Hat
import com.hambaksanta.merrycri.core.character.PaletteColor
import com.hambaksanta.merrycri.core.character.Top
import com.hambaksanta.merrycri.core.character.hatType
import com.hambaksanta.merrycri.core.character.topType
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

/**
 * 옷장. 담당: A
 * SceneScaffold 를 쓰지만 걷지 않고(onFloorTap 무시), 작은 캐릭터 대신 큰 미리보기를 보여준다.
 * 지금은 모자·상의 종류와 모자·상의 색만 바꿀 수 있는 임시판.
 * TODO(A, 단계 1): 부위 탭(모자·목도리·장갑·상의·부츠), 아이템 가로 스크롤, 부위별 색 8개
 */
@Composable
fun WardrobeScene(presence: PresenceViewModel, onDone: () -> Unit) {
    val defaultNick = stringResource(R.string.nick_default)
    var nick by remember { mutableStateOf(presence.me.value.nick) }
    var outfit by remember { mutableStateOf(presence.me.value.outfit) }

    SceneScaffold(
        sceneId = SceneId.WARDROBE,
        presence = presence,
        onBackToPlaza = null,
        showCharacters = false,
        onFloorTap = { _, _ -> },
        bottomBar = {
            Button(
                onClick = {
                    presence.setProfile(nick.ifBlank { defaultNick }, outfit)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.wardrobe_go), fontSize = 18.sp) }
        },
        objects = {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, start = 24.dp, end = 24.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                CharacterView(
                    outfit = outfit,
                    holding = null,
                    nick = nick.ifBlank { stringResource(R.string.me_default) },
                    modifier = Modifier.scale(2.2f),
                )
                Spacer(Modifier.height(72.dp))

                OutlinedButton(onClick = {
                    val next = Hat.entries[(outfit.hatType().ordinal + 1) % Hat.entries.size]
                    outfit = outfit.copy(hat = next.id)
                }) {
                    Text(stringResource(R.string.wardrobe_hat, stringResource(outfit.hatType().labelRes)))
                }
                OutlinedButton(onClick = {
                    val next = Top.entries[(outfit.topType().ordinal + 1) % Top.entries.size]
                    outfit = outfit.copy(top = next.id)
                }) {
                    Text(stringResource(R.string.wardrobe_top, stringResource(outfit.topType().labelRes)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaletteColor.entries.forEach { c ->
                        Button(
                            onClick = { outfit = outfit.copy(colors = outfit.colors.copy(top = c.id, hat = c.id)) },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.color),
                            contentPadding = PaddingValues(0.dp),
                        ) {}
                    }
                }
                OutlinedTextField(
                    value = nick,
                    onValueChange = { nick = it.take(NICK_MAX) },
                    label = { Text(stringResource(R.string.wardrobe_nick_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

private const val NICK_MAX = 10

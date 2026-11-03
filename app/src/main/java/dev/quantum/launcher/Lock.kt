@file:OptIn(ExperimentalMaterial3Api::class)

package dev.quantum.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api

// ---------- ロック画面(パスワード) ----------

@Composable
fun HexAvatar() {
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f * 0.95f
            val path = Path()
            for (i in 0..5) {
                val a = Math.toRadians(60.0 * i - 90.0).toFloat()
                val p = Offset(center.x + r * cos(a), center.y + r * sin(a))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, Color(0xFF1B1A24))
            drawPath(path, Brush.linearGradient(listOf(QBlue, QViolet)), style = Stroke(2.dp.toPx()))
        }
        AtomLogo(Modifier.size(46.dp), spin = true)
    }
}

@Composable
fun LockScreen(
    hasPassword: Boolean,
    hour: String,
    minute: String,
    date: String,
    onSetPassword: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onUnlock: () -> Unit,
    lang: String,
    onLang: (String) -> Unit
) {
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    val tooShort = stringResource(R.string.pw_too_short)
    val wrongPw = stringResource(R.string.pw_wrong)

    fun submit() {
        if (!hasPassword) {
            if (pw.length >= 4) {
                onSetPassword(pw)
                onUnlock()
            } else {
                err = tooShort
            }
        } else if (onCheck(pw)) {
            onUnlock()
        } else {
            err = wrongPw
            pw = ""
        }
    }

    Box(Modifier.fillMaxSize()) {
        LocalBackdrop.current?.let { BackdropLayer(it) }
        Box(Modifier.fillMaxSize().background(Color(0x99000000)))
        Column(
            Modifier.align(Alignment.Center).padding(24.dp).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(hour, color = Color(0xFFF4A6B8), fontSize = 88.sp, fontWeight = FontWeight.Black)
                Text(
                    minute, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
            Text(date, color = Color(0xAAFFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(28.dp))
            HexAvatar()
            Spacer(Modifier.height(8.dp))
            Text("Quantum", color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            TextField(
                value = pw,
                onValueChange = { pw = it; err = "" },
                singleLine = true,
                placeholder = { Text(stringResource(if (hasPassword) R.string.pw_enter else R.string.pw_new)) },
                leadingIcon = { QIcon(Icons.Filled.Lock, 16.dp) },
                trailingIcon = {
                    IconButton(onClick = { submit() }) { QIcon(Icons.Filled.ArrowForward, 18.dp) }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(50),
                colors = fieldColors(),
                modifier = Modifier.width(280.dp)
            )
            if (err.isNotEmpty()) {
                Text(err, color = Color(0xFFFF8A9B), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        LanguagePill(
            current = lang,
            onSelect = onLang,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}

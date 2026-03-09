package com.heyanle.easybangumi4.splash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.heyanle.easybangumi4.R
import com.heyanle.easybangumi4.splash.step.SampleGuildHeader
import com.heyanle.inject.core.Inject
/**
 * Created by heyanlin on 2024/7/4.
 */
@Composable
fun Splash() {
    val controller: SplashGuildController by Inject.injectLazy()
    val sp = LocalSplashActivity.current
    var currentStep by remember(controller.realStep.size) {
        mutableIntStateOf(0)
    }

    LaunchedEffect(key1 = Unit){
        if (controller.realStep.isEmpty()){
            sp.jumpToMain()
        }
    }

    currentStep = currentStep.coerceIn(0, (controller.realStep.size - 1).coerceAtLeast(0))

    Column {
        SampleGuildHeader()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            controller.realStep.getOrNull(currentStep)?.Compose()
        }
        HorizontalDivider()
        Spacer(modifier = Modifier.size(16.dp))
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = {
                if (currentStep < controller.realStep.size - 1) {
                    currentStep += 1
                } else {
                    sp.jumpToMain()
                }
            }) {
            Text(stringResource(id = com.heyanle.easy_i18n.R.string.next))

        }
        Spacer(modifier = Modifier.size(16.dp))
    }


}

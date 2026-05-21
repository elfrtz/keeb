package com.elfrtz.keeb.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.R

/** Same layers as [@drawable/ic_launcher]: background color + foreground vector. */
@Composable
fun KeebLogoMark(modifier: Modifier = Modifier.size(48.dp)) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorResource(R.color.ic_launcher_background))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Keeb",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

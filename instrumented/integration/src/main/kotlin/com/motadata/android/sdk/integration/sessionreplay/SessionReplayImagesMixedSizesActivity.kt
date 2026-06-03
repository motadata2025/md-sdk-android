/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sdk.integration.sessionreplay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.widget.ImageView
import com.motadata.android.sdk.integration.R
import com.motadata.android.sdk.integration.RuntimeConfig
import com.motadata.android.sdk.utils.getImagePrivacy
import com.motadata.android.sessionreplay.SessionReplayConfiguration
import com.motadata.android.sessionreplay.SessionReplayPrivacy

internal class SessionReplayImagesMixedSizesActivity : BaseSessionReplayActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sr_images_mixed_sizes_layout)

        val density = resources.displayMetrics.density
        val smallImageView = findViewById<ImageView>(R.id.smallImageView)
        val largeImageView = findViewById<ImageView>(R.id.largeImageView)

        val smallBitmap = createColorBitmap(
            (SMALL_IMAGE_SIZE_DP * density).toInt(),
            (SMALL_IMAGE_SIZE_DP * density).toInt(),
            SMALL_IMAGE_COLOR
        )
        smallImageView.setImageBitmap(smallBitmap)

        val largeBitmap = createColorBitmap(
            (LARGE_IMAGE_SIZE_DP * density).toInt(),
            (LARGE_IMAGE_SIZE_DP * density).toInt(),
            LARGE_IMAGE_COLOR
        )
        largeImageView.setImageBitmap(largeBitmap)
    }

    private fun createColorBitmap(widthPx: Int, heightPx: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), paint)
        return bitmap
    }

    @Suppress("DEPRECATION")
    override fun sessionReplayConfiguration(
        privacy: SessionReplayPrivacy,
        sampleRate: Float
    ): SessionReplayConfiguration {
        val imagePrivacy = intent.getImagePrivacy()
        return if (imagePrivacy != null) {
            RuntimeConfig.sessionReplayConfigBuilder(sampleRate)
                .setPrivacy(privacy)
                .setImagePrivacy(imagePrivacy)
                .build()
        } else {
            super.sessionReplayConfiguration(privacy, sampleRate)
        }
    }

    companion object {
        private const val SMALL_IMAGE_SIZE_DP = 80
        private const val LARGE_IMAGE_SIZE_DP = 150
        private const val SMALL_IMAGE_COLOR = 0xFF6200EE.toInt()
        private const val LARGE_IMAGE_COLOR = 0xFF03DAC5.toInt()
    }
}

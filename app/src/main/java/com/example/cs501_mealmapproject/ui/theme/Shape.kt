package com.example.cs501_mealmapproject.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),   // A soft radius for small components like buttons
    medium = RoundedCornerShape(12.dp),  // A slightly larger radius for cards and dialogs
    large = RoundedCornerShape(16.dp)    // A generous radius for larger surfaces like bottom sheets
)

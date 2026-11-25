package com.example.mealprep.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class HeroSlideItem(
    val emoji: String,
    val badge: String,
    val title: String,
    val description: String
)

private val heroSlides = listOf(
    HeroSlideItem(
        emoji = "🥗",
        badge = "SMART MEAL PREP",
        title = "Katering Sehat Sesuai Target Tubuhmu",
        description = "Atur porsi harian untuk defisit kalori, jaga berat badan, atau naik massa otot tanpa ribet hitung manual."
    ),
    HeroSlideItem(
        emoji = "📸",
        badge = "AI CALORIE TRACKER",
        title = "Scan Makanan & Pantau Makro",
        description = "Cek estimasi kalori, protein, karbohidrat, dan lemak biar asupan harian tetap terukur walau makan di luar."
    ),
    HeroSlideItem(
        emoji = "🚀",
        badge = "MULAI SEKARANG",
        title = "Hitung Kebutuhan Kalori Pertamamu",
        description = "Masukkan data tinggi, berat, usia, dan aktivitasmu untuk mendapatkan target kalori & makro harian."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { heroSlides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == heroSlides.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Bar atas: tombol Lewati
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished) {
                    Text(if (isLastPage) "Masuk" else "Lewati")
                }
            }

            // Area Hero Slide
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val item = heroSlides[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Hero visual card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = item.emoji,
                                fontSize = 84.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = item.badge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Page indicator dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(heroSlides.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Tombol Lanjut / Mulai
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isLastPage) "Mulai Hitung Kalori" else "Lanjut",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
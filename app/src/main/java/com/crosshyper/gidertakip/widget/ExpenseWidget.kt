package com.crosshyper.gidertakip.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.crosshyper.gidertakip.MainActivity
import com.crosshyper.gidertakip.data.AppDatabase
import kotlinx.coroutines.flow.first
import androidx.glance.GlanceTheme

class ExpenseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val totalAmount = database.expenseDao().getTotalExpense().first() ?: 0.0

        provideContent {
            GlanceTheme {
                WidgetContent(totalAmount.toString())
            }
        }
    }

    @Composable
    private fun WidgetContent(total: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Toplam Gider",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface)
                )
                Text(
                    text = "$total TL",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary
                    )
                )
                Text(
                    text = "Hızlı Ekle +",
                    modifier = GlanceModifier.padding(top = 8.dp),
                    style = TextStyle(
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.secondary
                    )
                )
            }
        }
    }
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}

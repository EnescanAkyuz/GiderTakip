package com.crosshyper.gidertakip.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import com.crosshyper.gidertakip.data.local.database.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale

class ExpenseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val expenses = database.expenseDao().observeAllExpenses().first()
        val now = System.currentTimeMillis()
        val monthTotal = expenses.filter { isInCurrentMonth(it.date, now) }.sumOf { it.amount }
        val upcomingCount = expenses.count {
            it.nextDueDate != null && it.nextDueDate >= now && it.nextDueDate <= now + 1000L * 60 * 60 * 24 * 30
        }

        provideContent {
            GlanceTheme {
                WidgetContent(monthTotal = monthTotal, upcomingCount = upcomingCount)
            }
        }
    }

    @Composable
    private fun WidgetContent(monthTotal: Double, upcomingCount: Int) {
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
                    text = "Bu Ay",
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurface)
                )
                Text(
                    text = String.format(Locale("tr", "TR"), "%,.0f TL", monthTotal),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary
                    )
                )
                Text(
                    text = "Yaklaşan ödeme: $upcomingCount",
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

    private fun isInCurrentMonth(timestamp: Long, now: Long): Boolean {
        val current = Calendar.getInstance().apply { timeInMillis = now }
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            current.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}

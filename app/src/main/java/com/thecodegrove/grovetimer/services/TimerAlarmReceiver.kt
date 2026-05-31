package com.thecodegrove.grovetimer.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SleepTimerService.ACTION_TIMER_EXPIRED) return

        val serviceIntent = Intent(context, SleepTimerService::class.java).apply {
            action = SleepTimerService.ACTION_TIMER_EXPIRED
        }
        context.startService(serviceIntent)
    }
}

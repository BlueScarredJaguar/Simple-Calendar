package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getDefaultReminderTypeIndex
import com.simplemobiletools.calendar.extensions.getDefaultReminderValue
import com.simplemobiletools.calendar.extensions.setupReminderPeriod
import com.simplemobiletools.calendar.helpers.DAY_MINS
import com.simplemobiletools.calendar.helpers.HOUR_MINS
import com.simplemobiletools.calendar.helpers.REMINDER_CUSTOM
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    val GET_RINGTONE_URI = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupSundayFirst()
        setupWeeklyStart()
        setupWeeklyEnd()
        setupWeekNumbers()
        setupVibrate()
        setupReminderSound()
        setupEventReminder()
        updateTextColors(settings_holder)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
        }
    }

    private fun setupWeeklyStart() {
        settings_start_weekly_at.apply {
            adapter = getWeeklyAdapter()
            setSelection(config.startWeeklyAt)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (selectedItemPosition >= config.endWeeklyAt) {
                        toast(R.string.day_end_before_start)
                        setSelection(config.startWeeklyAt)
                    } else {
                        config.startWeeklyAt = selectedItemPosition
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    private fun setupWeeklyEnd() {
        settings_end_weekly_at.apply {
            adapter = getWeeklyAdapter()
            setSelection(config.endWeeklyAt)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (selectedItemPosition <= config.startWeeklyAt) {
                        toast(R.string.day_end_before_start)
                        setSelection(config.endWeeklyAt)
                    } else {
                        config.endWeeklyAt = selectedItemPosition
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    private fun setupWeekNumbers() {
        settings_week_numbers.isChecked = config.displayWeekNumbers
        settings_week_numbers_holder.setOnClickListener {
            settings_week_numbers.toggle()
            config.displayWeekNumbers = settings_week_numbers.isChecked
        }
    }

    private fun setupReminderSound() {
        val noRingtone = resources.getString(R.string.no_ringtone_selected)
        if (config.reminderSound.isEmpty()) {
            settings_reminder_sound.text = noRingtone
        } else {
            settings_reminder_sound.text = RingtoneManager.getRingtone(this, Uri.parse(config.reminderSound))?.getTitle(this) ?: noRingtone
        }
        settings_reminder_sound_holder.setOnClickListener {
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, resources.getString(R.string.notification_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(config.reminderSound))

                if (resolveActivity(packageManager) != null)
                    startActivityForResult(this, GET_RINGTONE_URI)
                else {
                    toast(R.string.no_ringtone_picker)
                }
            }
        }
    }

    private fun setupVibrate() {
        settings_vibrate.isChecked = config.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            config.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupEventReminder() {
        val reminderType = config.defaultReminderType
        settings_default_reminder.setSelection(getDefaultReminderTypeIndex())
        custom_reminder_save.setTextColor(custom_reminder_other_val.currentTextColor)
        setupReminderPeriod(custom_reminder_other_period, custom_reminder_value)

        settings_custom_reminder_holder.beVisibleIf(reminderType == REMINDER_CUSTOM)
        custom_reminder_save.setOnClickListener { saveReminder() }

        settings_default_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, itemIndex: Int, p3: Long) {
                if (itemIndex == 2) {
                    settings_custom_reminder_holder.visibility = View.VISIBLE
                    showKeyboard(custom_reminder_value)
                } else {
                    hideKeyboard()
                    settings_custom_reminder_holder.visibility = View.GONE
                }

                config.defaultReminderType = getDefaultReminderValue(itemIndex)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    private fun saveReminder() {
        val multiplier = when (custom_reminder_other_period.selectedItemPosition) {
            1 -> HOUR_MINS
            2 -> DAY_MINS
            else -> 1
        }

        val value = custom_reminder_value.value
        config.defaultReminderMinutes = Integer.valueOf(if (value.isEmpty()) "0" else value) * multiplier
        config.defaultReminderType = REMINDER_CUSTOM
        toast(R.string.reminder_saved)
        hideKeyboard()
    }

    private fun getWeeklyAdapter(): ArrayAdapter<String> {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        for (i in 0..24) {
            adapter.add("$i:00")
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == GET_RINGTONE_URI) {
            val uri = resultData?.getParcelableExtra<Parcelable>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri == null) {
                config.reminderSound = ""
            } else {
                settings_reminder_sound.text = RingtoneManager.getRingtone(this, uri as Uri).getTitle(this)
                config.reminderSound = uri.toString()
            }
        }
    }
}

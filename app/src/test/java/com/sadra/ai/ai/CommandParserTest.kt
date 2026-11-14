package com.sadra.ai.ai

import org.junit.Assert.*
import org.junit.Test

class CommandParserTest {
    @Test fun openRubika() = assertEquals(Command.OpenApp("روبیکا"), CommandParser.parse("روبیکا رو باز کن"))
    @Test fun alarmTomorrowSeven() = assertEquals(Command.SetAlarm(7, 0), CommandParser.parse("فردا ساعت هفت بیدارم کن"))
    @Test fun alarmHalfPastSevenEvening() = assertEquals(Command.SetAlarm(19, 30), CommandParser.parse("ساعت هفت و نیم عصر بیدارم کن"))
    @Test fun alarmPersianDigits() = assertEquals(Command.SetAlarm(6, 45), CommandParser.parse("ساعت ۶ و ۴۵ دقیقه بیدارم کن"))
    @Test fun timerDigits() = assertEquals(Command.SetTimer(300), CommandParser.parse("تایمر ۵ دقیقه"))
    @Test fun timerWords() = assertEquals(Command.SetTimer(1500), CommandParser.parse("یه تایمر بیست و پنج دقیقه بذار"))
    @Test fun volumeDown() = assertEquals(Command.Volume(false), CommandParser.parse("صدای گوشی رو کم کن"))
    @Test fun volumeUp() = assertEquals(Command.Volume(true), CommandParser.parse("صدا رو زیاد کن"))
    @Test fun volumeExactPersianDigits() = assertEquals(Command.Volume(percent = 50), CommandParser.parse("صدا را روی ۵۰ درصد بگذار"))
    @Test fun volumeExactWords() = assertEquals(Command.Volume(percent = 75), CommandParser.parse("ولوم را روی هفتاد و پنج درصد تنظیم کن"))
    @Test fun wifi() = assertEquals(Command.OpenWifiSettings, CommandParser.parse("برو تو تنظیمات وای فای"))
    @Test fun search() = assertEquals(Command.WebSearch("بوگاتی"), CommandParser.parse("درباره بوگاتی برام سرچ کن"))
    @Test fun next() = assertEquals(Command.Media(MediaAction.NEXT), CommandParser.parse("آهنگ بعدی"))
    @Test fun pause() = assertEquals(Command.Media(MediaAction.PAUSE), CommandParser.parse("آهنگ رو متوقف کن"))
    @Test fun playMood() = assertEquals(Command.Media(MediaAction.PLAY, "آهنگ شاد"), CommandParser.parse("یه آهنگ شاد پخش کن"))
    @Test fun time() = assertEquals(Command.CurrentTime, CommandParser.parse("ساعت چنده؟"))
    @Test fun smallTalkGoesToAi() = assertNull(CommandParser.parse("حوصله‌ام سر رفته"))
    @Test fun arabicLettersNormalized() = assertEquals(Command.OpenApp("کروم"), CommandParser.parse("كروم رو باز كن"))
}

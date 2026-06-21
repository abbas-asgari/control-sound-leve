# SpeedVolume - کنترل خودکار صدا بر اساس سرعت GPS

## ویژگی‌های اپلیکیشن

- **GPS دقیق** با استفاده از FusedLocationProvider گوگل
- **تغییر صدای روان** با انیمیشن ۰.۸ ثانیه‌ای (نه ناگهانی)
- **اجرا در پس‌زمینه** با Foreground Service (حتی وقتی اپ بسته است)
- **تغییر همزمان** صدای موسیقی و زنگ
- **نمایش Notification** با سرعت و صدای فعلی
- **رابط فارسی** با پشتیبانی RTL

## جدول سرعت ← صدا

| سرعت (km/h) | میزان صدا |
|---|---|
| زیر ۱۰ | ۲۰٪ |
| ۱۰ – ۳۰ | ۳۰٪ |
| ۳۰ – ۶۰ | ۵۰٪ |
| ۶۰ – ۹۰ | ۷۰٪ |
| ۹۰ – ۱۲۰ | ۸۵٪ |
| بالای ۱۲۰ | ۱۰۰٪ |

---

## مراحل ساخت APK در Android Studio

### پیش‌نیازها
- Android Studio Hedgehog (2023.1.1) یا جدیدتر
- حداقل ۸ گیگابایت RAM
- اتصال اینترنت برای دانلود وابستگی‌ها

### مراحل

**۱. باز کردن پروژه**
```
File → Open → پوشه SpeedVolume را انتخاب کنید
```

**۲. همگام‌سازی Gradle**
```
اگر Android Studio خودکار نپرسید:
File → Sync Project with Gradle Files
```

**۳. ساخت APK**
```
Build → Build Bundle(s)/APK(s) → Build APK(s)
```

**۴. پیدا کردن فایل APK**
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## نصب روی گوشی

**روش ۱ - کابل USB:**
1. Developer Options را در گوشی فعال کنید
2. USB Debugging را روشن کنید
3. در Android Studio: Run → Run 'app'

**روش ۲ - انتقال APK:**
1. فایل `app-debug.apk` را به گوشی منتقل کنید
2. در تنظیمات گوشی: نصب از منابع ناشناس را فعال کنید
3. روی فایل APK کلیک کنید و نصب کنید

---

## ساختار پروژه

```
SpeedVolume/
├── app/
│   ├── src/main/
│   │   ├── java/com/speedvolume/app/
│   │   │   ├── MainActivity.kt          ← رابط کاربری
│   │   │   └── SpeedVolumeService.kt    ← منطق اصلی + GPS + صدا
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml    ← طراحی صفحه اصلی
│   │   │   │   └── item_table_row.xml   ← ردیف جدول
│   │   │   └── values/
│   │   │       ├── colors.xml           ← رنگ‌ها
│   │   │       ├── strings.xml          ← متن‌ها
│   │   │       └── styles.xml           ← استایل‌ها
│   │   └── AndroidManifest.xml          ← مجوزها و تنظیمات
│   └── build.gradle                     ← وابستگی‌ها
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## سفارشی‌سازی

برای تغییر جدول سرعت-صدا، فایل `SpeedVolumeService.kt` را باز کنید و `SPEED_VOLUME_TABLE` را ویرایش کنید:

```kotlin
val SPEED_VOLUME_TABLE = listOf(
    Triple(0f,   10f,  20),   // زیر ۱۰  → ۲۰٪
    Triple(10f,  30f,  30),   // ۱۰–۳۰   → ۳۰٪
    // ... اعداد را تغییر دهید
)
```

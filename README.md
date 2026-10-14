# ChatDZ 🇩🇿 v3.1 – دردشة حقيقية بين الأصدقاء (Firebase)

تم إعداد المشروع بملف `google-services.json` الخاص بمشروعك:
**Project ID:** `chat-118ef`

---

## الحالة الحالية

| الخدمة              | الحالة                          |
|---------------------|---------------------------------|
| Authentication      | ✅ Anonymous مفعّل              |
| Cloud Firestore     | ✅ مفعّل                        |
| Storage             | ❌ غير مفعّل (اختياري)          |

**بدون Storage** يمكنك:
- إرسال **نصوص** و**استطلاعات** و**مواقع** بشكل Realtime بين الأجهزة
- إنشاء محادثات ومشاركة معرفها مع الأصدقاء

**مع Storage** (لاحقاً إن فعّلته) ستعمل الصور والصوت والملفات أيضاً.

> ملاحظة: Firebase Storage له خطة مجانية (Spark) بحدود معينة. يمكنك تفعيله لاحقاً بدون دفع إذا بقيت ضمن الحدود.

---

## طريقة الاستخدام بين الأصدقاء

1. كل صديق يثبّت التطبيق ويفتحه ويختار اسماً.
2. أحدكم ينشئ محادثة جديدة.
3. سيظهر **معرف المحادثة (Chat ID)** — انسخه وشاركه (واتساب / تيليجرام...).
4. الأصدقاء ينضمون بنفس المعرف (من زر الانضمام إن وُجد، أو يمكن إضافته لاحقاً).
5. الرسائل النصية تصل فوراً للجميع.

---

## البناء

افتح المشروع في **Android Studio** ثم:
```
Build → Make Project
Run
```

أو من الطرفية:
```bash
./gradlew assembleDebug
```

---

## ملفات مهمة

- `app/google-services.json` ← ملف مشروعك الحقيقي موجود
- `firebase/FirebaseChatRepository.kt` ← كل منطق Firebase
- `ui/components/AuthScreen.kt` ← شاشة الدخول

---

## قواعد Firestore المقترحة (للاختبار)

في Firebase Console → Firestore → Rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## الإصدار

**v3.1** – مدمج مع مشروعك `chat-118ef` + إرسال Realtime للنصوص والاستطلاعات والمواقع + دعم Storage اختياري.

Made with ❤️ 🇩🇿

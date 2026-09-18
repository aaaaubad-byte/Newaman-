# أمان — خريطة أزرار التطبيق وقاعدة البيانات

## النتيجة الأساسية

ملف `DATABASE_REBUILD_COMPLETE.sql` يعيد إنشاء المخطط، وسياسات RLS، والأدوار والصلاحيات، والـViews، والـTriggers، ودوال PostgreSQL التي تنفذ عمليات التطبيق. أما **التنقل بين التبويبات** فلا تنفذه قاعدة البيانات؛ بل ينفذه `MainActivity.kt` و`AmanViewModel.kt` عبر `navigateTo(route)`. قاعدة البيانات تنفذ العملية بعد ضغط الزر وتعيد النتيجة، ثم يعيد التطبيق تحميل البيانات وينقل المستخدم إلى المسار المناسب.

## ترتيب التنفيذ

ينفذ المستخدم الملف الكامل `DATABASE_REBUILD_COMPLETE.sql` مرة واحدة في Supabase SQL Editor بصلاحية مالك المشروع. لا يتم تنفيذ ملفات Android أو مفاتيح سرية داخل SQL Editor. بعد التنفيذ يجب إنشاء حساب تجريبي من التطبيق أو من Supabase Auth، لأن جدول `public.users` مرتبط بـ`auth.users`.

## مصفوفة العميل

| الشاشة أو الزر | ما يستدعيه التطبيق | البيانات التي يقرأها أو يكتبها | النتيجة والتنقل |
|---|---|---|---|
| تسجيل الدخول | Supabase Auth ثم `currentActor()` | `auth.users`, `users`, `roles` | يحدد نوع الحساب ويعرض `home` للعميل أو `overview` للإدارة |
| إنشاء حساب | Supabase Auth sign-up ثم ملف المستخدم عبر trigger | `auth.users`, `users` | تسجيل العميل ثم الدخول إلى `home` |
| إضافة رقم | `add_customer_number(p_number)` | يطابق `telecom_providers/provider_prefixes`، ثم ينشئ `phone_numbers/customer_numbers` | يعاد تحميل `customer_my_numbers` وتظهر نافذة نجاح؛ الرقم **غير مفعل للحماية** |
| تفعيل الحماية | `submit_protection_request(p_customer_number_id,p_package_id,p_payment_method_id,p_reference,p_idempotency_key)` | يقرأ الشركة والباقات ووسيلة الدفع، ويكتب `protection_requests` | ينشئ طلبًا، ثم ينتقل التطبيق إلى `requests` |
| تجديد الحماية | `submit_renewal_request(p_protection_id,p_package_id,p_payment_method_id,p_reference)` | يقرأ `protections/packages/payment_methods` ويكتب طلب تجديد | يظهر الطلب في `requests` |
| إعادة إرسال طلب مرفوض | `resubmit_rejected_request(...)` | يقرأ الطلب السابق ويكتب طلبًا جديدًا مرتبطًا بالسابق | يعود الطلب إلى المراجعة |
| تبويب الأرقام | قراءة `customer_my_numbers` و`customer_my_protections` | `customer_numbers`, `phone_numbers`, `protections` | جدول الرقم والشركة والحالة؛ التفاصيل لا تنشئ حماية تلقائيًا |
| تبويب الطلبات | قراءة `protection_requests` مع العلاقات | `protection_requests`, `packages`, `payment_methods` | جدول مختصر؛ النقر يفتح تفاصيل الطلب |
| تبويب الحمايات | قراءة `customer_my_protections` | `protections`, `packages`, `customer_numbers` | يعرض الحالة وتاريخ النهاية والأيام المتبقية |
| قراءة إشعار | PATCH على `client_notifications` | يغير `is_read/read_at` للعميل الحالي فقط | يفتح `action_route` إن وجد |
| تحديد كل الإشعارات كمقروءة | PATCH على `client_notifications` | يحدّث إشعارات العميل غير المقروءة | يبقى المستخدم في `notifications` |
| تعديل الحساب | `update_my_profile(p_full_name,p_phone)` | يحدّث `users` للمستخدم الحالي | يعرض رسالة نجاح ولا يغير التبويب |

## مصفوفة الإدارة

| الشاشة أو الزر | ما يستدعيه التطبيق | البيانات التي يقرأها أو يكتبها | النتيجة والتنقل |
|---|---|---|---|
| لوحة الإدارة | قراءة `get_admin_dashboard_summary()` وكيانات الإدارة | الطلبات، الحمايات، المهام، العملاء، المعاملات | تعرض مؤشرات وتفتح التبويب المرتبط عبر route |
| قبول طلب حماية | `approve_protection_request(p_request_id)` | يكتب `protections`, يحدّث `protection_requests`, وينشئ `payment_tasks`, `financial_transactions`, `client_notifications`, `audit_logs` | الطلب مقبول والحماية تبدأ؛ المهمة الأولية تظهر في `tasks` |
| اعتماد/قبول شامل | `verify_and_approve_protection_request(p_request_id)` | يتحقق من الدفع ثم ينفذ منطق القبول داخل معاملة قاعدة البيانات | يزيل الطلب من قائمة الانتظار ويحدّث الحماية والمهام |
| التحقق من الدفع | `verify_request_payment(p_request_id,p_verified,p_note)` | يحدّث حالة التحقق في `protection_requests` ويسجل المنفذ | يسمح للمستخدم المخول بمتابعة القبول |
| رفض الطلب | `reject_protection_request(p_request_id,p_rejection_reason)` | يحدّث الطلب وينشئ إشعارًا للعميل وسجل تدقيق | يظهر سبب الرفض للعميل في `requests` |
| إكمال المهمة | `complete_payment_task(p_task_id,p_telecom_ref)` | يحدّث `payment_tasks`, يكتب `manual_payment_logs`, وينشئ المهمة التالية حسب إعداد الشركة | تتحول المهمة إلى مكتملة وتظهر المهمة الدورية التالية عند تحقق شروط الحماية |
| جدولة المهمة | `reschedule_payment_task(p_task_id,p_new_due_date,p_reason)` | يغير موعد `payment_tasks` ويسجل السبب في `audit_logs` | تعود المهمة إلى قائمة المهام حسب نافذة التصنيف |
| تبويب المهام | قراءة `admin_payment_tasks` | يجمع `payment_tasks`, الشركة، الرقم، الحماية، والتصنيف الزمني | جدول الرقم والشركة والتصنيف؛ الأزرار: نسخ، إكمال، جدولة |
| تبويب الطلبات | قراءة `protection_requests` مع العلاقات | طلبات كل العملاء للمدير وفق RLS والصلاحية | جدول مختصر؛ التفاصيل تعرض القبول والرفض والتحقق |
| تبويب الحمايات | قراءة `admin_all_protections` | جميع الحمايات المسموحة للإدارة | جدول الرقم والشركة والحالة والتفاصيل |
| تبويب الأرقام | قراءة `customer_my_numbers` بصلاحية الإدارة | أرقام العملاء مع الشركة والحالة المشتقة من الحمايات النشطة | جدول الرقم والشركة والحالة؛ لا يعني وجود الرقم أنه محمي |
| تبويب العملاء | قراءة `users` بنوع `customer` | ملف العميل وإحصاءات أرقامه وحماياته | تفاصيل العميل دون كشف بيانات العملاء لبعضهم |
| إضافة/تعديل شركة | `admin_upsert_provider(...)` | يكتب `telecom_providers` ويسجل `audit_logs` | يعاد تحميل الكتالوج |
| إضافة بادئة شركة | `admin_upsert_provider_prefix(...)` | يكتب `provider_prefixes` | تستخدم البادئة لاكتشاف شركة الرقم |
| تعديل بادئات شركة | `admin_replace_provider_prefixes(p_provider_id,p_prefixes)` | يستبدل بادئات الشركة بشكل ذري | يعاد تحميل الشركات |
| إضافة/تعديل باقة | `admin_upsert_package(...)` | يكتب `packages` المرتبطة بـ`provider_id` | تظهر للعميل فقط الباقات النشطة، ويمكن فلترتها حسب الشركة |
| إضافة/تعديل وسيلة دفع | `admin_upsert_payment_method(...)` | يكتب `payment_methods` | تظهر للعميل في القائمة المنسدلة فقط إذا كانت نشطة |
| تعديل إعداد نظام | `admin_set_system_setting(p_key,p_value)` | يكتب `system_settings` | يعاد تحميل الإعدادات |
| إعدادات مهام الشركة | `admin_upsert_task_settings(...)` | يكتب إعدادات الدورة والفاصل والتفعيل لكل شركة | يؤثر على إنشاء المهمة التالية |
| تصنيف زمني للشركة | `admin_upsert_task_classification(...)` | يكتب حدود الأيام وترتيب التصنيف | يحدد ظهور المهمة كقادمة أو قريبة أو مستحقة |
| إعدادات الإشعارات | `admin_upsert_notification_setting(...)` | يكتب `notification_settings` | يتحكم في تنبيهات العميل والإدارة |
| قراءة إشعار إداري | PATCH على `admin_notifications` | يغير قراءة الإشعار للإدارة المسموحة | يبقى في `notifications` أو يفتح العنصر المرتبط |
| تعيين دور موظف | `admin_set_user_role(p_user_id,p_role_id)` | يحدّث `users.role_id` | تتغير الصلاحيات بعد إعادة تسجيل/تحميل الجلسة |
| تغيير حالة موظف | `admin_set_user_status(p_user_id,p_status)` | يحدّث `users.status` | الحساب الموقوف يمنع من استخدام التطبيق |
| صلاحيات الدور | `admin_replace_role_permissions(...)` | يستبدل `role_permissions` حسب رموز الصلاحيات | تحدد RLS والـRPC ما يمكن للموظف تنفيذه |

## القوائم المنسدلة

اختيار الباقة ووسيلة الدفع ليسا تنقلًا مستقلًا ولا يحتاجان جدولًا جديدًا. التطبيق يقرأ:

- الباقات من `packages` مع `provider_id = شركة الرقم` و`is_active = true`.
- وسائل الدفع من `payment_methods` مع `is_active = true`.
- الشركات والبادئات من `telecom_providers/provider_prefixes`.

بعد الاختيار يرسل التطبيق معرفات UUID إلى RPC الطلب، وليس الاسم المعروض للمستخدم. لذلك لا يجوز حذف شركة أو باقة مستخدمة في طلب سابق؛ يتم تعطيلها بواسطة `is_active` للحفاظ على السجل التاريخي.

## الأمان والعزل

كل RPC حساس يستخدم `security definer` مع `private.has_permission(...)`. سياسات RLS تمنع العميل من قراءة بيانات الإدارة أو عملاء آخرين، وتقيّد قراءات العميل بالسجلات التي يملكها. إخفاء زر الإدارة في الواجهة ليس آلية الأمان الوحيدة؛ التحقق الحقيقي موجود في RLS وRPC.

## تحقق بعد تشغيل SQL

```sql
select routine_name
from information_schema.routines
where routine_schema = 'public'
  and routine_name in (
    'add_customer_number',
    'submit_protection_request',
    'approve_protection_request',
    'reject_protection_request',
    'complete_payment_task',
    'reschedule_payment_task',
    'admin_upsert_provider',
    'admin_upsert_provider_prefix',
    'admin_upsert_package',
    'admin_upsert_payment_method',
    'admin_set_system_setting',
    'admin_upsert_notification_setting'
  )
order by routine_name;

select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
    'users','telecom_providers','provider_prefixes','packages',
    'payment_methods','phone_numbers','customer_numbers',
    'protection_requests','protections','payment_tasks',
    'task_settings','task_time_classifications',
    'client_notifications','admin_notifications'
  )
order by table_name;
```

إذا فشل استعلام أو زر بعد إعادة البناء، يجب تسجيل رسالة Supabase ورمز HTTP؛ لا ينبغي إنشاء جداول بديلة بأسماء مختلفة، لأن التطبيق مربوط بهذه الأسماء والـRPC المحددة أعلاه.

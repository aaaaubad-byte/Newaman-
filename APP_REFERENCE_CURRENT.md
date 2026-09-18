# AMAN Android Application — Current Reference

## 1. Purpose and current scope

AMAN is an Arabic RTL Android application for customer number management, protection requests, telecom protection packages, payment-method tracking, administration, and scheduled payment tasks. The current implementation uses Jetpack Compose and communicates with Supabase through HTTPS REST and RPC calls. The Android client uses only the Supabase URL and anonymous/publishable key; service-role keys and database passwords are prohibited.

The production source of truth for this reference is Supabase project `ilhgwludktlktzwxryhs` (New aman). The live RPC contracts use `p_full_name`/`p_email` for `update_my_profile`, `p_wallet_id`/`p_transfer_ref` for renewal and rejected-request submissions, and `p_new_due_date` for task rescheduling. Manager task completion uses `complete_payment_task(p_task_id uuid)` without a telecom reference. The live `admin_notifications` table uses `title`, `message`, `related_entity_type`, and `related_entity_id`; it has no employee-specific `admin_id` or `read_at` columns.

The current task model is provider-scoped. Adding a customer number does not create an administrative task. The first task is created only after an administrator approves a protection request. After that task is completed, the next recurring task is created using the interval configured for the number's telecom provider. Time classifications are notifications/work queues for administrators: they make approaching payment dates visible without requiring a telecom-provider API.

The current project does **not** implement a health-region or wage-type data model. No table, model, RPC, query, or screen for “health region by wage type” exists in this version.

## 2. Technology and project structure

| Area | Current implementation |
|---|---|
| Platform | Android, min SDK 24, target SDK 36 |
| UI | Kotlin Jetpack Compose, Material 3 |
| Language direction | Arabic RTL |
| Networking | OkHttp with Supabase REST/RPC endpoints |
| State | Kotlin coroutines and StateFlow in `AmanViewModel` |
| Authentication | Supabase Auth through `SupabaseClient` and `SessionManager` |
| Local security | PIN lock and biometric authentication support |
| Authorization | Android RBAC visibility plus server-side RLS/RPC enforcement |
| Database | Supabase PostgreSQL |
| Package | `com.aman.app` |
| Main activity | `com.example.MainActivity` |

Important source locations:

```text
app/src/main/java/com/example/MainActivity.kt
app/src/main/java/com/example/core/network/SupabaseClient.kt
app/src/main/java/com/example/core/network/SupabaseConfig.kt
app/src/main/java/com/example/data/model/Models.kt
app/src/main/java/com/example/data/repository/AmanRepository.kt
app/src/main/java/com/example/ui/viewmodel/AmanViewModel.kt
app/src/main/java/com/example/ui/screens/customer/CustomerScreens.kt
app/src/main/java/com/example/ui/screens/admin/AdminScreens.kt
app/src/main/java/com/example/ui/screens/auth/AuthScreens.kt
app/src/main/java/com/example/ui/screens/customer/CustomerExtraScreens.kt
```

## 3. Configuration and build

The application reads configuration in this order:

1. Root `supabase.local.properties` if present.
2. Gradle project properties.
3. Environment variables.

Required properties:

```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<anon-or-publishable-key>
```

The URL must be the project base URL, not a REST path. Never package `service_role`, `sb_secret`, a database password, a management API key, or an owner token.

Example local-only configuration is provided in:

```text
supabase.local.properties.example
```

A build without configuration succeeds technically but cannot authenticate or load project data. Always provide configuration before building an installable test APK.

Typical commands:

```bash
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk-21
./gradlew testDebugUnitTest assembleDebug
./gradlew testDebugUnitTest assembleRelease
```

Release output is unsigned unless a signing configuration is supplied. An unsigned release APK is for technical review only and is not a publishable production artifact.

## 4. Authentication and session flow

The authentication screens are in `AuthScreens.kt`:

- Login.
- Register.
- Password recovery.
- PIN lock.

The session flow is managed by `SessionManager`, `AmanViewModel`, and `SupabaseClient`. The application must be built with the correct Supabase properties for registration and login to work. A generic “تعذر” error commonly indicates an empty or invalid URL/key, an unavailable network, or a Supabase Auth/database response.

## 5. Customer journeys

### 5.1 Add a number

```text
Customer login
→ Add number
→ Validate number format and length
→ Detect provider by prefix
→ Call add_customer_number
→ Reload customer numbers
```

The customer does not manually choose the telecom provider. Provider detection is based on provider prefixes returned from Supabase.

### 5.2 Request protection

```text
Select customer number
→ Detect provider
→ Load active packages for that provider
→ Select package
→ Select active payment method
→ Enter external payment reference
→ Submit protection request
```

The package list is filtered by:

```text
package.provider_id == customer_number.provider_id
and package.is_active == true
```

The server remains the source of truth for package duration and price; the client must not be trusted to alter those values.

### 5.3 Protection approval

```text
Administrator reviews payment
→ Verify payment if required
→ Approve request
→ Create active protection
→ Create initial_activation task immediately
→ Notify customer
→ Record audit event
```

Adding a number alone does not create an administrative payment task.

### 5.4 Protection renewal

Renewal uses the existing protection reference and must remain separate from a new protection request. The server must enforce renewal eligibility and avoid overlapping active protections.

## 6. Administration journeys

Current administrative screens include:

- Overview/dashboard.
- Protection requests.
- Payment tasks.
- Audit log.
- Settings.
- Telecom providers.
- Payment methods/wallets.
- Protections.
- Customers.
- Customer numbers.
- Notifications.
- Roles and permissions.
- Employees.

### 6.1 Package management

The administration settings screen includes package management actions:

- Add package.
- Edit package.
- Select provider.
- Set protection duration in days.
- Set price.
- Set currency from system settings.
- Activate/deactivate package.
- Preserve sort order.

Package data is loaded from `packages` and written through the package RPC exposed by `AmanRepository`.

## 7. Current task model

### 7.1 Task types

| Value | Meaning |
|---|---|
| `initial_activation` | Immediate first payment task created after protection approval |
| `recurring` | Subsequent task created after the previous task is completed |

### 7.2 Provider-specific intervals

Each active provider has a `task_settings` row with its own `default_interval_days`. Example business values:

| Provider | Example interval |
|---|---:|
| Yemen Mobile | 90 days |
| YOU | 120 days |
| Sabafon | 180 days |
| Y | configured value |

### 7.3 Task lifecycle

```text
Protection approved
→ initial_activation due now
→ Administrator performs external payment
→ Administrator completes task using the system-known amount
→ recurring task due after provider interval
→ task enters configured time window
→ classification changes dynamically as due date approaches
```

The current version does not require a telecom-provider API. `days_remaining` is calculated from the internally scheduled due date. The task screen filters by provider and classification and orders by due date.

### 7.4 Classification meaning

Classifications are administrative work-queue windows, not external provider statuses. Example:

| Classification | Remaining days |
|---|---:|
| قادمة | 11–30 |
| قريبة | 1–10 |
| مستحقة | 0 |

The initial activation task is displayed as `مستحقة` independently of recurring time-window classification.

## 8. Supabase objects used by the app

### Core tables

```text
users
roles
permissions
role_permissions
telecom_providers
provider_prefixes
packages
payment_methods
phone_numbers
customer_numbers
protection_requests
protections
protection_history
payment_tasks
periodic_payment_plans
financial_transactions
transaction_numbers
manual_payment_logs
notifications/admin_notifications/client_notifications
task_settings
task_time_classifications
notification_settings
audit_logs
system_settings
```

### Important views

```text
my_profile
customer_my_numbers
customer_my_protections
admin_all_protections
admin_payment_tasks
```

### Important RPCs

```text
get_current_actor
add_customer_number
submit_protection_request
submit_renewal_request
resubmit_rejected_request
verify_request_payment
approve_protection_request
verify_and_approve_protection_request
reject_protection_request
complete_payment_task
reschedule_payment_task
admin_upsert_task_settings
admin_upsert_task_classification
admin_replace_provider_prefixes
admin_replace_role_permissions
get_admin_dashboard_summary
update_my_profile
admin_set_user_role
admin_set_user_status
```

The current live-schema reference is:

```text
DATABASE_CURRENT_STRUCTURE.csv
```

It is a snapshot/reference of the current production schema; it must not be used to recreate an older schema.

## 9. Data isolation and security

The customer must only receive:

- Their own profile.
- Their own numbers.
- Their own protection requests.
- Their own protections.
- Their own client notifications.
- Active public catalog data needed for the customer flow.

Administrative data such as audit logs, staff, roles, task settings, financial records, and internal notifications must remain protected by RLS and server-side permissions. UI hiding is not a security boundary.

All sensitive mutations should go through permission-checked RPCs. The Android application must never contain a service-role credential.

## 10. Current limitations and known gaps

The following are not implemented or cannot be claimed complete without additional requirements:

1. Health-region data by wage type is absent from the current model and UI.
2. Telecom-provider API integration is intentionally not part of the current scope.
3. Production Release signing is not configured in the source package.
4. A full instrumentation/UI test suite is not present; current automated coverage is primarily unit testing of task classification.
5. Full production verification requires running the configured APK against the actual Supabase project and testing role isolation.

## 11. Recommended next improvements

For a more professional production experience, prioritize:

1. A visible connection state and retry action.
2. Screen-level loading, empty, and error states.
3. A date picker for task rescheduling.
4. Independent customer and number detail screens in administration.
5. A dedicated package-management screen if package volume grows.
6. Protection against repeated taps and duplicate submissions.
7. A signed Release build configuration.
8. UI/instrumentation tests for registration, package selection, request submission, approval, and task completion.
9. A separately specified health-region/wage-type model before implementing that feature.

## 12. Latest UX and reliability improvements

The latest source revision adds a configuration guard in `SupabaseClient`: an APK built without Supabase properties now reports that the application is not connected instead of returning an opaque generic failure. Common Auth responses such as invalid credentials, unconfirmed email, duplicate email, and unreachable Supabase are translated to Arabic messages.

The administration package area now supports name/provider search, provider filtering, and an option to include or hide inactive packages. Task rescheduling now uses a native date picker rather than requiring manual `YYYY-MM-DD` entry. Existing task and protection forms already disable their primary actions while the shared operation is loading, and protection submission retains an idempotency key for repeated-tap protection.

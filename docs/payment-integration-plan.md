# Payment Integration Plan — Mindful Sports

## Overview

In-app purchase system for the Mindful Sports family of apps (Kotlin Multiplatform + Compose Multiplatform) supporting Android and iOS across 175+ countries.

This document covers two scopes:
1. **Single-app payment implementation** (original MindfulTennis plan)
2. **Multi-sport payment architecture** (isolation strategy for 10 separate apps)

---

## Technology Choice: RevenueCat KMP SDK

| Option | Verdict |
|---|---|
| **RevenueCat KMP** (official) | Best — official KMP SDK, Compose Multiplatform paywall UI, handles both stores |
| Adapty KMP | Good alternative, less mature KMP support |
| DIY (Play Billing + StoreKit 2) | Avoid — requires Swift bridging, no shared validation, high maintenance |

RevenueCat abstracts Google Play Billing + StoreKit 2 behind a single unified API, validates receipts server-side (security), and ships a prebuilt Compose Multiplatform Paywall UI. Free tier covers up to $2.5k/month revenue per app, then 1%.

### Full Platform Comparison

| Criteria | RevenueCat | Direct (Stores) | Adapty | Glassfy |
|----------|-----------|----------------|--------|---------|
| KMP support | ✅ | ⚠️ Manual | ✅ | ✅ |
| Server-side receipt validation | ✅ Auto | ❌ DIY | ✅ Auto | ✅ Auto |
| Cross-platform dashboard | ✅ | ❌ | ✅ | ✅ |
| Webhook events | ✅ Rich | ⚠️ Basic | ✅ | ✅ |
| Remote paywalls & A/B testing | ✅ | ❌ | ✅ | ✅ |
| Free tier | $2.5k MTR | $0 | $1k MTR | $500 MTR |
| Scaling cost | 1% of revenue | $0 | 1.5% | Flat fee |
| Per-app isolation | ✅ Projects | ✅ Accounts | ✅ Projects | ✅ |
| Community / docs | ⭐⭐⭐⭐⭐ | N/A | ⭐⭐⭐ | ⭐⭐ |

**Why not Direct (stores)?** Server-side receipt validation is non-negotiable for a subscription business. Without it, purchases can be spoofed. Doing it yourself adds weeks of backend infrastructure per platform. For 10 apps × 2 platforms, that multiplies to 20 billing integrations to maintain.

**Why not Adapty?** Credible alternative. Revisit if RevenueCat's 1% fee becomes painful above $50k MRR per app.

---

## Subscription Plans (Per App)

```
┌─────────────────────────────────────────────────────────┐
│  3-Day Free Trial  (auto-starts on any subscription)    │
├──────────────┬───────────────┬──────────────┬──────────┤
│   Monthly    │   Quarterly   │   Annual     │ Lifetime │
│   $7.99/mo   │  $17.99/3mo   │  $59.99/yr  │ $149.99  │
│              │  (~$6/mo 25%) │ (~$5/mo 37%)│ one-time │
└──────────────┴───────────────┴──────────────┴──────────┘
```

These price points apply to each sport independently. A user pays $7.99/month for tennis and a separate $7.99/month for badminton — they are entirely distinct subscriptions.

### Product ID Naming Convention

Each sport's store products follow the same pattern:

| Sport | Monthly | Quarterly | Annual | Lifetime |
|-------|---------|-----------|--------|---------|
| Tennis | `mindful_tennis_monthly` | `mindful_tennis_quarterly` | `mindful_tennis_annual` | `mindful_tennis_lifetime` |
| Badminton | `mindful_badminton_monthly` | `mindful_badminton_quarterly` | `mindful_badminton_annual` | `mindful_badminton_lifetime` |
| Pickleball | `mindful_pickleball_monthly` | `mindful_pickleball_quarterly` | `mindful_pickleball_annual` | `mindful_pickleball_lifetime` |
| Squash | `mindful_squash_monthly` | `mindful_squash_quarterly` | `mindful_squash_annual` | `mindful_squash_lifetime` |
| Table Tennis | `mindful_tabletennis_monthly` | `mindful_tabletennis_quarterly` | `mindful_tabletennis_annual` | `mindful_tabletennis_lifetime` |
| Padel | `mindful_padel_monthly` | `mindful_padel_quarterly` | `mindful_padel_annual` | `mindful_padel_lifetime` |
| Racquetball | `mindful_racquetball_monthly` | `mindful_racquetball_quarterly` | `mindful_racquetball_annual` | `mindful_racquetball_lifetime` |
| Platform Tennis | `mindful_platformtennis_monthly` | `mindful_platformtennis_quarterly` | `mindful_platformtennis_annual` | `mindful_platformtennis_lifetime` |
| Pop Tennis | `mindful_poptennis_monthly` | `mindful_poptennis_quarterly` | `mindful_poptennis_annual` | `mindful_poptennis_lifetime` |
| Beach Tennis | `mindful_beachtennis_monthly` | `mindful_beachtennis_quarterly` | `mindful_beachtennis_annual` | `mindful_beachtennis_lifetime` |

### RevenueCat Entitlement IDs (Same Across All 10 Projects)

```
premium_monthly
premium_quarterly
premium_annual
premium_lifetime
```

The product IDs differ per sport; the entitlement check in app code is always the same string. This means the `PremiumRepository` is written once and works for all 10 apps.

---

## Feature Gating

| Tier | Access |
|---|---|
| Free | Log sessions, view last 10 sessions in history |
| Trial (3 days) | Full premium access |
| Premium | Unlimited sessions, analytics, export, all future features |

---

## Multi-Sport Payment Isolation Architecture

> **The Rule:** Buying a subscription in one sport app gives NO access to any other sport app. A tennis subscription is for tennis only.

This is not just a product decision — it is required by App Store and Play Store guidelines when the apps are separate listings.

### Isolation is enforced at 4 independent layers

#### Layer 1 — Separate RevenueCat Projects

Each sport has its own RevenueCat project with its own API key. There is no way to share entitlements across projects.

```
RevenueCat Organization
├── Project: MindfulTennis         → API key: rc_tennis_xxx
├── Project: MindfulBadminton      → API key: rc_badminton_xxx
├── Project: MindfulPickleball     → API key: rc_pickleball_xxx
├── Project: MindfulSquash         → API key: rc_squash_xxx
├── Project: MindfulTableTennis    → API key: rc_tabletennis_xxx
├── Project: MindfulPadel          → API key: rc_padel_xxx
├── Project: MindfulRacquetball    → API key: rc_racquetball_xxx
├── Project: MindfulPlatformTennis → API key: rc_platformtennis_xxx
├── Project: MindfulPopTennis      → API key: rc_poptennis_xxx
└── Project: MindfulBeachTennis    → API key: rc_beachtennis_xxx
```

#### Layer 2 — Separate App Store / Play Store Listings

Each app is a distinct store listing with its own bundle ID:
- Android: `com.mindful.tennis`, `com.mindful.badminton`, ...
- iOS: `com.mindful.tennis`, `com.mindful.badminton`, ...

Apple and Google billing are tied to the app's bundle ID — cross-app subscription sharing is impossible at the store level.

#### Layer 3 — Supabase `subscriptions` Table (Sport-Scoped)

RevenueCat webhooks fire on every subscription lifecycle event. A Supabase Edge Function handles the webhook and writes to a `subscriptions` table tagged with `sport_id`. RLS ensures each app only reads its own sport's rows.

```sql
CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sport_id            TEXT NOT NULL,   -- "tennis", "badminton", etc.
    rc_customer_id      TEXT,
    status              TEXT NOT NULL CHECK (status IN (
                            'trial', 'active', 'expired', 'cancelled', 'grace_period'
                        )),
    plan                TEXT CHECK (plan IN ('monthly', 'quarterly', 'annual', 'lifetime')),
    is_trial            BOOLEAN DEFAULT FALSE,
    trial_ends_at       TIMESTAMPTZ,
    current_period_end  TIMESTAMPTZ,
    store               TEXT CHECK (store IN ('app_store', 'play_store')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, sport_id, store)
);

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- Users can only read their own subscription for the app's sport_id
-- sport_id is carried as a JWT custom claim set at login
CREATE POLICY "own_sport_subscriptions" ON subscriptions
    FOR ALL USING (
        auth.uid()::TEXT = user_id::TEXT AND
        sport_id = (auth.jwt() ->> 'sport_id')
    );

CREATE INDEX idx_subscriptions_user_sport ON subscriptions(user_id, sport_id, status);
```

#### Layer 4 — Compile-Time API Key Injection

The RevenueCat API key is baked into each app's binary at build time via `BuildConfig` (Android) and `xcconfig` (iOS). It is physically impossible for the tennis app to initialize RevenueCat with the badminton key at runtime.

```kotlin
// Initialized at app startup — sport-specific key from BuildConfig
Purchases.configure(
    PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_KEY)
        .appUserID(supabaseUserId)
        .build()
)
```

#### Why a User Cannot Game the System

1. **App Store / Play Store** — purchases are bound to the app's bundle ID at the store level
2. **RevenueCat** — purchases land in the sport-specific project; no cross-project entitlement query is possible
3. **Supabase RLS** — `sport_id` JWT claim filters all subscription reads at the database level
4. **Client code** — RevenueCat key is baked into a signed binary; modification requires reverse-engineering the app

---

## Architecture

```
shared/
  commonMain/
    domain/
      model/
        SubscriptionPlan.kt        ← Monthly | Quarterly | Annual | Lifetime
        SubscriptionStatus.kt      ← Trial | Active | Expired | Cancelled
      repository/
        SubscriptionRepository.kt  ← interface
      usecase/
        GetSubscriptionStatusUseCase.kt
        PurchaseSubscriptionUseCase.kt
        RestorePurchasesUseCase.kt
    ui/
      paywall/
        PaywallScreen.kt           ← RevenueCat Compose Multiplatform Paywall
        PaywallViewModel.kt
      settings/
        SubscriptionManagementScreen.kt

  androidMain/
    data/repository/
      SubscriptionRepositoryImpl.kt   ← RevenueCat Android SDK, key from BuildConfig

  iosMain/
    data/repository/
      SubscriptionRepositoryImpl.kt   ← RevenueCat iOS SDK, key from xcconfig

Supabase/
  subscriptions table               ← synced via RevenueCat webhook
  Edge Function: rc-webhook         ← validates & stores subscription events per sport
```

> No `expect/actual` pattern needed. RevenueCat's `purchases-kmp` library provides a unified API for both platforms directly in `commonMain`. The only platform split is SDK initialization (needs `Context` on Android).

---

## Data Flow

```
User taps "Start Free Trial"
        ↓
PaywallScreen (Compose MP)
        ↓
RevenueCat SDK → Google Play Billing / StoreKit 2
        ↓
Purchase confirmed → RC validates receipt server-side (sport-specific RC project)
        ↓
RC fires webhook → Supabase Edge Function (includes sport_id from RC metadata)
        ↓
Supabase `subscriptions` table updated (row tagged with sport_id)
        ↓
App reads entitlement from RC cache → unlocks premium features
```

---

## RevenueCat Webhook Events

| Event | Action |
|---|---|
| `INITIAL_PURCHASE` | Create row in `subscriptions`, status = active |
| `TRIAL_STARTED` | status = trial, record trial_ends_at |
| `TRIAL_CONVERTED` | status = active |
| `RENEWAL` | Update current_period_end |
| `CANCELLATION` | status = cancelled (access until period end) |
| `EXPIRATION` | status = expired |
| `BILLING_ISSUE` | status = grace_period |

Each event payload from RevenueCat includes the `app_user_id` (which is the Supabase user ID) and the product ID (which encodes the sport — e.g., `mindful_tennis_monthly`). The Edge Function derives `sport_id` from the product ID.

---

## Implementation Phases

### Phase 1 — Store & RevenueCat Setup (Day 1–2)

For **each sport app** (start with tennis, then replicate):

1. Create RevenueCat project → add iOS app + Android app
2. **App Store Connect**:
   - Create subscription group: "Mindful [Sport] Premium"
   - Create 3 auto-renewing subscriptions (monthly, quarterly, annual) with 3-day free trial introductory offer
   - Create 1 non-consumable IAP for lifetime
3. **Google Play Console**:
   - Create subscription with 3 base plans (monthly / quarterly / annual) with 3-day trial phase
   - Create 1 in-app product for lifetime purchase
4. Link both stores to RevenueCat dashboard
5. Create entitlements in RevenueCat: `premium_monthly`, `premium_quarterly`, `premium_annual`, `premium_lifetime`
6. Create an "Offering" with all 4 packages

### Phase 2 — Supabase Webhook (Day 2)

1. Create `subscriptions` table (schema above)
2. Write Supabase Edge Function `rc-webhook`:
   - Verify RevenueCat webhook signature (Authorization header)
   - Parse `sport_id` from product ID (e.g., split `mindful_tennis_monthly` → `tennis`)
   - Parse event type and upsert into `subscriptions` table
3. Register webhook URL in each RevenueCat project dashboard → Settings → Integrations

### Phase 3 — SDK Integration (Day 3–4)

Add to `gradle/libs.versions.toml`:
```toml
[versions]
revenuecat-kmp = "1.8.0"

[libraries]
revenuecat-kmp    = { module = "com.revenuecat.purchases:purchases-kmp",    version.ref = "revenuecat-kmp" }
revenuecat-kmp-ui = { module = "com.revenuecat.purchases:purchases-kmp-ui", version.ref = "revenuecat-kmp" }
```

Add to `shared/build.gradle.kts`:
```kotlin
commonMain.dependencies {
    implementation(libs.revenuecat.kmp)
    implementation(libs.revenuecat.kmp.ui)
}
```

Initialization (sport-specific key from `SportConfig`, which reads `BuildConfig.REVENUECAT_KEY`):
- **Android:** `Application.onCreate()` → `Purchases.configure(context, sportConfig.revenueCatKey)`
- **iOS:** `iOSApp.swift` → `Purchases.configure(withAPIKey: sportConfig.revenueCatKey)`
- **User identification:** use Supabase `user.id` as RevenueCat Customer ID — ties purchases to account across devices and app reinstalls

### Phase 4 — UI (Day 4–5)

1. **PaywallScreen**: Use RevenueCat's prebuilt `PaywallView` composable (Compose Multiplatform). Customize with sport-specific branding, plan descriptions, and trial messaging.
2. **SubscriptionManagementScreen**: Current plan, trial end date, "Manage Subscription" deep link to store settings, "Restore Purchases" button.
3. **Premium gating**: `GetSubscriptionStatusUseCase` returns non-premium → navigate to PaywallScreen.
4. Add `paywall` and `subscription_management` routes to the navigation graph.

### Phase 5 — Testing (Day 5–6)

1. **Android**: Google Play sandbox test accounts (Play Console → License Testing)
2. **iOS**: StoreKit sandbox accounts (Xcode → Settings → Accounts)
3. Test full lifecycle per sport: trial start → trial running → conversion → renewal → cancellation → expiration → restore
4. Verify Supabase webhook receives and stores all event types with correct `sport_id`
5. Test that purchasing tennis premium does NOT unlock badminton premium (the core isolation test)
6. Test offline entitlement cache (RC caches status locally — app works without network)

---

## Cost Summary

| Service | Cost |
|---|---|
| RevenueCat | Free ≤ $2.5k MRR per app (10 free tiers), then 1% of revenue |
| Apple App Store | 15% (< $1M revenue/year) or 30% |
| Google Play Store | 15% (first $1M) or 30% |
| Supabase | Existing plan, webhook Edge Functions included |

With 10 apps each on the free tier, you can generate up to **$25,000/month combined** before paying RevenueCat anything.

---

## Pre-Implementation Checklist (Manual Steps)

Before writing any code, complete these in the dashboards. Repeat for each sport:

- [ ] Create RevenueCat account + organization
- [ ] Create 10 RevenueCat projects (one per sport)
- [ ] **App Store Connect (×10):** subscription group + 3 subscriptions + 1 non-consumable + 3-day intro offers
- [ ] **Google Play Console (×10):** subscription + 3 base plans with 3-day trial + 1 in-app product
- [ ] Link all store apps to corresponding RevenueCat projects
- [ ] Obtain RevenueCat API keys (iOS + Android) for each sport → store in Azure Key Vault
- [ ] Register Supabase Edge Function webhook URL in each RevenueCat project

---

## References

- [RevenueCat KMP SDK](https://github.com/RevenueCat/purchases-kmp)
- [RevenueCat KMP Docs](https://www.revenuecat.com/docs/getting-started/installation/kotlin-multiplatform)
- [RevenueCat Compose Paywall](https://www.revenuecat.com/docs/tools/paywalls)
- [RevenueCat Webhook Docs](https://www.revenuecat.com/docs/integrations/webhooks)
- [Google Play Billing Integration](https://developer.android.com/google/play/billing/integrate)
- [Google Play Subscriptions & Trials](https://developer.android.com/google/play/billing/subscriptions)
- [App Store Introductory Offers](https://developer.apple.com/documentation/storekit/offering-introductory-pricing-in-your-app)

# Multi-Sport Racket App — Architecture & Build Pipeline Analysis

> **Branch:** `feature/multi-sport-architecture`
> **Date:** 2026-04-09
> **Status:** Planning / Pre-implementation

---

## Table of Contents

1. [The 10 Sports](#the-10-sports)
2. [Architecture Overview](#architecture-overview)
3. [Payment Platform Analysis](#payment-platform-analysis)
4. [Payment Isolation Strategy](#payment-isolation-strategy)
5. [Azure DevOps CI/CD Pipeline](#azure-devops-cicd-pipeline)
6. [Fastlane Setup](#fastlane-setup)
7. [Database Strategy](#database-strategy)
8. [Build Configuration](#build-configuration)
9. [Implementation Sequence](#implementation-sequence)

---

## The 10 Sports

| # | Sport | App Name | Scoring System | Unique Skill Dimensions |
|---|-------|----------|----------------|------------------------|
| 1 | Tennis | Mindful Tennis | 6-game sets, deuce, tiebreak | Volley, Slice |
| 2 | Badminton | Mindful Badminton | Rally to 21, best of 3 | Smash, Clear, Drop Shot |
| 3 | Pickleball | Mindful Pickleball | 11 pts, win by 2 | Dink, Third-Shot Drop |
| 4 | Squash | Mindful Squash | 11 pts, best of 5 | Drop Shot, Boast |
| 5 | Table Tennis | Mindful Table Tennis | 11 pts, best of 7 | Loop/Topspin, Block |
| 6 | Padel | Mindful Padel | Same as tennis | Bandeja, Víbora |
| 7 | Racquetball | Mindful Racquetball | 15 pts, 2 games + tiebreak | Kill Shot, Ceiling Ball |
| 8 | Platform Tennis | Mindful Platform Tennis | Like tennis, screen play | Screen Rally, Lob |
| 9 | Pop Tennis | Mindful Pop Tennis | Like tennis, no-ad option | Overhead, Touch Volley |
| 10 | Beach Tennis | Mindful Beach Tennis | Like tennis, no-bounce | Sand Movement, Smash |

---

## Architecture Overview

```
ONE CODEBASE
├── shared/                        ← KMP: all business logic, repos, sync, DI
│   └── commonMain/
│       ├── sport/                 ← NEW: SportConfig, SportRegistry, ScoringRules
│       ├── domain/model/          ← sport-agnostic models (+ sport_id field)
│       └── ...existing...
├── composeApp/                    ← Android: 10 product flavors
│   └── src/
│       ├── tennisMain/            ← Tennis assets, colors, icons
│       ├── badmintonMain/         ← Badminton assets, colors, icons
│       └── ... (10 flavor dirs)
├── iosApp/                        ← iOS: 10 Xcode schemes, one project
│   ├── Configurations/
│   │   ├── Tennis.xcconfig
│   │   ├── Badminton.xcconfig
│   │   └── ...
│   └── Assets.xcassets/
│       ├── Tennis/AppIcon.appiconset
│       ├── Badminton/AppIcon.appiconset
│       └── ...
├── fastlane/                      ← NEW: Fastlane for automated builds & uploads
│   ├── Fastfile
│   ├── Appfile
│   └── Matchfile
└── azure-pipelines/               ← NEW: Azure DevOps pipeline definitions
    ├── android-pipeline.yml
    ├── ios-pipeline.yml
    └── templates/
        ├── android-sport.yml
        └── ios-sport.yml
```

**Core principle:** One shared module, 20 deployable apps (10 Android + 10 iOS). Credentials and assets are the only things that differ at compile time. All business logic, sync, and UI structure is written once.

---

## Payment Platform Analysis

### The Candidates

Four serious options exist for in-app subscription management in a KMP app:

---

### Option 1: RevenueCat ⭐ Recommended

**What it is:** A dedicated mobile subscription management platform that sits between your app and the App Store / Play Store billing APIs. It handles purchase validation, receipt verification, entitlement logic, and analytics — across both platforms from one SDK.

**What it offers:**

| Capability | Detail |
|-----------|--------|
| **Cross-platform SDK** | Kotlin Multiplatform-compatible via their Android + iOS SDKs. One abstraction layer wraps StoreKit 2 (iOS) and Google Play Billing (Android) |
| **Entitlements** | Named access levels (`premium_monthly`, `premium_annual`) that your app checks — completely decoupled from product IDs. Product IDs can change; entitlement checks never do |
| **Server-side receipt validation** | RevenueCat's servers validate receipts with Apple/Google. You never validate receipts in your app code — eliminates an entire class of fraud |
| **Webhook events** | Fires events on subscription started, renewed, cancelled, billing retry, refunded — lets your Supabase backend react to subscription state changes |
| **Customer portal** | RevenueCat dashboard shows every subscriber, their status, transaction history, and lifetime value — without you building any of this |
| **Paywalls SDK** | Pre-built paywall UI that can be A/B tested and updated remotely without app releases |
| **Offerings** | Remote configuration of which products to show — change pricing experiments without a code deploy |
| **Free tier** | $0 up to $2,500 MTR (monthly tracked revenue) — enough to validate each sport before paying |
| **Per-app isolation** | Each app is a separate RevenueCat "Project". Subscriptions are completely isolated by default |
| **Analytics** | Churn, MRR, LTV, conversion rates broken down per app, per entitlement, per country |
| **Restore purchases** | One-line API call handles the "Restore Purchases" requirement from both stores |

**Pricing:** Free up to $2.5k MTR, then 1% of revenue. For 10 apps, each app's revenue is counted separately against its own MTR threshold.

**KMP Integration:**

```kotlin
// shared/commonMain — define entitlements
object Entitlements {
    const val PREMIUM_MONTHLY  = "premium_monthly"
    const val PREMIUM_ANNUAL   = "premium_annual"
    const val PREMIUM_LIFETIME = "premium_lifetime"
}

// The RevenueCat SDK is initialized per-platform with the sport-specific key.
// Platform check is done in androidMain/iosMain actual implementations.
```

---

### Option 2: Google Play Billing + StoreKit 2 (Direct)

**What it is:** Use both stores' native billing APIs directly, with no intermediary.

**Pros:**
- Zero ongoing cost (no revenue share)
- Full control over the billing flow

**Cons:**
- Must write and maintain two completely separate billing implementations (StoreKit 2 for iOS, Google Play Billing for Android)
- Server-side receipt validation is your responsibility — if you skip it, purchases can be spoofed
- No unified dashboard — you check Play Console and App Store Connect separately
- No webhook events — you must poll or use platform-specific server notifications (both have different formats and auth mechanisms)
- For 10 apps, you manage 20 separate billing integrations
- Entitlement logic lives in your code — if a receipt expires or is refunded, you find out via polling

**Verdict:** Viable for a single app. For 10 apps with cross-platform builds, the maintenance burden multiplies fast.

---

### Option 3: Adapty

**What it is:** A RevenueCat competitor with a similar feature set.

**Pros:**
- KMP SDK available (Adapty published a Kotlin Multiplatform SDK in 2024)
- Remote paywalls with A/B testing
- Free tier: up to $1k MTR

**Cons:**
- Smaller community and ecosystem than RevenueCat
- Webhook events less mature
- Analytics dashboard less detailed
- RevenueCat has longer track record and larger user base for debugging edge cases

**Verdict:** A credible alternative if RevenueCat pricing becomes a concern at scale. Worth reconsidering if you reach $50k+ MTR per app.

---

### Option 4: Glassfy

**What it is:** A newer subscription SDK, positioned as a lighter alternative to RevenueCat.

**Pros:**
- KMP support
- Per-app pricing model (flat fee rather than % of revenue above threshold)

**Cons:**
- Smallest community of the four
- Less documentation for KMP-specific integration
- Webhook ecosystem less developed

**Verdict:** Only consider if you have a strong revenue projection that makes RevenueCat's % model expensive.

---

### Payment Platform Decision Matrix

| Criteria | RevenueCat | Direct (Stores) | Adapty | Glassfy |
|----------|-----------|----------------|--------|---------|
| KMP support | ✅ | ⚠️ Manual | ✅ | ✅ |
| Server validation | ✅ Auto | ❌ DIY | ✅ Auto | ✅ Auto |
| Cross-platform dashboard | ✅ | ❌ | ✅ | ✅ |
| Webhook events | ✅ Rich | ⚠️ Basic | ✅ | ✅ |
| Remote paywalls | ✅ | ❌ | ✅ | ✅ |
| Free tier | $2.5k MTR | $0 | $1k MTR | $500 MTR |
| Scaling cost | 1% of revenue | $0 | 1.5% | Flat fee |
| Per-app isolation | ✅ Projects | ✅ Accounts | ✅ Projects | ✅ |
| Community / docs | ⭐⭐⭐⭐⭐ | N/A | ⭐⭐⭐ | ⭐⭐ |

**Recommendation: RevenueCat.** The free tier covers validation of all 10 apps. The KMP integration is well-documented. Server-side receipt validation is non-negotiable for a subscription business — doing it yourself adds weeks of infrastructure work that RevenueCat eliminates.

---

## Payment Isolation Strategy

### The Rule

> **Buying a subscription in one sport app gives NO access to any other sport app.** A tennis subscription is for tennis only.

This is not just a feature — it is required by App Store and Play Store guidelines when the apps are separate listings.

### How Isolation Works

#### Layer 1 — Separate RevenueCat Projects

Each sport has its own RevenueCat project with its own API key. There is no way to share entitlements across projects by default.

```
RevenueCat Organization
├── Project: MindfulTennis       → API key: rc_tennis_xxx
├── Project: MindfulBadminton    → API key: rc_badminton_xxx
├── Project: MindfulPickleball   → API key: rc_pickleball_xxx
└── ... (10 projects)
```

#### Layer 2 — Separate App Store / Play Store Listings

Each app is a distinct listing with its own:
- Android: `com.mindful.tennis`, `com.mindful.badminton`, ...
- iOS: `com.mindful.tennis`, `com.mindful.badminton`, ...
- Each listing has its own subscription products (e.g., `mindful_tennis_premium_monthly`)
- Apple and Google billing are tied to the app's bundle ID — cross-app access is impossible at the store level

#### Layer 3 — Supabase Subscription State (Sport-Scoped)

RevenueCat webhooks fire when a subscription is purchased, renewed, or cancelled. A Supabase Edge Function handles the webhook and writes to a `subscriptions` table:

```sql
CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    sport_id    TEXT NOT NULL,                         -- "tennis", "badminton", etc.
    status      TEXT NOT NULL,                         -- "active", "expired", "cancelled"
    product_id  TEXT NOT NULL,                         -- "mindful_tennis_premium_monthly"
    platform    TEXT NOT NULL,                         -- "ios" or "android"
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, sport_id, platform)
);

-- RLS: users see only their own subscriptions, filtered by sport
CREATE POLICY "own_subscriptions" ON subscriptions
    FOR ALL USING (
        auth.uid() = user_id AND
        sport_id = (auth.jwt() ->> 'sport_id')
    );
```

The app reads its own subscription status from this table — it only sees rows for its `sport_id`.

#### Layer 4 — Client-Side Entitlement Check

```kotlin
// shared/commonMain
class PremiumRepository(
    private val revenueCat: RevenueCatClient,  // sport-specific key injected at startup
    private val sportConfig: SportConfig,
) {
    suspend fun isPremium(userId: String): Boolean {
        return revenueCat.getCustomerInfo(userId)
            .entitlements[Entitlements.PREMIUM_MONTHLY]
            ?.isActive == true
    }
}
```

Each app's `RevenueCatClient` is initialized with that sport's API key — it is physically impossible to check another sport's entitlement.

#### Why a User Cannot Game the System

1. **App Store / Play Store** — purchases are bound to the app's bundle ID
2. **RevenueCat** — purchases land in the sport-specific project, no cross-project query is possible
3. **Supabase RLS** — `sport_id` JWT claim filters all subscription reads at the database level
4. **Client code** — RevenueCat key is baked into the build at compile time; runtime modification would require reverse-engineering a signed build

---

## Azure DevOps CI/CD Pipeline

### Why Azure DevOps Instead of GitHub Actions

| Factor | GitHub Actions | Azure DevOps |
|--------|---------------|--------------|
| Integration with Azure services | Good | Native |
| macOS Hosted Agents | Available (limited minutes on free) | Available (Microsoft-hosted or self-hosted) |
| Variable Groups & Key Vault | Secrets only | Full Azure Key Vault integration for 10 sets of credentials |
| Pipeline templates | Reusable workflows | YAML templates + template repositories |
| Parallel jobs | Limited on free tier | Configurable parallelism |
| Artifact management | GitHub Packages | Azure Artifacts (better for large APKs/IPAs) |
| Audit logging | Basic | Enterprise-grade |

For 10 apps × 2 platforms = 20 pipelines, Azure DevOps Variable Groups backed by Azure Key Vault is significantly cleaner than managing 200+ GitHub secrets.

---

### Credential Storage — Azure Key Vault

```
Azure Key Vault: mindful-sports-kv
├── tennis-supabase-url
├── tennis-supabase-anon-key
├── tennis-revenuecat-android-key
├── tennis-revenuecat-ios-key
├── tennis-google-client-id
├── tennis-keystore-password
├── tennis-keystore-alias
├── tennis-keystore-file-base64
├── badminton-supabase-url
└── ... (8 keys × 10 sports = 80 secrets)
```

One Azure DevOps Variable Group per sport links to the Key Vault. Pipelines reference the group — no secrets in YAML files.

---

### Pipeline Structure

```
azure-pipelines/
├── android-pipeline.yml          ← triggers all 10 Android builds
├── ios-pipeline.yml              ← triggers all 10 iOS builds
└── templates/
    ├── android-sport-build.yml   ← reusable template for one Android sport
    └── ios-sport-build.yml       ← reusable template for one iOS sport
```

---

### `azure-pipelines/templates/android-sport-build.yml`

```yaml
# Reusable template — build one Android sport flavor
parameters:
  - name: sportId         # e.g. "tennis"
    type: string
  - name: sportFlavorName # e.g. "Tennis" (capitalized)
    type: string
  - name: packageName     # e.g. "com.mindful.tennis"
    type: string

steps:
  - task: AzureKeyVault@2
    displayName: 'Load ${{ parameters.sportId }} secrets'
    inputs:
      azureSubscription: 'MindfulSports-ServiceConnection'
      KeyVaultName: 'mindful-sports-kv'
      SecretsFilter: >
        ${{ parameters.sportId }}-supabase-url,
        ${{ parameters.sportId }}-supabase-anon-key,
        ${{ parameters.sportId }}-revenuecat-android-key,
        ${{ parameters.sportId }}-google-client-id,
        ${{ parameters.sportId }}-keystore-password,
        ${{ parameters.sportId }}-keystore-alias,
        ${{ parameters.sportId }}-keystore-file-base64

  - script: |
      echo "$(${{ parameters.sportId }}-keystore-file-base64)" | base64 -d > keystore.jks
    displayName: 'Decode keystore'

  - script: |
      cat >> local.properties << EOF
      ${{ parameters.sportId }}.supabase.url=$(${{ parameters.sportId }}-supabase-url)
      ${{ parameters.sportId }}.supabase.anon.key=$(${{ parameters.sportId }}-supabase-anon-key)
      ${{ parameters.sportId }}.revenuecat.android.key=$(${{ parameters.sportId }}-revenuecat-android-key)
      ${{ parameters.sportId }}.google.client.id=$(${{ parameters.sportId }}-google-client-id)
      EOF
    displayName: 'Write local.properties'

  - task: Gradle@3
    displayName: 'Build ${{ parameters.sportFlavorName }} Release AAB'
    inputs:
      gradleWrapperFile: 'gradlew'
      tasks: ':composeApp:bundle${{ parameters.sportFlavorName }}Release'
      options: >
        -Pandroid.injected.signing.store.file=$(System.DefaultWorkingDirectory)/keystore.jks
        -Pandroid.injected.signing.store.password=$(${{ parameters.sportId }}-keystore-password)
        -Pandroid.injected.signing.key.alias=$(${{ parameters.sportId }}-keystore-alias)
        -Pandroid.injected.signing.key.password=$(${{ parameters.sportId }}-keystore-password)

  - task: GooglePlayRelease@4
    displayName: 'Upload to Play Store internal track'
    inputs:
      serviceAccountJsonPlainText: '$(play-store-service-account-json)'
      applicationId: '${{ parameters.packageName }}'
      action: 'SingleBundle'
      bundleFile: 'composeApp/build/outputs/bundle/${{ parameters.sportId }}Release/*.aab'
      track: 'internal'
```

---

### `azure-pipelines/android-pipeline.yml`

```yaml
trigger:
  branches:
    include:
      - main
  paths:
    include:
      - composeApp/**
      - shared/**

pool:
  vmImage: 'ubuntu-latest'

strategy:
  matrix:
    Tennis:
      sportId: tennis
      sportFlavorName: Tennis
      packageName: com.mindful.tennis
    Badminton:
      sportId: badminton
      sportFlavorName: Badminton
      packageName: com.mindful.badminton
    Pickleball:
      sportId: pickleball
      sportFlavorName: Pickleball
      packageName: com.mindful.pickleball
    Squash:
      sportId: squash
      sportFlavorName: Squash
      packageName: com.mindful.squash
    TableTennis:
      sportId: tabletennis
      sportFlavorName: Tabletennis
      packageName: com.mindful.tabletennis
    Padel:
      sportId: padel
      sportFlavorName: Padel
      packageName: com.mindful.padel
    Racquetball:
      sportId: racquetball
      sportFlavorName: Racquetball
      packageName: com.mindful.racquetball
    PlatformTennis:
      sportId: platformtennis
      sportFlavorName: Platformtennis
      packageName: com.mindful.platformtennis
    PopTennis:
      sportId: poptennis
      sportFlavorName: Poptennis
      packageName: com.mindful.poptennis
    BeachTennis:
      sportId: beachtennis
      sportFlavorName: Beachtennis
      packageName: com.mindful.beachtennis

  maxParallel: 5   # Run 5 sport builds simultaneously

steps:
  - template: templates/android-sport-build.yml
    parameters:
      sportId: $(sportId)
      sportFlavorName: $(sportFlavorName)
      packageName: $(packageName)
```

---

### `azure-pipelines/templates/ios-sport-build.yml`

```yaml
parameters:
  - name: sportId
    type: string
  - name: sportName          # e.g. "Tennis"
    type: string
  - name: bundleId           # e.g. "com.mindful.tennis"
    type: string

steps:
  - task: AzureKeyVault@2
    displayName: 'Load ${{ parameters.sportId }} secrets'
    inputs:
      azureSubscription: 'MindfulSports-ServiceConnection'
      KeyVaultName: 'mindful-sports-kv'
      SecretsFilter: >
        ${{ parameters.sportId }}-supabase-url,
        ${{ parameters.sportId }}-supabase-anon-key,
        ${{ parameters.sportId }}-revenuecat-ios-key,
        ${{ parameters.sportId }}-google-client-id,
        ${{ parameters.sportId }}-match-certificates-password

  - task: InstallAppleCertificate@2
    displayName: 'Install distribution certificate'
    inputs:
      certSecureFile: 'distribution.p12'
      certPwd: '$(${{ parameters.sportId }}-match-certificates-password)'

  - task: InstallAppleProvisioningProfile@1
    displayName: 'Install provisioning profile'
    inputs:
      provProfileSecureFile: '${{ parameters.sportId }}_distribution.mobileprovision'

  - script: |
      bundle exec fastlane ios build_sport \
        sport:"${{ parameters.sportId }}" \
        sport_name:"${{ parameters.sportName }}" \
        bundle_id:"${{ parameters.bundleId }}" \
        supabase_url:"$(${{ parameters.sportId }}-supabase-url)" \
        supabase_key:"$(${{ parameters.sportId }}-supabase-anon-key)" \
        revenuecat_key:"$(${{ parameters.sportId }}-revenuecat-ios-key)"
    displayName: 'Fastlane build & upload ${{ parameters.sportName }}'
```

---

## Fastlane Setup

Fastlane handles code signing (via `match`), building, and App Store Connect uploads. Azure DevOps calls Fastlane — Azure handles scheduling and secrets injection, Fastlane handles the Apple toolchain.

### Why Fastlane

| Capability | Detail |
|-----------|--------|
| `match` | Syncs code signing certificates and provisioning profiles across machines via a git repo or Azure Blob Storage. One command sets up any CI agent |
| `gym` | Wraps `xcodebuild archive` with sane defaults, better error output, and automatic IPA export |
| `deliver` / `pilot` | Uploads to App Store Connect / TestFlight without touching the web UI |
| `supply` | Uploads Android AABs to Play Store (alternative to the Google Play DevOps task) |
| `spaceship` | Underlying API client — lets you automate App Store Connect tasks (create listings, manage testers) |
| Multi-platform | One `Fastfile` handles both iOS and Android lanes |

### Directory Structure

```
fastlane/
├── Fastfile           ← lane definitions
├── Appfile            ← app identifiers (overridden per lane)
├── Matchfile          ← code signing config
└── Pluginfile         ← fastlane plugins
```

---

### `fastlane/Matchfile`

```ruby
# Certificates and profiles stored in Azure Blob Storage
# (alternative to a private git repo — works better in Azure ecosystem)
storage_mode("azure_storage")
azure_storage_account(ENV["AZURE_STORAGE_ACCOUNT"])
azure_storage_access_key(ENV["AZURE_STORAGE_ACCESS_KEY"])
azure_storage_container("fastlane-match-certs")

type("appstore")
readonly(true)    # CI only reads, never generates new certs
```

---

### `fastlane/Fastfile`

```ruby
default_platform(:ios)

# ─── SHARED HELPERS ──────────────────────────────────────────────────────────

SPORTS = %w[
  tennis badminton pickleball squash tabletennis
  padel racquetball platformtennis poptennis beachtennis
].freeze

SPORT_NAMES = {
  "tennis"         => "Tennis",
  "badminton"      => "Badminton",
  "pickleball"     => "Pickleball",
  "squash"         => "Squash",
  "tabletennis"    => "Table Tennis",
  "padel"          => "Padel",
  "racquetball"    => "Racquetball",
  "platformtennis" => "Platform Tennis",
  "poptennis"      => "Pop Tennis",
  "beachtennis"    => "Beach Tennis",
}.freeze

# ─── iOS LANES ───────────────────────────────────────────────────────────────

platform :ios do

  # Called by Azure DevOps for each sport — params injected from Key Vault
  lane :build_sport do |options|
    sport      = options[:sport]
    bundle_id  = options[:bundle_id]
    sport_name = options[:sport_name]

    # Sync code signing assets for this bundle ID
    match(
      type: "appstore",
      app_identifier: bundle_id,
      readonly: true,
    )

    # Build the IPA using the sport-specific scheme
    gym(
      scheme:              "Mindful#{sport_name.gsub(' ', '')}",
      configuration:       "Release-#{sport_name.gsub(' ', '')}",
      export_method:       "app-store",
      output_directory:    "build/ios/#{sport}",
      output_name:         "Mindful#{sport_name.gsub(' ', '')}.ipa",
      xcargs:              "SUPABASE_URL=#{options[:supabase_url]} SUPABASE_ANON_KEY=#{options[:supabase_key]} REVENUECAT_KEY=#{options[:revenuecat_key]}",
    )

    # Upload to TestFlight
    pilot(
      app_identifier:  bundle_id,
      ipa:             "build/ios/#{sport}/Mindful#{sport_name.gsub(' ', '')}.ipa",
      skip_waiting_for_build_processing: true,
      changelog:       "Automated build from Azure DevOps — #{sport_name}",
    )
  end

  # Local lane: build all 10 sports from dev machine
  lane :build_all do
    SPORTS.each do |sport|
      sport_name = SPORT_NAMES[sport]
      bundle_id  = "com.mindful.#{sport}"
      build_sport(
        sport:         sport,
        sport_name:    sport_name,
        bundle_id:     bundle_id,
        supabase_url:  ENV["#{sport.upcase}_SUPABASE_URL"],
        supabase_key:  ENV["#{sport.upcase}_SUPABASE_ANON_KEY"],
        revenuecat_key: ENV["#{sport.upcase}_REVENUECAT_IOS_KEY"],
      )
    end
  end

  # Local lane: setup match certs for all 10 apps (run once on new machine)
  lane :setup_signing do
    SPORTS.each do |sport|
      match(
        type:           "appstore",
        app_identifier: "com.mindful.#{sport}",
        readonly:       false,  # generates if missing
      )
    end
  end

  # Create App Store Connect listings for all 10 apps (run once)
  lane :create_app_store_listings do
    SPORTS.each do |sport|
      produce(
        app_identifier: "com.mindful.#{sport}",
        app_name:       "Mindful #{SPORT_NAMES[sport]}",
        language:       "English",
        app_version:    "1.0",
        sku:            "mindful_#{sport}",
      )
    end
  end

end

# ─── ANDROID LANES ───────────────────────────────────────────────────────────

platform :android do

  lane :build_sport do |options|
    sport            = options[:sport]
    sport_capitalized = sport.capitalize

    gradle(
      task:            "bundle",
      flavor:          sport_capitalized,
      build_type:      "Release",
      project_dir:     "./",
      flags:           "--no-daemon",
      properties: {
        "android.injected.signing.store.file"     => options[:keystore_path],
        "android.injected.signing.store.password" => options[:keystore_password],
        "android.injected.signing.key.alias"      => options[:key_alias],
        "android.injected.signing.key.password"   => options[:key_password],
      }
    )

    supply(
      package_name:   "com.mindful.#{sport}",
      aab:            "composeApp/build/outputs/bundle/#{sport}Release/*.aab",
      track:          "internal",
      release_status: "draft",
    )
  end

  lane :build_all do
    SPORTS.each do |sport|
      build_sport(
        sport:             sport,
        keystore_path:     ENV["#{sport.upcase}_KEYSTORE_PATH"],
        keystore_password: ENV["#{sport.upcase}_KEYSTORE_PASSWORD"],
        key_alias:         ENV["#{sport.upcase}_KEY_ALIAS"],
        key_password:      ENV["#{sport.upcase}_KEY_PASSWORD"],
      )
    end
  end

end
```

---

### Code Signing Strategy (match)

| Approach | For |
|----------|-----|
| `match` with Azure Blob Storage | Stores all 10 distribution certificates + 10 provisioning profiles in one container |
| `readonly: true` on CI | CI agents only download — never modify certs |
| `readonly: false` locally | A designated team member regenerates certs when they expire |
| Separate `match` call per bundle ID | Each of the 10 apps has its own provisioning profile |

This replaces manual Xcode-managed signing and eliminates "certificate expired" build failures.

---

## Database Strategy

### Single Supabase Project — Sport-Scoped Data

All 10 apps share **one Supabase project**:
- One `users` table — sign in once, identity works across all sports
- All sport data is tagged with `sport_id` column
- RLS policies enforce isolation — the badminton app never reads tennis data

### Schema Additions

```sql
-- Add sport_id discriminator to all sport-specific tables
ALTER TABLE sessions       ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';
ALTER TABLE focus_points   ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';
ALTER TABLE opponents      ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';
ALTER TABLE partners       ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';

-- Subscription state (driven by RevenueCat webhooks)
CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sport_id    TEXT NOT NULL,
    status      TEXT NOT NULL CHECK (status IN ('active', 'expired', 'cancelled', 'grace_period')),
    product_id  TEXT NOT NULL,
    platform    TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, sport_id, platform)
);

-- Performance indexes
CREATE INDEX idx_sessions_sport    ON sessions(user_id, sport_id, updated_at);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id, sport_id, status);

-- RLS
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_sport_subscriptions" ON subscriptions
    FOR ALL USING (
        auth.uid() = user_id AND
        sport_id = (auth.jwt() ->> 'sport_id')
    );
```

### JWT Custom Claim — Sport Context

When a user authenticates in any sport app, the Supabase session is enriched with the sport context:

```kotlin
// AuthRepositoryImpl — called right after any sign-in
supabase.auth.updateUser {
    data = buildJsonObject {
        put("sport_id", sportConfig.sportId)
    }
}
```

The JWT then carries `sport_id`, and RLS policies filter automatically at the database level.

---

## Build Configuration

### `local.properties` (never committed)

```properties
# Tennis
tennis.supabase.url=https://xxxxx.supabase.co
tennis.supabase.anon.key=eyJ...
tennis.revenuecat.android.key=goog_xxx
tennis.revenuecat.ios.key=appl_xxx
tennis.google.client.id=xxx.apps.googleusercontent.com

# Badminton
badminton.supabase.url=https://yyyyy.supabase.co
badminton.supabase.anon.key=eyJ...
badminton.revenuecat.android.key=goog_yyy
badminton.revenuecat.ios.key=appl_yyy
badminton.google.client.id=yyy.apps.googleusercontent.com

# ... repeat for all 10 sports
```

### Per-Sport Product IDs (Play Store / App Store)

| Sport | Monthly | Annual | Lifetime |
|-------|---------|--------|---------|
| Tennis | `mindful_tennis_monthly` | `mindful_tennis_annual` | `mindful_tennis_lifetime` |
| Badminton | `mindful_badminton_monthly` | `mindful_badminton_annual` | `mindful_badminton_lifetime` |
| ... | ... | ... | ... |

Same entitlement IDs across all RevenueCat projects: `premium_monthly`, `premium_annual`, `premium_lifetime`. The product IDs differ; the entitlement check in code is always the same string.

---

## Implementation Sequence

| Phase | Scope | What gets done |
|-------|-------|----------------|
| **1** | SportConfig system | Add `SportConfig`, `ScoringRules`, `SportTerminology` to shared module. Wire into Koin DI. Tennis values first. All existing tests pass. |
| **2** | DB migration | Add `sport_id` to Room entities + migration. Add to Supabase schema. Backfill `'tennis'`. |
| **3** | Android flavors | `flavorDimensions`, 10 `productFlavors`, 10 `*Main/res/` directories. Move tennis assets to `tennisMain/`. Build all 10 (empty assets for 9 new sports is fine at this stage). |
| **4** | iOS schemes | 10 xcconfig files, 10 Xcode schemes, 10 asset catalog groups. |
| **5** | Fastlane | `Fastfile`, `Matchfile`, `Appfile`. Run `setup_signing` locally. Verify `build_sport` lane works for tennis. |
| **6** | Azure DevOps | Create Azure Key Vault, populate 80 secrets (8 × 10). Create Variable Groups. Define `android-pipeline.yml` and `ios-pipeline.yml`. First run builds tennis only to validate. |
| **7** | RevenueCat | Create 10 RevenueCat projects. Add SDK to shared module. Initialize with sport-specific key. Implement `PremiumRepository`. Add paywall UI. |
| **8** | Supabase webhook | Edge Function to receive RevenueCat webhooks and write to `subscriptions` table. |
| **9** | Sport assets | Design and add icons, colors, and illustrations for remaining 9 sports. |
| **10** | Store listings | Create 10 Play Store + 10 App Store listings. Run `create_app_store_listings` Fastlane lane. Submit tennis for review first as pilot. |

---

## Key Decisions Summary

| Decision | Choice | Reason |
|----------|--------|--------|
| Payment platform | RevenueCat | Server-side validation, KMP support, free tier for validation, zero infrastructure |
| Payment isolation | Separate RevenueCat projects + separate store listings | Store-enforced isolation, no code tricks needed |
| CI/CD platform | Azure DevOps | Native Azure Key Vault integration, better for 80+ secrets across 10 apps |
| Secret management | Azure Key Vault + Variable Groups | One vault, per-sport groups, no secrets in YAML |
| Build automation | Fastlane (called by Azure DevOps) | Handles Apple toolchain complexity that Azure DevOps tasks cannot match |
| Code signing | Fastlane match + Azure Blob Storage | Eliminates certificate drift across machines |
| Database | One Supabase project + `sport_id` discriminator | Shared identity, isolated data, one backend to maintain |
| Rating aspects | Fixed 8 slots, label from `SportTerminology` | No schema change, UI-layer localization only |

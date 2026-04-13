# Visual Redesign: Sporadic Reminder App

## Context

The app currently uses entirely default Material 3 dynamic colors with **no custom palette, no custom typography, no added icons, and plain vanilla card/button styles**. It looks like a raw Material 3 template. The goal is to introduce a distinctive, colorful, modern design with colored buttons, icons throughout, accent highlights, and visual polish -- while staying within Jetpack Compose + Material 3.

## Design Direction: Catppuccin Color Palette

Uses **Catppuccin Latte** for light theme and **Catppuccin Mocha** for dark theme. Primary=Mauve (signature purple), Secondary=Teal, Tertiary=Peach. Each status type maps to a Catppuccin color (Yellow=pending, Green=done, Red=skipped, Sapphire=snoozed).

### Material 3 Color Mapping -- Light (Catppuccin Latte)

| Slot | Color | Hex |
|------|-------|-----|
| primary | Mauve | `#8839EF` |
| onPrimary | White | `#FFFFFF` |
| primaryContainer | Light Lavender | `#ECDCFF` |
| onPrimaryContainer | Deep Purple | `#2B0057` |
| secondary | Teal | `#179299` |
| onSecondary | White | `#FFFFFF` |
| secondaryContainer | Light Teal | `#C8F5F0` |
| onSecondaryContainer | Dark Teal | `#003834` |
| tertiary | Peach | `#FE640B` |
| onTertiary | White | `#FFFFFF` |
| tertiaryContainer | Light Peach | `#FFE0CC` |
| onTertiaryContainer | Dark Orange | `#3D1600` |
| error | Red | `#D20F39` |
| onError | White | `#FFFFFF` |
| errorContainer | Light Red | `#FFDAD6` |
| onErrorContainer | Dark Red | `#410002` |
| background | Base | `#EFF1F5` |
| onBackground | Text | `#4C4F69` |
| surface | Base | `#EFF1F5` |
| onSurface | Text | `#4C4F69` |
| surfaceVariant | Surface0 | `#CCD0DA` |
| onSurfaceVariant | Subtext0 | `#6C6F85` |
| outline | Overlay1 | `#8C8FA1` |
| outlineVariant | Surface1 | `#BCC0CC` |
| inverseSurface | Mocha Base | `#1E1E2E` |
| inverseOnSurface | Mocha Text | `#CDD6F4` |
| inversePrimary | Mocha Mauve | `#CBA6F7` |

### Material 3 Color Mapping -- Dark (Catppuccin Mocha)

| Slot | Color | Hex |
|------|-------|-----|
| primary | Mauve | `#CBA6F7` |
| onPrimary | Deep Purple | `#3A0078` |
| primaryContainer | Mid Purple | `#5B1DAB` |
| onPrimaryContainer | Light Lavender | `#ECDCFF` |
| secondary | Teal | `#94E2D5` |
| onSecondary | Dark Teal | `#003830` |
| secondaryContainer | Deep Teal | `#005048` |
| onSecondaryContainer | Light Teal | `#B8F8EC` |
| tertiary | Peach | `#FAB387` |
| onTertiary | Dark Orange | `#4A1E00` |
| tertiaryContainer | Deep Orange | `#6D3A00` |
| onTertiaryContainer | Light Peach | `#FFDCC6` |
| error | Red | `#F38BA8` |
| onError | Deep Red | `#690016` |
| errorContainer | Dark Red | `#930026` |
| onErrorContainer | Light Red | `#FFD9DF` |
| background | Base | `#1E1E2E` |
| onBackground | Text | `#CDD6F4` |
| surface | Base | `#1E1E2E` |
| onSurface | Text | `#CDD6F4` |
| surfaceVariant | Surface0 | `#313244` |
| onSurfaceVariant | Subtext0 | `#A6ADC8` |
| outline | Overlay0 | `#6C7086` |
| outlineVariant | Surface1 | `#45475A` |
| inverseSurface | Latte Base | `#EFF1F5` |
| inverseOnSurface | Latte Text | `#4C4F69` |
| inversePrimary | Latte Mauve | `#8839EF` |

### Semantic Status Colors (SporadicColors)

**Light (Latte):**
| Status | Color | Hex | Container | On-Container |
|--------|-------|-----|-----------|-------------|
| Pending | Yellow | `#DF8E1D` | `#FFF3D6` | `#4A3000` |
| Done | Green | `#40A02B` | `#D8F5D3` | `#0A3200` |
| Skipped | Red | `#D20F39` | `#FFDAD6` | `#410002` |
| Snoozed | Sapphire | `#209FB5` | `#D0EFFA` | `#003544` |

**Dark (Mocha):**
| Status | Color | Hex | Container | On-Container |
|--------|-------|-----|-----------|-------------|
| Pending | Yellow | `#F9E2AF` | `#4A3800` | `#F9E2AF` |
| Done | Green | `#A6E3A1` | `#1A4A18` | `#A6E3A1` |
| Skipped | Red | `#F38BA8` | `#5E1028` | `#F38BA8` |
| Snoozed | Sapphire | `#74C7EC` | `#0D3B5E` | `#74C7EC` |

**Priority Colors (Light / Dark):**
| Priority | Light | Dark | Light Container | Dark Container |
|----------|-------|------|-----------------|----------------|
| Low | `#40A02B` | `#A6E3A1` | `#D8F5D3` | `#1A4A18` |
| Default | `#1E66F5` | `#89B4FA` | `#DCE4FF` | `#1A3468` |
| High | `#FE640B` | `#FAB387` | `#FFE0CC` | `#4A2800` |
| Urgent | `#D20F39` | `#F38BA8` | `#FFDAD6` | `#5E1028` |

---

## Phase 1: Foundation (Theme + Dependencies)

### 1. `app/build.gradle.kts` -- Add icons-extended
- Add `implementation("androidx.compose.material:material-icons-extended")` (version managed by BOM, tree-shaken by R8)

### 2. NEW `ui/theme/SporadicColors.kt` -- Custom semantic colors
- Create `SporadicColors` data class with status colors (pending/done/skipped/snoozed containers + on-colors) and priority colors (low/default/high/urgent)
- Light and dark instances
- `LocalSporadicColors` CompositionLocal
- Extension `MaterialTheme.sporadicColors` for easy access

### 3. `ui/theme/SporadicTheme.kt` -- Full theme overhaul
- **Replace empty `lightColorScheme()` / `darkColorScheme()`** with Catppuccin Latte/Mocha palettes (hex values from tables above)
- **Remove dynamic color support** (wallpaper-based theming) -- always use custom Catppuccin palette
- Add custom `Typography` (SemiBold headlines, Medium titles)
- Add custom `Shapes` (8dp small, 12dp medium, 16dp large)
- Provide `SporadicColors` via `CompositionLocalProvider`

### 4. `res/values/colors.xml` -- Update launcher background
- Change `ic_launcher_background` to `#8839EF` (Catppuccin Mauve)

---

## Phase 2: High-Impact Screens

### 5. `ui/dashboard/DashboardScreen.kt`
- **StatCards**: Each gets a unique accent color + icon:
  - Pending: amber bg + `Schedule` icon
  - Done: green bg + `CheckCircle` icon
  - Skipped: red bg + `Cancel` icon
  - Snoozed: blue bg + `Snooze` icon
- **FiredAlarmCard**: Leading notification icon, colored action buttons (Done=Green filled, Skip=Red outlined, Snooze=Sapphire outlined) with icons in each button
- **FAB**: `tertiaryContainer` color (Catppuccin Peach tint) for distinction from Mauve primary
- **"Today" header**: Primary (Mauve) color styling

### 6. `ui/reminders/ReminderListScreen.kt`
- **ReminderCard**: Add circular icon container (primaryContainer bg + Notifications icon), styled name/time text, colored badge for notification count
- **GroupCard**: Add folder icon in circular secondaryContainer, "Shared schedule" as a tinted chip
- **Section headers**: Primary-colored with leading icons (Folder for Groups, Notifications for Reminders)
- **"+ New Group"**: Add `Add` icon, primary color
- **FAB**: Peach tint like Dashboard

---

## Phase 3: Form Screens

### 7. `ui/reminders/ReminderEditScreen.kt`
- **Priority segmented buttons**: Active color changes per priority (Green/Blue/Peach/Red from Catppuccin) with leading icons (ArrowDownward/Remove/ArrowUpward/Warning)
- **Day-of-week chips**: Primary Mauve when selected, checkmark leadingIcon
- **DND segmented buttons**: Skip=red container, Snooze=blue container with icons
- **Form rows**: Add leading icons (MusicNote for tone, Vibration for vibrate, AccessTime for times, Folder for group)
- **Save button**: Check icon prefix

### 8. `ui/groups/GroupEditScreen.kt`
- Same form styling patterns as ReminderEditScreen
- Schedule icon for shared schedule toggle
- Conditional schedule section wrapped in surfaceVariant card
- Save button with check icon

---

## Phase 4: Polish

### 9. `ui/settings/SettingsScreen.kt`
- Section headers with icons (Palette for Theme, Storage for Data)
- Theme segments with icons (LightMode/DarkMode/SettingsBrightness)
- Export button with Upload icon, Import with Download icon
- Error messages in errorContainer card

### 10. `ui/navigation/SporadicNavGraph.kt`
- Minimal changes -- the new colorScheme automatically styles the NavigationBar
- Explicit `indicatorColor = primaryContainer` for the lavender pill highlight on selected tab

---

## Files Summary

| File | Action | Impact |
|------|--------|--------|
| `app/build.gradle.kts` | Modify | Add icons-extended |
| `ui/theme/SporadicColors.kt` | **Create** | Custom semantic color system |
| `ui/theme/SporadicTheme.kt` | Modify | Full color/typography/shapes overhaul |
| `res/values/colors.xml` | Modify | Brand color for launcher |
| `ui/dashboard/DashboardScreen.kt` | Modify | Colored stat cards, styled alarm cards |
| `ui/reminders/ReminderListScreen.kt` | Modify | Icons, colored cards |
| `ui/reminders/ReminderEditScreen.kt` | Modify | Colored chips, form icons |
| `ui/groups/GroupEditScreen.kt` | Modify | Consistent form styling |
| `ui/settings/SettingsScreen.kt` | Modify | Icons, styled sections |
| `ui/navigation/SporadicNavGraph.kt` | Modify | Bottom bar color tuning |

## Verification

1. Build the project: `./gradlew assembleDebug` from the `sporadic-reminder-app` directory
2. Verify no compilation errors
3. Check that the custom color scheme renders correctly in both light and dark modes
4. Verify each screen has the expected icons and color accents
5. Confirm the bottom navigation shows Mauve highlight on selected tab

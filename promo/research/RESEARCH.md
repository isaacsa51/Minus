# Minus promo: research notes and sources

Everything shown in `output/minus-promo.mp4` traces back to a file in this repository or to the reference video. Paths are relative to the repo root.

## 1. Reference video: motion study

Source: `C:\Users\Isaac-PC\Downloads\Google Material 3 Expressive  Data's New Language - Scene (1080p, h264).mp4` (1920×1080, 24 fps, 50.5 s). Contact sheets sampled every 0.5 s and denser 6 fps sheets are in `research/reference-frames/`.

| Reference technique (timestamp) | Where it is adapted in the promo |
|---|---|
| Blurred aura rings with giant cropped type sliding sideways, blur resolving to sharp (0:01–0:03) | `S02Wordmark`: opens with a fade from black into the gradient, then aura of the app's pill/status colors, "Minus" at 820 px cropped to "inu", panning, then zooming out sharp |
| Aura flattens to a solid color, the brand mark builds, camera dives into one component to cut to the next word (0:03–0:06.5) | `S03Splash`: the aura flattens to the splash background, the logo builds with the app's own splash AVD, then the camera dives into the green $ coin, which becomes the green background of "What" |
| Word-per-cut kinetic type with a background swap per word: heavy "What", italic "if", condensed chip "DATA" with a cursor (0:06.5–0:08) | `S04Question`: "What" (heavy) → "if" (slnt −10 italic) → condensed "BUDGET" chip with cursor click |
| Bars with riding percentage pills that count up, dashed gridlines, cursor (0:08–0:09) | `S07Split`: the four split modes as bars with value pills counting to the verified amounts |
| Dotted grid, words in mixed containers (rect, pill with selection handles, dark chip), phrase collapses into a small tag, typed text field with caret (0:15.5–0:19) | `S04Question` second half: [What] (if) [your budget] → tag → typed "did the math?" |
| Phone zooms in, cursor taps, components break out of the device and enlarge (0:10–0:15, 0:20–0:24) | `S05Pill` → `S06Sheet`: typing $60 on the numpad, then the pill breaks out of the phone and a tap opens the BudgetPeriodSheet at full width |
| Collage zoomed in, camera pans, then the whole layout scales down into a device frame (0:26–0:29.5) | `S09Subs`: Subscriptions screen zoomed in, pan to the calendar, then zoom out into the phone |
| Toolbar / button-group shape morphs (0:30–0:32) | `S08Choice`: leftover cards morph corner radii when selection moves (the app's `MorphCornerShape`) |
| Aura bloom with blurred brand type, then end card with the logo drawn in (0:40–0:50) | `S11Outro`: the gradient keeps moving behind the end card, the logo rebuilds with the same splash animation on the icon's #F2F3EB circle, then "Minus", the tagline and the three store badges |

Every scene exits with a Gaussian blur and fade over 14 frames (`Promo.tsx`, `BlurFadeOut`).

Motion vocabulary carried over: emphasized-decelerate entrances (`cubic-bezier(0.05,0.7,0.1,1)`), emphasized-accelerate exits, M3 Expressive spatial springs (stiffness 380 / damping ratio 0.8, bouncy 420 / 0.5) for overshoot on chips, blur-to-sharp text reveals, hard cuts timed on beats, and cursor-driven interactions.

## 2. Brand: colors, type, shapes

- Light color scheme: `app/src/main/java/com/serranoie/app/minus/presentation/ui/theme/Color.kt` (`primaryLight` `#516526`, `primaryContainerLight` `#D4EC9E`, `tertiaryContainerLight` `#EDE68C`, `surfaceLight` `#FAFAEE`, …) wired in `Theme.kt` `lightScheme`. All tokens are mirrored in `remotion/src/theme.ts`.
- Status colors: `BudgetStatusColors` in `Color.kt` (`#81C784`, `#FFB74D`, `#E57373`).
- Composited colors (pill fill/track per spend level, editor sheet, min/max cards, category colors) were sampled from the Paparazzi snapshots in `app/src/test/snapshots/images/` (light theme), e.g. `BudgetPillPaletteScreenshotTest_budgetPillPalette_light.png`, `MainScreenScreenshotTest_mainScreenPhoneIdle.png`, `MinMaxSpentCardScreenshotTest_*.png`, `SubscriptionsScreenshotTest_subscriptionsDueSoonAndUpcoming.png`.
- Logo: path data copied from `app/src/main/res/drawable/ic_launcher_foreground.xml` (tile colors `#45483C`, `#1A1C15`, `#A3CD51`; icon background `#E5E7DD` from `ic_launcher_background.xml`).
- Typeface: `app/src/main/res/font/google_sans_flex.ttf` (axes wght 1–1000, wdth 25–151, ROND 0–100, slnt −10–0, opsz 6–144, GRAD). Rounded (ROND 100) is the app default (`isRoundedFontEnabled` defaults to `true`). Type tokens follow `Type.kt`: `titleMediumCondensed` = wght 600 / wdth 85 for the pill, emphasized titles at wdth 135, amounts in condensed widths. `opsz` is left at the default (no optical sizing), like the app.
- Shapes: `Shape.kt` (`extraLarge` 28 dp) plus the stadium pill (`CircleShape`) and leftover cards' first/last corner morph.

## 3. App logic verified in code (and by test)

### Daily budget
`BudgetStateCalculator.calculateBudgetState` (`presentation/ui/budget/BudgetStateCalculator.kt`, lines 107–160):

- **Same every day** (`STATIC`): `dailyBudget = splitBudget / periodDays`. Never changes with spending.
- **Rebalanced daily** (`DYNAMIC`, the default): `(total + income − deductions − spent − day-1 surplus + spentToday) / daysRemaining`. Yesterday's spending is respread over the days left.
- **Saved for later** (`CARRY_OVER`): `remainingToday = earnedAllowance(splitBudget, dayNumber, totalDays) + surplus + income − deductions − spent`. Unspent money (or debt) accumulates day to day (`earnedAllowance` in `editor/sheets/split/SplitCalculations.kt` line 41).
- **Your call each day** (`ASK_ME`): `replayLeftovers` (lines 196–253) holds yesterday's unspent money in a pending pool. *Spread* raises the rate by `pool ÷ days left`; *Add it all to today* adds the pool to today only. Overspending is always spread.

UI names and helper copy come from `res/values/strings.xml` lines 136–139 and 755–772 ("Same every day", "Rebalanced daily", "Saved for later", "Your call each day", "Total ÷ period days", "(Total − Spent) ÷ days left").

### Remaining surplus
- Daily leftover in *Your call each day*: the pill switches to its surplus face "Pending extra money · Tap to manage: $X" (`BudgetPill.kt`, `unresolved_surplus_title`). Tapping it opens `LeftoverChoiceList` (`theme/component/numpad/LeftoverChoiceList.kt` lines 93–112): Spread shows `remainingToday + pool / daysRemaining`, then "$X for upcoming days". Carry shows `remainingToday + pool`, then "$dailyBudget for tomorrow".
- End-of-period surplus ("Ask me / Spread all / Day 1 lump", strings 740–746): *Spread all* folds the surplus into the next period's split budget. *Day 1 lump* (`rollOverCarryForward`) keeps it out of the split and adds it to day 1 (`carryForFirstDay`).

### Scenario used on screen, run through the real calculator
`research/verification/PromoScenarioTest.kt` (run once inside `app/src/test` with `./gradlew :app:testFossDebugUnitTest --tests "*PromoScenarioTest*"`, both tests passed; output in `research/verification/test-output.txt`). Budget $1,400 for 14 days (1–14 Jul 2026), $60 spent on day 1:

| Mode | Day 2 "Today" |
|---|---|
| Same every day | $100.00 |
| Rebalanced daily | $103.08 |
| Saved for later | $140.00 |
| Your call each day | $100.00 + $40.00 pending → $103.08 (spread) or $140.00 (add to today) |

Surplus check: $70 carried into a $1,470 period → Spread all = $105/day; Day 1 lump = $170 on day 1, then $100.

### Splash animation (S03 and the outro)
`res/drawable/ic_splash_icon_animated.xml` targets the groups of `ic_splash_icon.xml`, scaling each about its own tile centre. `plus` goes 0→1 over 280 ms at 0 ms, `minus` the same at 70 ms, `multiply` at 140 ms, all with `@android:interpolator/overshoot` (tension 2). `badge` scales 0→1 over 380 ms at 220 ms with overshoot and rotates −60°→0° with `fast_out_slow_in`. The total is 600 ms (`splash_icon_animation_duration`). Colors come from `res/values/colors.xml`: `splash_background` #FAFAEE, `splash_icon` #1A1C15, `splash_icon_variant` #45483C, `splash_icon_accent` #A3CD51. `components/SplashLogo.tsx` evaluates these curves per frame in milliseconds, so the build matches the device animation. The outro reuses it on the launcher icon's circle color #F2F3EB, sampled from `assets/app_icon.png`.

### BudgetPeriodSheet (S06)
Tapping the pill opens `BudgetPeriodSheet` in a `ModalBottomSheet` (`editor/Editor.kt` around line 574). Its view mode, `ViewBudgetContent` (`editor/sheets/BudgetPeriodSheet.kt` lines 341–605), shows the following for the example on day 1 (1 Jul), after $60:
- "Total budget" title with the edit pencil.
- `SpendBudgetCard`: "$60", "Spent", "Available: 96%". The value is `1 − 60/1400` = 95.7%, formatted with zero decimals. The wavy fill is 4.3% wide.
- `TotalBudgetCard` on `onSurface`: "$1,400", "Total budget", "01 Jul ⟶ 14 Jul" (`prettyDate(..., "dd MMM", simplifyIfToday = false)`).
- `DaysLeftCard`: "13 Days remaining". `countDaysToToday` counts the days after today, and the wavy ring progress is `1 − 13/14 − 0.01`.
- "How do you want to split the budget?" over connected toggles Daily / Weekly / Biweekly (Monthly needs ≥30 days, `availablePeriodsFor`).
- `CalculatedSplitCard`: "Calculated amount" $100 for Daily and $700 for Weekly (`staticBlockBudget` = 1400 × 7 / 14; Rebalanced daily gives the same on day 1). The hint reads "Tap for the full breakdown" because the Editor passes `onShowFormula`.
- "Finalize budget period early" outlined button.

### Budget Pill
`theme/component/budget/pill/BudgetPill.kt`: stadium card, progress fill from the left (spend ÷ period budget), color mixed from good → not good → bad by progress and harmonized to `primary`, label "Today" + amount in `titleMediumCondensed`, amount centered and scaled 1.3 while a draft is typed, hold = scale to 0.9 over 300 ms, then the formula opens (`FORMULA_HOLD_SCALE`, `FORMULA_HOLD_MILLIS`). Interaction copy: `tutorial_budget_pill_description`.

### Editor: typing, calculator, categories, budget adjustments (S05)
- **Calculator by swipe:** a vertical drag of 110 px on the numpad turns on calculation mode (`theme/component/numpad/Numpad.kt` lines 150–207). That adds an operator row ÷ × + − and an "=" key, as in `NumpadScreenshotTest_numpadCalculationMode.png`. The expression shows as "$45+15" with "= $60" underneath (`editor/Editor.kt` around line 640). The pill counts down with the evaluated draft (`numpadDraftAmount`).
- **Saving and clearing switch calculator mode off.** Saving goes through `TransactionAction.ClearEditorFlags` → `SetCalculationMode(false)`. Holding backspace goes through `ResetInputTapped` → `handleReset`, which also turns it off. So the video swipes up again before each +/− example.
- **Category:** the editor's tag chip shows "Category" until named (`editor/category/EditableCategoryTag.kt`, `add_new_category`). The typed name is saved with the expense, and `BudgetTransactionHandler` calls `findOrCreateCategory(comment)`, which creates the category. Suggestion chips sit beside it, as in `EditorScreenshotTest_editorEditingAmountWithTagsAndComment.png`.
- **Leading + / −:** `BudgetTransactionHandler.kt` lines 75–89 negate a "+" amount, so it counts as income and raises the budget. A "−" amount becomes a deduction (`isAdjustment = true`). While typing, the pill shows `calculationPreview`: "$200 will be added" / "$50 will be subtracted" (`budget_pill_calc_added` / `_subtracted`, `BudgetViewModel.kt` lines 722–740).
- **The pill's fill follows `calculateBudgetMetrics` with the signed draft.** "+200" takes today's spend below zero, so the fill empties. "−50" pushes today past its $100, so the fill is full and coral. A bare "+" or "−" displays as "+$0" / "-$0", because the editor formats an empty remainder as 0.
- The examples are previewed and then cleared, so the story's budget stays $1,400 with $60 spent.

### Subscriptions
`presentation/ui/subscriptions/*` provides the hero ("Monthly commitment … /mo", "X% of your budget this period"), the view toggle "Whole period / Weekly / Category", the calendar, "By billing frequency", "Due soon" and "Upcoming" item cards (avatar, name, "In N days (MMM d)" / "In N weeks (MMM d)", amount, Mark Paid / Skip / edit / delete).

`SubscriptionsCalendarSection` renders only the weeks that overlap the period. For 1–14 Jul that's three rows, with 15–18 Jul drawn outlined as outside the period.

Monthly equivalent = `RecurringExpenseCalculator.calculateMonthlyEquivalent` (weekly × 4.33, biweekly × 2.17). The example data:

| Subscription | Amount | Schedule |
|---|---|---|
| Coffee club | $5 | weekly from 3 Jul |
| Streaming | $15.99 | monthly, 5th |
| Gym | $30 | monthly, 15th |
| Phone plan | $25 | monthly, 22nd |

That gives $92.64/mo. Charges inside the period are $25.99 (3, 5 and 10 Jul), and the code's zero-decimal percent format shows that as 2% of $1,400.

`DUE_SOON_WINDOW_DAYS = 7` splits the list: Coffee club and Streaming are due soon; Gym and Phone plan are upcoming. Dates were chosen so no subtitle reads "In 1 weeks".

### Analytics
`presentation/ui/analytics/Analytics.kt` `AnalyticsCompactLayout` (line 1007): `SpendsChart` (Total Spent trend), `CalendarHeatmap`, `MinMaxSpentCard` ×2, `SpendBudgetCard`, `CategoriesChartCard`. Heatmap intensity = `amount/dailyAllowance·0.6 + count/maxCount·0.4`, the highest day scaled ≥1.15, and intensity > 1 rendered in `errorContainer` (`CalendarHeatmap.kt` lines 117–122, 196–203, 368–370). Example dataset (`S10Analytics.tsx`): 14 daily totals starting with the same $60 day 1; total $719.05; max single expense $120.40 (Groceries, 5 Jul); min $4.50 (Coffee, 9 Jul).

### Copy sources
- Tagline "Your money, minus the guesswork." comes from the `README.md` title.
- Distribution: the Google Play, GitHub and IzzyOnDroid badge images match the `README.md` links (`assets/badges/`).
- Headlines paraphrase the app's own strings: typing on the numpad (store description), "Tap it to see the whole period" (`tutorial_budget_pill_description`: "Tap it to open your period setup…"), recurring reminders (`RecurrentExpenseNotificationWorker`, README "make reminders for your recurring expenses").

## 4. What was not invented
Every component on screen exists in the app: splash animation, Budget Pill, editor numpad and edit buttons, BudgetPeriodSheet (spent card, total budget card, days-left ring, split toggles, calculated amount), budget behaviour mode names, leftover choice cards, Subscriptions screen, Analytics trend, min/max cards and heatmap, launcher icon. Layouts were rebuilt as vector/HTML from the snapshot geometry rather than screenshots, so they stay sharp at 1080×1920.

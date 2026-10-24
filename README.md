# Tatkal Helper V2 (personal use)

Autofills your saved details in the official IRCTC Rail Connect app and on irctc.co.in in your phone browser and reminds you before the Tatkal window.
Sideload only. Not for Play Store.

## What it does
- Encrypted local storage (Android Keystore) for journey, passengers and your optional IRCTC ID/password (never card numbers, UPI PINs or CAPTCHA)
- Exact alarm at 9:55 (AC) / 10:55 (Sleeper) that arms autofill and posts an "Open IRCTC" notification
- AccessibilityService (IRCTC app, plus Chrome/Samsung Internet/Edge/Brave/Firefox only while an irctc.co.in page is open): fills station/date/train/name/age fields, ticks master-list
  passengers by exact name, selects your payment option label, and moves focus to the CAPTCHA box
- OTP autofill: reads the OTP SMS only while armed and only from your sender keywords

## What it deliberately does NOT do
- Read or solve CAPTCHA
- Tap Pay / Book / Submit / Confirm / Proceed (you do the final taps)
- Bypass IRCTC's minimum wait before payment

## Build
1. Open the folder in Android Studio (Koala or newer, JDK 17). Let it sync (Gradle 8.7).
2. Run on your phone (USB debugging) or Build > Build APK and install it.

## First-time setup
1. Open the app, enter details, tap Save. Allow SMS + notification permissions.
2. "Enable accessibility service" > Tatkal Helper > On.
   Android 13+: if it is greyed out, App info > ⋮ > Allow restricted settings, then retry.
3. Set an alarm. At alarm time, tap the notification (or "Arm autofill and open IRCTC").

## Website mode
Use "Arm autofill and open IRCTC website" (or the "Website" button on the alarm notification). It opens the
site in your default browser, so that browser must be one of those listed in `Irctc.BROWSERS`. The login form is filled from your saved ID/password (you type the CAPTCHA and tap Login); mobile and email fields are never touched. Website field names differ from the app, so run
the Debug dump on the website too and tune the regexes. Browsers expose web pages to accessibility with less
detail than apps, so some fields may need a tap.

## UPI
Pick Paytm, PhonePe, Google Pay or BHIM, and optionally save your UPI ID. After the "UPI" payment option is
selected, the service clicks that app's tile and fills any UPI ID box it finds. Choose "UPI ID only" to skip the
app tile. Your UPI PIN is never stored or entered by the app; on some gateways tapping an app tile jumps straight
to that app, where you approve the payment with your own PIN.

## Children
Children 5 and above travel with a berth, so list them under Passengers. Children under 5 (no berth) go in the
"Children under 5" card; the service fills the infant name/age fields it can identify by an "infant" or "child"
label. Age/gender on that section are often dropdowns, which are not automated.

## Tuning (important)
IRCTC's screens change and field labels are not public. Tap "Debug: log next IRCTC screen",
open the screen, then read Logcat (tag `TatkalHelper`) for real ids/hints and adjust the regexes at the
bottom of `AutofillService.kt`. Station fields are usually autocomplete dropdowns: the text is filled
but you may still need to tap the suggestion. Gender/berth dropdowns are not automated; save passengers
in your IRCTC Master List and the app will tick them by name.

## Notes
- Automating IRCTC can still violate its terms and get your account flagged. Use at your own risk.
- Remove the `queries` block or the package filter only if you know why.

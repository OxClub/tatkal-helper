package com.example.tatkalhelper

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Fills saved details in the IRCTC Rail Connect app and on irctc.co.in in a browser while armed.
 *
 * Deliberate limits (do not remove):
 *  - never reads or solves the CAPTCHA image; it only moves focus to the CAPTCHA box so you can type
 *  - never taps Pay / Book / Submit / Confirm / Proceed; the only clicks are master-list passenger
 *    selection and the payment-option label you configured
 *  - only runs in the IRCTC app, or in a browser while an irctc.co.in page is open
 *  - fills the login form only from your saved ID/password, never taps Login (CAPTCHA stays yours),
 *    and never touches mobile or email fields
 */
class AutofillService : AccessibilityService() {

    private var cfg: Config = Config()
    private var cfgGen = -1
    private var lastRun = 0L

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        val isBrowser = pkg in Irctc.BROWSERS
        if (pkg != Irctc.PKG && !isBrowser) return
        AutofillState.lastPkg = pkg
        AutofillState.events++
        if (!AutofillState.isArmed() && !AutofillState.dumpNext) return

        val now = SystemClock.uptimeMillis()
        if (now - lastRun < 250) return
        lastRun = now

        val root = rootInActiveWindow ?: return
        val nodes = ArrayList<AccessibilityNodeInfo>()
        flatten(root, nodes)

        if (AutofillState.dumpNext) {
            AutofillState.dumpNext = false
            val sb = StringBuilder()
            dump(root, 0, sb)
            AutofillState.lastDump = sb.toString()
            Log.d("TatkalHelper", AutofillState.lastDump)
        }

        // In a browser, act only while the address bar shows an irctc.co.in page.
        val onIrctc = !isBrowser || nodes.any { textOf(it).contains(Irctc.WEB_HOST, ignoreCase = true) }
        AutofillState.irctcPageSeen = onIrctc
        if (!onIrctc) return
        if (!AutofillState.isArmed()) return

        if (cfgGen != AutofillState.generation) {
            cfg = Store(this).load()
            cfgGen = AutofillState.generation
        }

        fillFields(nodes)
        selectPassengers(nodes)
        selectPayment(nodes)
        selectUpiApp(nodes)
    }

    // ---------- field filling ----------

    private fun fillFields(nodes: List<AccessibilityNodeInfo>) {
        var nameIdx = 0
        var ageIdx = 0
        var childNameIdx = 0
        var childAgeIdx = 0
        for (n in nodes) {
            if (!n.isEditable || !n.isVisibleToUser) continue
            val label = labelOf(n)
            when {
                CAPTCHA.containsMatchIn(label) -> once("captcha-focus") { focus(n) }
                OTP.containsMatchIn(label) -> AutofillState.takeOtp()?.let {
                    if (setText(n, it)) AutofillState.clearOtp()
                }
                UPI_ID.containsMatchIn(label) -> once("upi") { setText(n, cfg.upiId) }
                n.isPassword -> once("login-pass") { setText(n, cfg.password) }
                LOGIN_USER.containsMatchIn(label) -> once("login-user") { setText(n, cfg.username) }
                SKIP.containsMatchIn(label) -> {} // other user/login/mobile/email fields: never touch
                INFANT.containsMatchIn(label) && NAME.containsMatchIn(label) -> {
                    val i = childNameIdx++
                    cfg.children.getOrNull(i)?.let { c -> once("cname$i") { setText(n, c.name) } }
                }
                INFANT.containsMatchIn(label) && AGE.containsMatchIn(label) -> {
                    val i = childAgeIdx++
                    cfg.children.getOrNull(i)?.let { c -> once("cage$i") { setText(n, c.age) } }
                }
                FROM.containsMatchIn(label) -> once("from") { setText(n, cfg.from) }
                TO.containsMatchIn(label) -> once("to") { setText(n, cfg.to) }
                DATE.containsMatchIn(label) -> once("date") { setText(n, cfg.date) }
                TRAIN.containsMatchIn(label) -> once("train") { setText(n, cfg.trainNo) }
                NAME.containsMatchIn(label) -> {
                    val i = nameIdx++
                    cfg.passengers.getOrNull(i)?.let { p -> once("name$i") { setText(n, p.name) } }
                }
                AGE.containsMatchIn(label) -> {
                    val i = ageIdx++
                    cfg.passengers.getOrNull(i)?.let { p -> once("age$i") { setText(n, p.age) } }
                }
            }
        }
    }

    // ---------- the only two kinds of clicks we make ----------

    /** Ticks passengers already saved in your IRCTC master list, matched by exact name. */
    private fun selectPassengers(nodes: List<AccessibilityNodeInfo>) {
        for (p in cfg.passengers) {
            if (p.name.isBlank()) continue
            val node = nodes.firstOrNull { it.isVisibleToUser && textOf(it).equals(p.name, ignoreCase = true) } ?: continue
            once("pick:${p.name.lowercase()}") { safeClick(node) }
        }
    }

    private fun selectPayment(nodes: List<AccessibilityNodeInfo>) {
        val want = cfg.paymentLabel.trim()
        if (want.isEmpty()) return
        val node = nodes.firstOrNull {
            it.isVisibleToUser && !it.isEditable && textOf(it).contains(want, ignoreCase = true)
        } ?: return
        once("payment") { safeClick(node) }
    }

    /** Picks your preferred UPI app tile once the UPI options are on screen. Never taps Pay/Book. */
    private fun selectUpiApp(nodes: List<AccessibilityNodeInfo>) {
        val pattern = when (cfg.upiApp) {
            "Paytm" -> Regex("paytm", RegexOption.IGNORE_CASE)
            "PhonePe" -> Regex("phone ?pe", RegexOption.IGNORE_CASE)
            "Google Pay" -> Regex("google ?pay|g ?pay", RegexOption.IGNORE_CASE)
            "BHIM" -> Regex("bhim", RegexOption.IGNORE_CASE)
            else -> return // "UPI ID": only fill the UPI ID box
        }
        val node = nodes.firstOrNull {
            it.isVisibleToUser && !it.isEditable && pattern.containsMatchIn(textOf(it))
        } ?: return
        once("upiapp") { safeClick(node) }
    }

    // ---------- helpers ----------

    private fun once(key: String, block: () -> Boolean) {
        if (key !in AutofillState.done && block()) AutofillState.done.add(key)
    }

    private fun setText(n: AccessibilityNodeInfo, value: String): Boolean {
        if (value.isBlank()) return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun focus(n: AccessibilityNodeInfo): Boolean {
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return n.performAction(AccessibilityNodeInfo.ACTION_CLICK) // opens the keyboard
    }

    private fun safeClick(n: AccessibilityNodeInfo): Boolean {
        val target = clickable(n) ?: return false
        if (DENY.containsMatchIn(labelOf(target) + " " + textOf(target) + " " + textOf(n))) return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun clickable(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = n
        var i = 0
        while (cur != null && i < 5) {
            if (cur.isClickable) return cur
            cur = cur.parent
            i++
        }
        return null
    }

    private fun textOf(n: AccessibilityNodeInfo): String =
        (n.text?.toString() ?: n.contentDescription?.toString() ?: "").trim()

    private fun labelOf(n: AccessibilityNodeInfo): String {
        val l = listOfNotNull(n.hintText, n.contentDescription, n.viewIdResourceName)
            .joinToString(" ").lowercase()
        return if (l.isBlank()) (n.text?.toString() ?: "").lowercase() else l
    }

    private fun flatten(n: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        out.add(n)
        for (i in 0 until n.childCount) n.getChild(i)?.let { flatten(it, out) }
    }

    /** Dumps the screen's node tree (password text masked). Copy it from the app's "Copy last screen dump" button. */
    private fun dump(n: AccessibilityNodeInfo, depth: Int, sb: StringBuilder) {
        if (sb.length > 30000) return
        sb.append("  ".repeat(depth))
            .append(n.className).append(" id=").append(n.viewIdResourceName)
            .append(" text=").append(if (n.isPassword) "***" else n.text)
            .append(" desc=").append(n.contentDescription)
            .append(" hint=").append(n.hintText)
            .append(" click=").append(n.isClickable)
            .append(" edit=").append(n.isEditable).append('\n')
        for (i in 0 until n.childCount) n.getChild(i)?.let { dump(it, depth + 1, sb) }
    }

    companion object {
        // Tune these after looking at the Logcat dump of the real screens.
        private val CAPTCHA = Regex("captcha")
        private val OTP = Regex("\\botp\\b")
        private val FROM = Regex("\\b(from|source|origin)\\b")
        private val TO = Regex("\\b(to|destination)\\b")
        private val DATE = Regex("\\b(date|doj)\\b")
        private val TRAIN = Regex("\\btrain\\b")
        private val NAME = Regex("\\bname\\b")
        private val AGE = Regex("\\bage\\b")
        private val UPI_ID = Regex("upi ?id|\\bvpa\\b|virtual payment")
        private val INFANT = Regex("infant|child")
        private val LOGIN_USER = Regex("user ?name|user ?id|login ?id")
        private val SKIP = Regex("user|login|password|passwd|pwd|\\bpin\\b|mobile|phone|e-?mail")

        /** Anything matching this is never clicked, as a second safety net. */
        private val DENY = Regex(
            "\\b(pay now|pay (&|and) book|book now|submit|confirm|proceed|continue|make payment)\\b",
            RegexOption.IGNORE_CASE
        )
    }
}

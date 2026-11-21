package studio.astral.launcher

import androidx.activity.ComponentActivity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(9, 11, 21)
    private val panel = Color.argb(145, 42, 48, 75)
    private val accent = Color.rgb(165, 153, 255)
    private val border = Color.argb(105, 205, 198, 255)
    private lateinit var root: LinearLayout
    private lateinit var drawerScroll: ScrollView
    private lateinit var appList: LinearLayout
    private lateinit var search: EditText
    private var apps: List<android.content.pm.ResolveInfo> = emptyList()
    private var drawerOpen = false

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { WallpaperManager.getInstance(this).setStream(it) }
                recreate()
            } catch (_: Exception) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        showHome()
    }

    private fun glass(color: Int = panel, radius: Float = 26f): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.argb(175, 66, 72, 105), color, Color.argb(155, 25, 29, 49))
        ).apply {
            cornerRadius = radius
            setStroke(1, border)
        }

    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildRoot(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 18)
            background = GradientDrawable().apply { setColor(bg) }
            clipToPadding = false
        }
        drawerScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root, ScrollView.LayoutParams(-1, -2))
        }
        setContentView(drawerScroll)
        return root
    }

    private fun animateScreen(change: () -> Unit) {
        val oldRoot = if (::root.isInitialized) root else null
        if (oldRoot == null || !oldRoot.isAttachedToWindow) {
            change()
            return
        }
        oldRoot.animate().alpha(0f).translationY(12f).setDuration(110).withEndAction {
            change()
            root.alpha = 0f
            root.translationY = -10f
            root.animate().alpha(1f).translationY(0f).setDuration(190).start()
        }.start()
    }

    private fun showHome() {
        drawerOpen = false
        buildRoot()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(text("ASTRAL", 13f, accent, true).apply { letterSpacing = .28f },
            LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text("✦  GLASS EDITION", 10f, Color.LTGRAY))
        root.addView(top)

        val clock = text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 54f, Color.WHITE, true)
        clock.setPadding(0, 48, 0, 0)
        root.addView(clock)
        root.addView(text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
            .replaceFirstChar { it.uppercase() }, 15f, Color.LTGRAY))

        text("Tu espacio. Tu universo.", 17f, Color.WHITE, true).also {
            it.setPadding(0, 32, 0, 16)
            root.addView(it)
        }

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        actions.addView(actionTile("⌕", "Buscar") { openDrawer() },
            LinearLayout.LayoutParams(0, 96, 1f).apply { marginEnd = 8 })
        actions.addView(actionTile("▧", "Fondo") { wallpaperPicker.launch("image/*") },
            LinearLayout.LayoutParams(0, 96, 1f).apply { marginStart = 8 })
        root.addView(actions)

        text("ACCESOS RÁPIDOS", 11f, Color.LTGRAY, true).also {
            it.setPadding(2, 32, 0, 14)
            root.addView(it)
        }
        val dock = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        apps.take(5).forEach { info ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(3, 7, 3, 5)
                background = glass(Color.argb(85, 43, 48, 75), 20f)
                setOnClickListener { launch(info) }
            }
            val icon = ImageView(this).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = ImageView.ScaleType.FIT_CENTER
                // Preserve the app's original icon colors; do not apply a monochrome tint.
            }
            item.addView(icon, LinearLayout.LayoutParams(42, 42))
            val name = text(info.loadLabel(packageManager).toString(), 9.5f, Color.WHITE)
            name.maxLines = 1
            name.ellipsize = TextUtils.TruncateAt.END
            name.gravity = Gravity.CENTER
            name.includeFontPadding = false
            item.addView(name, LinearLayout.LayoutParams(-1, 20))
            dock.addView(item, LinearLayout.LayoutParams(0, 78, 1f).apply {
                marginStart = 3; marginEnd = 3
            })
        }
        root.addView(dock)

        val all = TextView(this).apply {
            text = "Desliza hacia arriba o toca aquí para ver todas las apps  ↑"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setPadding(8, 24, 8, 24)
            setOnClickListener { openDrawer() }
        }
        root.addView(all)
        root.setOnTouchListener(object : View.OnTouchListener {
            private var y = 0f
            override fun onTouch(v: View?, e: android.view.MotionEvent): Boolean {
                if (e.action == android.view.MotionEvent.ACTION_DOWN) y = e.y
                if (e.action == android.view.MotionEvent.ACTION_UP && y - e.y > 70) openDrawer()
                return false
            }
        })
    }

    private fun actionTile(symbol: String, title: String, click: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glass(panel, 24f)
            setPadding(10, 8, 10, 8)
            elevation = 5f
            setOnClickListener { click() }
            val glyph = text(symbol, 27f, accent, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(glyph, LinearLayout.LayoutParams(-1, 38))
            val caption = text(title, 13f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
            }
            addView(caption, LinearLayout.LayoutParams(-1, 24))
        }

    private fun openDrawer() {
        if (drawerOpen) return
        animateScreen {
            drawerOpen = true
            buildRoot()
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            header.addView(text("Aplicaciones", 25f, Color.WHITE, true),
                LinearLayout.LayoutParams(0, -2, 1f))
            header.addView(text("✕", 22f, accent, true).apply {
                setPadding(12, 8, 2, 8)
                setOnClickListener { animateScreen { showHome() } }
            })
            root.addView(header)

            search = EditText(this).apply {
                hint = "Buscar una aplicación…"
                setSingleLine(true)
                maxLines = 1
                textSize = 15f
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                background = glass(Color.argb(155, 38, 43, 68), 22f)
                setPadding(18, 0, 18, 0)
                gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
            }
            val params = LinearLayout.LayoutParams(-1, 58).apply {
                topMargin = 20
                bottomMargin = 18
            }
            root.addView(search, params)
            appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            root.addView(appList)
            renderApps("")
            search.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderApps(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
            drawerScroll.post { drawerScroll.scrollTo(0, 0) }
        }
    }

    private fun renderApps(query: String) {
        appList.removeAllViews()
        apps.filter { it.loadLabel(packageManager).toString().contains(query, true) }.forEach { info ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12, 6, 12, 6)
                background = glass(Color.argb(55, 49, 54, 82), 18f)
                setOnClickListener { launch(info) }
            }
            val icon = ImageView(this).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            row.addView(icon, LinearLayout.LayoutParams(42, 42).apply { marginEnd = 16 })
            val appName = text(info.loadLabel(packageManager).toString(), 15f)
            appName.maxLines = 1
            appName.ellipsize = TextUtils.TruncateAt.END
            row.addView(appName, LinearLayout.LayoutParams(0, -2, 1f))
            appList.addView(row, LinearLayout.LayoutParams(-1, 62).apply { bottomMargin = 7 })
        }
    }

    private fun launch(info: android.content.pm.ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
        try { startActivity(intent) } catch (_: Exception) { }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (drawerOpen) animateScreen { showHome() } else super.onBackPressed()
    }
}

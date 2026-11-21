package studio.astral.launcher

import androidx.activity.ComponentActivity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import androidx.core.view.setPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(10, 12, 22)
    private val panel = Color.argb(175, 30, 34, 55)
    private val accent = Color.rgb(145, 130, 255)
    private lateinit var root: LinearLayout
    private lateinit var appList: LinearLayout
    private lateinit var search: EditText
    private var apps: List<android.content.pm.ResolveInfo> = emptyList()

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use {
                    WallpaperManager.getInstance(this).setStream(it)
                }
                recreate()
            } catch (_: Exception) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        showHome()
    }

    private fun rounded(color: Int, radius: Float = 28f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun showHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22)
            background = rounded(bg, 0f)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroll)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = text("ASTRAL", 13f, accent, true).apply { letterSpacing = .28f }
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text("✦  GLASS EDITION", 10f, Color.LTGRAY))
        root.addView(top)

        val clock = text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 54f, Color.WHITE, true)
        clock.setPadding(0, 54, 0, 0)
        root.addView(clock)
        root.addView(text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()).replaceFirstChar { it.uppercase() }, 15f, Color.LTGRAY))

        val greeting = text("Tu espacio. Tu universo.", 17f, Color.WHITE, true)
        greeting.setPadding(0, 34, 0, 16)
        root.addView(greeting)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        actions.addView(actionTile("⌕", "Buscar", 0) { openDrawer() }, LinearLayout.LayoutParams(0, 92, 1f).apply { marginEnd = 8 })
        actions.addView(actionTile("▧", "Fondo", 1) { wallpaperPicker.launch("image/*") }, LinearLayout.LayoutParams(0, 92, 1f).apply { marginStart = 8 })
        root.addView(actions)

        val label = text("ACCESOS RÁPIDOS", 11f, Color.LTGRAY, true)
        label.setPadding(2, 34, 0, 14)
        root.addView(label)
        val favorites = apps.take(5)
        val dock = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        favorites.forEach { info ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(4)
                setOnClickListener { launch(info) }
            }
            val icon = ImageView(this).apply { setImageDrawable(info.loadIcon(packageManager)); scaleType = ImageView.ScaleType.FIT_CENTER }
            item.addView(icon, LinearLayout.LayoutParams(48, 48))
            item.addView(text(info.loadLabel(packageManager).toString().take(9), 10f, Color.WHITE))
            dock.addView(item, LinearLayout.LayoutParams(0, 78, 1f))
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
                return true
            }
        })
    }

    private fun actionTile(symbol: String, title: String, ignored: Int, click: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(panel, 24f)
            setOnClickListener { click() }
            addView(text(symbol, 27f, accent, true))
            addView(text(title, 12f, Color.WHITE))
        }

    private fun openDrawer() {
        root.removeAllViews()
        root.orientation = LinearLayout.VERTICAL
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(text("Aplicaciones", 25f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        val close = text("✕", 22f, accent, true).apply { setOnClickListener { showHome() } }
        header.addView(close)
        root.addView(header)
        search = EditText(this).apply {
            hint = "Buscar una aplicación…"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            background = rounded(panel, 22f)
            setPadding(18)
        }
        val params = LinearLayout.LayoutParams(-1, 54).apply { topMargin = 22; bottomMargin = 16 }
        root.addView(search, params)
        appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(appList)
        renderApps("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun renderApps(query: String) {
        appList.removeAllViews()
        apps.filter { it.loadLabel(packageManager).toString().contains(query, true) }.forEach { info ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(10)
                setOnClickListener { launch(info) }
            }
            val icon = ImageView(this).apply { setImageDrawable(info.loadIcon(packageManager)) }
            row.addView(icon, LinearLayout.LayoutParams(42, 42).apply { marginEnd = 16 })
            row.addView(text(info.loadLabel(packageManager).toString(), 15f))
            appList.addView(row, LinearLayout.LayoutParams(-1, 58))
        }
    }

    private fun launch(info: android.content.pm.ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
        try { startActivity(intent) } catch (_: Exception) { }
    }
}

package com.promptlab.workshop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView webView;
    private View toastBack;
    private boolean backPressedOnce = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int padTop = 0, padBottom = 0, padLeft = 0, padRight = 0;
    private String currentTheme = "light";

    public static class Bridge {
        private final Activity activity;
        Bridge(Activity a) { activity = a; }

        @JavascriptInterface
        public void setTheme(String theme) {
            activity.runOnUiThread(() -> {
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).applyThemeChrome(theme);
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        applyThemeChrome("light");

        webView = findViewById(R.id.webview);
        toastBack = findViewById(R.id.toastBack);

        // 不要把内容画进状态栏/导航栏（避免顶栏与系统栏重叠）
        setupSystemBars();
        setupWindowInsets();

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        String ua = s.getUserAgentString();
        if (ua != null && !ua.contains("Chrome/")) {
            s.setUserAgentString(ua + " Chrome/121.0.0.0 Mobile");
        }

        webView.addJavascriptInterface(new Bridge(this), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 外链（GitHub 主页等）用系统浏览器打开，避免离开本应用
                if (url != null
                        && (url.startsWith("http://") || url.startsWith("https://"))
                        && !url.contains("android_asset")) {
                    try {
                        android.content.Intent i = new android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url));
                        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(
                        "(function(){try{return localStorage.getItem('pl_theme')||document.documentElement.getAttribute('data-theme')||'light';}catch(e){return document.documentElement.getAttribute('data-theme')||'light';}})()",
                        value -> {
                            String theme = (value != null) ? value.replace("\"", "") : "light";
                            if (!"dark".equals(theme)) theme = "light";
                            currentTheme = theme;
                            applyThemeChrome(theme);
                            resolveAndApplyInsets();
                        });
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        webView.setBackgroundColor(Color.parseColor("#F2F4F3"));
        webView.loadUrl("file:///android_asset/index.html");
    }

    /**
     * 系统栏：不用 LAYOUT_FULLSCREEN，让 WebView 画在系统栏下方，
     * 顶栏就不会和状态栏叠在一起；系统栏颜色跟主题。
     */
    private void setupSystemBars() {
        Window window = getWindow();
        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        // 清掉可能存在的「画到状态栏后面」标志
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        flags |= View.SYSTEM_UI_FLAG_VISIBLE;
        decor.setSystemUiVisibility(flags);
        applyThemeChrome(currentTheme);
    }

    private void setupWindowInsets() {
        final View decor = getWindow().getDecorView();
        View.OnApplyWindowInsetsListener listener = (v, insets) -> {
            readInsets(insets);
            resolveAndApplyInsets();
            return insets;
        };
        decor.setOnApplyWindowInsetsListener(listener);
        View content = findViewById(android.R.id.content);
        if (content != null) {
            content.setOnApplyWindowInsetsListener(listener);
        }
        decor.post(this::resolveAndApplyInsets);
        // 多帧后再取一次，覆盖部分机型首帧 inset=0
        decor.postDelayed(this::resolveAndApplyInsets, 300);
        decor.postDelayed(this::resolveAndApplyInsets, 800);
    }

    @SuppressWarnings("deprecation")
    private void readInsets(WindowInsets insets) {
        if (insets == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout());
            padTop = bars.top;
            padBottom = bars.bottom;
            padLeft = bars.left;
            padRight = bars.right;
        } else {
            padTop = insets.getSystemWindowInsetTop();
            padBottom = insets.getSystemWindowInsetBottom();
            padLeft = insets.getSystemWindowInsetLeft();
            padRight = insets.getSystemWindowInsetRight();
        }
    }

    /** inset 为 0 时用系统资源高度兜底，再写到 WebView 与 CSS 变量 */
    private void resolveAndApplyInsets() {
        int top = padTop;
        int bottom = padBottom;
        int left = padLeft;
        int right = padRight;
        if (top <= 0) top = getStatusBarHeightPx();
        // 未开 edge-to-edge 时系统已让出导航栏，bottom 保持 inset 原值即可
        if (bottom < 0) bottom = 0;

        padTop = top;
        padBottom = bottom;
        padLeft = left;
        padRight = right;

        if (webView != null) {
            boolean contentAlreadyBelowBars =
                    getWindow() != null
                    && ((getWindow().getDecorView().getSystemUiVisibility()
                            & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == 0);
            int webPadTop;
            int webPadBottom;
            if (contentAlreadyBelowBars) {
                // 系统已让出状态栏：WebView 不再加 top，避免双倍空白
                webPadTop = 0;
                webPadBottom = Math.max(bottom, 0);
            } else {
                // 极少数机型仍画到栏后面：用 inset/资源高度垫高 WebView
                webPadTop = Math.max(top, getStatusBarHeightPx());
                webPadBottom = Math.max(bottom, 0);
            }
            webView.setPadding(left, webPadTop, right, webPadBottom);
            // CSS 变量只补「WebView 未处理」的部分，防止与 padding 叠加
            int cssTop = Math.max(0, top - webPadTop);
            int cssBottom = Math.max(0, bottom - webPadBottom);
            padTop = cssTop;
            padBottom = cssBottom;
            injectSafeAreaToWeb();
        }
        if (toastBack != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) toastBack.getLayoutParams();
            lp.bottomMargin = dp(48) + Math.max(padBottom, 0);
            toastBack.setLayoutParams(lp);
        }
    }

    private int getStatusBarHeightPx() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : dp(24);
    }

    private void injectSafeAreaToWeb() {
        if (webView == null) return;
        String js = "(function(){var r=document.documentElement;"
                + "r.style.setProperty('--safe-top','" + padTop + "px');"
                + "r.style.setProperty('--safe-bottom','" + padBottom + "px');"
                + "r.style.setProperty('--safe-left','" + padLeft + "px');"
                + "r.style.setProperty('--safe-right','" + padRight + "px');"
                + "r.classList.add('android-safe-ready');"
                + "})();";
        webView.evaluateJavascript(js, null);
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    /** 顶栏/底栏与网页深浅色同步：状态栏、导航栏、窗口背景、图标前景一起改 */
    void applyThemeChrome(String theme) {
        currentTheme = theme != null ? theme : "light";
        boolean dark = "dark".equals(currentTheme);
        int paper = dark ? Color.parseColor("#111416") : Color.parseColor("#F2F4F3");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 系统栏与页面同色（不透明，避免 OEM 把透明栏画成默认浅色）
            window.setStatusBarColor(paper);
            window.setNavigationBarColor(paper);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(paper));

            View decor = window.getDecorView();
            decor.setBackgroundColor(paper);
            int flags = decor.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (dark) {
                // 深色底 → 浅色状态栏图标；取消浅色导航栏图标
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                // 浅色底 → 深色状态栏/导航栏图标
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decor.setSystemUiVisibility(flags);

            if (Build.VERSION.SDK_INT >= 29) {
                window.setStatusBarContrastEnforced(false);
                window.setNavigationBarContrastEnforced(false);
            }
        }

        // 根布局 / WebView 背景同步，系统栏区域与内容一致
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(paper);
        }
        View frame = findViewById(R.id.webview) != null ? findViewById(R.id.webview).getParent() instanceof View ? (View) findViewById(R.id.webview).getParent() : null : null;
        if (frame != null) {
            frame.setBackgroundColor(paper);
        }
        if (webView != null) {
            webView.setBackgroundColor(paper);
        }
        if (toastBack != null) {
            toastBack.setBackgroundColor(Color.parseColor("#CC323232"));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (backPressedOnce) {
            super.onBackPressed();
            return;
        }
        backPressedOnce = true;
        if (toastBack != null) {
            toastBack.setVisibility(View.VISIBLE);
            handler.postDelayed(() -> {
                backPressedOnce = false;
                if (toastBack != null) toastBack.setVisibility(View.GONE);
            }, 1600);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        getWindow().getDecorView().post(this::resolveAndApplyInsets);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupSystemBars();
            resolveAndApplyInsets();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

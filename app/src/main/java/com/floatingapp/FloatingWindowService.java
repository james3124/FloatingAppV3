package com.floatingapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Chronometer;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.floatingapp.utils.AppPreferences;

import java.util.ArrayList;
import java.util.List;

public class FloatingWindowService extends Service {

    public static final String ACTION_START  = "ACTION_START";
    public static final String ACTION_STOP   = "ACTION_STOP";
    public static final String ACTION_TOGGLE = "ACTION_TOGGLE";
    public static final String CHANNEL_ID    = "FloatingWindowChannel";
    public static final int    NOTIFICATION_ID = 1;

    public static boolean isRunning = false;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private AppPreferences prefs;

    private int screenW, screenH;
    private boolean isMinimized = false;
    private boolean isLocked    = false;

    // Drag tracking
    private int   initialX, initialY;
    private float initialTouchX, initialTouchY;
    private long  touchDownTime;

    // Resize edge tracking
    private boolean isResizing = false;
    private int resizeInitialW, resizeInitialH;
    private float resizeInitialTouchX, resizeInitialTouchY;

    // Pinch-to-zoom resize
    private ScaleGestureDetector scaleDetector;
    private int preScaleW, preScaleH;

    // Tabs
    private int currentTab = 0; // 0=webview 1=notes 2=calc 3=timer 4=media
    private WebView webView;
    private EditText notesEditor;
    private Chronometer chronometer;
    private boolean timerRunning = false;
    private long timerPausedAt = 0;

    // Multi-window list (static so multiple windows can be tracked)
    public static List<FloatingWindowService> instances = new ArrayList<>();

    // Calculator state
    private StringBuilder calcInput = new StringBuilder();
    private double calcResult = 0;
    private boolean calcNewNumber = true;
    private String calcOperator = "";
    private TextView calcDisplay;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = new AppPreferences(this);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenW = metrics.widthPixels;
        screenH = metrics.heightPixels;

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                preScaleW = params.width;
                preScaleH = params.height;
                return true;
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                int newW = Math.max(260, Math.min(screenW - 20, (int)(preScaleW * factor)));
                int newH = Math.max(300, Math.min(screenH - 100, (int)(preScaleH * factor)));
                params.width  = newW;
                params.height = newH;
                windowManager.updateViewLayout(floatingView, params);
                prefs.setInt(AppPreferences.KEY_WIN_W, newW);
                prefs.setInt(AppPreferences.KEY_WIN_H, newH);
                return true;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action) && !isRunning) {
            startForegroundNotification();
            showFloatingWindow();
            isRunning = true;
            instances.add(this);
        } else if (ACTION_STOP.equals(action)) {
            stopSelf();
        } else if (ACTION_TOGGLE.equals(action) && isRunning) {
            toggleMinimize();
        }
        return START_STICKY;
    }

    // ─── Notification ────────────────────────────────────────────────────────

    private void startForegroundNotification() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Floating Window", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Keeps the floating window active");
        getSystemService(NotificationManager.class).createNotificationChannel(ch);

        PendingIntent stopPI = PendingIntent.getService(this, 0,
            new Intent(this, FloatingWindowService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent togglePI = PendingIntent.getService(this, 1,
            new Intent(this, FloatingWindowService.class).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent openPI = PendingIntent.getActivity(this, 2,
            new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Window Active")
            .setContentText("Running above other apps")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openPI)
            .addAction(android.R.drawable.ic_menu_view, "Toggle", togglePI)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPI)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, n);
    }

    // ─── Show Window ──────────────────────────────────────────────────────────

    private void showFloatingWindow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_window, null);

        // Restore saved size
        String sizeKey = prefs.getString(AppPreferences.KEY_DEFAULT_SIZE, "medium");
        int[] size = AppPreferences.getSizePreset(sizeKey);
        int savedW = prefs.getInt(AppPreferences.KEY_WIN_W, size[0]);
        int savedH = prefs.getInt(AppPreferences.KEY_WIN_H, size[1]);

        // Restore saved position
        String corner = prefs.getString(AppPreferences.KEY_DEFAULT_CORNER, "tl");
        int[] defaultPos = AppPreferences.getCornerPosition(corner, screenW, screenH, savedW, savedH);
        int savedX = prefs.getInt(AppPreferences.KEY_WIN_X, defaultPos[0]);
        int savedY = prefs.getInt(AppPreferences.KEY_WIN_Y, defaultPos[1]);

        params = new WindowManager.LayoutParams(
            savedW, savedH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = savedX;
        params.y = savedY;

        windowManager.addView(floatingView, params);

        // Restore opacity
        float opacity = prefs.getFloat(AppPreferences.KEY_OPACITY, 1.0f);
        floatingView.setAlpha(opacity);

        // Restore lock
        isLocked = prefs.getBoolean(AppPreferences.KEY_LOCKED, false);

        // Restore minimized state
        boolean wasMinimized = prefs.getBoolean(AppPreferences.KEY_MINIMIZED, false);

        setupControls();
        setupTabs();

        if (wasMinimized) setMinimized(true, false);

        // Apply theme color
        applyThemeColor();

        // Entrance animation
        animateIn();
    }

    private void animateIn() {
        ScaleAnimation anim = new ScaleAnimation(0.5f, 1f, 0.5f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(220);
        floatingView.startAnimation(anim);
    }

    // ─── Controls Setup ───────────────────────────────────────────────────────

    private void setupControls() {
        View titleBar      = floatingView.findViewById(R.id.titleBar);
        ImageButton btnMin = floatingView.findViewById(R.id.btnMinimize);
        ImageButton btnClose = floatingView.findViewById(R.id.btnClose);
        ImageButton btnLock  = floatingView.findViewById(R.id.btnLock);
        ImageButton btnSettings = floatingView.findViewById(R.id.btnSettings);
        SeekBar opacityBar  = floatingView.findViewById(R.id.opacityBar);
        LinearLayout minimizedView = floatingView.findViewById(R.id.minimizedView);
        ImageButton btnBubbleExpand = floatingView.findViewById(R.id.btnBubbleExpand);
        ImageButton btnBubbleClose  = floatingView.findViewById(R.id.btnBubbleClose);
        TextView tvTitle = floatingView.findViewById(R.id.tvTitle);
        View resizeHandle = floatingView.findViewById(R.id.resizeHandle);

        // Title
        String title = prefs.getString(AppPreferences.KEY_WINDOW_TITLE, "Floating Window");
        tvTitle.setText(title);

        // Lock button
        updateLockButton(btnLock);
        btnLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            prefs.setBoolean(AppPreferences.KEY_LOCKED, isLocked);
            updateLockButton(btnLock);
            Toast.makeText(this, isLocked ? "Window locked" : "Window unlocked", Toast.LENGTH_SHORT).show();
        });

        // Minimize / close
        btnMin.setOnClickListener(v -> setMinimized(true, true));
        btnClose.setOnClickListener(v -> stopSelf());
        btnBubbleExpand.setOnClickListener(v -> setMinimized(false, true));
        btnBubbleClose.setOnClickListener(v -> stopSelf());

        // Settings shortcut
        btnSettings.setOnClickListener(v -> {
            Intent i = new Intent(this, SettingsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        // Opacity seekbar
        opacityBar.setProgress((int)(prefs.getFloat(AppPreferences.KEY_OPACITY, 1.0f) * 100));
        opacityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                float alpha = Math.max(0.2f, p / 100f);
                floatingView.setAlpha(alpha);
                prefs.setFloat(AppPreferences.KEY_OPACITY, alpha);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        // Drag title bar
        setupDrag(titleBar);
        setupDrag(minimizedView);

        // Double-tap title bar to minimize
        titleBar.setOnClickListener(null);
        titleBar.setOnTouchListener((v, event) -> {
            handleDragTouch(v, event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                long elapsed = System.currentTimeMillis() - touchDownTime;
                float dx = Math.abs(event.getRawX() - initialTouchX);
                float dy = Math.abs(event.getRawY() - initialTouchY);
                if (elapsed < 200 && dx < 10 && dy < 10) {
                    // single tap — do nothing special (was click-on-drag-end)
                }
            }
            return true;
        });

        // Pinch to resize on content area
        View contentArea = floatingView.findViewById(R.id.contentArea);
        contentArea.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            return false;
        });

        // Resize handle (bottom-right corner drag)
        setupResizeHandle(resizeHandle);
    }

    private void updateLockButton(ImageButton btn) {
        btn.setImageResource(isLocked
            ? android.R.drawable.ic_lock_lock
            : android.R.drawable.ic_lock_idle_lock);
    }

    // ─── Drag ─────────────────────────────────────────────────────────────────

    private void setupDrag(View v) {
        v.setOnTouchListener((view, event) -> {
            handleDragTouch(view, event);
            return true;
        });
    }

    private void handleDragTouch(View v, MotionEvent event) {
        if (isLocked) return;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                touchDownTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int)(event.getRawX() - initialTouchX);
                int dy = (int)(event.getRawY() - initialTouchY);
                params.x = initialX + dx;
                params.y = initialY + dy;
                windowManager.updateViewLayout(floatingView, params);
                break;
            case MotionEvent.ACTION_UP:
                if (prefs.getBoolean(AppPreferences.KEY_SNAP_TO_EDGE, true)) {
                    snapToEdge();
                }
                savePosition();
                break;
        }
    }

    private void snapToEdge() {
        int centerX = params.x + params.width / 2;
        int target = centerX < screenW / 2 ? 0 : screenW - params.width;
        final int startX = params.x;
        final int endX = target;
        Handler h = new Handler(Looper.getMainLooper());
        final long start = System.currentTimeMillis();
        final int duration = 200;
        Runnable r = new Runnable() {
            @Override public void run() {
                float progress = Math.min(1f, (System.currentTimeMillis() - start) / (float) duration);
                float ease = 1 - (1 - progress) * (1 - progress); // ease out quad
                params.x = (int)(startX + (endX - startX) * ease);
                if (floatingView != null) windowManager.updateViewLayout(floatingView, params);
                if (progress < 1f) h.postDelayed(this, 16);
                else savePosition();
            }
        };
        h.post(r);
    }

    private void savePosition() {
        prefs.setInt(AppPreferences.KEY_WIN_X, params.x);
        prefs.setInt(AppPreferences.KEY_WIN_Y, params.y);
    }

    // ─── Resize Handle ────────────────────────────────────────────────────────

    private void setupResizeHandle(View handle) {
        handle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isResizing = true;
                    resizeInitialW = params.width;
                    resizeInitialH = params.height;
                    resizeInitialTouchX = event.getRawX();
                    resizeInitialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!isResizing) return false;
                    int newW = (int)(resizeInitialW + (event.getRawX() - resizeInitialTouchX));
                    int newH = (int)(resizeInitialH + (event.getRawY() - resizeInitialTouchY));
                    newW = Math.max(260, Math.min(screenW - 20, newW));
                    newH = Math.max(300, Math.min(screenH - 100, newH));
                    params.width  = newW;
                    params.height = newH;
                    windowManager.updateViewLayout(floatingView, params);
                    prefs.setInt(AppPreferences.KEY_WIN_W, newW);
                    prefs.setInt(AppPreferences.KEY_WIN_H, newH);
                    return true;
                case MotionEvent.ACTION_UP:
                    isResizing = false;
                    return true;
            }
            return false;
        });
    }

    // ─── Minimize / Restore ───────────────────────────────────────────────────

    private void toggleMinimize() {
        setMinimized(!isMinimized, true);
    }

    private void setMinimized(boolean minimize, boolean animate) {
        isMinimized = minimize;
        prefs.setBoolean(AppPreferences.KEY_MINIMIZED, minimize);

        LinearLayout expandedView  = floatingView.findViewById(R.id.expandedView);
        LinearLayout minimizedView = floatingView.findViewById(R.id.minimizedView);

        if (animate) {
            ScaleAnimation anim = new ScaleAnimation(
                minimize ? 1f : 0.3f, minimize ? 0.3f : 1f,
                minimize ? 1f : 0.3f, minimize ? 0.3f : 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(180);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation a) {}
                public void onAnimationRepeat(Animation a) {}
                public void onAnimationEnd(Animation a) {
                    applyMinimizeVisibility(expandedView, minimizedView, minimize);
                }
            });
            floatingView.startAnimation(anim);
        } else {
            applyMinimizeVisibility(expandedView, minimizedView, minimize);
        }
    }

    private void applyMinimizeVisibility(LinearLayout expanded, LinearLayout minimized, boolean minimize) {
        if (minimize) {
            expanded.setVisibility(View.GONE);
            minimized.setVisibility(View.VISIBLE);
            params.width  = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        } else {
            minimized.setVisibility(View.GONE);
            expanded.setVisibility(View.VISIBLE);
            params.width  = prefs.getInt(AppPreferences.KEY_WIN_W, 360);
            params.height = prefs.getInt(AppPreferences.KEY_WIN_H, 460);
        }
        windowManager.updateViewLayout(floatingView, params);
    }

    // ─── Tabs Setup ───────────────────────────────────────────────────────────

    private void setupTabs() {
        View tabWebView = floatingView.findViewById(R.id.tabWebView);
        View tabNotes   = floatingView.findViewById(R.id.tabNotes);
        View tabCalc    = floatingView.findViewById(R.id.tabCalc);
        View tabTimer   = floatingView.findViewById(R.id.tabTimer);
        View tabMedia   = floatingView.findViewById(R.id.tabMedia);

        View btnTabWebView = floatingView.findViewById(R.id.btnTabWebView);
        View btnTabNotes   = floatingView.findViewById(R.id.btnTabNotes);
        View btnTabCalc    = floatingView.findViewById(R.id.btnTabCalc);
        View btnTabTimer   = floatingView.findViewById(R.id.btnTabTimer);
        View btnTabMedia   = floatingView.findViewById(R.id.btnTabMedia);

        btnTabWebView.setOnClickListener(v -> switchTab(0));
        btnTabNotes.setOnClickListener(v -> switchTab(1));
        btnTabCalc.setOnClickListener(v -> switchTab(2));
        btnTabTimer.setOnClickListener(v -> switchTab(3));
        btnTabMedia.setOnClickListener(v -> switchTab(4));

        setupWebViewTab();
        setupNotesTab();
        setupCalcTab();
        setupTimerTab();
        setupMediaTab();

        int lastTab = prefs.getInt(AppPreferences.KEY_LAST_TAB, 0);
        switchTab(lastTab);
    }

    private void switchTab(int tab) {
        currentTab = tab;
        prefs.setInt(AppPreferences.KEY_LAST_TAB, tab);

        int[] tabViews = {R.id.tabWebView, R.id.tabNotes, R.id.tabCalc, R.id.tabTimer, R.id.tabMedia};
        int[] tabBtns  = {R.id.btnTabWebView, R.id.btnTabNotes, R.id.btnTabCalc, R.id.btnTabTimer, R.id.btnTabMedia};

        for (int i = 0; i < tabViews.length; i++) {
            View tv = floatingView.findViewById(tabViews[i]);
            View tb = floatingView.findViewById(tabBtns[i]);
            tv.setVisibility(i == tab ? View.VISIBLE : View.GONE);
            tb.setSelected(i == tab);
        }
    }

    // ─── WebView Tab ──────────────────────────────────────────────────────────

    private void setupWebViewTab() {
        webView = floatingView.findViewById(R.id.webView);
        EditText urlBar = floatingView.findViewById(R.id.urlBar);
        ImageButton btnGo   = floatingView.findViewById(R.id.btnGo);
        ImageButton btnBack = floatingView.findViewById(R.id.btnBack);
        ImageButton btnFwd  = floatingView.findViewById(R.id.btnFwd);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                urlBar.setText(url);
                prefs.setString(AppPreferences.KEY_LAST_URL, url);
            }
        });

        // Fix focusable for URL input
        webView.setOnTouchListener((v, e) -> {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            windowManager.updateViewLayout(floatingView, params);
            return false;
        });
        urlBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            } else {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }
            windowManager.updateViewLayout(floatingView, params);
        });

        String lastUrl = prefs.getString(AppPreferences.KEY_LAST_URL, "https://www.google.com");
        urlBar.setText(lastUrl);

        btnGo.setOnClickListener(v -> {
            String url = urlBar.getText().toString().trim();
            if (!url.startsWith("http")) url = "https://" + url;
            webView.loadUrl(url);
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowManager.updateViewLayout(floatingView, params);
        });
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnFwd.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });

        webView.loadUrl(lastUrl);
    }

    // ─── Notes Tab ────────────────────────────────────────────────────────────

    private void setupNotesTab() {
        notesEditor = floatingView.findViewById(R.id.notesEditor);
        ImageButton btnSaveNote = floatingView.findViewById(R.id.btnSaveNote);
        ImageButton btnClearNote = floatingView.findViewById(R.id.btnClearNote);

        String saved = prefs.getString("notes_content", "");
        notesEditor.setText(saved);

        notesEditor.setOnFocusChangeListener((v, hasFocus) -> {
            params.flags = hasFocus
                ? WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowManager.updateViewLayout(floatingView, params);
        });

        btnSaveNote.setOnClickListener(v -> {
            prefs.setString("notes_content", notesEditor.getText().toString());
            Toast.makeText(this, "Notes saved!", Toast.LENGTH_SHORT).show();
        });
        btnClearNote.setOnClickListener(v -> {
            notesEditor.setText("");
            prefs.setString("notes_content", "");
        });
    }

    // ─── Calculator Tab ───────────────────────────────────────────────────────

    private void setupCalcTab() {
        calcDisplay = floatingView.findViewById(R.id.calcDisplay);
        calcDisplay.setText("0");

        int[] numBtns = {
            R.id.calc0, R.id.calc1, R.id.calc2, R.id.calc3, R.id.calc4,
            R.id.calc5, R.id.calc6, R.id.calc7, R.id.calc8, R.id.calc9,
            R.id.calcDot
        };
        String[] numVals = {"0","1","2","3","4","5","6","7","8","9","."};

        for (int i = 0; i < numBtns.length; i++) {
            final String val = numVals[i];
            floatingView.findViewById(numBtns[i]).setOnClickListener(v -> calcAppend(val));
        }

        floatingView.findViewById(R.id.calcPlus).setOnClickListener(v -> calcOp("+"));
        floatingView.findViewById(R.id.calcMinus).setOnClickListener(v -> calcOp("-"));
        floatingView.findViewById(R.id.calcMul).setOnClickListener(v -> calcOp("×"));
        floatingView.findViewById(R.id.calcDiv).setOnClickListener(v -> calcOp("÷"));
        floatingView.findViewById(R.id.calcEquals).setOnClickListener(v -> calcEquals());
        floatingView.findViewById(R.id.calcClear).setOnClickListener(v -> calcClear());
        floatingView.findViewById(R.id.calcDel).setOnClickListener(v -> calcDelete());
        floatingView.findViewById(R.id.calcPct).setOnClickListener(v -> calcPercent());
        floatingView.findViewById(R.id.calcSign).setOnClickListener(v -> calcToggleSign());
    }

    private void calcAppend(String s) {
        if (calcNewNumber) { calcInput.setLength(0); calcNewNumber = false; }
        if (s.equals(".") && calcInput.toString().contains(".")) return;
        if (calcInput.length() >= 12) return;
        calcInput.append(s);
        calcDisplay.setText(calcInput.length() == 0 ? "0" : calcInput.toString());
    }
    private void calcOp(String op) {
        calcEquals();
        calcOperator = op;
        calcResult = Double.parseDouble(calcDisplay.getText().toString());
        calcNewNumber = true;
    }
    private void calcEquals() {
        if (calcOperator.isEmpty()) return;
        double b = Double.parseDouble(calcDisplay.getText().toString());
        double res;
        switch (calcOperator) {
            case "+": res = calcResult + b; break;
            case "-": res = calcResult - b; break;
            case "×": res = calcResult * b; break;
            case "÷": res = b == 0 ? 0 : calcResult / b; break;
            default: return;
        }
        calcOperator = "";
        calcNewNumber = true;
        String out = res == (long)res ? String.valueOf((long)res) : String.valueOf(res);
        calcDisplay.setText(out);
        calcInput = new StringBuilder(out);
    }
    private void calcClear() { calcInput.setLength(0); calcResult = 0; calcOperator = ""; calcNewNumber = true; calcDisplay.setText("0"); }
    private void calcDelete() {
        if (calcInput.length() > 0) { calcInput.deleteCharAt(calcInput.length()-1); }
        calcDisplay.setText(calcInput.length() == 0 ? "0" : calcInput.toString());
    }
    private void calcPercent() {
        try { double v = Double.parseDouble(calcDisplay.getText().toString()) / 100; String s = v == (long)v ? String.valueOf((long)v) : String.valueOf(v); calcDisplay.setText(s); calcInput = new StringBuilder(s); } catch (Exception ignored) {}
    }
    private void calcToggleSign() {
        try { double v = -Double.parseDouble(calcDisplay.getText().toString()); String s = v == (long)v ? String.valueOf((long)v) : String.valueOf(v); calcDisplay.setText(s); calcInput = new StringBuilder(s); } catch (Exception ignored) {}
    }

    // ─── Timer Tab ────────────────────────────────────────────────────────────

    private void setupTimerTab() {
        chronometer = floatingView.findViewById(R.id.chronometer);
        ImageButton btnTimerStart = floatingView.findViewById(R.id.btnTimerStart);
        ImageButton btnTimerPause = floatingView.findViewById(R.id.btnTimerPause);
        ImageButton btnTimerReset = floatingView.findViewById(R.id.btnTimerReset);

        btnTimerStart.setOnClickListener(v -> {
            if (!timerRunning) {
                if (timerPausedAt > 0) {
                    chronometer.setBase(chronometer.getBase() + (SystemClock.elapsedRealtime() - timerPausedAt));
                } else {
                    chronometer.setBase(SystemClock.elapsedRealtime());
                }
                chronometer.start();
                timerRunning = true;
                timerPausedAt = 0;
            }
        });
        btnTimerPause.setOnClickListener(v -> {
            if (timerRunning) {
                chronometer.stop();
                timerPausedAt = SystemClock.elapsedRealtime();
                timerRunning = false;
            }
        });
        btnTimerReset.setOnClickListener(v -> {
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            timerRunning = false;
            timerPausedAt = 0;
        });
    }

    // ─── Media Tab ────────────────────────────────────────────────────────────

    private void setupMediaTab() {
        ImageButton btnPrev  = floatingView.findViewById(R.id.btnMediaPrev);
        ImageButton btnPlay  = floatingView.findViewById(R.id.btnMediaPlay);
        ImageButton btnNext  = floatingView.findViewById(R.id.btnMediaNext);
        TextView tvMediaInfo = floatingView.findViewById(R.id.tvMediaInfo);

        tvMediaInfo.setText("Media controls send broadcast intents to\nyour music app");

        btnPrev.setOnClickListener(v -> sendMediaCommand("PREVIOUS"));
        btnPlay.setOnClickListener(v -> sendMediaCommand("PLAY_PAUSE"));
        btnNext.setOnClickListener(v -> sendMediaCommand("NEXT"));
    }

    private void sendMediaCommand(String cmd) {
        // Sends broadcast; media apps that listen to standard intents will respond
        Intent i = new Intent("com.floatingapp.MEDIA_COMMAND");
        i.putExtra("command", cmd);
        sendBroadcast(i);
        Toast.makeText(this, "Media: " + cmd, Toast.LENGTH_SHORT).show();
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    private void applyThemeColor() {
        // Theme color is applied via tint; the layout already uses the accent color
        // For a full implementation, dynamically tint the title bar here
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        instances.remove(this);
        if (floatingView != null && windowManager != null) {
            if (webView != null) webView.destroy();
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}

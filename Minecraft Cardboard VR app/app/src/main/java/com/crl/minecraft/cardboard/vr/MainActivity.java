package com.crl.minecraft.cardboard.vr;

import com.crl.minecraft.cardboard.vr.GyroService;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.animation.ValueAnimator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity {
    private EditText portE;
    private Button btn;
    private TextView status;
    private DotView statusDot;
    private SoundPool sounds;
    private int clickSound;
    private TextView viewerStatus;
    private Button clearViewerButton;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(22, 20, 16));
        getWindow().setNavigationBarColor(Color.rgb(22, 20, 16));
        getWindow().getDecorView().setSystemUiVisibility(0);

        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        sounds = new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audio).build();
        clickSound = sounds.load(this, R.raw.mc_click, 1);

        SharedPreferences sp = getSharedPreferences("gyro", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(34), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(35, 31, 24));

        TextView eyebrow = text("CRAFTED FOR CARDBOARD VR", 11, Color.rgb(166, 148, 112), true);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow, matchWrap());

        TextView title = text("Minecraft Cardboard VR", 25, Color.rgb(245, 229, 190), true);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(dp(2), 0, dp(2), Color.rgb(0, 0, 0));
        LinearLayout.LayoutParams titleLp = matchWrap();
        titleLp.topMargin = dp(8);
        root.addView(title, titleLp);

        TextView subtitle = text("Minecraft VR  //  Google Cardboard VR", 12,
                Color.rgb(190, 178, 151), false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = matchWrap();
        subLp.topMargin = dp(7);
        root.addView(subtitle, subLp);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(panelBackground());
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelLp.topMargin = dp(30);
        root.addView(panel, panelLp);

        TextView portLabel = text("UDP PORT", 12, Color.rgb(219, 199, 151), true);
        panel.addView(portLabel, matchWrap());

        portE = new EditText(this);
        portE.setSingleLine(true);
        portE.setInputType(InputType.TYPE_CLASS_NUMBER);
        portE.setText(String.valueOf(sp.getInt("port", 9876)));
        portE.setTextSize(18);
        portE.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        portE.setTextColor(Color.rgb(255, 241, 207));
        portE.setHintTextColor(Color.rgb(135, 123, 101));
        portE.setHint("9876");
        portE.setSelectAllOnFocus(true);
        portE.setPadding(dp(13), dp(9), dp(13), dp(9));
        portE.setBackground(editBackground());
        LinearLayout.LayoutParams portLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        portLp.topMargin = dp(9);
        panel.addView(portE, portLp);

        TextView portHint = text("Must match the port in your Minecraft mod.", 11,
                Color.rgb(163, 151, 128), false);
        LinearLayout.LayoutParams hintLp = matchWrap();
        hintLp.topMargin = dp(7);
        panel.addView(portHint, hintLp);

        btn = new Button(this);
        btn.setAllCaps(false);
        btn.setTextSize(16);
        btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btn.setTextColor(Color.rgb(255, 239, 199));
        btn.setPadding(dp(12), 0, dp(12), 0);
        btn.setMinHeight(dp(54));
        btn.setBackground(buttonBackground(false));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        btnLp.topMargin = dp(22);
        panel.addView(btn, btnLp);

        LinearLayout statusPanel = new LinearLayout(this);
        statusPanel.setOrientation(LinearLayout.HORIZONTAL);
        statusPanel.setGravity(Gravity.CENTER_VERTICAL);
        statusPanel.setPadding(dp(13), dp(13), dp(13), dp(13));
        statusPanel.setBackground(statusBackground());
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(17);
        panel.addView(statusPanel, statusLp);

        statusDot = new DotView(this);
        statusPanel.addView(statusDot, new LinearLayout.LayoutParams(dp(36), dp(36)));

        status = text("", 12, Color.rgb(225, 215, 190), false);
        status.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams statusTextLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statusTextLp.leftMargin = dp(11);
        statusPanel.addView(status, statusTextLp);

        // This viewer setup UI is intentionally independent of GyroService.
        LinearLayout viewerPanel = new LinearLayout(this);
        viewerPanel.setOrientation(LinearLayout.VERTICAL);
        viewerPanel.setPadding(dp(18), dp(17), dp(18), dp(17));
        viewerPanel.setBackground(panelBackground());
        LinearLayout.LayoutParams viewerPanelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        viewerPanelLp.topMargin = dp(16);
        root.addView(viewerPanel, viewerPanelLp);

        TextView viewerTitle = text("CARDBOARD VIEWER", 13,
                Color.rgb(245, 229, 190), true);
        viewerTitle.setLetterSpacing(0.08f);
        viewerPanel.addView(viewerTitle, matchWrap());

        TextView viewerHint = text("Scan the QR code printed on your headset to save its lens and display parameters.",
                11, Color.rgb(163, 151, 128), false);
        viewerHint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams viewerHintLp = matchWrap();
        viewerHintLp.topMargin = dp(7);
        viewerPanel.addView(viewerHint, viewerHintLp);

        Button scanViewerButton = new Button(this);
        scanViewerButton.setAllCaps(false);
        scanViewerButton.setText("Scan Cardboard QR Code");
        scanViewerButton.setTextSize(14);
        scanViewerButton.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        scanViewerButton.setTextColor(Color.rgb(255, 239, 199));
        scanViewerButton.setPadding(dp(8), 0, dp(8), 0);
        scanViewerButton.setMinHeight(dp(50));
        scanViewerButton.setBackground(buttonBackground(false));
        LinearLayout.LayoutParams scanViewerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        scanViewerLp.topMargin = dp(14);
        viewerPanel.addView(scanViewerButton, scanViewerLp);

        LinearLayout viewerInfoPanel = new LinearLayout(this);
        viewerInfoPanel.setOrientation(LinearLayout.VERTICAL);
        viewerInfoPanel.setPadding(dp(12), dp(11), dp(12), dp(11));
        viewerInfoPanel.setBackground(statusBackground());
        LinearLayout.LayoutParams viewerInfoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        viewerInfoLp.topMargin = dp(12);
        viewerPanel.addView(viewerInfoPanel, viewerInfoLp);

        viewerStatus = text("No viewer configured. Scan a Cardboard QR code to load its parameters.",
                11, Color.rgb(225, 215, 190), false);
        viewerStatus.setLineSpacing(dp(3), 1f);
        viewerInfoPanel.addView(viewerStatus, matchWrap());

        clearViewerButton = new Button(this);
        clearViewerButton.setAllCaps(false);
        clearViewerButton.setText("Clear Viewer");
        clearViewerButton.setTextSize(12);
        clearViewerButton.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clearViewerButton.setTextColor(Color.rgb(220, 207, 177));
        clearViewerButton.setPadding(dp(8), 0, dp(8), 0);
        clearViewerButton.setMinHeight(dp(42));
        clearViewerButton.setBackground(secondaryButtonBackground());
        LinearLayout.LayoutParams clearViewerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        clearViewerLp.topMargin = dp(10);
        viewerPanel.addView(clearViewerButton, clearViewerLp);

        scanViewerButton.setOnClickListener(v -> {
            playClick();
            IntentIntegrator scanner = new IntentIntegrator(this);
            scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            scanner.setPrompt("Scan the QR code on your Google Cardboard viewer");
            scanner.setBeepEnabled(false);
            scanner.setOrientationLocked(false);
            scanner.setBarcodeImageEnabled(false);
            scanner.initiateScan();
        });

        clearViewerButton.setOnClickListener(v -> {
            playClick();
            sp.edit().remove("viewer_proto").remove("viewer_vendor")
                    .remove("viewer_model").remove("viewer_screen_to_lens")
                    .remove("viewer_inter_lens").remove("viewer_tray_to_lens")
                    .remove("viewer_fov").remove("viewer_distortion")
                    .remove("viewer_vertical_alignment").remove("viewer_primary_button")
                    .apply();
            refreshViewer(sp);
        });
        refreshViewer(sp);

        TextView footer = text("KEEP THIS APP RUNNING WHILE PLAYING", 10,
                Color.rgb(132, 119, 94), true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = matchWrap();
        footerLp.topMargin = dp(25);
        root.addView(footer, footerLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(35, 31, 24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        btn.setOnClickListener(v -> {
            playClick();
            if (GyroService.running) {
                stopService(new Intent(this, GyroService.class));
            } else {
                int port = 9876;
                try { port = Integer.parseInt(portE.getText().toString().trim()); } catch (Exception ignored) {}
                if (port < 1024 || port > 65535) port = 9876;
                sp.edit().putInt("port", port).apply();
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                }
                Intent i = new Intent(this, GyroService.class).putExtra("port", port);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            }
            btn.postDelayed(this::refresh, 400);
            btn.postDelayed(this::refresh, 1000);
        });
        refresh();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable rect(int color, int strokeColor, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(0);
        d.setStroke(dp(stroke), strokeColor);
        return d;
    }

    private android.graphics.drawable.Drawable panelBackground() {
        GradientDrawable face = rect(Color.rgb(57, 51, 39), Color.rgb(112, 96, 66), 2);
        GradientDrawable shadow = rect(Color.rgb(20, 18, 14), Color.rgb(20, 18, 14), 1);
        return new LayerDrawable(new android.graphics.drawable.Drawable[]{shadow, face}) {{
            setLayerInset(1, 0, 0, 0, dp(4));
        }};
    }

    private android.graphics.drawable.Drawable editBackground() {
        GradientDrawable d = rect(Color.rgb(27, 25, 20), Color.rgb(119, 102, 69), 2);
        return d;
    }

    private android.graphics.drawable.Drawable statusBackground() {
        return rect(Color.rgb(39, 36, 28), Color.rgb(83, 75, 57), 1);
    }

    private android.graphics.drawable.Drawable buttonBackground(boolean running) {
        int faceColor = running ? Color.rgb(83, 78, 76) : Color.rgb(43, 125, 57);
        int edgeColor = running ? Color.rgb(151, 103, 96) : Color.rgb(112, 205, 119);
        int shadowColor = running ? Color.rgb(39, 30, 29) : Color.rgb(20, 57, 27);
        GradientDrawable normalFace = rect(faceColor, edgeColor, 2);
        GradientDrawable normalShadow = rect(shadowColor, shadowColor, 1);
        LayerDrawable normal = new LayerDrawable(new android.graphics.drawable.Drawable[]{normalShadow, normalFace});
        normal.setLayerInset(1, 0, 0, 0, dp(5));

        GradientDrawable pressedFace = rect(
                running ? Color.rgb(65, 58, 57) : Color.rgb(33, 99, 44),
                running ? Color.rgb(124, 83, 78) : Color.rgb(84, 160, 91), 2);
        GradientDrawable pressedShadow = rect(
                running ? Color.rgb(27, 22, 21) : Color.rgb(15, 42, 20),
                running ? Color.rgb(27, 22, 21) : Color.rgb(15, 42, 20), 1);
        LayerDrawable pressed = new LayerDrawable(new android.graphics.drawable.Drawable[]{pressedShadow, pressedFace});
        pressed.setLayerInset(1, 0, dp(3), 0, 0);

        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        return states;
    }

    private android.graphics.drawable.Drawable secondaryButtonBackground() {
        GradientDrawable face = rect(Color.rgb(57, 51, 39), Color.rgb(132, 113, 79), 2);
        GradientDrawable shadow = rect(Color.rgb(25, 22, 17), Color.rgb(25, 22, 17), 1);
        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{shadow, face});
        layers.setLayerInset(1, 0, 0, 0, dp(4));
        return layers;
    }

    private void refreshViewer(SharedPreferences sp) {
        if (viewerStatus == null || clearViewerButton == null) return;
        String model = sp.getString("viewer_model", "");
        String vendor = sp.getString("viewer_vendor", "");
        if (sp.getString("viewer_proto", null) == null) {
            viewerStatus.setText("No viewer configured. Scan a Cardboard QR code to load its parameters.");
            clearViewerButton.setEnabled(false);
            clearViewerButton.setAlpha(0.45f);
            return;
        }

        float screenToLens = sp.getFloat("viewer_screen_to_lens", 0f) * 1000f;
        float interLens = sp.getFloat("viewer_inter_lens", 0f) * 1000f;
        float trayToLens = sp.getFloat("viewer_tray_to_lens", 0f) * 1000f;
        String fov = sp.getString("viewer_fov", "");
        String distortion = sp.getString("viewer_distortion", "");
        String name = model.isEmpty() ? "Cardboard viewer" : model;
        if (!vendor.isEmpty()) name = vendor + " — " + name;

        StringBuilder summary = new StringBuilder("SAVED VIEWER: ").append(name)
                .append("\nScreen to lens: ").append(formatMm(screenToLens)).append(" mm")
                .append("\nLens spacing: ").append(formatMm(interLens)).append(" mm");
        if (trayToLens > 0f) summary.append("\nTray to lens: ").append(formatMm(trayToLens)).append(" mm");
        if (!fov.isEmpty()) summary.append("\nLeft-eye FOV (L/R/B/T): ").append(fov).append("°");
        if (!distortion.isEmpty()) summary.append("\nDistortion: ").append(distortion);
        summary.append("\nSaved on this phone. Boardcraft viewer-receiver support is still required for automatic mod loading.");
        viewerStatus.setText(summary.toString());
        clearViewerButton.setEnabled(true);
        clearViewerButton.setAlpha(1f);
    }

    private String formatMm(float value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                if (viewerStatus != null && getSharedPreferences("gyro", MODE_PRIVATE)
                        .getString("viewer_proto", null) == null) {
                    viewerStatus.setText("Scan cancelled. No viewer settings were changed.");
                }
            } else {
                try {
                    CardboardViewerParams params = CardboardViewerParams.fromQr(result.getContents());
                    SharedPreferences.Editor edit = getSharedPreferences("gyro", MODE_PRIVATE).edit();
                    edit.putString("viewer_proto", params.encodedProto);
                    edit.putString("viewer_vendor", params.vendor);
                    edit.putString("viewer_model", params.model);
                    edit.putFloat("viewer_screen_to_lens", params.screenToLensMeters);
                    edit.putFloat("viewer_inter_lens", params.interLensMeters);
                    edit.putFloat("viewer_tray_to_lens", params.trayToLensMeters);
                    edit.putString("viewer_fov", params.fovSummary());
                    edit.putString("viewer_distortion", params.distortionSummary());
                    edit.putInt("viewer_vertical_alignment", params.verticalAlignment);
                    edit.putInt("viewer_primary_button", params.primaryButton);
                    edit.apply();
                    refreshViewer(getSharedPreferences("gyro", MODE_PRIVATE));
                } catch (Exception e) {
                    if (viewerStatus != null) {
                        viewerStatus.setText("That QR code does not contain valid Google Cardboard viewer parameters. Try scanning the headset QR again.");
                    }
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void playClick() {
        if (sounds != null && clickSound != 0) sounds.play(clickSound, 0.75f, 0.75f, 1, 0, 1f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean r = GyroService.running;
        btn.setText(r ? "Stop" : "Start");
        btn.setBackground(buttonBackground(r));
        statusDot.setRunning(r);
        status.setText(r
                ? "Started — gyroscope data is now being sent to Minecraft on port "
                    + getSharedPreferences("gyro", MODE_PRIVATE).getInt("port", 9876)
                    + ". Keep this app open while you play."
                : "Stopped — gyroscope data transmission is off. Press Start when you're ready.");
        portE.setEnabled(!r);
        portE.setAlpha(r ? 0.55f : 1f);
    }

    @Override
    protected void onDestroy() {
        if (sounds != null) {
            sounds.release();
            sounds = null;
        }
        super.onDestroy();
    }

    private class DotView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean active;
        private float pulse = 0.72f;
        private ValueAnimator animator;

        DotView(android.content.Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

        void setRunning(boolean running) {
            if (active == running) return;
            active = running;
            if (animator != null) animator.cancel();
            if (running) {
                animator = ValueAnimator.ofFloat(0.58f, 1f);
                animator.setDuration(1700);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(a -> { pulse = (float) a.getAnimatedValue(); invalidate(); });
                animator.start();
            } else {
                pulse = 0.72f;
                invalidate();
            }
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = dp(5) * (active ? pulse : 1f);
            int color = active ? Color.rgb(87, 255, 93) : Color.rgb(244, 65, 55);

            // Keep the glow comfortably inside the larger view bounds.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(active
                    ? Color.argb((int) (42 * pulse), 80, 255, 90)
                    : Color.argb(32, 255, 65, 55));
            paint.setShadowLayer(dp(active ? 5 : 3), 0, 0,
                    active ? Color.rgb(67, 255, 83) : Color.rgb(244, 65, 55));
            c.drawCircle(cx, cy, radius + dp(active ? 4 : 2), paint);
            paint.clearShadowLayer();

            paint.setColor(color);
            c.drawCircle(cx, cy, radius, paint);
        }
    }
}

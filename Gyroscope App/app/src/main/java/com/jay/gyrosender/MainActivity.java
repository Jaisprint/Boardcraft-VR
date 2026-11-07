package com.jay.gyrosender;

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
import android.widget.TextView;

public class MainActivity extends Activity {
    private EditText portE;
    private Button btn;
    private TextView status;
    private DotView statusDot;
    private SoundPool sounds;
    private int clickSound;

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
        clickSound = sounds.load(this, com.jay.gyrosender.R.raw.mc_click, 1);

        SharedPreferences sp = getSharedPreferences("gyro", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(34), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(35, 31, 24));

        TextView eyebrow = text("CRAFTED FOR CARDBOARD VR", 11, Color.rgb(166, 148, 112), true);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow, matchWrap());

        TextView title = text("GYROSCOPE SENDER", 27, Color.rgb(245, 229, 190), true);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(dp(2), 0, dp(2), Color.rgb(0, 0, 0));
        LinearLayout.LayoutParams titleLp = matchWrap();
        titleLp.topMargin = dp(8);
        root.addView(title, titleLp);

        TextView subtitle = text("Minecraft VR  //  VR Head Tracking", 12,
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

        TextView footer = text("KEEP THIS APP RUNNING WHILE PLAYING", 10,
                Color.rgb(132, 119, 94), true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = matchWrap();
        footerLp.topMargin = dp(25);
        root.addView(footer, footerLp);

        setContentView(root);

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

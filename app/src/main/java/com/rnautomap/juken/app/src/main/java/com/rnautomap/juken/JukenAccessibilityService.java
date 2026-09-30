package com.rnautomap.juken;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JukenAccessibilityService extends AccessibilityService {

    public static volatile boolean running = false;

    private long lastSend = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        running = true;
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null || event.getPackageName() == null) {
            return;
        }

        /*
         * Hanya membaca tampilan aplikasi JUKEN asli.
         */
        if (!"com.bintangracingteam.juken"
                .contentEquals(event.getPackageName())) {
            return;
        }

        long now = System.currentTimeMillis();

        /*
         * Batasi pembacaan supaya tidak terlalu berat.
         */
        if (now - lastSend < 120) {
            return;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return;
        }

        ArrayList<String> texts =
                new ArrayList<>();

        collectText(root, texts, 0);

        root.recycle();

        if (texts.isEmpty()) {
            return;
        }

        String all = join(texts);

        Double rpm =
                findValue(
                        all,
                        "RPM",
                        0,
                        20000
                );

        Double tps =
                findValue(
                        all,
                        "TPS",
                        0,
                        100
                );

        Double afr =
                findValue(
                        all,
                        "AFR",
                        5,
                        30
                );

        Intent intent =
                new Intent(
                        "com.rnautomap.juken.JUKEN_LIVE"
                );

        intent.setPackage(getPackageName());

        if (rpm != null) {
            intent.putExtra("rpm", rpm);
        }

        if (tps != null) {
            intent.putExtra("tps", tps);
        }

        if (afr != null) {
            intent.putExtra("afr", afr);
        }

        /*
         * Data mentah untuk pemeriksaan
         * kalau format teks JUKEN berbeda.
         */
        if (all.length() > 1000) {
            all = all.substring(0, 1000);
        }

        intent.putExtra("raw", all);

        sendBroadcast(intent);

        lastSend = now;
    }

    private void collectText(
            AccessibilityNodeInfo node,
            List<String> output,
            int depth) {

        if (node == null || depth > 25) {
            return;
        }

        CharSequence text =
                node.getText();

        CharSequence description =
                node.getContentDescription();

        if (text != null &&
                !text.toString().trim().isEmpty()) {

            output.add(
                    text.toString().trim()
            );
        }

        if (description != null &&
                !description.toString().trim().isEmpty()) {

            String d =
                    description.toString().trim();

            if (text == null ||
                    !d.equals(text.toString())) {

                output.add(d);
            }
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                collectText(
                        child,
                        output,
                        depth + 1
                );

                child.recycle();
            }
        }
    }

    private String join(
            List<String> values) {

        StringBuilder result =
                new StringBuilder();

        for (String value : values) {

            if (result.length() > 0) {
                result.append(" | ");
            }

            result.append(value);
        }

        return result.toString();
    }

    private Double findValue(
            String text,
            String key,
            double min,
            double max) {

        Pattern[] patterns = {

                Pattern.compile(
                        "(?i)" +
                        key +
                        "\\s*[:=]?\\s*" +
                        "(-?\\d+(?:[.,]\\d+)?)"
                ),

                Pattern.compile(
                        "(?i)" +
                        "(-?\\d+(?:[.,]\\d+)?)" +
                        "\\s*" +
                        key
                )
        };

        for (Pattern pattern : patterns) {

            Matcher matcher =
                    pattern.matcher(text);

            if (!matcher.find()) {
                continue;
            }

            try {

                double value =
                        Double.parseDouble(
                                matcher.group(1)
                                        .replace(',', '.')
                        );

                if (value >= min &&
                        value <= max) {

                    return value;
                }

            } catch (Exception ignored) {
            }
        }

        return null;
    }
}

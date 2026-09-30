package com.rnautomap.juken;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.webkit.*;
import android.widget.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status;
    private WebView web;
    private BroadcastReceiver liveReceiver;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(Color.rgb(13,13,13));
        getWindow().setNavigationBarColor(Color.rgb(13,13,13));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(13,13,13));
        root.setFitsSystemWindows(true);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        bar.setMinimumHeight(dp(64));

        status = new TextView(this);
        status.setText("JUKEN Companion: pembaca belum aktif");
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams sp =
                new LinearLayout.LayoutParams(0, dp(48), 1);
        sp.setMargins(0,0,dp(6),0);
        bar.addView(status, sp);

        Button reader = new Button(this);
        reader.setText("AKTIFKAN PEMBACA");
        reader.setTextSize(11);
        reader.setSingleLine(true);
        reader.setOnClickListener(v -> openAccessibility());

        bar.addView(reader,
                new LinearLayout.LayoutParams(dp(128),dp(48)));

        Button juken = new Button(this);
        juken.setText("BUKA JUKEN");
        juken.setTextSize(11);
        juken.setSingleLine(true);
        juken.setOnClickListener(v -> openJuken());

        bar.addView(juken,
                new LinearLayout.LayoutParams(dp(90),dp(48)));

        root.addView(bar);

        web = new WebView(this);

        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);

        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                openExternal(request.getUrl().toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                openExternal(url);
                return true;
            }
        });

        web.addJavascriptInterface(new JsBridge(), "JukenBT");

        web.loadUrl(
                "file:///android_asset/kalkulator-peta-ecu.html"
        );

        root.addView(web,
                new LinearLayout.LayoutParams(
                        -1, 0, 1));

        setContentView(root);

        liveReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(
                    Context context,
                    Intent intent) {

                if (!"com.rnautomap.juken.JUKEN_LIVE"
                        .equals(intent.getAction())) {
                    return;
                }

                boolean hasRpm =
                        intent.hasExtra("rpm");

                boolean hasTps =
                        intent.hasExtra("tps");

                boolean hasAfr =
                        intent.hasExtra("afr");

                double rpm =
                        intent.getDoubleExtra("rpm",0);

                double tps =
                        intent.getDoubleExtra("tps",0);

                double afr =
                        intent.getDoubleExtra("afr",0);

                String raw =
                        intent.getStringExtra("raw");

                if (hasRpm || hasTps || hasAfr) {

                    String json =
                            String.format(
                                    Locale.US,
                                    "{\"rpm\":%.0f,\"tps\":%.2f,\"afr\":%.3f,\"raw\":\"%s\"}",
                                    rpm,
                                    tps,
                                    afr,
                                    escape(raw == null ? "" : raw)
                            );

                    web.evaluateJavascript(
                            "window.onJukenLive&&window.onJukenLive("
                                    + json + ")",
                            null
                    );

                    setStatus(
                            "Data JUKEN terbaca · RPM/TPS/AFR aktif"
                    );

                } else {

                    setStatus(
                            "JUKEN terdeteksi · mencari RPM/TPS/AFR"
                    );
                }
            }
        };
    }

    private void openAccessibility() {

        try {

            startActivity(
                    new Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                    )
            );

            setStatus(
                    "Aktifkan RNAUTOMAP — Juken Companion"
            );

        } catch(Exception e) {

            setStatus(
                    "Gagal membuka Aksesibilitas"
            );
        }
    }

    private void openJuken() {

        try {

            Intent i =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    "com.bintangracingteam.juken"
                            );

            if(i != null) {

                startActivity(i);

                setStatus(
                        "JUKEN asli dibuka · pembaca aktif"
                );

            } else {

                setStatus(
                        "Aplikasi JUKEN asli tidak ditemukan"
                );
            }

        } catch(Exception e) {

            setStatus(
                    "Gagal membuka JUKEN"
            );
        }
    }

    private void openExternal(String url) {

        try {

            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    )
            );

        } catch(Exception ignored) {}
    }

    private String escape(String s) {

        return s
                .replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\r","")
                .replace("\n"," ");
    }

    private int dp(int value) {

        return Math.round(
                value *
                getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void setStatus(String text) {

        runOnUiThread(() ->
                status.setText("JUKEN: " + text)
        );
    }

    public class JsBridge {

        @JavascriptInterface
        public boolean isConnected() {

            return JukenAccessibilityService.running;
        }

        @JavascriptInterface
        public void send(String command) {

            // Companion mode.
            // JUKEN asli tetap menjadi
            // satu-satunya aplikasi yang
            // berkomunikasi dengan ECU.
        }

        @JavascriptInterface
        public void openUrl(String url) {

            if(url == null) return;

            if(url.startsWith("http://") ||
               url.startsWith("https://")) {

                openExternal(url);
            }
        }

        @JavascriptInterface
        public String consult(
                String symptom,
                String rpm,
                String tps,
                String plug) {

            return localDiagnosis(
                    symptom,
                    rpm,
                    tps,
                    plug
            );
        }
    }

    private String localDiagnosis(
            String symptom,
            String rpm,
            String tps,
            String plug) {

        String x =
                (symptom + " " + plug)
                        .toLowerCase(Locale.US);

        boolean lean =
                x.contains("kering") ||
                x.contains("lean") ||
                x.contains("ngempos") ||
                x.contains("kurang bensin");

        boolean rich =
                x.contains("basah") ||
                x.contains("rich") ||
                x.contains("hitam") ||
                x.contains("boros") ||
                x.contains("bau bensin");

        String diagnosis;
        String explanation;
        int fuel = 0;

        if(lean) {

            fuel = 4;

            diagnosis =
                    "Gejala mengarah ke campuran cenderung miskin.";

            explanation =
                    "Uji koreksi fuel kecil sekitar +4%. " +
                    "Validasi menggunakan AFR sebelum koreksi berikutnya.";

        } else if(rich) {

            fuel = -4;

            diagnosis =
                    "Gejala mengarah ke campuran cenderung kaya.";

            explanation =
                    "Uji pengurangan fuel kecil sekitar -4%. " +
                    "Validasi menggunakan AFR.";

        } else {

            diagnosis =
                    "Data belum cukup untuk menentukan arah koreksi.";

            explanation =
                    "Masukkan AFR aktual, RPM, TPS dan kondisi busi.";
        }

        return String.format(
                Locale.US,
                "{\"diagnosis\":\"%s\",\"explanation\":\"%s\",\"fuelAdjustPercent\":%d}",
                escape(diagnosis),
                escape(explanation),
                fuel
        );
    }

    @Override
    protected void onResume() {

        super.onResume();

        try {

            IntentFilter filter =
                    new IntentFilter(
                            "com.rnautomap.juken.JUKEN_LIVE"
                    );

            if(Build.VERSION.SDK_INT >= 33) {

                registerReceiver(
                        liveReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );

            } else {

                registerReceiver(
                        liveReceiver,
                        filter
                );
            }

        } catch(Exception ignored) {}

        if(JukenAccessibilityService.running) {

            setStatus(
                    "Pembaca aktif · buka JUKEN Live Data"
            );
        }
    }

    @Override
    protected void onPause() {

        try {

            unregisterReceiver(
                    liveReceiver
            );

        } catch(Exception ignored) {}

        super.onPause();
    }
}

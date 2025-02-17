package com.levelpixel.dunebrowser;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.LinearLayout;

import com.levelpixel.DuneWebView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // UI components
    private EditText urlInput;
    private ImageButton backButton, forwardButton, refreshButton, settingsButton, goButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;

    // WebView component from Dune library
    private DuneWebView duneWebView;

    // Default URL to load
    private static final String DEFAULT_URL = "https://www.google.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize all views and listeners
        initializeViews();
        setupSwipeRefresh();
        setupListeners();
    }

    /**
     * Initializes all UI components and configures DuneWebView settings.
     */
    private void initializeViews() {
        urlInput = findViewById(R.id.urlInput);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        settingsButton = findViewById(R.id.settingsButton);
        goButton = findViewById(R.id.goButton);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        duneWebView = findViewById(R.id.duneWebView);

        // Configure DuneWebView with security and privacy settings
        duneWebView.setAdBlockEnabled(true);  //IF YOU WANT TO BLOCK ADS AND TRACKERS
        duneWebView.setPopupBlockEnabled(true); //IF YOU WANT TO BLOCK ANNOYING POPUP
        duneWebView.setRedirectBlockEnabled(true); // IF YOU WANT TO BLOCK ANNOYING RANDOM REDIRECT
        duneWebView.setUseSystemDownloader(true); //IF YOU WANT TO USE SYSTEM DEFAULT DOWNLOAD MANAGER , YOU CAN USE YOUR OWN DOWNLOAD MANAGER TOO

        // Load default ad blocklist
        duneWebView.loadAdBlockListFromResource(true, null);

        // Load custom blocked domains
        duneWebView.addCustomBlockedDomain("ads.example.com");
        duneWebView.addCustomBlockedDomain("trackers.example.com");

        // Example for removing and clearing blocklist entries
        duneWebView.removeBlockedDomain("ads.example.com");
        duneWebView.clearBlocklist();

        // Load the default URL in DuneWebView
        duneWebView.loadUrl(DEFAULT_URL);

        // Set up progress view listener to track page loading
        duneWebView.setProgressListener(progress -> {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress);
            if (progress == 100) {
                progressBar.setVisibility(View.INVISIBLE);
            }
            urlInput.setText(duneWebView.getUrl());
        });

        // Example to check if a specific domain is blocked
        boolean isFacebookBlocked = duneWebView.isBlockedDomain("facebook.com");
        int blockedHostsSize = duneWebView.getBlocklistSize();
    }

    /**
     * Configures SwipeRefreshLayout to refresh the page on swipe gesture.
     */
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            duneWebView.reload();
            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 500);
        });

        // Set color scheme for refresh indicator based on app theme attributes
        int colorPrimary = getColorFromAttr(this, R.attr.backgroundColor);
        int backgroundColor = getColorFromAttr(this, R.attr.backgroundColor);
        int colorSecondary = getColorFromAttr(this, R.attr.colorTextSecondary);

        swipeRefreshLayout.setColorSchemeColors(colorPrimary, colorSecondary, backgroundColor);
    }

    /**
     * Utility method to fetch color attribute from the app's theme.
     *
     * @param context The application context.
     * @param attr The attribute to fetch.
     * @return The color value of the specified attribute.
     */
    private static int getColorFromAttr(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{attr});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    /**
     * Sets up button listeners and handles user interactions.
     */
    private void setupListeners() {
        // Show 'Go' button only if there's input text
        urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                goButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        goButton.setOnClickListener(v -> {
            loadUrl();
            hideKeyboard();
        });

        backButton.setOnClickListener(v -> {
            if (duneWebView.canGoBack()) {
                duneWebView.goBack();
            } else {
                duneWebView.loadUrl(DEFAULT_URL);
            }
        });

        forwardButton.setOnClickListener(v -> {
            if (duneWebView.canGoForward()) {
                duneWebView.goForward();
            }
        });

        refreshButton.setOnClickListener(v -> duneWebView.reload());

        settingsButton.setOnClickListener(v -> {
            // Settings button action can be defined here
        });

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            loadUrl();
            hideKeyboard();
            return true;
        });
    }

    /**
     * Hides the keyboard and clears focus from the URL input field.
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            clearUrlInputFocus();
        }
    }

    /**
     * Clears focus from the URL input field and focuses on the WebView.
     */
    private void clearUrlInputFocus() {
        urlInput.clearFocus();
        duneWebView.requestFocus();
    }

    // URL processing configurations
    private static final String[] URL_PROTOCOLS = {"http://", "https://", "ftp://", "file://"};
    private static final String[] COMMON_DOMAINS = {".com", ".org", ".net", ".edu", ".gov", ".io", ".co", ".me"};
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(([\\w-]+\\.)+[\\w-]+|localhost)(:[0-9]+)?(/.*)?$"
    );

    /**
     * Loads a URL or search query from the URL input field.
     */
    private void loadUrl() {
        String input = urlInput.getText().toString().trim();

        if (input.isEmpty()) {
            showError("Please enter a URL or search term");
            return;
        }

        String processedUrl = processInput(input);
        duneWebView.loadUrl(processedUrl);
    }

    /**
     * Processes the input text and determines whether it's a URL or search term.
     *
     * @param input The user input from the URL field.
     * @return A URL with protocol or a search URL.
     */
    private String processInput(String input) {
        String cleanInput = input.trim().toLowerCase();

        if (hasValidProtocol(cleanInput)) {
            return input;
        }

        if (looksLikeUrl(cleanInput)) {
            return "https://" + input;
        }

        return buildSearchUrl(input);
    }

    /**
     * Checks if the input has a valid URL protocol.
     */
    private boolean hasValidProtocol(String url) {
        for (String protocol : URL_PROTOCOLS) {
            if (url.startsWith(protocol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the input resembles a URL by checking domain patterns.
     */
    private boolean looksLikeUrl(String input) {
        String domainPart = input.split("/")[0].split("\\?")[0];

        if (IP_ADDRESS_PATTERN.matcher(domainPart).matches()) {
            return true;
        }

        for (String domain : COMMON_DOMAINS) {
            if (input.contains(domain)) {
                return true;
            }
        }

        return URL_PATTERN.matcher(domainPart).matches() || (input.contains(".") && !input.contains(" ") && !input.contains("@") && input.length() >= 3);
    }

    /**
     * Constructs a search URL for a given search query.
     */
    private String buildSearchUrl(String query) {
        try {
            return "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("MainActivity", "Error encoding search query", e);
            return "https://www.google.com/search?q=" + query.replace(" ", "+");
        }
    }

    /**
     * Displays a toast message for errors.
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

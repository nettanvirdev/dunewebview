package com.levelpixel.dunebrowser;


import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextWatcher;
import android.text.Editable;


import com.levelpixel.dunebrowser.databinding.ActivityMainBinding;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    

    // Default URL to load
    private static final String DEFAULT_URL = "https://www.google.com";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        // Enable edge-to-edge display
        EdgeToEdge.enable(this);
        
        // Inflate the binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


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
     * Initializes all UI components and configures  binding.duneWebView settings.
     */
    private void initializeViews() {
        
        // Configure  binding.duneWebView with security and privacy settings
        binding.duneWebView.setAdBlockEnabled(true);  //IF YOU WANT TO BLOCK ADS AND TRACKERS
         binding.duneWebView.setPopupBlockEnabled(true); //IF YOU WANT TO BLOCK ANNOYING POPUP
         binding.duneWebView.setRedirectBlockEnabled(true); // IF YOU WANT TO BLOCK ANNOYING RANDOM REDIRECT
         binding.duneWebView.setUseSystemDownloader(true); //IF YOU WANT TO USE SYSTEM DEFAULT DOWNLOAD MANAGER , YOU CAN USE YOUR OWN DOWNLOAD MANAGER TOO

        // Load default ad blocklist
         binding.duneWebView.loadAdBlockListFromResource(true, null);

        // Load custom blocked domains
         binding.duneWebView.addCustomBlockedDomain("ads.example.com");
         binding.duneWebView.addCustomBlockedDomain("trackers.example.com");

        // Example for removing and clearing blocklist entries
         binding.duneWebView.removeBlockedDomain("ads.example.com");
         binding.duneWebView.clearBlocklist();

        // Load the default URL in  binding.duneWebView
         binding.duneWebView.loadUrl(DEFAULT_URL);

        // Set up progress view listener to track page loading
         binding.duneWebView.setProgressListener(progress -> {
             binding.progressBar.setVisibility(View.VISIBLE);
             binding.progressBar.setProgress(progress);
            if (progress == 100) {
                binding.progressBar.setVisibility(View.INVISIBLE);
            }
             binding.urlInput.setText( binding.duneWebView.getUrl());
        });

        // Example to check if a specific domain is blocked
        boolean isFacebookBlocked =  binding.duneWebView.isBlockedDomain("facebook.com");
        int blockedHostsSize =  binding.duneWebView.getBlocklistSize();
    }

    /**
     * Configures SwipeRefreshLayout to refresh the page on swipe gesture.
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
             binding.duneWebView.reload();
            new Handler().postDelayed(() ->  binding.swipeRefreshLayout.setRefreshing(false), 500);
        });

        // Set color scheme for refresh indicator based on app theme attributes
        int colorPrimary = getColorFromAttr(this, R.attr.backgroundColor);
        int backgroundColor = getColorFromAttr(this, R.attr.backgroundColor);
        int colorSecondary = getColorFromAttr(this, R.attr.colorTextSecondary);

        binding.swipeRefreshLayout.setColorSchemeColors(colorPrimary, colorSecondary, backgroundColor);
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
        binding.urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.goButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.goButton.setOnClickListener(v -> {
            loadUrl();
            hideKeyboard();
        });

        binding.backButton.setOnClickListener(v -> {
            if ( binding.duneWebView.canGoBack()) {
                 binding.duneWebView.goBack();
            } else {
                 binding.duneWebView.loadUrl(DEFAULT_URL);
            }
        });

        binding.forwardButton.setOnClickListener(v -> {
            if ( binding.duneWebView.canGoForward()) {
                 binding.duneWebView.goForward();
            }
        });

        binding.refreshButton.setOnClickListener(v ->  binding.duneWebView.reload());

        binding.settingsButton.setOnClickListener(v -> {
            // Settings button action can be defined here
        });

        binding. urlInput.setOnEditorActionListener((v, actionId, event) -> {
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
        binding.urlInput.clearFocus();
         binding.duneWebView.requestFocus();
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
        String input =  binding.urlInput.getText().toString().trim();

        if (input.isEmpty()) {
            showError("Please enter a URL or search term");
            return;
        }

        String processedUrl = processInput(input);
         binding.duneWebView.loadUrl(processedUrl);
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

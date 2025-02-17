package com.levelpixel;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.levelpixel.duneweb.R;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * EnhancedWebView extends Android's WebView with additional security and user experience features:
 * - Ad blocking
 * - Popup blocking
 * - Redirect prevention
 * - Custom download handling
 * - Progress tracking
 * - Overlay blocking
 */
public class DuneWebView extends WebView {
    // Feature flags for enabling/disabling functionality
    private boolean adBlockEnabled = true;
    private boolean popupBlockEnabled = true;
    private boolean redirectBlockEnabled = true;
    private boolean useSystemDownloader = true;

    // Set to store domains that should be blocked
    private Set<String> adBlockList;

    // Interfaces for callback functionality
    private OnProgressChangedListener progressListener;
    private DownloadListener customDownloadListener;

    /**
     * JavaScript code to detect and remove unwanted overlay elements.
     * - Checks for fixed/absolute positioned elements
     * - Looks for common ad-related keywords
     * - Identifies corner advertisements based on position and size
     * - Runs every second to catch dynamically added elements
     */
    private static final String BLOCK_OVERLAY_JS =
            "function blockUnwantedOverlays() {" +
                    "  const elementsToCheck = document.querySelectorAll('div, iframe, span');" +
                    "  elementsToCheck.forEach(el => {" +
                    "    const style = window.getComputedStyle(el);" +
                    "    const rect = el.getBoundingClientRect();" +
                    "    if (style.position === 'fixed' || style.position === 'absolute') {" +
                    "      const hasAdKeywords = el.innerHTML.toLowerCase().match(/(adsby|sponsored|advertisement|click here|you won|congratulation|lucky winner)/i);" +
                    "      const isCornerAd = (rect.width < 400 && rect.height < 400) && " +
                    "                         ((rect.top < 10 && rect.left < 10) || " +
                    "                          (rect.top < 10 && rect.right > window.innerWidth - 10) || " +
                    "                          (rect.bottom > window.innerHeight - 10 && rect.left < 10) || " +
                    "                          (rect.bottom > window.innerHeight - 10 && rect.right > window.innerWidth - 10));" +
                    "      if (hasAdKeywords || isCornerAd) {" +
                    "        el.remove();" +
                    "      }" +
                    "    }" +
                    "  });" +
                    "}" +
                    "setInterval(blockUnwantedOverlays, 1000);";

    /**
     * JavaScript code to prevent unwanted redirects.
     * - Tracks the original link clicked by the user
     * - Monitors for suspicious redirects (e.g., through tracking URLs)
     * - Restores the original destination if a suspicious redirect is detected
     */
    private static final String REDIRECT_HANDLER_JS =
            "let lastClickTime = 0;" +
                    "let originalHref = '';" +
                    "document.addEventListener('click', function(e) {" +
                    "  const target = e.target;" +
                    "  const closestLink = target.closest('a');" +
                    "  if (closestLink) {" +
                    "    originalHref = closestLink.href;" +
                    "    lastClickTime = Date.now();" +
                    "  }" +
                    "}, true);" +
                    "document.addEventListener('beforeunload', function(e) {" +
                    "  if (Date.now() - lastClickTime > 100) return;" +
                    "  const currentUrl = window.location.href;" +
                    "  if (originalHref && currentUrl !== originalHref) {" +
                    "    const suspiciousRedirect = currentUrl.includes('click.php') || " +
                    "                               currentUrl.includes('redirect') || " +
                    "                               currentUrl.includes('track.php') || " +
                    "                               currentUrl.includes('/ad/') || " +
                    "                               currentUrl.includes('/ads/');" +
                    "    if (suspiciousRedirect) {" +
                    "      e.preventDefault();" +
                    "      window.location.href = originalHref;" +
                    "    }" +
                    "  }" +
                    "});";

    /**
     * Interface for tracking page load progress
     */
    public interface OnProgressChangedListener {
        void onProgressChanged(int progress);
    }

    /**
     * Constructor for programmatic instantiation
     */
    public DuneWebView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor for XML layout instantiation
     */
    public DuneWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes the WebView with enhanced features
     */
    private void init() {
        setupWebView();
        setupWebViewClient();
        setupDownloadListener();
        adBlockList = new HashSet<>();
    }

    /**
     * Configures WebView settings for optimal browsing experience:
     * - Enables JavaScript and zoom controls
     * - Configures caching and storage
     * - Sets security preferences
     * - Optimizes layout and scrolling behavior
     */
    private void setupWebView() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(true);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        setScrollbarFadingEnabled(true);

        // Additional security settings
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSaveFormData(false);
    }

    /**
     * Sets up the WebViewClient to handle:
     * - Page loading completion
     * - Resource interception for ad blocking
     * - URL loading override for popup/redirect blocking
     * Also configures WebChromeClient for progress tracking
     */
    private void setupWebViewClient() {
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject protection scripts after page loads
                if (adBlockEnabled) {
                    view.evaluateJavascript(BLOCK_OVERLAY_JS, null);
                }
                if (redirectBlockEnabled) {
                    view.evaluateJavascript(REDIRECT_HANDLER_JS, null);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (adBlockEnabled) {
                    String url = request.getUrl().toString().toLowerCase();
                    String host = request.getUrl().getHost();

                    // Check against blocklist
                    if (host != null && adBlockList.contains(host)) {
                        return createEmptyResponse();
                    }

                    // Check for common ad patterns
                    if (isAdRequest(url)) {
                        return createEmptyResponse();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (popupBlockEnabled || redirectBlockEnabled) {
                    String url = request.getUrl().toString().toLowerCase();
                    return isSuspiciousUrl(url);
                }
                return false;
            }
        });

        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressListener != null) {
                    progressListener.onProgressChanged(newProgress);
                }
            }
        });
    }

    /**
     * Creates an empty response for blocked requests
     */
    private WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream("".getBytes()));
    }

    /**
     * Checks if a URL matches common ad patterns
     */
    private boolean isAdRequest(String url) {
        return url.contains("/ad/") ||
                url.contains("/ads/") ||
                url.contains("pop-under") ||
                url.contains("popunder") ||
                url.contains("click.php") ||
                url.contains("track.php") ||
                url.contains("banner.") ||
                url.contains("analytics.") ||
                url.contains("tracker.");
    }

    /**
     * Checks if a URL is potentially suspicious (popup/redirect)
     */
    private boolean isSuspiciousUrl(String url) {
        return url.contains("popup") ||
                url.contains("click.php") ||
                url.contains("redirect") ||
                url.contains("ad.") ||
                url.contains("/pop/") ||
                url.contains("track.php");
    }

    /**
     * Configures download handling:
     * - Uses system download manager by default
     * - Supports custom download handling through listener
     * - Manages cookies and user agent for downloads
     * - Shows download progress notification
     */
    private void setupDownloadListener() {
        setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (customDownloadListener != null) {
                customDownloadListener.onDownloadStart(url, userAgent, contentDisposition,
                        mimeType, contentLength);
                return;
            }

            if (useSystemDownloader) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                // Set request headers
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);

                // Configure download
                request.setDescription("Downloading file...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType));

                // Start download
                DownloadManager dm = (DownloadManager) getContext()
                        .getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getContext(), "Downloading File", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Public API methods

    /**
     * Enable/disable ad blocking
     */
    public void setAdBlockEnabled(boolean enabled) {
        this.adBlockEnabled = enabled;
    }

    /**
     * Enable/disable popup blocking
     */
    public void setPopupBlockEnabled(boolean enabled) {
        this.popupBlockEnabled = enabled;
    }

    /**
     * Enable/disable redirect blocking
     */
    public void setRedirectBlockEnabled(boolean enabled) {
        this.redirectBlockEnabled = enabled;
    }

    /**
     * Enable/disable system download manager
     */
    public void setUseSystemDownloader(boolean enabled) {
        this.useSystemDownloader = enabled;
    }

    /**
     * Set custom download listener
     */
    public void setCustomDownloadListener(DownloadListener listener) {
        this.customDownloadListener = listener;
    }

    /**
     * Set progress change listener
     */
    public void setProgressListener(OnProgressChangedListener listener) {
        this.progressListener = listener;
    }

    /**
     * Load ad block rules from a raw resource file
     * Format: One domain per line
     */
    public void loadAdBlockListFromResource(boolean useDefaultHosts, @Nullable Integer resourceId) {
        try {
            InputStream fis;
            if (useDefaultHosts || resourceId == null) {
                fis = getContext().getResources().openRawResource(R.raw.adblockserverlist);
            } else {
                fis = getContext().getResources().openRawResource(resourceId);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    adBlockList.add(line.trim().toLowerCase());
                }
            }
            br.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Add a domain to the block list
     */
    public void addCustomBlockedDomain(String domain) {
        adBlockList.add(domain.toLowerCase());
    }

    /**
     * Remove a domain from the block list
     */
    public void removeBlockedDomain(String domain) {
        adBlockList.remove(domain.toLowerCase());
    }

    /**
     * Clear all domains from the block list
     */
    public void clearBlocklist() {
        adBlockList.clear();
    }

    /**
     * Get the current size of the block list
     */
    public int getBlocklistSize() {
        return adBlockList.size();
    }

    /**
     * Check if a domain is currently blocked
     */
    public boolean isBlockedDomain(String domain) {
        return adBlockList.contains(domain.toLowerCase());
    }
}
# DuneWebView

🚀 A modern, secure, and feature-rich WebView component for Android that enhances the browsing experience with built-in protection and advanced customization options.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)

## ✨ Key Features

- 🛡️ **Security First**
  - Ad blocking with customizable rules
  - Popup blocking and redirect protection
  - Overlay detection and removal
  - Safe browsing environment

- 📱 **Enhanced User Experience**
  - Smart download management
  - Progress tracking
  - Gesture support
  - Optimized performance

- ⚙️ **Developer Friendly**
  - Highly customizable
  - Easy integration
  - Extensive configuration options
  - Both Java and Kotlin support

## 📦 Installation

### Manual

Copy the `DuneWebView.java` file to your project's package directory.
or Copy the duneweb-release.aar/duneweb-debug.aar in your project's `lib` folder

## 🎯 Basic Usage

### XML Layout

```xml
<com.levelpixel.DuneWebView
    android:id="@+id/webView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Kotlin Implementation

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var webView: DuneWebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        
        // Enable protection features
        webView.apply {
            setAdBlockEnabled(true)
            setPopupBlockEnabled(true)
            setRedirectBlockEnabled(true)
            loadUrl("https://example.com")
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
```

### Java Implementation

```java
public class MainActivity extends AppCompatActivity {
    private DuneWebView webView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        webView = findViewById(R.id.webView);
        
        // Enable protection features
        webView.setAdBlockEnabled(true);
        webView.setPopupBlockEnabled(true);
        webView.setRedirectBlockEnabled(true);
        webView.loadUrl("https://example.com");
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
```

## 🛠️ Advanced Configuration

### Ad Blocking (Kotlin)

```kotlin
webView.apply {
    // Load blocklist from resource
    loadAdBlockListFromResource(R.raw.adblockserverlist)
    
    // Add custom domains
    addCustomBlockedDomain("ads.example.com")
    addCustomBlockedDomain("analytics.example.com")
    
    // Remove specific domain
    removeBlockedDomain("allowedads.example.com")
    
    // Check blocking status
    val isBlocked = isBlockedDomain("ads.example.com")
    
    // Clear entire blocklist
    clearBlocklist()
}
```

### Download Management (Java)

```java
// Custom download handling
webView.setCustomDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
    request.setTitle("Download")
           .setDescription("Downloading file...")
           .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    
    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    dm.enqueue(request);
});

// Or use system downloader
webView.setUseSystemDownloader(true);
```

### Progress Tracking (Kotlin)

```kotlin
webView.setProgressListener { progress ->
    progressBar.apply {
        setProgress(progress)
        visibility = if (progress == 100) View.GONE else View.VISIBLE
    }
}
```

### Complete Example (Kotlin)

```kotlin
class BrowserActivity : AppCompatActivity() {
    private lateinit var webView: DuneWebView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        
        webView.apply {
            // Security features
            setAdBlockEnabled(true)
            setPopupBlockEnabled(true)
            setRedirectBlockEnabled(true)
            
            // Load blocklist
            loadAdBlockListFromResource(R.raw.adblockserverlist)
            
            // Custom domains
            addCustomBlockedDomain("ads.example.com")
            
            // Progress tracking
            setProgressListener { progress ->
                progressBar.apply {
                    setProgress(progress)
                    visibility = if (progress == 100) View.GONE else View.VISIBLE
                }
            }
            
            // Download handling
            setUseSystemDownloader(true)
            
            // Load initial URL
            loadUrl("https://example.com")
        }
    }
}
```

## 📱 Required Permissions

Add these to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28"/>
<uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
```

## 🔧 Customization Options

| Feature | Method | Description |
|---------|---------|-------------|
| Ad Blocking | `setAdBlockEnabled(Boolean)` | Enable/disable ad blocking |
| Popup Blocking | `setPopupBlockEnabled(Boolean)` | Enable/disable popup blocking |
| Redirect Protection | `setRedirectBlockEnabled(Boolean)` | Enable/disable redirect protection |
| Download Handler | `setUseSystemDownloader(Boolean)` | Toggle system download manager |
| Progress Tracking | `setProgressListener(listener)` | Set progress callback |
| Custom Downloads | `setCustomDownloadListener(listener)` | Custom download handling |

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

Created by LevelPixel

## 📞 Support

For support, please open an issue in the repository or contact us at support@levelpixel.com

---

⭐ Don't forget to star the repo if you find it useful!

## 📸 Example Screenshots

Here are some example screenshots of DuneWebView in action:

### Main Activity

![Main Activity](images/image1.png)

### Browser Activity

![Browser Activity](images/image2.png)
-keep @interface android.webkit.JavascriptInterface

-keepclassmembers class * {
	@android.webkit.JavascriptInterface <methods>;
}
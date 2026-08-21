package com.example.socketapp.ui.tradingview

import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TradingViewWebViewLifecycleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun technicalAnalysisIsReadyOnlyAfterVisibleContentIsRendered() {
        val pageLoaded = AtomicBoolean(false)
        val webViewReference = AtomicReference<WebView>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            READINESS_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { pageLoaded.get() }
        val webView = webViewReference.get()

        assertFalse(evaluateBoolean(webView, TECHNICAL_ANALYSIS_READY_CHECK))
        evaluate(webView, "window.populateTechnicalAnalysis()")
        assertTrue(evaluateBoolean(webView, TECHNICAL_ANALYSIS_READY_CHECK))
    }

    @Test
    fun companyProfileIsReadyWhenContentIsDistributedAcrossShadowRoots() {
        val pageLoaded = AtomicBoolean(false)
        val webViewReference = AtomicReference<WebView>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            COMPANY_PROFILE_READINESS_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { pageLoaded.get() }
        val webView = webViewReference.get()

        assertFalse(evaluateBoolean(webView, COMPANY_PROFILE_READY_CHECK))
        evaluate(webView, "window.populateCompanyProfile()")
        assertTrue(evaluateBoolean(webView, COMPANY_PROFILE_READY_CHECK))
    }

    @Test
    fun iframeWidgetIsReadyOnlyAfterFrameLoadEvent() {
        val pageLoaded = AtomicBoolean(false)
        val webViewReference = AtomicReference<WebView>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            IFRAME_READINESS_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { pageLoaded.get() }
        val webView = webViewReference.get()

        assertFalse(evaluateBoolean(webView, WIDGET_IFRAME_CHECK))
        evaluate(webView, "window.markTradingViewFrameLoaded()")
        assertTrue(evaluateBoolean(webView, WIDGET_IFRAME_CHECK))
    }

    @Test
    fun marketOverviewIsReadyOnlyAfterChartIsRendered() {
        val pageLoaded = AtomicBoolean(false)
        val webViewReference = AtomicReference<WebView>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            MARKET_OVERVIEW_READINESS_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { pageLoaded.get() }
        val webView = webViewReference.get()

        assertFalse(evaluateBoolean(webView, MARKET_OVERVIEW_READY_CHECK))
        evaluate(webView, "window.populateMarketOverview()")
        assertTrue(evaluateBoolean(webView, MARKET_OVERVIEW_READY_CHECK))
    }

    @Test
    fun scrollObserverReattachesAfterScrollableContainerIsReplaced() {
        val pageLoaded = AtomicBoolean(false)
        val webViewReference = AtomicReference<WebView>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            SCROLL_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { pageLoaded.get() }
        val webView = webViewReference.get()
        evaluate(webView, INSTALL_TEST_SCROLL_BRIDGE_SCRIPT)
        assertTrue(
            evaluateBoolean(
                webView,
                "typeof window.tradingViewScrollBounds.postMessage === 'function'",
            ),
        )
        assertTrue(
            evaluateBoolean(
                webView,
                "document.getElementById('scroller').scrollHeight > " +
                    "document.getElementById('scroller').clientHeight + 1",
            ),
        )
        evaluate(webView, WIDGET_INTERNAL_SCROLL_OBSERVER_INSTALL)
        waitForBoolean(webView, "window.__testReportedBounds.includes($BOUNDS_TOP)")

        evaluate(webView, REPLACE_AND_SCROLL_TO_BOTTOM_SCRIPT)

        waitForBoolean(webView, "window.__testReportedBounds.includes($BOUNDS_BOTTOM)")
    }

    @Test
    fun noScrollableContentHandsVerticalMovementToOuterScroll() {
        val webViewReference = AtomicReference<WebView>()
        val boundaryDelta = AtomicReference<Float?>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    createTradingViewWebView(
                        ctx = context,
                        monitoredUrl = "",
                        widgetReadyCheck = "false",
                        handlesInternalVerticalScroll = true,
                        onLoading = {},
                        onReady = {},
                        onFailure = {},
                        onInternalScrollBoundary = { boundaryDelta.set(it) },
                        timeoutHolder = TimeoutHolder(),
                        backgroundColor = Color.WHITE,
                    ).apply {
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            EMPTY_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        val webView = waitForWebView(webViewReference)
        waitForBoolean(webView, "document.readyState === 'complete'")
        dispatchGesture(webView, startX = 100f, startY = 300f, endX = 100f, endY = 150f)

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { boundaryDelta.get() != null }
        assertTrue(boundaryDelta.get()!! < 0f)
    }

    @Test
    fun horizontalGestureDoesNotMoveOuterVerticalScroll() {
        val webViewReference = AtomicReference<WebView>()
        val boundaryDelta = AtomicReference<Float?>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    createTradingViewWebView(
                        ctx = context,
                        monitoredUrl = "",
                        widgetReadyCheck = "false",
                        handlesInternalVerticalScroll = true,
                        onLoading = {},
                        onReady = {},
                        onFailure = {},
                        onInternalScrollBoundary = { boundaryDelta.set(it) },
                        timeoutHolder = TimeoutHolder(),
                        backgroundColor = Color.WHITE,
                    ).apply {
                        loadDataWithBaseURL(
                            TEST_BASE_URL,
                            EMPTY_TEST_HTML,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        val webView = waitForWebView(webViewReference)
        waitForBoolean(webView, "document.readyState === 'complete'")
        dispatchGesture(webView, startX = 100f, startY = 300f, endX = 300f, endY = 295f)
        Thread.sleep(200L)

        assertNull(boundaryDelta.get())
    }

    @Test
    fun dynamicallyCreatedIframesReceiveAndKeepTheApprovedSandbox() {
        val pageLoaded = AtomicBoolean(false)
        val webViewReference = AtomicReference<WebView>()

        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            BASE_URL,
                            buildTradingViewWidgetHtml(
                                templateHtml = SANDBOX_TEST_TEMPLATE,
                                scriptSrc = "",
                                configJson = "",
                            ),
                            "text/html",
                            "UTF-8",
                            null,
                        )
                        webViewReference.set(this)
                    }
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { pageLoaded.get() }
        val webView = webViewReference.get()
        val allFramesHaveSandbox = """
            document.getElementById('created-frame').getAttribute('sandbox') ===
              '$TRADING_VIEW_IFRAME_SANDBOX' &&
            document.getElementById('shadow-host').shadowRoot
              .getElementById('shadow-frame').getAttribute('sandbox') ===
              '$TRADING_VIEW_IFRAME_SANDBOX'
        """.trimIndent()

        waitForBoolean(webView, allFramesHaveSandbox)
        evaluate(
            webView,
            """
                document.getElementById('created-frame').removeAttribute('sandbox');
                document.getElementById('shadow-host').shadowRoot
                  .getElementById('shadow-frame').removeAttribute('sandbox');
            """.trimIndent(),
        )
        waitForBoolean(webView, allFramesHaveSandbox)
    }

    private fun waitForWebView(reference: AtomicReference<WebView>): WebView {
        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { reference.get() != null }
        return reference.get()
    }

    private fun dispatchGesture(
        webView: WebView,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
    ) {
        val downTime = SystemClock.uptimeMillis()
        composeRule.runOnUiThread {
            val downEvent = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                startX,
                startY,
                0,
            )
            webView.dispatchTouchEvent(downEvent)
            downEvent.recycle()
        }
        Thread.sleep(150L)
        composeRule.runOnUiThread {
            val moveEvent = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_MOVE,
                endX,
                endY,
                0,
            )
            webView.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
            val upEvent = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                endX,
                endY,
                0,
            )
            webView.dispatchTouchEvent(upEvent)
            upEvent.recycle()
        }
    }

    private fun evaluateBoolean(webView: WebView, script: String): Boolean {
        val result = AtomicReference<String>()
        composeRule.runOnUiThread {
            webView.evaluateJavascript(script, result::set)
        }
        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { result.get() != null }
        return result.get() == "true"
    }

    private fun evaluate(webView: WebView, script: String) {
        val completed = AtomicBoolean(false)
        composeRule.runOnUiThread {
            webView.evaluateJavascript(script) { completed.set(true) }
        }
        composeRule.waitUntil(timeoutMillis = TEST_TIMEOUT_MS) { completed.get() }
    }

    private fun waitForBoolean(webView: WebView, script: String) {
        val deadline = System.currentTimeMillis() + TEST_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (evaluateBoolean(webView, script)) return
            Thread.sleep(50L)
        }
        assertTrue("JavaScript condition was not satisfied: $script", false)
    }

    private companion object {
        const val TEST_ORIGIN = "https://www.tradingview-widget.com"
        const val TEST_BASE_URL = "$TEST_ORIGIN/"
        const val TEST_TIMEOUT_MS = 5_000L
        const val BOUNDS_TOP = 1
        const val BOUNDS_BOTTOM = 2
        const val EMPTY_TEST_HTML = "<!doctype html><html><body></body></html>"
        const val SANDBOX_TEST_TEMPLATE = """
            <!doctype html>
            <html>
            <head></head>
            <body>
              <div id="shadow-host"></div>
              <script>
                var createdFrame = document.createElement("iframe");
                createdFrame.id = "created-frame";
                document.body.appendChild(createdFrame);

                var shadowRoot = document.getElementById("shadow-host")
                  .attachShadow({ mode: "open" });
                shadowRoot.innerHTML = "<iframe id='shadow-frame'></iframe>";
              </script>
            </body>
            </html>
        """

        const val READINESS_TEST_HTML = """
            <!doctype html>
            <html>
            <body>
              <script>
                const originalAttachShadow = Element.prototype.attachShadow;
                window.__tradingViewShadowRoots = [];
                Element.prototype.attachShadow = function(options) {
                  const root = originalAttachShadow.call(this, options);
                  window.__tradingViewShadowRoots.push(root);
                  return root;
                };

                class TestTechnicalAnalysis extends HTMLElement {
                  constructor() {
                    super();
                    this.root = this.attachShadow({ mode: "closed" });
                    this.root.innerHTML = "<div id='content'></div>";
                  }
                }
                class TestTechnicalLegend extends HTMLElement {
                  constructor() {
                    super();
                    this.root = this.attachShadow({ mode: "closed" });
                    this.root.innerHTML = "<div id='legend'></div>";
                  }
                }
                customElements.define("tv-technical-analysis", TestTechnicalAnalysis);
                customElements.define("test-technical-legend", TestTechnicalLegend);

                window.populateTechnicalAnalysis = function() {
                  const content = window.__tradingViewShadowRoots[0].querySelector("#content");
                  content.innerHTML = "<span>Comprar</span><span>Neutral</span>";
                  const legend = window.__tradingViewShadowRoots[1].querySelector("#legend");
                  legend.innerHTML = "<span>Vender</span>";
                };
              </script>
              <tv-technical-analysis></tv-technical-analysis>
              <test-technical-legend></test-technical-legend>
            </body>
            </html>
        """

        const val COMPANY_PROFILE_READINESS_TEST_HTML = """
            <!doctype html>
            <html>
            <body>
              <script>
                const originalAttachShadow = Element.prototype.attachShadow;
                window.__tradingViewShadowRoots = [];
                Element.prototype.attachShadow = function(options) {
                  const root = originalAttachShadow.call(this, options);
                  window.__tradingViewShadowRoots.push(root);
                  return root;
                };

                class TestCompanyProfile extends HTMLElement {}
                class TestCompanySummary extends HTMLElement {
                  constructor() {
                    super();
                    this.root = this.attachShadow({ mode: "closed" });
                    this.root.innerHTML = "<div id='rows'></div>";
                  }
                }
                class TestCompanyDescription extends HTMLElement {
                  constructor() {
                    super();
                    this.root = this.attachShadow({ mode: "closed" });
                    this.root.innerHTML = "<p id='description'></p>";
                  }
                }
                customElements.define("tv-company-profile", TestCompanyProfile);
                customElements.define("test-company-summary", TestCompanySummary);
                customElements.define("test-company-description", TestCompanyDescription);

                window.populateCompanyProfile = function() {
                  window.__tradingViewShadowRoots[0].querySelector("#rows").innerHTML =
                    "<div class='row'><span class='value'>Tecnología electrónica</span></div>" +
                    "<div class='row'><span class='value'>Equipos de telecomunicaciones</span></div>";
                  const description =
                    window.__tradingViewShadowRoots[1].querySelector("#description");
                  description.className = "description";
                  description.textContent = "Descripción de la empresa";
                };
              </script>
              <tv-company-profile></tv-company-profile>
              <test-company-summary></test-company-summary>
              <test-company-description></test-company-description>
            </body>
            </html>
        """

        const val IFRAME_READINESS_TEST_HTML = """
            <!doctype html>
            <html>
            <body>
              <div class="tradingview-widget-container">
                <iframe id="widget-frame"></iframe>
              </div>
              <script>
                window.__tradingViewWidgetFrameLoaded = false;
                window.markTradingViewFrameLoaded = function() {
                  window.__tradingViewWidgetFrameLoaded = true;
                };
              </script>
            </body>
            </html>
        """

        const val MARKET_OVERVIEW_READINESS_TEST_HTML = """
            <!doctype html>
            <html>
            <body>
              <script>
                const originalAttachShadow = Element.prototype.attachShadow;
                window.__tradingViewShadowRoots = [];
                Element.prototype.attachShadow = function(options) {
                  const root = originalAttachShadow.call(this, options);
                  window.__tradingViewShadowRoots.push(root);
                  return root;
                };

                class TestMarketOverview extends HTMLElement {
                  constructor() {
                    super();
                    this.root = this.attachShadow({ mode: "closed" });
                    this.root.innerHTML = "<div id='chart'></div>";
                  }
                }
                customElements.define("tv-market-overview", TestMarketOverview);

                window.populateMarketOverview = function() {
                  const chart = window.__tradingViewShadowRoots[0].querySelector("#chart");
                  chart.innerHTML =
                    "<svg viewBox='0 0 100 100' style='display:block;width:100px;height:100px'>" +
                    "<path d='M0 80 L40 20 L100 60'></path></svg>";
                };
              </script>
              <tv-market-overview></tv-market-overview>
            </body>
            </html>
        """

        const val SCROLL_TEST_HTML = """
            <!doctype html>
            <html>
            <head>
              <style>
                .scroller { height: 100px; overflow-y: scroll; }
                .content { height: 400px; }
              </style>
            </head>
            <body>
              <div id="host">
                <div id="scroller" class="scroller" style="height:100px;overflow-y:scroll">
                  <div class="content" style="height:400px"></div>
                </div>
              </div>
              <script>
                window.configureTestScroller = function(scroller) {
                  Object.defineProperty(scroller, 'clientHeight', { value: 100 });
                  Object.defineProperty(scroller, 'scrollHeight', { value: 400 });
                  Object.defineProperty(scroller, 'scrollTop', { value: 0, writable: true });
                };
                window.configureTestScroller(document.getElementById('scroller'));
              </script>
            </body>
            </html>
        """

        const val INSTALL_TEST_SCROLL_BRIDGE_SCRIPT = """
            window.__testReportedBounds = [];
            window.tradingViewScrollBounds = {
              postMessage: function(message) {
                window.__testReportedBounds.push(Number(message));
              }
            };
        """

        const val REPLACE_AND_SCROLL_TO_BOTTOM_SCRIPT = """
            (function() {
              document.getElementById('host').innerHTML =
                '<div id="replacement" class="scroller"><div class="content"></div></div>';
              window.configureTestScroller(document.getElementById('replacement'));
              window.setTimeout(function() {
                var replacement = document.getElementById('replacement');
                replacement.scrollTop = replacement.scrollHeight;
                replacement.dispatchEvent(new Event('scroll'));
              }, 250);
            })()
        """
    }
}

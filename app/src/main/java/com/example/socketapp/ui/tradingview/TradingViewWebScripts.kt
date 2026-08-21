package com.example.socketapp.ui.tradingview

internal const val TRADING_VIEW_IFRAME_SANDBOX =
    "allow-scripts allow-same-origin allow-forms"

internal const val TRADING_VIEW_IFRAME_SANDBOX_BOOTSTRAP = """
    <script>
      (function enforceTradingViewIframeSandbox() {
        var policy = "allow-scripts allow-same-origin allow-forms";

        function sandboxFrame(frame) {
          if (!frame || frame.tagName !== "IFRAME") return;
          if (frame.getAttribute("sandbox") !== policy) {
            frame.setAttribute("sandbox", policy);
          }
        }

        function scan(root) {
          sandboxFrame(root);
          if (root && root.querySelectorAll) {
            Array.from(root.querySelectorAll("iframe")).forEach(sandboxFrame);
          }
        }

        function observe(root) {
          if (!root || !window.MutationObserver) return;
          scan(root);
          new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
              if (mutation.type === "attributes") {
                sandboxFrame(mutation.target);
                return;
              }
              Array.from(mutation.addedNodes || []).forEach(scan);
            });
          }).observe(root, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ["sandbox"]
          });
        }

        var originalCreateElement = Document.prototype.createElement;
        Document.prototype.createElement = function() {
          var element = originalCreateElement.apply(this, arguments);
          sandboxFrame(element);
          return element;
        };

        var originalAttachShadow = Element.prototype.attachShadow;
        Element.prototype.attachShadow = function() {
          var root = originalAttachShadow.apply(this, arguments);
          observe(root);
          return root;
        };

        observe(document.documentElement);
        window.__tradingViewIframeSandboxPolicy = policy;
      })();
    </script>
"""

internal const val WIDGET_IFRAME_CHECK =
    "window.__tradingViewWidgetFrameLoaded === true"

internal const val WIDGET_INTERNAL_SCROLL_BOUNDS_CHECK = """
    (function() {
      var candidates = [];
      function collectScrollContainers(root, view) {
        Array.from(root.querySelectorAll('*')).forEach(function(element) {
          var style = view.getComputedStyle(element);
          if (element.scrollHeight > element.clientHeight + 1 &&
              (style.overflowY === 'auto' || style.overflowY === 'scroll')) {
            candidates.push(element);
          }
          if (element.shadowRoot) collectScrollContainers(element.shadowRoot, view);
          if (element.tagName === 'IFRAME') {
            try {
              if (element.contentDocument && element.contentWindow) {
                collectScrollContainers(element.contentDocument, element.contentWindow);
              }
            } catch (_) {}
          }
        });
      }
      collectScrollContainers(document, window);
      (window.__tradingViewShadowRoots || []).forEach(function(root) {
        collectScrollContainers(root, window);
      });
      if (candidates.length === 0) return document.querySelector('iframe') ? -1 : 3;
      candidates.sort(function(left, right) {
        return (right.scrollHeight - right.clientHeight) - (left.scrollHeight - left.clientHeight);
      });
      var scrollContainer = candidates[0];
      var atTop = scrollContainer.scrollTop <= 1;
      var atBottom = scrollContainer.scrollTop + scrollContainer.clientHeight >=
        scrollContainer.scrollHeight - 1;
      return (atTop ? 1 : 0) + (atBottom ? 2 : 0);
    })()
"""

internal const val WIDGET_INTERNAL_SCROLL_OBSERVER_INSTALL = """
    (function() {
      if (window.__tradingViewScrollObserverInstalled) return true;
      window.__tradingViewScrollObserverInstalled = true;

      var scrollContainer = null;
      var resizeObserver = null;
      var lastBounds = null;
      var lastReportTime = 0;
      var attempts = 0;
      var refreshTimer = null;
      var healthCheckTimer = null;
      var observedRoots = [];
      var mutationObservers = [];

      function scheduleFindScrollContainer() {
        if (refreshTimer !== null) return;
        refreshTimer = window.setTimeout(function() {
          refreshTimer = null;
          findScrollContainer();
        }, 50);
      }

      function observeRoot(root) {
        if (!window.MutationObserver || observedRoots.indexOf(root) !== -1) return;
        var observer = new MutationObserver(scheduleFindScrollContainer);
        observer.observe(root, { childList: true, subtree: true });
        observedRoots.push(root);
        mutationObservers.push(observer);
      }

      function reportBounds() {
        if (!scrollContainer || !window.tradingViewScrollBounds) return;
        var atTop = scrollContainer.scrollTop <= 1;
        var atBottom = scrollContainer.scrollTop + scrollContainer.clientHeight >=
          scrollContainer.scrollHeight - 1;
        var bounds = (atTop ? 1 : 0) + (atBottom ? 2 : 0);
        var now = Date.now();
        if (bounds !== lastBounds || now - lastReportTime >= 1000) {
          lastBounds = bounds;
          lastReportTime = now;
          window.tradingViewScrollBounds.postMessage(String(bounds));
        }
      }

      function findScrollContainer() {
        try {
          var candidates = [];
          function collectScrollContainers(root, view) {
            observeRoot(root);
            Array.from(root.querySelectorAll('*')).forEach(function(element) {
              var style = view.getComputedStyle(element);
              if (element.scrollHeight > element.clientHeight + 1 &&
                  (style.overflowY === 'auto' || style.overflowY === 'scroll')) {
                candidates.push(element);
              }
              if (element.shadowRoot) collectScrollContainers(element.shadowRoot, view);
              if (element.tagName === 'IFRAME') {
                try {
                  if (element.contentDocument && element.contentWindow) {
                    collectScrollContainers(element.contentDocument, element.contentWindow);
                  }
                } catch (_) {}
              }
            });
          }
          collectScrollContainers(document, window);
          (window.__tradingViewShadowRoots || []).forEach(function(root) {
            collectScrollContainers(root, window);
          });
          if (candidates.length === 0) return false;
          candidates.sort(function(left, right) {
            return (right.scrollHeight - right.clientHeight) - (left.scrollHeight - left.clientHeight);
          });

          var candidate = candidates[0];
          if (candidate !== scrollContainer) {
            if (scrollContainer) scrollContainer.removeEventListener('scroll', reportBounds);
            if (resizeObserver) resizeObserver.disconnect();
            scrollContainer = candidate;
            lastBounds = null;
            scrollContainer.addEventListener('scroll', reportBounds, { passive: true });
            if (window.ResizeObserver) {
              resizeObserver = new ResizeObserver(reportBounds);
              resizeObserver.observe(scrollContainer);
            }
          }
          reportBounds();
          return true;
        } catch (_) {
          return false;
        }
      }

      function installWhenReady() {
        if (findScrollContainer()) {
          if (healthCheckTimer === null) {
            healthCheckTimer = window.setInterval(findScrollContainer, 1000);
          }
          return;
        }
        attempts += 1;
        if (attempts < 150) window.setTimeout(installWhenReady, 100);
      }

      installWhenReady();
      return true;
    })()
"""

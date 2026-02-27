import UIKit

// MARK: - Gestures

extension KLineChartView {

    func setupGestures() {
        let pan = HorizontalPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        pan.maximumNumberOfTouches = 1
        pan.delegate = self
        addGestureRecognizer(pan)

        let pinch = UIPinchGestureRecognizer(target: self, action: #selector(handlePinch(_:)))
        addGestureRecognizer(pinch)

        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        longPress.minimumPressDuration = 0.3
        addGestureRecognizer(longPress)

        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        addGestureRecognizer(tap)
    }

    @objc func handleTap(_ gesture: UITapGestureRecognizer) {
        let point = gesture.location(in: self)
        if crosshairIndex >= 0 {
            crosshairIndex = -1
            onCrosshairDismiss()
            chartLayer.setNeedsDisplay()
            return
        }
        if currentPriceLabelRect.contains(point) {
            animateScrollToEnd()
        }
    }

    @objc func handlePan(_ gesture: UIPanGestureRecognizer) {
        if isLongPressing || gestureMode == 2 {
            gesture.isEnabled = false
            gesture.isEnabled = true
            return
        }
        switch gesture.state {
        case .began:
            gestureMode = 1
            if crosshairIndex >= 0 {
                crosshairIndex = -1
                onCrosshairDismiss()
            }
            panStartX = translateX
        case .changed:
            let translation = gesture.translation(in: self)
            translateX = clampTranslateX(panStartX + translation.x)
            recalcVisibleRange()
            chartLayer.setNeedsDisplay()
            // Check if near left edge to load more history
            checkAndLoadMoreIfNeeded()
        case .ended, .cancelled:
            let velocity = gesture.velocity(in: self).x
            applyDecay(velocity: velocity)
            checkAndLoadMoreIfNeeded()
        default:
            break
        }
    }

    @objc func handlePinch(_ gesture: UIPinchGestureRecognizer) {
        if isLongPressing || gestureMode == 1 { return }
        switch gesture.state {
        case .began:
            gestureMode = 2
            if crosshairIndex >= 0 {
                crosshairIndex = -1
                onCrosshairDismiss()
            }
            pinchStartWidth = candleWidth
            pinchStartTranslateX = translateX
            pinchStartFocalX = gesture.location(in: self).x
        case .changed:
            userHasZoomed = true
            let newWidth = max(config.minCandleWidth, min(config.maxCandleWidth, pinchStartWidth * gesture.scale))

            let totalOld = pinchStartWidth + config.candleSpacing
            let totalNew = newWidth + config.candleSpacing
            let contentX = (pinchStartFocalX - pinchStartTranslateX) / totalOld
            let newTx = pinchStartFocalX - contentX * totalNew

            candleWidth = newWidth
            translateX = clampTranslateX(newTx)
            recalcVisibleRange()
            chartLayer.setNeedsDisplay()
        default:
            break
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        let totalTouches = event?.allTouches?.filter { $0.phase == .began || $0.phase == .stationary || $0.phase == .moved }.count ?? 0
        if totalTouches >= 2 {
            gestureMode = 2
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        if let event = event, (event.allTouches?.filter { $0.phase != .ended && $0.phase != .cancelled }.count ?? 0) == 0 {
            gestureMode = 0
        }
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        gestureMode = 0
    }

    @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        let location = gesture.location(in: self)
        switch gesture.state {
        case .began:
            isLongPressing = true
            updateCrosshair(at: location)
        case .changed:
            updateCrosshair(at: location)
        case .ended, .cancelled:
            isLongPressing = false
            chartLayer.setNeedsDisplay()
        default:
            break
        }
    }

    func updateCrosshair(at point: CGPoint) {
        let total = candleWidth + config.candleSpacing
        let index = Int((point.x - translateX) / total)
        guard index >= 0 && index < dataItems.count else { return }

        crosshairIndex = index
        crosshairY = point.y
        let item = dataItems[index]
        onCrosshairChange([
            "index": index,
            "timestamp": item.timestamp,
            "open": item.open,
            "high": item.high,
            "low": item.low,
            "close": item.close,
            "volume": item.volume,
            "x": point.x,
            "y": point.y,
        ])
        chartLayer.setNeedsDisplay()
    }
}

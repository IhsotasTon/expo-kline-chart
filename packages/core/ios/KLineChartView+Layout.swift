import UIKit

// MARK: - Layout Calculations

extension KLineChartView {

    struct AnimateScrollState {
        let target: CGFloat
        let stepDelta: CGFloat
        var remaining: Int
        let displayLink: CADisplayLink
    }

    struct DecayState {
        var velocity: CGFloat
        let deceleration: CGFloat
        var remaining: Int
        let displayLink: CADisplayLink
    }

    var chartWidth: CGFloat { bounds.width }
    var mainChartHeight: CGFloat { mainChartFixedHeight }
    var subChartsTop: CGFloat { mainChartHeight }
    var totalSubChartsHeight: CGFloat { CGFloat(subIndicatorTypes.count) * subChartFixedHeight }
    var totalContentHeight: CGFloat { mainChartHeight + totalSubChartsHeight + timeAxisHeight }

    /// Height to report to RN so the view can be sized to content (no internal vertical scroll).
    var reportedContentHeight: CGFloat {
        totalContentHeight
    }

    func isNearEnd() -> Bool {
        if dataItems.isEmpty { return true }
        let total = candleWidth + config.candleSpacing
        let contentWidth = CGFloat(dataItems.count) * total
        let endTx = chartWidth - contentWidth
        return translateX >= endTx - total && translateX <= endTx + total * 2
    }

    func scrollToEnd() {
        let contentWidth = CGFloat(dataItems.count) * (candleWidth + config.candleSpacing)
        let target: CGFloat
        if contentWidth <= chartWidth {
            target = 0
        } else {
            target = clampTranslateX(chartWidth - contentWidth - chartWidth / 3)
        }
        translateX = target
        recalcVisibleRange()
    }

    func animateScrollToEnd() {
        let contentWidth = CGFloat(dataItems.count) * (candleWidth + config.candleSpacing)
        let target: CGFloat
        if contentWidth <= chartWidth {
            target = 0
        } else {
            target = clampTranslateX(chartWidth - contentWidth - chartWidth / 3)
        }
        let distance = target - translateX
        let steps = 30 // ~0.5s at 60fps
        let displayLink = CADisplayLink(target: self, selector: #selector(animateScrollStep))
        animateScrollState = AnimateScrollState(target: target, stepDelta: distance / CGFloat(steps), remaining: steps, displayLink: displayLink)
        displayLink.add(to: .main, forMode: .common)
    }

    @objc func animateScrollStep() {
        guard var state = animateScrollState else { return }
        state.remaining -= 1
        if state.remaining <= 0 {
            translateX = state.target
            state.displayLink.invalidate()
            animateScrollState = nil
        } else {
            translateX += state.stepDelta
        }
        recalcVisibleRange()
        chartLayer.setNeedsDisplay()
        if state.remaining > 0 {
            animateScrollState = state
        }
    }

    func clampTranslateX(_ tx: CGFloat) -> CGFloat {
        let contentWidth = CGFloat(dataItems.count) * (candleWidth + config.candleSpacing)
        let minTx = chartWidth - contentWidth - chartWidth / 2
        let maxTx: CGFloat = 0
        return max(minTx, min(maxTx, tx))
    }

    func recalcVisibleRange() {
        let total = candleWidth + config.candleSpacing
        let start = max(0, Int(floor(-translateX / total)))
        let visibleCount = Int(ceil(chartWidth / total)) + 2
        let end = min(dataItems.count - 1, start + visibleCount)
        visibleStart = start
        visibleEnd = max(start, end)
    }

    func applyDecay(velocity: CGFloat) {
        guard abs(velocity) > 50 else { return }
        let deceleration: CGFloat = 0.998
        let duration: TimeInterval = 0.8
        let steps = Int(duration * 60)
        let currentVelocity = velocity / 60.0

        let displayLink = CADisplayLink(target: self, selector: #selector(decayStep))
        decayState = DecayState(velocity: currentVelocity, deceleration: deceleration, remaining: steps, displayLink: displayLink)
        displayLink.add(to: .main, forMode: .common)
    }

    @objc func decayStep() {
        guard var state = decayState else { return }
        state.velocity *= state.deceleration
        translateX = clampTranslateX(translateX + state.velocity)
        state.remaining -= 1
        recalcVisibleRange()
        chartLayer.setNeedsDisplay()

        // Check if momentum scroll reached left edge
        checkAndLoadMoreIfNeeded()

        if state.remaining <= 0 || abs(state.velocity) < 0.1 {
            state.displayLink.invalidate()
            decayState = nil
        } else {
            decayState = state
        }
    }
}

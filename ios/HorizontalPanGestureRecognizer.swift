import UIKit

// MARK: - Horizontal-only pan (fails when vertical so RN ScrollView can scroll)

private let kPanDirectionThreshold: CGFloat = 8

final class HorizontalPanGestureRecognizer: UIPanGestureRecognizer {
    private var initialPoint: CGPoint = .zero
    private var directionDecided = false
    private var isHorizontal = false

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent) {
        directionDecided = false
        isHorizontal = false
        if let t = touches.first {
            initialPoint = t.location(in: view)
        }
        super.touchesBegan(touches, with: event)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent) {
        if !directionDecided, let t = touches.first, let v = view {
            let p = t.location(in: v)
            let dx = abs(p.x - initialPoint.x)
            let dy = abs(p.y - initialPoint.y)
            if dx >= kPanDirectionThreshold || dy >= kPanDirectionThreshold {
                directionDecided = true
                isHorizontal = dx >= dy
                if !isHorizontal {
                    state = .failed
                    return
                }
            }
        }
        super.touchesMoved(touches, with: event)
    }

    override func reset() {
        directionDecided = false
        isHorizontal = false
        super.reset()
    }
}

//
//  DirectionView.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/21/22.
//

import UIKit

class DirectionView: UIView {
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = .clear
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func draw(_ rect: CGRect) {
    
        let triangle = UIBezierPath()
        triangle.lineWidth = 3
        let y = rect.height - sqrt(pow(rect.width, 2) - pow(rect.width/2, 2))
        triangle.move(to: CGPoint(x: 0, y: rect.height + 5 - y/2))
        triangle.addLine(to: CGPoint(x: rect.width, y: rect.height + 5 - y/2))
        triangle.addLine(to: CGPoint(x: rect.width/2, y: y + 5 - y/2))
        triangle.addLine(to: CGPoint(x: 0, y: rect.height + 5 - y/2))

        UIColor.systemBlue.set()
        triangle.fill()
        triangle.stroke()
        triangle.close()
    }
}


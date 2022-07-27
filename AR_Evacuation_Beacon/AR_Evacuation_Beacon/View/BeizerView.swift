//
//  BeizerView.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/21/22.
//

import UIKit

class BeizerView: UIView {
    
    var path: [Position] = [] {
        didSet {
            self.draw(self.frame)
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func drawCell(point: [(CGFloat, CGFloat)]) {
        let path = UIBezierPath()
        var firstMove: CGPoint = CGPoint(x: 0.0, y: 0.0)
        for i in point.indices {
            if i == 0 {
                firstMove = CGPoint(x: point[i].0 * 10, y: (point[i].1) * 10)
                path.move(to: firstMove)
            }
            if i < point.count - 1 {
                path.addLine(to: CGPoint(x: point[i+1].0 * 10, y: (point[i+1].1) * 10))
            }
        }
        path.addLine(to: firstMove)
        path.close()
        UIColor.systemYellow.withAlphaComponent(0.7).set()
        path.fill()
    }
    
    // x: 10
    override func draw(_ rect: CGRect) {
        var x: CGFloat = 10
        var y: CGFloat = 10
        
        for point in path {

            if let location1 = mapDic[point] {
                drawCell(point: location1)
            }
            if let location2 = micDic2[point] {
                drawCell(point: location2)
            }
            if let location3 = micDic0[point] {
                drawCell(point: location3)
            }

        }
    }
}

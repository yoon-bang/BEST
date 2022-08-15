//
//  BeizerView.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/21/22.
//

import UIKit

class BeizerView: UIView {
    
    var path: [Position] = []
    var firecell: [Position] = []
    var firePredictedCell: [Position] = []
    var conjestionCell: [Position] = []
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func drawCell(point: [(CGFloat, CGFloat)], color: UIColor) {
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
        color.withAlphaComponent(0.7).set()
        path.fill()
    }
    
    private func drawPoint(positions: [Position], color: UIColor) {
        for point in positions {

            if let location1 = mapDic[point] {
                drawCell(point: location1, color: color)
            }
            if let location2 = micDic2[point] {
                drawCell(point: location2, color: color)
            }
            if let location3 = micDic0[point] {
                drawCell(point: location3, color: color)
            }

        }
    }
    
    // x: 10
    override func draw(_ rect: CGRect) {
        var x: CGFloat = 10
        var y: CGFloat = 10
        
        drawPoint(positions: path, color: .systemGreen)
        drawPoint(positions: conjestionCell, color: .systemYellow)
        drawPoint(positions: firePredictedCell, color: .systemOrange)
        drawPoint(positions: firecell, color: .systemRed)
        
    }
}

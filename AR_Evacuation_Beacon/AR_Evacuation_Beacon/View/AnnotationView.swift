//
//  AnnotationView.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/21/22.
//

import UIKit
import CoreLocation

class IndoorAnnotationView: UIView {
    
    var currentPoint: CGPoint = CGPoint(x: 0, y: 0)
    private var circleView = UIView()
    private var directionView = DirectionView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        loadView()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func loadView() {
        circleView = makeCircleView()
        circleView.frame = CGRect(x: 0, y: 10, width: 20, height: 20)
        circleView.layer.backgroundColor = UIColor.systemBlue.cgColor
        circleView.layer.cornerRadius = circleView.frame.height / 2
        circleView.layer.shadowOpacity = 0.5
        circleView.layer.shadowRadius = 7
        addSubview(circleView)
        
        directionView = makeDirectionView()
        directionView.frame = CGRect(x: 0, y: 0, width: circleView.frame.width * 0.7, height: circleView.frame.height * 0.5)
        directionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(directionView)
        
        directionView.centerXAnchor.constraint(equalTo: circleView.centerXAnchor).isActive = true
        directionView.widthAnchor.constraint(equalTo: circleView.widthAnchor, multiplier: 0.7).isActive = true
        directionView.heightAnchor.constraint(equalTo: circleView.heightAnchor, multiplier: 0.5).isActive = true
        directionView.bottomAnchor.constraint(equalTo: circleView.topAnchor).isActive = true
        directionView.isHidden = true
    }
    
    private func makeCircleView() -> UIView {
        let view = UIView()
        view.frame = CGRect(x: 0, y: 0, width: 20, height: 20)
        return view
    }
    
    private func makeDirectionView() -> DirectionView {
        return DirectionView()
    }
    
    func showDirectionView() {
        directionView.isHidden = false
    }
    
    func hideDirectionView() {
        directionView.isHidden = true
    }
    
    func rotate(from point1: Position, to point2: Position) {
        
        let start = transformCellToCGPoint(cellname: point1)
        let end = transformCellToCGPoint(cellname: point2)
        
        let degree = angleBetween2Points(from: start, to: end)
        
        UIView.animate(withDuration: 0.4, delay:0) {
            self.transform = CGAffineTransform.init(rotationAngle: (.pi / 180 * CGFloat((170 + degree))))
        }
    }
    
    private func angleBetween2Points(from: CGPoint, to: CGPoint) -> Float {
        var degree: Float = 0.0
        let tan = atan2(from.x - to.x, from.y - to.y) * 180 / .pi
        if tan < 0 {
            degree = Float(-tan) + 180.0
        } else {
            degree = 180.0 - Float(tan)
        }
        return degree
    }
   
    
    func move(to cellname: Position, completion: @escaping() -> Void) {
        UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseIn) {
            self.move(to: self.transformCellToCGPoint(cellname: cellname))
        } completion: { success in
            if success {
                completion()
            }
        }
    }
    
    private func move(to: CGPoint) {
        self.frame.origin.x = to.x
        self.frame.origin.y = to.y
    }
    
    private func transformCellToCGPoint(cellname: Position) -> CGPoint {
        
        var start: (x: CGFloat, y: CGFloat) = (0, 0)
        var end: (x: CGFloat, y: CGFloat) = (0, 0)
        
        if let firstFloorCellpoints = mapDic[cellname] {
            start = firstFloorCellpoints[0]
            end = firstFloorCellpoints[2]
        } else if let secondFloorCellpoints = micDic2[cellname] {
            start = secondFloorCellpoints[0]
            end = secondFloorCellpoints[2]
        } else if let baseFloorCellPoints = micDic0[cellname] {
            start = baseFloorCellPoints[0]
            end = baseFloorCellPoints[2]
        } else {
            return CGPoint(x: 0, y: 0)
        }
    
        let width = abs(start.x - end.x) / 2
        let height = abs(start.y - end.y) / 2
        
        currentPoint = CGPoint(x: (start.x + width) * 10, y: (start.y + height) * 10)
        return CGPoint(x: (start.x + width) * 10, y: (start.y + height) * 10)
    }
    
}

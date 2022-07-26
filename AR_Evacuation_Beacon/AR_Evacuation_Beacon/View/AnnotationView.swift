//
//  AnnotationView.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/21/22.
//

import UIKit
import CoreLocation

class IndoorAnnotationView: UIView, CLLocationManagerDelegate {
    
    let locationManager: CLLocationManager = {
        $0.requestWhenInUseAuthorization()
        $0.startUpdatingHeading()
        return $0
    }(CLLocationManager())

    private var heading: Double = 360.0
    var currentPoint: CGPoint = CGPoint(x: 0, y: 0)

    override init(frame: CGRect) {
        super.init(frame: frame)
        loadView()
        locationManager.delegate = self
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func loadView() {
        let circleView = makeCircleView()
        circleView.frame = CGRect(x: 0, y: 10, width: 20, height: 20)
        circleView.layer.backgroundColor = UIColor.systemBlue.cgColor
        circleView.layer.cornerRadius = circleView.frame.height / 2
        circleView.layer.shadowOpacity = 0.5
        circleView.layer.shadowRadius = 7
        addSubview(circleView)
        
        let directionView = makeDirectionView()
        directionView.frame = CGRect(x: 0, y: 0, width: circleView.frame.width * 0.7, height: circleView.frame.height * 0.5)
        directionView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(directionView)
        
        directionView.centerXAnchor.constraint(equalTo: circleView.centerXAnchor).isActive = true
        directionView.widthAnchor.constraint(equalTo: circleView.widthAnchor, multiplier: 0.7).isActive = true
        directionView.heightAnchor.constraint(equalTo: circleView.heightAnchor, multiplier: 0.5).isActive = true
        directionView.bottomAnchor.constraint(equalTo: circleView.topAnchor).isActive = true
    }
    
    private func makeCircleView() -> UIView {
        let view = UIView()
        view.frame = CGRect(x: 0, y: 0, width: 20, height: 20)
        return view
    }
    
    private func makeDirectionView() -> UIView {
        return DirectionView()
    }
    
    
    func rotate() {
        UIView.animate(withDuration: 0.4, delay:0) {
            self.transform = CGAffineTransform.init(rotationAngle: (.pi / 360 * (self.heading + 170)) * 2)
        }
    }
   
    
    func move(to cellname: String, completion: @escaping() -> Void) {
        UIView.animate(withDuration: 1.0, delay: 0, options: .curveEaseIn) {
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
    
    private func transformCellToCGPoint(cellname: String) -> CGPoint {
        
        var start: (CGFloat, CGFloat) = (0, 0)
        var end: (CGFloat, CGFloat) = (0, 0)
        
        if let firstFloorCellpoints = mapDic[cellname] {
            start = firstFloorCellpoints[0]
            end = firstFloorCellpoints[1]
        } else if let secondFloorCellpoints = micDic2[cellname] {
            start = secondFloorCellpoints[0]
            end = secondFloorCellpoints[1]
        } else if let baseFloorCellPoints = micDic0[cellname] {
            start = baseFloorCellPoints[0]
            end = baseFloorCellPoints[1]
        } else {
            return CGPoint(x: 0, y: 0)
        }
    
        let width = abs(start.0 - end.0) / 2
        let height = abs(start.1 - end.1) / 2
        
        currentPoint = CGPoint(x: (start.0 + width) * 10, y: (start.1 + height + 5) * 10)
        return CGPoint(x: (start.0 + width) * 10, y: (start.1 + height + 5) * 10)
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        self.heading = newHeading.trueHeading
        rotate()
    }
    
}

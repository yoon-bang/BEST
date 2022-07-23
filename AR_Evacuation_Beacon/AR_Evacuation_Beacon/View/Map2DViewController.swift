//
//  2DMapViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/20/22.
//

import UIKit
import MapKit

let mapDic: [String: [(CGFloat, CGFloat)]] = [
    "S03": [(1,3), (5,3), (5,8), (1,8)],
    "S02": [(5,3),(13.3, 8),(13.3, 3), (5, 8)],
    "S01": [(5, 8), (13.3, 8), (13.3,13), (5, 13)],
    "E01": [(13.3, 3), (25, 3), (25, 13), (13.3, 13)],
    "R03": [(25,3), (38,3), (38,12), (25,12)],
    "R04": [(1, 13), (17,13), (17, 23.5), (1, 23.5)],
    "A01": [(17, 13), (25, 13), (25, 21.4), (17, 21.4)],
    "R02": [(25,12.5), (38,12.5), (38, 20.3), (25, 20.3)],
    "R01": [(1, 23.3), (17, 23.3), (17, 47), (1, 47)],
    "A02": [(17, 21.3), (24.6, 21.3), (24.6, 29), (17, 29)],
    "A03": [(17, 29), (24.6, 29), (24.6, 37), (17,37)],
    "A04": [(17, 37), (24.6, 37), (24.6, 44.5), (17, 44.5)],
    "A05": [(17, 44.5), (24.6, 44.5), (24.6, 52), (17, 52)],
    "A06": [(17, 52.1), (24.6, 52.1), (24.6, 59.5), (17, 59.5)],
    "A07": [(17, 59.5), (24.6, 59.5),(24.6, 67), (17, 67)],
    "A08": [(24.6, 21.3), (24.6, 40.5), (38, 40.5), (38, 21.3)],
    "A09": [(24.6, 40.5), (24.6, 59.5), (38, 59.5), (38, 40.5)],
    "A10": [(24.6, 59.5), (31.3, 59.5), (31.3, 68), (24.6, 68)],
    "A11": [(31.3, 59.5), (38, 59), (38, 68), (31.3, 68)],
    "E03": [(33.6, 68), (33.6, 75.2), (38, 75.2), (38, 68)],
    "R05": [(1, 47), (17, 47), (17, 56), (1, 56)],
    "H02": [(15.5, 67.6), (24.5, 67.6), (24.5, 75.2), (18.4, 75.2), (18.4, 70.9), (15.5, 70.9)],
    "S07": [(5, 66), (15.5, 66), (15.5, 70.9), (5, 70.9)],
    "S06": [(5, 70.9), (5, 75), (18.4, 75.2), (18.4, 70.9)],
    "E02": [(1, 66), (5.2, 66), (5.2, 75.2), (1, 75.2)]
]

// 큰방에서는 measure 점을 찍지 않는다.

class Map2DViewController: UIViewController {
    
    @IBOutlet weak var mapImagView: UIImageView!
    var bezierView: BeizerView = BeizerView()
    var annotationView = IndoorAnnotationView()
    
    var path = [String]()
    
    var userlocation = ""
    
    override func viewDidLoad() {
        super.viewDidLoad()
        mapImagView.image = UIImage(named: "KSW_1")!
        annotationView = loadAnnotationView()
        NotificationCenter.default.addObserver(self, selector: #selector(movenotification(_:)), name: .movePosition, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(getPath(_:)), name: .path, object: nil)
    }
    
    @objc func movenotification(_ noti: Notification) {
        guard let location = noti.object as? String else {return}
        userlocation = location
        print("from beaconVC", userlocation)
        annotationView.move(to: userlocation)
        
    }
    
    @objc func getPath(_ noti: Notification) {
        guard let path = noti.object as? [String] else {return}
        
        if self.path.count == 0 {
            self.path = path
            self.bezierView.path = path
            bezierView.frame = self.view.frame
            bezierView.backgroundColor = .clear
            mapImagView.addSubview(bezierView)
        } else {
            bezierView.removeFromSuperview()
            bezierView = BeizerView(frame: self.view.frame)
            self.bezierView.path = path
            bezierView.backgroundColor = .clear
            mapImagView.addSubview(bezierView)

        }
    }
    
    private func loadAnnotationView() -> IndoorAnnotationView {
        let annotationView = IndoorAnnotationView(frame: CGRect(x: 100, y: 100, width: 20, height: 20))
        annotationView.translatesAutoresizingMaskIntoConstraints = false
        self.mapImagView.addSubview(annotationView)
        return annotationView
    }
    
}

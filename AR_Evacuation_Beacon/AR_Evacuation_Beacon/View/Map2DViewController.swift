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
    "S02": [(5,3),(13.3, 3),(13.3, 8), (5, 8)],
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

let micDic2: [String: [(CGFloat, CGFloat)]] = [
                                                "S03" : [(0.7, 2.5), (5.3, 2.5), (5.3, 12.7), (0.7, 12.7)],
                                                "S02" : [(5.3, 2.5), (14, 2.5), (14, 7.6) ,(5.3, 7.6) ],
                                                "S04" : [(5.3, 7.6), (14, 7.6), (14, 12.7), (5.3, 12.7) ],
                                                "H01" : [(14, 2.5), (28.5, 2.5), (28.5, 12.7), (14, 12.7)],
                                                "S05" : [(0.7, 63), (5, 63), (5, 75), (0.7, 75)],
                                                "S06" : [(0.7, 75), (16, 75), (16, 79.7), (0.7, 79.7)]
                                                ]


let micDic0: [String: [(CGFloat, CGFloat)]] = ["E02":[(0.9, 67), (5, 67), (5, 76.4), (0.9, 76.4)],
                                               "S08":[(5, 71.8), (11.6, 71.8), (11.6, 76.4), (5, 76.4)],
                                               "S09":[(11.6, 67), (16, 67), (16, 76.4), (11.6, 76.4)],
                                               "U01":[(8, 60), (16,60), (16, 67), (11.6, 67), (11.6, 72), (8, 72)]
                                                ]

// 큰방에서는 measure 점을 찍지 않는다.

class Map2DViewController: UIViewController {
    
    var mapImagView: UIImageView = UIImageView()
    var firstFloorPathView: BeizerView = BeizerView()
    var secondFloorPathView: BeizerView = BeizerView()
    var baseFloorPathView: BeizerView = BeizerView()
    var annotationView = IndoorAnnotationView()
    
    var path = [String]()
    
    var userlocation = ""
    var previousUserLocation: [String] = [] {
        didSet {
            if previousUserLocation.count > 4 {
                previousUserLocation.removeFirst()
            }
        }
    }
    var imageName: String = "KSW_0"
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        view.addSubview(mapImagView)
        mapImagView.alpha = 0.5
        mapImagView.translatesAutoresizingMaskIntoConstraints = false
        mapImagView.heightAnchor.constraint(equalTo: view.heightAnchor).isActive = true
        mapImagView.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
        mapImagView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        mapImagView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        mapImagView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        mapImagView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        
        mapImagView.image = UIImage(named: "KSW_1")!
        annotationView = loadAnnotationView()
        NotificationCenter.default.addObserver(self, selector: #selector(movenotification(_:)), name: .movePosition, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(getPath(_:)), name: .path, object: nil)
    }
    
    @objc func movenotification(_ noti: Notification) {
        guard let location = noti.object as? String else {return}
        userlocation = location
        print("from beaconVC", userlocation)
        annotationView.move(to: userlocation) { [weak self] in
            
            guard let self = self else {return}
            guard let end = self.path.last else {return}
            
            if self.userlocation == end {
                return
            }
            
            if !self.previousUserLocation.isEmpty {
    //            getRightMapView()
                self.changeMapView()
            }
            self.previousUserLocation.append(location)
        }
        
        // if map image is not the one?
        // 만약에 1층칸이 previous 2 ~3 개인데, 1층칸 맵이 아니라면, 1층칸 맵으로 변경해야지
        
        
    }
    
    @objc func getPath(_ noti: Notification) {
        guard let path = noti.object as? [String] else {return}
        self.path = path
        // 여기서 path 받으면 beizerview 3개 만들고 갈아껴야할듯
        if self.path.count == 0 {
            bringStartingPathView()
        } else {
            didPathChanged()
            bringStartingPathView()
            mapImagView.addSubview(firstFloorPathView)

        }
    }
    
    private func changeMapView() {
        
        if checkPathForImageChange0To1() && imageName == "KSW_0" {
            imageName = "KSW_1"
            mapImagView.image = UIImage(named: imageName)!
            changeBeizerpathView(to: firstFloorPathView)
            previousUserLocation.removeAll()
        } else if checkPathForImageChange1To0() && imageName == "KSW_1" {
            imageName = "KSW_0"
            mapImagView.image = UIImage(named: imageName)!
            changeBeizerpathView(to: baseFloorPathView)
            previousUserLocation.removeAll()
        } else if checkPathForImageChange1To2() && imageName == "KSW_1" {
            imageName = "KSW_2"
            mapImagView.image = UIImage(named: imageName)!
            changeBeizerpathView(to: secondFloorPathView)
            previousUserLocation.removeAll()
        } else if checkPathForImageChange2To1() && imageName == "KSW_2" {
            previousUserLocation.removeAll()
            changeBeizerpathView(to: firstFloorPathView)
            imageName = "KSW_1"
            mapImagView.image = UIImage(named: imageName)!
        } else if checkPathForImageChange1To2BackDoor() && imageName == "KSW_1" {
            previousUserLocation.removeAll()
            changeBeizerpathView(to: secondFloorPathView)
            imageName = "KSW_2"
            mapImagView.image = UIImage(named: imageName)!
        } else if checkPathForImageChange2To1BackDoor() && imageName == "KSW_2" {
            previousUserLocation.removeAll()
            imageName = "KSW_1"
            changeBeizerpathView(to: firstFloorPathView)
            mapImagView.image = UIImage(named: imageName)!
        }
    }
    
    private func getRightMapView() {
        if Set(Array(mapDic.keys.map { $0 })).intersection(previousUserLocation).count > 1 && (imageName != "KSW_1" || firstFloorPathView.isHidden == true) {
            mapImagView.image = UIImage(named: "KSW_1")
            changeBeizerpathView(to: firstFloorPathView)
        }
        
        //2floors
        if Set(Array(micDic2.keys.map { $0 })).intersection(previousUserLocation).count > 1 && (imageName != "KSW_2" || secondFloorPathView.isHidden == true){
            mapImagView.image = UIImage(named: "KSW_2")
            changeBeizerpathView(to: secondFloorPathView)
        }
        
        // 0 floor
        if Set(Array(micDic0.keys.map { $0 })).intersection(previousUserLocation).count > 1 && (imageName != "KSW_0" || baseFloorPathView.isHidden == true){
            mapImagView.image = UIImage(named: "KSW_0")
            changeBeizerpathView(to: baseFloorPathView)
        }
    }
    
    
    
    private func changeBeizerpathView(to pathView: BeizerView) {
        if pathView == firstFloorPathView {
            firstFloorPathView.isHidden = false
            secondFloorPathView.isHidden = true
            baseFloorPathView.isHidden = true
        } else if pathView == secondFloorPathView {
            firstFloorPathView.isHidden = true
            secondFloorPathView.isHidden = false
            baseFloorPathView.isHidden = true
        } else {
            firstFloorPathView.isHidden = true
            secondFloorPathView.isHidden = true
            baseFloorPathView.isHidden = false
        }
    }
    
    private func checkPathForImageChange2To1BackDoor() -> Bool {
        return userlocation == "S06" && previousUserLocation.contains("S05")
    }
    
    private func checkPathForImageChange2To1() -> Bool {
        return (userlocation == "S03") && (previousUserLocation.contains("S04") || previousUserLocation.contains("H01"))
    }
    
    private func checkPathForImageChange1To2() -> Bool {
        return (userlocation == "S03" || userlocation == "S04" || userlocation == "H01") && (previousUserLocation.contains("S02") || previousUserLocation.contains("E01"))
    }
    
    private func checkPathForImageChange1To0() -> Bool {
        return (userlocation == "E02" || userlocation == "S08") && (previousUserLocation.contains("S07") || previousUserLocation.contains("H02"))
    }
    
    private func checkPathForImageChange0To1() -> Bool {
        return (userlocation == "E02" || userlocation == "S07") && (previousUserLocation.contains("S08") || previousUserLocation.contains("S09"))
    }
    
    private func checkPathForImageChange1To2BackDoor() -> Bool {
        return userlocation == "S06" && (previousUserLocation.contains("H02") || previousUserLocation.contains("A07"))
    }
    
    private func injectPathToViews() {
        var firstMapPath: [String] = []
        var secondMapPath: [String] = []
        var baseMapPath: [String] = []
        
        path.forEach { point in
            if mapDic.keys.contains(point) {
                firstMapPath.append(point)
            }
            if micDic2.keys.contains(point) {
                secondMapPath.append(point)
            }
            if micDic0.keys.contains(point) {
                baseMapPath.append(point)
            }
        }
        
        firstFloorPathView.path = firstMapPath
        secondFloorPathView.path = secondMapPath
        baseFloorPathView.path = baseMapPath
        
    }
    
    private func bringStartingPathView() {
        guard let startpoint = path.first else {return}
        injectPathToViews()
        addPathView()
        if mapDic.keys.contains(startpoint) {
            changeBeizerpathView(to: firstFloorPathView)
            imageName = "KSW_1"
        } else if micDic2.keys.contains(startpoint) {
            changeBeizerpathView(to: secondFloorPathView)
            imageName = "KSW_2"
        } else if micDic0.keys.contains(startpoint) {
            changeBeizerpathView(to: baseFloorPathView)
            imageName = "KSW_0"
        }
        mapImagView.image = UIImage(named: imageName)!
    }
    
    
    // if path change -> Should remove the pathviews and start again
    private func didPathChanged() {
        self.firstFloorPathView.path = path
        firstFloorPathView.removeFromSuperview()
        firstFloorPathView = BeizerView(frame: self.view.frame)
        firstFloorPathView.backgroundColor = .clear
        mapImagView.addSubview(firstFloorPathView)
        
        secondFloorPathView.removeFromSuperview()
        secondFloorPathView = BeizerView(frame: self.view.frame)
        self.secondFloorPathView.path = path
        secondFloorPathView.backgroundColor = .clear
        mapImagView.addSubview(secondFloorPathView)
        
        baseFloorPathView.removeFromSuperview()
        baseFloorPathView = BeizerView(frame: self.view.frame)
        self.baseFloorPathView.path = path
        baseFloorPathView.backgroundColor = .clear
        mapImagView.addSubview(baseFloorPathView)
    }
    
    private func addPathView() {
        firstFloorPathView.frame = self.view.frame
        firstFloorPathView.backgroundColor = .clear
        mapImagView.addSubview(firstFloorPathView)
        
//        secondFloorPathView.frame = self.view.frame
//        secondFloorPathView.backgroundColor = .clear
//        mapImagView.addSubview(secondFloorPathView)
//
//        baseFloorPathView.frame = self.view.frame
//        baseFloorPathView.backgroundColor = .clear
//        mapImagView.addSubview(baseFloorPathView)
    }
    
    private func loadAnnotationView() -> IndoorAnnotationView {
        let annotationView = IndoorAnnotationView(frame: CGRect(x: 100, y: 100, width: 20, height: 20))
        annotationView.translatesAutoresizingMaskIntoConstraints = false
        self.mapImagView.addSubview(annotationView)
        return annotationView
    }
    
}

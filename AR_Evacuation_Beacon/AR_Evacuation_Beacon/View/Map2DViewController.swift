//
//  2DMapViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/20/22.
//

import UIKit
import MapKit

let mapDic: [String: [(CGFloat, CGFloat)]] = [
    "S03": [(0.6, 0.6), (5,0.6), (5,6.4), (0.6,6.4)],
    "S02": [(5,0.6),(13, 0.6),(13, 6.4), (5, 6.4)],
    "S01": [(5, 6.4), (13, 6.4), (13,12), (5, 12)],
    "E01": [(13, 0.6), (25, 0.6), (25, 12), (13, 12)],
    "R03": [(25,0.6), (38.5,0.6), (38.5,11), (25,11)],
    "R02": [(25,11), (38.5,11), (38.5, 20.5), (25, 20.5)],
    "R04": [(0.6, 12), (16.7,12), (16.7, 24), (0.6, 24)],
    "A01": [(16.7, 12), (25, 12), (25, 21.6), (16.7, 21.6)],
    "A02": [(16.7, 21.6), (24.4, 21.6), (24.4, 30.5), (16.7, 30.5)],
    "R01": [(0.6, 24), (16.7, 24), (16.7, 51.2), (0.6, 51.2)],
    "A08": [(24.4, 21.6), (38.5, 21.6), (38.5, 43.8), (24.4, 43.8)],
    "A03": [(16.7, 30.5), (24.4, 30.5), (24.4,39.2), (16.7, 39.2)],
    "A04": [(16.7, 39.2), (24.4,39.2), (24.4, 48.2), (16.7, 48.2)],
    "A05": [(16.7, 48.2),(24.4, 48.2), (24.4, 57), (16.7, 57)],
    "A06": [(16.7, 57), (24.4, 57), (24.4, 66), (16.7, 66)],
    "A07": [(16.7, 66), (24.4, 66),(24.4, 75), (16.7, 75)],
    "A09": [(24.4, 43.8),(38.5, 43.8), (38.5, 66), (24.4, 66)],
    "A10": [(24.4, 66), (31.5, 66), (31.5, 76), (24.4, 76)],
    "A11": [(31.5, 66), (38.5, 66), (38.5, 76), (31.3, 76)],
    "E03": [(34, 76), (38.5, 76), (38.5, 84), (34, 84)],
    "R05": [(0.6, 51.2), (16.7, 51.2), (16.7,61), (0.6, 61)],
    "H02": [(15, 75), (24.5, 75), (24.5, 84), (18.2, 84), (18.2, 79), (15, 79)],
    "S07": [(5, 73.3), (15, 73.3), (15, 79), (5, 79)],
    "S06": [(5, 79), (18.2, 79), (18.2, 84), (5, 84)],
    "E02": [(0.6, 73.3), (5, 73.3), (5, 84), (0.6, 84)]
]

let micDic2: [String: [(CGFloat, CGFloat)]] = [
                                                "S03" : [(0, 0), (5, 0), (5, 11.7), (0, 11.7)],
                                                "S02" : [(5, 0), (14, 0), (14, 6) ,(5, 6) ],
                                                "S04" : [(5, 6), (14, 6), (14, 11.7), (5, 11.7) ],
                                                "H01" : [(14, 0), (28.5, 0), (28.5, 11.7), (14, 11.7)],
                                                "S05" : [(0, 68), (5, 68), (5, 84), (0, 84)],
                                                "S06" : [(0, 79), (15.7, 79), (15.7, 84), (0, 84)]
                                                ]


let micDic0: [String: [(CGFloat, CGFloat)]] = ["E02":[(0.6, 73.2), (5, 73.2), (5, 84), (0.6, 84)],
                                               "S08":[(5, 79), (11.6, 79), (11.6, 84), (5, 84)],
                                               "S09":[(11.6, 74), (16, 74), (16, 84), (11.6, 84)],
                                               "U01":[(7.7, 65), (16,65), (16, 74), (11.6, 74), (11.6, 79), (7.7, 79)]
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
        mapImagView.contentMode = .scaleToFill // fill
        mapImagView.translatesAutoresizingMaskIntoConstraints = false
//        mapImagView.heightAnchor.constraint(equalTo: view.heightAnchor).isActive = true
//        mapImagView.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
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

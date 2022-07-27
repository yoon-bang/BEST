//
//  2DMapViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/20/22.
//

import UIKit
import MapKit
import SocketIO

class Map2DViewController: UIViewController {
    
    var mapImagView: UIImageView = UIImageView()
    var firstFloorPathView: BeizerView = BeizerView()
    var secondFloorPathView: BeizerView = BeizerView()
    var baseFloorPathView: BeizerView = BeizerView()
    var annotationView = IndoorAnnotationView()
    
    var path = [Position]()
    
    var userlocation: Position = .unknown
    var prevLocation: Position = .unknown
    var previousUserLocation: [Position] = [] {
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
        guard let location = noti.object as? Position else {return}
        
        userlocation = location
        print("from beaconVC", userlocation)
        
        // rotate
        if annotationView.currentPoint == CGPoint(x: 0, y: 0) {
            
        } else {
            annotationView.showDirectionView()
            annotationView.rotate(from: prevLocation, to: userlocation)
            
        }
        
        
        //move
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
            self.prevLocation = location
        }
        
        // if map image is not the one?
        // 만약에 1층칸이 previous 2 ~3 개인데, 1층칸 맵이 아니라면, 1층칸 맵으로 변경해야지
        
        
    }
    
    @objc func getPath(_ noti: Notification) {
        guard let path = noti.object as? [Position] else {return}
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
        return userlocation == .S06 && previousUserLocation.contains(.S05)
    }
    
    private func checkPathForImageChange2To1() -> Bool {
        return (userlocation == .S03) && (previousUserLocation.contains(.S04) || previousUserLocation.contains(.H01))
    }
    
    private func checkPathForImageChange1To2() -> Bool {
        return (userlocation == .S03 || userlocation == .S04 || userlocation == .H01) && (previousUserLocation.contains(.S02) || previousUserLocation.contains(.E01))
    }
    
    private func checkPathForImageChange1To0() -> Bool {
        return (userlocation == .E02 || userlocation == .S08) && (previousUserLocation.contains(.S07) || previousUserLocation.contains(.H02))
    }
    
    private func checkPathForImageChange0To1() -> Bool {
        return (userlocation == .E02 || userlocation == .S07) && (previousUserLocation.contains(.S08) || previousUserLocation.contains(.S09))
    }
    
    private func checkPathForImageChange1To2BackDoor() -> Bool {
        return userlocation == .S06 && (previousUserLocation.contains(.H02) || previousUserLocation.contains(.A07))
    }
    
    private func injectPathToViews() {
        var firstMapPath: [Position] = []
        var secondMapPath: [Position] = []
        var baseMapPath: [Position] = []
        
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
        
        secondFloorPathView.frame = self.view.frame
        secondFloorPathView.backgroundColor = .clear
        mapImagView.addSubview(secondFloorPathView)

        baseFloorPathView.frame = self.view.frame
        baseFloorPathView.backgroundColor = .clear
        mapImagView.addSubview(baseFloorPathView)
    }
    
    private func loadAnnotationView() -> IndoorAnnotationView {
        let annotationView = IndoorAnnotationView(frame: CGRect(x: 100, y: 100, width: 20, height: 20))
        annotationView.translatesAutoresizingMaskIntoConstraints = false
        self.mapImagView.addSubview(annotationView)
        return annotationView
    }
    
}

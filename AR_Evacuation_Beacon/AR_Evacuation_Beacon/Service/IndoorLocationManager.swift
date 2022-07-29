//
//  IndoorLocationManager.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/22/22.
//

// MARK: - 여기까지함
// TODO: 코드 수정하기
// 1. 비콘 잡히는 것 안보여줘도댐
// 2. 어차피 하나로 합칠거임
// 3. 비콘 5개 모이는 것을 어떠케 처리할까??
// TODO: 모든모델이 전부다 나오게하기
// 1. 모든 모델의 csv 저장하도록하기
// 2.

import Foundation
import CoreLocation
import SocketIO
import SwiftUI

enum Mode {
    case debug
    case collection
    case real
}

enum Direction: Int {
    case South
    case East
    case North
    case West
}

enum NavigationDirection: String, CustomStringConvertible {
    var description: String {
        switch self {
        case .forward:
            return "GO STRAIGHT"
        case .backward:
            return "GO BACK"
        case .danger:
            return "DANGER"
        case .goUpstair:
            return "GO UPSTAIR"
        case .goDownstair:
            return "GO DOWNSTAIR"
        case .stair:
            return "STAIRS, CAUTION"
        case .left:
            return "TURN LEFT"
        case .right:
            return "TURN RIGHT"
        }
    }
    
    case forward
    case backward
    case left
    case right
    case danger
    case stair
    case goUpstair
    case goDownstair
    
}

class IndoorLocationManager: NSObject, CLLocationManagerDelegate {
    
    // MARK: - Properties
    private var beaconRegion: CLBeaconRegion!
    private var beaconRegionConstraint: CLBeaconIdentityConstraint!
    private let locationManager: CLLocationManager = {
        $0.requestWhenInUseAuthorization()
        $0.startUpdatingHeading()
        return $0
    }(CLLocationManager())
    private var beaconManager = BeaconManager.shared
    private var classificationModels: [ModelInterpreter] = []
    private var previousBeacons = [Beacon]()
    private var heading: Double = 0.0
    private var firstposition = ""
    private var positionList: [String] = []
    
    private var currentBeacons = [Beacon]() {
        didSet {
            NotificationCenter.default.post(name: .beacons, object: currentBeacons)
        }
    }
    
    var mode: Mode = .collection
    
    private var userLocation = "A01"
    private var previousUserLocation: Position = .unknown
    
    init(mode: Mode) {
        super.init()
        beaconConfiguration()
        locationManager.delegate = self
        self.mode = mode
        modelNames.forEach {
            self.classificationModels.append(self.makeClassificationModel(modelName: $0))
        }
        
        // TEST
        getPath()
        testMoveUserLocation()
    }
    
}

// MARK: - Internal API for indoor Location
extension IndoorLocationManager {
    
    /*
     get Userlocation with beacon RSSI and Model
     find the overlap and get the most overlapped one
     if cannot find the overlapped one, get the location from the Model with 4 beacons
     since the Model with 4 beacons has best accuracy
     */
    private func userLocation(prevBeaconInfo: [Beacon], currentBeaconInfo: [Beacon]) {
        
        let csvlist = combineBeaconInfos(prev: prevBeaconInfo, current: currentBeaconInfo)
        
        let locations = classificationModels.enumerated().map { (index, model) in
            return model.classifyLocationOfUser(with: csvlist[index]) ?? "unknown"
        }
        
        // find the overlap and get the most overlapped one
        var dic: [String: Int] = [:]
        
        locations.forEach { location in
            if location != "unknown" {
                dic.updateValue((dic[location] ?? 0) + 1, forKey: location)
            }
        }
        
        // if cannot find the overlapped one, get the location from the Model with 4 beacons
        if mode == .real {
            if previousUserLocation == .unknown {
                previousUserLocation = Position(rawValue: locations[0]) ?? .unknown
            } else {
                let location = filterErrorWithHeading(previousLocation: previousUserLocation, currentLocation: Position(rawValue: locations[0]) ?? .unknown)
                sendLocationToServerWithSocket(location: location.rawValue)
                NotificationCenter.default.post(name: .movePosition, object: location)
                SocketIOManager.shared.receivePath { path in
                    NotificationCenter.default.post(name: .path, object: path) }
                previousUserLocation = location
            }
        } else if mode == .collection {
            NotificationCenter.default.post(name: .movePosition, object: locations)
        } else {
            NotificationCenter.default.post(name: .movePosition, object: locations[0])
        }
        
    }
    
    func getPath() {
//                SocketIOManager.shared.receivePath { path in
//                    NotificationCenter.default.post(name: .path, object: path)
//                }
        
        //TEST
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            NotificationCenter.default.post(name: .path, object: [Position.R02, Position.A01, Position.A02, Position.A03, Position.A04, Position.A05, Position.A06])
        }
    }
    
    func testMoveUserLocation() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            var userlocations: [Position] = [.R02, .A01, .A02, .A03, .A04, .A05, .A06]
            Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { timer in
                
                if userlocations.isEmpty {
                    timer.invalidate()
                    return
                }
                let location = userlocations.removeFirst()
                NotificationCenter.default.post(name: .movePosition, object: location)
                SocketIOManager.shared.sendLocation(location: location.rawValue)
                
            }
        }
    }
    
    private func filterErrorWithHeading(previousLocation: Position, currentLocation: Position) -> Position {
        
        if previousLocation == .unknown || currentLocation == .unknown {return .unknown}
        
        let adjacentCells = previousLocation.adjacentCell.flatMap { (ele: [Position]) -> [Position] in
            return ele
        }
        
        // is CurrentLocation is Adjacent with previousLocation?
        let currentDirection = headingToDirection(heading: self.heading)
        if adjacentCells.contains(currentLocation) {

            var candidateCells = previousLocation.adjacentCell[currentDirection.rawValue]
            candidateCells.removeAll { $0 == .unknown }
            // 인접셀이고, 방향과 맞는데, 그것을 빼줬다면, 맞는것이다.
            if candidateCells.contains(currentLocation) {
//                print("인접셀, 뱡향, 모델")
                return currentLocation
            } else {
                // 인접셀이고, 방향과 맞지 않는데, 그것을 빼줬다면?
                // 1. 움직였다고 해서 방향과 맞는 인접셀을 빼준다.
                // 2. 움직이지 않았다고 생각한다.
//                print("인접셀, 방향X, 그대로있기")
                return previousLocation // 일단은 2번
            }
        } else { // 인접셀이지 않을떄,
            // 2-1-a 빠르게 움직였다고 판단한다. 방향과 같다면, 움직였다고 생각하기 옆셀로 이동시키기
            let currentPosPoint = VectorService.transformCellToCGPoint(cellname: currentLocation)
            let prevPosPoint = VectorService.transformCellToCGPoint(cellname: previousLocation)
            let direction = headingToDirection(heading: Double(VectorService.vectorBetween2Points(from: prevPosPoint, to: currentPosPoint).angle))
            
            // 방향이 같을때, 인접셀중에 방향이 같은 셀로 움직인다.
            if direction == currentDirection {
//                print("인접셀X, 방향 같음, 변경")
                var candidateCells = previousLocation.adjacentCell[direction.rawValue]
                // 방향이 같은데, 혹시 unknown이 있다면 일단 다지운다.
                candidateCells.removeAll { $0 == .unknown }
                // 그리고 첫번째꺼를 빼고, 그렇지 않은 경우에는 제자리에 있게한다.
                return candidateCells.first ?? previousLocation
            } else {
//                print("인접셀X, 방향 다름, 변경")
                return previousLocation
            }
            
        }
    }
    
    private func sendLocationToServerWithSocket(location: String) {
        SocketIOManager.shared.sendLocation(location: location)
    }
    
}

// MARK: - Beacon CLlocationDelegate
extension IndoorLocationManager {
    
    // Authorize location tracking
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        
        switch status {
        case .authorizedAlways:
            if !locationManager.monitoredRegions.contains(beaconRegion) {
                locationManager.startMonitoring(for: beaconRegion)
            }
        case .authorizedWhenInUse:
            if !locationManager.monitoredRegions.contains(beaconRegion) {
                locationManager.startMonitoring(for: beaconRegion)
            }
        default:
            print("authorisation not granted")
        }
    }
    
    // determine the location of user with beacon area
    func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
        print("Did determine state for region \(region)")
        if state == .inside {
            locationManager.startRangingBeacons(satisfying: beaconRegionConstraint)
            print("inside")
        } else {
            print("outside")
        }
    }
    
    // start monitoring beacons
    func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        print("Did start monitoring region: \(region)\n")
    }
    
    // call when the user enter the beacon area
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        locationManager.startRangingBeacons(satisfying: beaconRegionConstraint)
        print("didEnter")
    }
    
    // call when the user exit the beacon area
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        locationManager.stopRangingBeacons(satisfying: beaconRegionConstraint)
        currentBeacons.removeAll()
        print("didExit")
    }
    
    // detect the beacons
    func locationManager(_ manager: CLLocationManager, didRange beacons: [CLBeacon], satisfying beaconConstraint: CLBeaconIdentityConstraint) {
        
        currentBeacons = beacons.map { beacon in
            return Beacon(beacon: beacon)
        }
        
        // check the user moving
        checkUserMoving(prev: currentBeacons)
        
        // get beacon signal twice, and combine them for get the all signal
        if previousBeacons.count == 0 {
            previousBeacons = currentBeacons
        } else {
            if direction {
                let csv = createCSVWithPrevAndCurrentBeacons(prev: previousBeacons, current: currentBeacons, heading: heading)
            } else {
//                userLocation(prevBeaconInfo: previousBeacons, currentBeaconInfo: currentBeacons)
                previousBeacons.removeAll()
            }
        }
    }
}

// MARK: -Private function
extension IndoorLocationManager {
    
    private func makeClassificationModel(modelName: String) -> ModelInterpreter {
        return ModelInterpreter(modelName: modelName)
    }
    
    private func checkUserMoving(prev: [Beacon]) {
        beaconManager.updateBeaconDictionary(beacons: prev)
        beaconManager.updateKalmanFilter()
    }
    
    private func beaconConfiguration() {
        
        let uuid = UUID(uuidString: "020012AC-4202-649D-EC11-B6CBC8814AD7")!
        beaconRegionConstraint = CLBeaconIdentityConstraint(uuid: uuid)
        beaconRegion = CLBeaconRegion(uuid: uuid, identifier: uuid.uuidString)
        
        if CLLocationManager.authorizationStatus() != .authorizedAlways {
            locationManager.requestAlwaysAuthorization()
        } else {
            locationManager.startMonitoring(for: beaconRegion)
        }
        
    }
    
    
    /*
     combine 2 info from beacons and make it to csv
     get the csv for all models
     */
    private func combineBeaconInfos(prev: [Beacon], current: [Beacon]) -> [String] {
        
        var tempDictionary = [String:Float]()
        var resultDictionary = [String:Float]()
        var result = [String]()
        
        let prevCSV = CSVService.makeBeaconInfoCSV(beacon: prev)
        let currentCSV = CSVService.makeBeaconInfoCSV(beacon: current)
        
        // choose max between prev and current Beacons
        for i in currentCSV.split(separator: ",").indices {
            tempDictionary.updateValue(max(Float(prevCSV.split(separator: ",")[i])!, Float(currentCSV.split(separator: ",")[i])!), forKey: features[i])
        }
        
        let arr = tempDictionary.sorted { $0.value > $1.value}
        
        beaconNumlist.forEach { beaconNum in
            for i in 0..<beaconNum {
                resultDictionary.updateValue(arr[i].value, forKey: arr[i].key)
            }
            result.append(CSVService.createCSVFromBeaconDictionary(from: resultDictionary))
            resultDictionary.removeAll()
        }
        
        return result
    }
    
}

// MARK: - function for if direction features will be used

extension IndoorLocationManager {
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        self.heading = newHeading.trueHeading
    }
    
    private func headingToDirection(heading: Double) -> Direction {
        if ((315.0 <= heading) && (heading < 360.0)) || ((0.0 <= heading) && (heading < 45)) {
            return Direction.North
        } else if ((45.0 <= heading) && (heading < 135.0)) {
            return Direction.East
        } else if ((135.0 <= heading) && (heading < 225.0)) {
            return Direction.South
        } else {
            return Direction.West
        }
    }
    
    @available(*, deprecated, renamed: "headingToDirection")
    private func headingToFloat(heading: Double) -> Float {
        
        if ((315.0 <= heading) && (heading < 360.0)) || ((0.0 <= heading) && (heading < 45)) {
            return 1.0 //North
        } else if ((45.0 <= heading) && (heading < 135.0)) {
            return 0.0 // East
        } else if ((135.0 <= heading) && (heading < 225.0)) {
            return 2.0 // South
        } else {
            return 3.0 // West
        }
    }
    
    private func createCSVWithPrevAndCurrentBeacons(prev: [Beacon], current: [Beacon], heading: Double) -> String {
        
        var tempDictionary = [String:Float]()
        var resultDictionary = [String:Float]()
        
        let prevCSV = CSVService.makeBeaconInfoCSV(beacon: prev)
        let currentCSV = CSVService.makeBeaconInfoCSV(beacon: current)
        
        // choose max between prev and current Beacons
        for i in currentCSV.split(separator: ",").indices {
            tempDictionary.updateValue(max(Float(prevCSV.split(separator: ",")[i])!, Float(currentCSV.split(separator: ",")[i])!), forKey: features[i])
        }
        
        var arr = tempDictionary.sorted { $0.value > $1.value }
        arr.append(contentsOf: ["direction":headingToFloat(heading: heading)])
        for i in 0..<beaconNum {
            resultDictionary.updateValue(arr[i].value, forKey: arr[i].key)
        }
        resultDictionary.updateValue(headingToFloat(heading: heading), forKey: "direction")
        return CSVService.createCSVFromBeaconDictionary(from: resultDictionary, direction: direction)
    }
    
}

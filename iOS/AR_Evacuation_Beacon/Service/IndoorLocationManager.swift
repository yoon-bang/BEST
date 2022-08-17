//
//  IndoorLocationManager.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/22/22.
//

import Foundation
import CoreLocation

enum Mode {
    case debug
    case collection
    case real
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
    private var locationlist: [String] = []
    
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
        SocketStreamManager.shared.delegate = self
        self.mode = mode
        modelNames.forEach {
            self.classificationModels.append(self.makeClassificationModel(modelName: $0))
        }
    
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
        
        locationlist.append(locations[0])
        
        var location = Position.unknown
        guard locationlist.count >= 5 else {return}
        
        if locationlist.count == 5 {
            var dic: [Position: Int] = [:]
            var new: [Dictionary<Position, Int>.Element] = []
            locationlist.forEach {
                let count = dic[Position(rawValue: $0)!] ?? 0
                dic.updateValue(count + 1, forKey: Position(rawValue: $0)!)
                new = dic.sorted {
                    $0.value > $1.value
                }
            }
            location = new[0].key
        } else {
            location = Position(rawValue: locations[0]) ?? .unknown
        }
        
        // if cannot find the overlapped one, get the location from the Model with 4 beacons
        if mode == .real {
            if previousUserLocation == .unknown {
                previousUserLocation = location
            } else {
                let location = filterErrorWithHeading(previousLocation: previousUserLocation, currentLocation: Position(rawValue: locations[0]) ?? .unknown)
                sendLocationToServerWithSocket(location: location.rawValue)
                NotificationCenter.default.post(name: .movePosition, object: location)
                previousUserLocation = location
            }
        } else if mode == .collection {
            NotificationCenter.default.post(name: .movePosition, object: locations)
        } else {
            NotificationCenter.default.post(name: .movePosition, object: locations[0])
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
            // if candidate cell and direction and heading are same?, that is answer
            if candidateCells.contains(currentLocation) {
                return currentLocation
            } else {
                // if adjacent cell, direction and heading not same, go previous cell
                return previousLocation
            }
        } else {
           // if not adjacent cell, but heading of user and angle between previous location and the result are same, we judge the user moves fast.
            let currentPosPoint = VectorService.transformCellToCGPoint(cellname: currentLocation)
            let prevPosPoint = VectorService.transformCellToCGPoint(cellname: previousLocation)
            let direction = headingToDirection(heading: Double(VectorService.vectorBetween2Points(from: prevPosPoint, to: currentPosPoint).angle))
            
            // then user can go to candidate cell with same direction
            if direction == currentDirection {
                var candidateCells = previousLocation.adjacentCell[direction.rawValue]
                candidateCells.removeAll { $0 == .unknown }
                return candidateCells.first ?? previousLocation
            } else {
                return previousLocation
            }
            
        }
    }
    
    private func sendLocationToServerWithSocket(location: String) {
        SocketStreamManager.shared.sendLocation(location: location)
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
                userLocation(prevBeaconInfo: previousBeacons, currentBeaconInfo: currentBeacons)
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
        
        if locationManager.authorizationStatus != .authorizedAlways {
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

// MARK: - PathDelegate
extension IndoorLocationManager: PathDelegate {
    
    func received(path: Path) {
        print(path)
        NotificationCenter.default.post(name: .path, object: path)
    }
    
}

// MARK: - function for if direction features will be used

extension IndoorLocationManager {
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        self.heading = newHeading.trueHeading
        NotificationCenter.default.post(name: .changeArrowAngle, object: heading)
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

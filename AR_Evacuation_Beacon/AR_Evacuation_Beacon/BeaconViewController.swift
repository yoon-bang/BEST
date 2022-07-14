//
//  BeaconViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//s

import UIKit

import CoreLocation
import UserNotifications
import FirebaseMLModelDownloader
import TensorFlowLite
import CoreMotion

var modelName: String = "ios_model_beacon7"
var beaconNum: Int = 7
var fileName: String = "ios_clf_data7R01"
var debugMode: Bool = true

class BeaconViewController: UITableViewController, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    
    let locationManager = CLLocationManager()
    private let sections: [String] = ["Location Estimation", "Beacons"]
    private let beaconManager = BeaconManager.shared
    private var rssiInterpreter = RssiInterpreter()
    private var beaconRegion: CLBeaconRegion!
    private var beaconRegionConstraint: CLBeaconIdentityConstraint!
    private var foundBeacons = [CLBeacon]()
    private var beaconInfoArr = [Beacon]()
    private var tableviewBeacon = [Beacon]()
    private var previousBeaconInfoArr = [Beacon]()
    private var isRanging = false
    private var threshold: Float = 15.0
    private var locationList: [String] = []
    private var csvlist = [String]()
    private var locationlistForCSV: [String] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        locationManager.delegate = self
        beaconConfiguration()
        tableView.register(UINib(nibName: LabelTableViewCell.nibName, bundle: nil), forCellReuseIdentifier: LabelTableViewCell.identifier)
        // MARK: - Debugmode - ViewDidLoad
        if debugMode {
            loadLocationsFromCSV()
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // MARK: - Debugmode - ViewWillAppear
        if debugMode {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                for csv in self.csvlist {
                    guard let location = self.rssiInterpreter.classifyLocationOfUser(with: csv) else {
                        return
                    }
                    print("정답: ", location)
//                    print("한번더 맵핑: ", classificationDic[location] ?? "unknown")
                }
            }
        }
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0 {
            return 1
        } else {
            return tableviewBeacon.count
        }
    }
    
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 0 {
            return sections[section]
        } else {
            return isRanging ? "Ranging Active" : "Ranging Inactive"
        }
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let estimationCell = tableView.dequeueReusableCell(withIdentifier: LabelTableViewCell.identifier, for: indexPath) as! LabelTableViewCell
        let cell = tableView.dequeueReusableCell(withIdentifier: "BeaconCell", for: indexPath) as! BeaconCell
        
        if indexPath.section == 0 {
            var result = ""
            if locationlistForCSV.count < 8 {
                locationlistForCSV.forEach { result += "\($0) " }
                estimationCell.estimationLabel.text = result
            } else {
                locationlistForCSV[locationlistForCSV.count-8 ..< locationlistForCSV.count]
                    .map { $0 }
                    .forEach { result += "\($0) "}
                estimationCell.estimationLabel.text = result
            }
            return estimationCell
        } else {
            let beacon = tableviewBeacon[indexPath.row]
            cell.idLabel.text = "\(beacon.uuid)"
            cell.majorLabel.text = "Major: \(beacon.major)"
            cell.minorLabel.text = "Minor: \(beacon.minor)"
            cell.rssiLabel.text = "rssi: \(beacon.rssi)"
            cell.filteredRssiLabel.text = "filtered: \(beacon.filteredRssi)"
            return cell
        }
        
    }
    
}

// MARK: - localization
@available(iOS 13.0, *)
extension BeaconViewController {
    
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
    
    func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
        print("Did determine state for region \(region)")
        if state == .inside {
            locationManager.startRangingBeacons(satisfying: beaconRegionConstraint)
            print("inside")
            isRanging = true
        } else {
            isRanging = false
            print("outside")
        }
        
        tableView.reloadData()
    }
    
    func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        print("Did start monitoring region: \(region)\n")
        tableView.reloadData()
    }
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        locationManager.startRangingBeacons(satisfying: beaconRegionConstraint)
        print("didEnter")
        tableView.reloadData()
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        locationManager.stopRangingBeacons(satisfying: beaconRegionConstraint)
        print("didExit")
        foundBeacons = []
        tableView.reloadData()
    }
    
    func locationManager(_ manager: CLLocationManager, didRange beacons: [CLBeacon], satisfying beaconConstraint: CLBeaconIdentityConstraint) {
        
        // foundBeacons -> 찾은 비콘
        foundBeacons = beacons
        beaconInfoArr = foundBeacons.map { beacon in
            return Beacon(beacon: beacon)
        }
        
        tableviewBeacon = beaconInfoArr.filter({
            return $0.rssi != 0
        })
        
        // MARK: - 2번으로 해결하기
        if previousBeaconInfoArr.count == 0{
            previousBeaconInfoArr = beaconInfoArr
        } else {
            if debugMode {
                for csv in csvlist {
                    guard let location = rssiInterpreter.classifyLocationOfUser(with: csv) else {
                        return
                    }
                    print("정답: ", location)
//                    print("한번더 맵핑: ", classificationDic[location] ?? "unknown")
                }
            } else {
                let csv =  makeBeaconInfoCSV(prev: previousBeaconInfoArr, current: beaconInfoArr)
                if locationList.count != 5 {
                    guard let location = rssiInterpreter.classifyLocationOfUser(with: csv) else {
                        locationList.append("unknown")
                        return
                    }
                    let locationLabel = classificationDic[location] ?? "unknown"
                    SocketIOManager.shared.sendLocation(location: locationLabel)
                    print(locationLabel)
                    locationlistForCSV.append(locationLabel)
                    locationList.append(locationLabel)
                }
                
                previousBeaconInfoArr.removeAll()
                
                if locationList.count == 3 {
                    //칼만필터 초기화
//                    beaconInfoArr.forEach {
//                        $0.reinitFilter()
//                    }
                } else if locationList.count == 5 {
                    print("5개 최빈값", mode(array: locationList))
                    locationList.removeAll()
                }
            }
        }
        tableView.reloadData()
    }
    
}

// MARK: - Private function
@available(iOS 13.0, *)
extension BeaconViewController {
    
    @IBAction private func saveCSV(_ sender: UIBarButtonItem) {
        let alert = UIAlertController(title: "finish, CSV has been saved", message: nil, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "yes", style: .default, handler: { _ in
            // 1번값 삭제
            self.locationlistForCSV.removeFirst()
            self.makeLocationInfoCSV(locationList: self.locationlistForCSV)
            self.locationlistForCSV.removeAll()
        }))
        self.present(alert, animated: true)
    }
    
    private func loadLocationsFromCSV() {
        let path = Bundle.main.path(forResource: fileName, ofType: "csv")!
        var arrlist = CSVService.parseCSVAt(url: URL(fileURLWithPath: path))
        arrlist.removeLast()
        for i in arrlist.indices {
            if i == 0 { continue }
            csvlist.append(CSVService.arrToCSV(arr: arrlist[i]))
        }
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
    
    private func makeLocationInfoCSV(locationList: [String]) {
        CSVService.createLocationCSV(with: locationList)
    }
    
    private func makeBeaconInfoCSV(prev: [Beacon], current: [Beacon]) -> String {
        
        let features = ["001","002","003","004","005","006","007","008","009","010",
                        "011","012","013","014","015","016","017","018","019","020",
                        "021", "022"]
        
        var result = ""
        var dct = [String:Float]()
        
        let prevCSV = makeBeaconInfoCSV(beacon: prev)
        let currentCSV = makeBeaconInfoCSV(beacon: current)
        for i in currentCSV.split(separator: ",").indices {
            dct.updateValue(Float(max(prevCSV.split(separator: ",")[i], currentCSV.split(separator: ",")[i])) ?? -1717.0, forKey: features[i])
        }
        var newDct = [String:Float]()
        let arr = dct.sorted { $0.value > $1.value}
        
        
        for i in 0..<beaconNum {
            newDct.updateValue(arr[i].value, forKey: arr[i].key)
        }
        
        return CSVService.createDic(from: newDct)
    }
    
    private func mode(array: [String]) -> String {
        var dictionary = [String: Int]()
        
        for index in array.indices {
            if let count = dictionary[array[index]] {
                dictionary[array[index]] = count+1
            } else {
                dictionary[array[index]] = 1
            }
        }
        
        return dictionary.max { $0.value < $1.value }!.key
    }
    
    private func makeBeaconInfoCSV(beacon: [Beacon]) -> String {
        var dct = [String:Float]()
        for beacon in beacon {
            if beacon.filteredRssi < -90 {
                dct.updateValue(-200 as Float, forKey: "\(beacon.getBeaconID())")
            } else {
                dct.updateValue(beacon.filteredRssi as Float, forKey: "\(beacon.getBeaconID())")
            }
        }
        return CSVService.createDic(from: dct)
    }
    
}

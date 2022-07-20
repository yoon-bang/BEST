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

// Build setting
let modelName: String = "ios_model_beacon7"
let beaconNum: Int = 7
let fileName: String = "ios_clf_data7S05"
let features = ["001","002","003","004","005","006","007","008","009","010",
                "011","012","013","014","015","016","017","018","019","020",
                "021", "022"]
let debugMode: Bool = false

class BeaconViewController: UITableViewController, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    
    // Need for detect beacon
    let locationManager: CLLocationManager = {
        $0.requestWhenInUseAuthorization()
        $0.startUpdatingHeading()
        return $0
    }(CLLocationManager())
    
    
    // MARK: - Private properties
    
    private let sections: [String] = ["Location Estimation", "Beacons"]
    
    private let beaconManager = BeaconManager.shared
    private var classificationModel = ModelInterpreter()
        
    private var beaconRegion: CLBeaconRegion!
    private var beaconRegionConstraint: CLBeaconIdentityConstraint!
    private var foundBeacons = [CLBeacon]()
    private var currentBeaconInfoArr = [Beacon]()
    private var tableviewBeacon = [Beacon]()
    private var previousBeaconInfoArr = [Beacon]()
    
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
                    guard let location = self.classificationModel.classifyLocationOfUser(with: csv) else {
                        return
                    }
                    print("answer: ", location)
                }
            }
        }
    }
    
    // MARK: - TableViewDelegate & TableViewDatasource
    // Use for show detected beacons
    
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
        return sections[section]
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let estimationCell = tableView.dequeueReusableCell(withIdentifier: LabelTableViewCell.identifier, for: indexPath) as! LabelTableViewCell
        let cell = tableView.dequeueReusableCell(withIdentifier: BeaconCell.identifier, for: indexPath) as! BeaconCell
        
        // section0 shows the estimated label from model
        // section1 shows the detected beacons
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
        
        tableView.reloadData()
    }
    
    // start monitoring beacons
    func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        print("Did start monitoring region: \(region)\n")
        tableView.reloadData()
    }
    
    // call when the user enter the beacon area
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        locationManager.startRangingBeacons(satisfying: beaconRegionConstraint)
        print("didEnter")
        tableView.reloadData()
    }
    
    // call when the user exit the beacon area
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        locationManager.stopRangingBeacons(satisfying: beaconRegionConstraint)
        print("didExit")
        foundBeacons = []
        tableView.reloadData()
    }
    
    // detect the beacons
    func locationManager(_ manager: CLLocationManager, didRange beacons: [CLBeacon], satisfying beaconConstraint: CLBeaconIdentityConstraint) {
        
        foundBeacons = beacons
        
        currentBeaconInfoArr = foundBeacons.map { beacon in
            return Beacon(beacon: beacon)
        }
        
        rearrangeKalmanFilter(prev: currentBeaconInfoArr, current: [])
        
        tableviewBeacon = currentBeaconInfoArr.filter({
            return $0.rssi != 0
        })
        
        // MARK: - 2번으로 해결하기
        if previousBeaconInfoArr.count == 0 {
            
            previousBeaconInfoArr = currentBeaconInfoArr
             
        } else {
            if !debugMode {
                
                let csv =  createCSVWithPrevAndCurrentBeacons(prev: previousBeaconInfoArr, current: currentBeaconInfoArr)
                guard let location = classificationModel.classifyLocationOfUser(with: csv) else {
                    locationList.append("unknown")
                    return
                }
                
                // Send location label to Server
                SocketIOManager.shared.sendLocation(location: location)
                
                locationlistForCSV.append(location)
                previousBeaconInfoArr.removeAll()
                
                //For Debug
                locationList.append(location)
            }
        }
        tableView.reloadData()
    }
    
}

// MARK: - Private function
@available(iOS 13.0, *)
extension BeaconViewController {
    
    // MARK: - Beacon related private function
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
    
    // MARK: - Kalman Filter related private function
    private func rearrangeKalmanFilter(prev: [Beacon], current: [Beacon]) {
        
        var newbeaconArr = prev
        
        newbeaconArr.sort { $0.filteredRssi > $1.filteredRssi}
        newbeaconArr = Array(newbeaconArr[0..<beaconNum])
        
        for beacon in newbeaconArr {
            beaconManager.beaconDictionary.updateValue(beacon, forKey: beacon.beaconID)
        }
        
        beaconManager.updateKalmanFilter()
        
    }
    
    // MARK: - IBAction
    @IBAction private func saveCSV(_ sender: UIBarButtonItem) {
        
        let alert = UIAlertController(title: "finish, CSV has been saved", message: nil, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "yes", style: .default, handler: { _ in
            self.locationlistForCSV.removeFirst()
            self.makeLocationInfoCSV(locationList: self.locationlistForCSV)
            self.locationlistForCSV.removeAll()
        }))
        
        self.present(alert, animated: true)
    }
    
    // MARK: - private function related to CSV
    
    private func loadLocationsFromCSV() {
        
        let path = Bundle.main.path(forResource: fileName, ofType: "csv")!
        var arrlist = CSVService.parseCSVAt(url: URL(fileURLWithPath: path))
        arrlist.removeLast()
        
        for i in arrlist.indices {
            if i == 0 { continue }
            csvlist.append(CSVService.arrToCSV(arr: arrlist[i]))
        }
    }
    
    private func makeLocationInfoCSV(locationList: [String]) {
        
        CSVService.createLocationCSV(with: locationList)
        
    }
    
    private func createCSVWithPrevAndCurrentBeacons(prev: [Beacon], current: [Beacon]) -> String {
        
        var tempDictionary = [String:Float]()
        var resultDictionary = [String:Float]()
        
        let prevCSV = makeBeaconInfoCSV(beacon: prev)
        let currentCSV = makeBeaconInfoCSV(beacon: current)
        
        // choose max between prev and current Beacons
        for i in currentCSV.split(separator: ",").indices {
            tempDictionary.updateValue(Float(max(prevCSV.split(separator: ",")[i], currentCSV.split(separator: ",")[i])) ?? -1717.0, forKey: features[i])
        }
        
        let arr = tempDictionary.sorted { $0.value > $1.value}
        
        for i in 0..<beaconNum {
            resultDictionary.updateValue(arr[i].value, forKey: arr[i].key)
        }
        
        return CSVService.createCSVFromBeaconDictionary(from: resultDictionary)
    }
    
    private func makeBeaconInfoCSV(beacon: [Beacon]) -> String {
        var dct = [String:Float]()
        for beacon in beacon {
            if beacon.filteredRssi < -90 {
                dct.updateValue(-200 as Float, forKey: "\(beacon.beaconID)")
            } else {
                dct.updateValue(beacon.filteredRssi as Float, forKey: "\(beacon.beaconID)")
            }
        }
        return CSVService.createCSVFromBeaconDictionary(from: dct)
    }
    
}

// MARK: - Extension For All

func max(_ left: Beacon, _ right: Beacon) -> Beacon {
    return left.rssi > right.rssi ? left : right
}

func mode(array: [String]) -> String {
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

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
let modelNames: [String] = ["ios_beacon4", "ios_beacon_5concat", "ios_beacon7concat", "ios_beacon22"]
let beaconNumlist: [Int] = [4,5,7,22]
let beaconNum: Int = 4
let fileName: String = "ios_clf_data4A03"
let features = ["001","002","003","004","005","006","007","008","009","010",
                "011","012","013","014","015","016","017","018","019","020",
                "021", "022"]
let direction = false


class BeaconViewController: UITableViewController, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    
    // Need for detect beacon
    let locationManager: CLLocationManager = {
        $0.requestWhenInUseAuthorization()
        $0.startUpdatingHeading()
        return $0
    }(CLLocationManager())
    
    
    // MARK: - Private properties
    private let beaconManager = BeaconManager.shared
    private let indoorLocationManager = IndoorLocationManager(mode: .collection)
        
    private var currentBeaconInfoArr = [Beacon]()
    private var tableviewBeacon = [Beacon]()
    private var previousBeaconInfoArr = [Beacon]()
    
    private let sections: [String] = ["Location Estimation", "Beacons"]
    private var csvlist = [String]()
    private var estimatedLocations: [String: [String]] = ["4":[], "5":[], "7": [], "22": []]
    
    override func viewDidLoad() {
        
        super.viewDidLoad()
        tableView.register(UINib(nibName: LabelTableViewCell.nibName, bundle: nil), forCellReuseIdentifier: LabelTableViewCell.identifier)
        
        NotificationCenter.default.addObserver(self, selector: #selector(currentBeacons(_:)), name: .beacons, object: nil)
        
        if indoorLocationManager.mode == .real {
            NotificationCenter.default.addObserver(self, selector: #selector(currentLocation(_:)), name: .movePosition, object: nil)
        } else {
            NotificationCenter.default.addObserver(self, selector: #selector(currentLocations(_:)), name: .movePosition, object: nil)

        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }
    
    @objc private func currentBeacons(_ noti: Notification) {
        guard let beacons = noti.object as? [Beacon] else {return}
        tableviewBeacon = beacons
        tableView.reloadData()
    }
    
    // TODO: 4개 받아올때, 1개 받아올때 나눠서 비동기 코드 작성하기
    
    @objc private func currentLocations(_ noti: Notification) {
        guard let locations = noti.object as? [String] else {return}
        
        locations.indices.forEach { (index) in
            var locationlist = estimatedLocations["\(beaconNumlist[index])"] ?? []
            locationlist.append(locations[index])
            estimatedLocations.updateValue(locationlist, forKey: "\(beaconNumlist[index])")
        }
        tableView.reloadData()
    }
    
    @objc private func currentLocation(_ noti: Notification) {
        guard let location = noti.object as? String else {return}
        // TODO: 1개일때, 어떻게진행할것인지?
        // TODO: 5개를 모아서 할지?
    }
    
    
    
    // MARK: - TableViewDelegate & TableViewDatasource
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0 {
            return estimatedLocations.keys.count
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
            guard let locations = estimatedLocations["\(beaconNumlist[indexPath.row])"] else { return cell }
            if locations.count < 8 {
                locations.forEach { result += "\($0) " }
                estimationCell.estimationLabel.text = result
            } else {
                locations[locations.count - 8 ..< locations.count]
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

// MARK: - Private function
@available(iOS 13.0, *)
extension BeaconViewController {

    // MARK: - IBAction
    @IBAction private func saveCSV(_ sender: UIBarButtonItem) {
        
        let alert = UIAlertController(title: "finish, CSV has been saved", message: nil, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "yes", style: .default, handler: { _ in
            
            var tempArr = self.estimatedLocations.sorted { $0.key < $1.key }
            tempArr.indices.forEach {
                tempArr[$0].value.removeFirst()
                CSVService.saveLocationCSV(key: tempArr[$0].key, value: tempArr[$0].value)
            }
            
            self.estimatedLocations.removeAll()
        }))
        
        self.present(alert, animated: true)
    }
    
   

}

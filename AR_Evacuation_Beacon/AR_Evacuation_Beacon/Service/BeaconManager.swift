//
//  BeaconManager.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import Foundation

class BeaconManager {
    
    private init() { }
    
    static let shared = BeaconManager()
    
    var beaconKalman: [String:KalmanFilter] = [:]
    var beaconDictionary: [String:Beacon] = [:]
    var threshold: Float = 8.0
    
    func updateKalmanFilter() {
        
        let sorted = beaconDictionary.sorted{ $0.value.filteredRssi > $1.value.filteredRssi }
        
        for beacon in sorted[0..<beaconNum] {
            if beacon.value.filteredRssi > -200 && beacon.value.rssi != 0 && abs(beacon.value.filteredRssi - Float(beacon.value.rssi)) > threshold {
                
                beaconDictionary.forEach { id, beacon in beacon.reinitFilter() }
//                print("update kalmanfiltered")
                break
            }
        }
        
    }
    
}

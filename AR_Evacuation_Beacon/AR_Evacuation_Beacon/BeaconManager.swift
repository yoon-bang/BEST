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
    
    var beaconKalman: [String:KalmanFilter] = [:] // 기존비콘의 칼만필터 저장하기
    var beaconDic: [String:Beacon] = [:] //전에 있던거 저장하기
    
    func updateKalmanFilter() {
        let sorted = beaconDic.sorted{ $0.value.filteredRssi > $1.value.filteredRssi }
        for beacon in sorted[0..<beaconNum] {
            if beacon.value.filteredRssi > -200 && beacon.value.rssi != 0 && abs(beacon.value.filteredRssi - Float(beacon.value.rssi)) > 8 {
                print("초기화되는놈", beacon.value.getBeaconID(), "filtered", beacon.value.filteredRssi, "raw", beacon.value.rssi)
                beaconDic.forEach { id, beacon in
                    beacon.reinitFilter()
                }
                print("update kalmanfiltered")
                break
            }
        }
        
    }
    
}

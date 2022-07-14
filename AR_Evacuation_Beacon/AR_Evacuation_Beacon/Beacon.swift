//
//  Beacon.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import Foundation
import CoreLocation

@available(iOS 13.0, *)

class Beacon {
    
    // initializer
    init(beacon: CLBeacon) {
        rssi = beacon.rssi
        major = beacon.major
        minor = beacon.minor
        uuid = beacon.uuid
        timestamp = beacon.timestamp
        proximity = beacon.proximity
        filter = KalmanFilter(R: 0.001, Q: 2)
    }
    
    var rssi: Int
    var filter: KalmanFilter
    var major: NSNumber
    var minor: NSNumber
    var uuid: UUID
    var timestamp: Date
    var proximity: CLProximity
    
    var filteredRssi: Float {
        get {
            //필터 불러오기
            if rssi != 0 {
                filter = BeaconManager.shared.beaconKalman["\(getBeaconID())"] ?? KalmanFilter(R: 0.001, Q: 2)
                BeaconManager.shared.beaconKalman.updateValue(filter, forKey: "\(getBeaconID())")
                return filter.filter(signal: Float(rssi))
            } 
            return -200.0
        }
    }
    
    func reinitFilter() {
        filter = KalmanFilter(R: 0.001, Q: 2)
        BeaconManager.shared.beaconKalman.updateValue(filter, forKey: "\(getBeaconID())")
    }
    
    func getBeaconID() -> String {
        if Int(exactly: self.minor)! < 10 {
            return "\(self.major)0\(self.minor)"
        }
        return "\(self.major)\(self.minor)"
    }
    
    func address(of object: UnsafeRawPointer) -> String{
        let address = Int(bitPattern: object)
        return String(format: "%p", address)
    }
}

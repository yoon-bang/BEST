//
//  Beacon.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import Foundation
import CoreLocation

var classificationDic = [
                         "S01":"A01",
                         "S04":"A02",
                         "S05":"A03",
                         "S06":"A04",
                         "S07":"A05",
                         "S08":"A06",
                         "S09":"A07",
                         "E01":"A08",
                         "E02":"A09",
                         "S02":"A10",
                         "S03":"A11",
                         "E03":"E01",
                         "E04":"E02",
                         "E05":"E03",
                         "R01":"E04",
                         "R02":"E05",
                         "R03":"H01",
                         "R04":"H02",
                         "R05":"R01",
                         "R06":"R02",
                         "H01":"R03",
                         "H02":"R04",
                         "A01":"R05",
                         "A02":"R06",
                         "A03":"S01",
                         "A04":"S02",
                         "A05":"S03",
                         "A06":"S04",
                         "A07":"S05",
                         "A08":"S06",
                         "A09":"S07",
                         "A10":"S08",
                         "A11":"S09",
                         "U01":"U01"
                         ]

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

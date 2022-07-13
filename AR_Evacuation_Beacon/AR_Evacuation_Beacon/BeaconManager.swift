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
    var beaconDic: [String:Float] = [:] //전에 있던거 저장하기

}

//
//  Operator+Extension.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/22/22.
//

import Foundation

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

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

extension Array {
    func mode<T: Hashable>() -> T {
        var dictionary = [T: Int]()
        for index in 0..<self.count {
            let a = self[index] as! T
            if let count = dictionary[a] {
                dictionary[self[index] as! T] = count+1
            } else {
                dictionary[self[index] as! T] = 1
            }
        }
        
        return dictionary.max { $0.value < $1.value }!.key
    }
}


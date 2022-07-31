//
//  Path.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/26/22.
//

import Foundation

struct Path: Equatable {
    
    init(path: [Position] = [], conjestionCell: [Position] = [], fireCell: [Position] = [], firePredictedCell: [Position] = []) {
        self.path = path
        self.conjestionCell = conjestionCell
        self.fireCell = fireCell
        self.firePredictedCell = firePredictedCell
    }
    
    var path: [Position] = []
    var conjestionCell: [Position] = []
    var fireCell: [Position] = []
    var firePredictedCell: [Position] = []
}



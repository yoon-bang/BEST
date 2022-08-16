//
//  NavigationDirection.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 8/16/22.
//

import Foundation

enum NavigationDirection: String, CustomStringConvertible {
    var description: String {
        switch self {
        case .forward:
            return "GO STRAIGHT"
        case .backward:
            return "GO BACK"
        case .danger:
            return "DANGER"
        case .goUpstair:
            return "GO UPSTAIR"
        case .goDownstair:
            return "GO DOWNSTAIR"
        case .stair:
            return "STAIRS, CAUTION"
        case .left:
            return "TURN LEFT"
        case .right:
            return "TURN RIGHT"
        }
    }
    
    case forward
    case backward
    case left
    case right
    case danger
    case stair
    case goUpstair
    case goDownstair
    
}

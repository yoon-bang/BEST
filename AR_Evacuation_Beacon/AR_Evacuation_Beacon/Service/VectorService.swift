//
//  VectorService.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/27/22.
//

import UIKit

class VectorService {
    
    static func changeDirection(degree: Float) -> Float {
        return Float(Float(270 - degree).degreesToRadians)
    }
    
    static func headingToDirection(degree: Float) -> Direction {
        var degree = degree
        if degree < 0 {
            degree += 360.0
        }
        if ((315.0 <= degree) && (degree < 360.0)) || ((0.0 <= degree) && (degree < 45)) {
            return Direction.North
        } else if ((45.0 <= degree) && (degree < 135.0)) {
            return Direction.East
        } else if ((135.0 <= degree) && (degree < 225.0)) {
            return Direction.South
        } else {
            return Direction.West
        }
    }
    
    
    static func vectorBetween2Points(from: CGPoint, to: CGPoint) -> (angle: Float, dist: Double) {
        var degree: Float = 0.0
        let tan = atan2(from.x - to.x, from.y - to.y) * 180 / .pi
        if tan < 0 {
            degree = Float(-tan) + 180.0
        } else {
            degree = 180.0 - Float(tan)
        }
        return (angle: degree, dist: sqrt(pow(from.x - to.x, 2) + pow(from.y - to.y, 2)))
        
    }
    
    static func transformCellToCGPoint(cellname: Position) -> CGPoint {
        
        var start: (x: CGFloat, y: CGFloat) = (0, 0)
        var end: (x: CGFloat, y: CGFloat) = (0, 0)
        
        if let firstFloorCellpoints = mapDic[cellname] {
            start = firstFloorCellpoints[0]
            end = firstFloorCellpoints[2]
        } else if let secondFloorCellpoints = micDic2[cellname] {
            start = secondFloorCellpoints[0]
            end = secondFloorCellpoints[2]
        } else if let baseFloorCellPoints = micDic0[cellname] {
            start = baseFloorCellPoints[0]
            end = baseFloorCellPoints[2]
        } else {
            return CGPoint(x: 0, y: 0)
        }
        
        let width = abs(start.x - end.x) / 2
        let height = abs(start.y - end.y) / 2
        
        return CGPoint(x: (start.x + width) * 10, y: (start.y + height) * 10)
    }
}

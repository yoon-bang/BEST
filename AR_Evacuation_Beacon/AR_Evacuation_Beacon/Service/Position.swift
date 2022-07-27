//
//  Position.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/23/22.
//

import UIKit

let mapDic: [Position: [(CGFloat, CGFloat)]] = [
    .S03: [(0.6, 0.6), (5,0.6), (5,6.4), (0.6,6.4)],
    .S02: [(5,0.6),(13, 0.6),(13, 6.4), (5, 6.4)],
    .S01: [(5, 6.4), (13, 6.4), (13,12), (5, 12)],
    .E01: [(13, 0.6), (25, 0.6), (25, 12), (13, 12)],
    .R03: [(25,0.6), (38.5,0.6), (38.5,11), (25,11)],
    .R02: [(25,11), (38.5,11), (38.5, 20.5), (25, 20.5)],
    .R04: [(0.6, 12), (16.7,12), (16.7, 24), (0.6, 24)],
    .A01: [(16.7, 12), (25, 12), (25, 21.6), (16.7, 21.6)],
    .A02: [(16.7, 21.6), (24.4, 21.6), (24.4, 30.5), (16.7, 30.5)],
    .R01: [(0.6, 24), (16.7, 24), (16.7, 51.2), (0.6, 51.2)],
    .A08: [(24.4, 21.6), (38.5, 21.6), (38.5, 43.8), (24.4, 43.8)],
    .A03: [(16.7, 30.5), (24.4, 30.5), (24.4,39.2), (16.7, 39.2)],
    .A04: [(16.7, 39.2), (24.4,39.2), (24.4, 48.2), (16.7, 48.2)],
    .A05: [(16.7, 48.2),(24.4, 48.2), (24.4, 57), (16.7, 57)],
    .A06: [(16.7, 57), (24.4, 57), (24.4, 66), (16.7, 66)],
    .A07: [(16.7, 66), (24.4, 66),(24.4, 75), (16.7, 75)],
    .A09: [(24.4, 43.8),(38.5, 43.8), (38.5, 66), (24.4, 66)],
    .A10: [(24.4, 66), (31.5, 66), (31.5, 76), (24.4, 76)],
    .A11: [(31.5, 66), (38.5, 66), (38.5, 76), (31.3, 76)],
    .E03: [(34, 76), (38.5, 76), (38.5, 84), (34, 84)],
    .R05: [(0.6, 51.2), (16.7, 51.2), (16.7,61), (0.6, 61)],
    .H02: [(15, 75), (24.5, 75), (24.5, 84), (18.2, 84), (18.2, 79), (15, 79)],
    .S07: [(5, 73.3), (15, 73.3), (15, 79), (5, 79)],
    .S06: [(5, 79), (18.2, 79), (18.2, 84), (5, 84)],
    .E02: [(0.6, 73.3), (5, 73.3), (5, 84), (0.6, 84)]
]

let micDic2: [Position: [(CGFloat, CGFloat)]] = [
    .S03 : [(0, 0), (5, 0), (5, 11.7), (0, 11.7)],
    .S02 : [(5, 0), (14, 0), (14, 6) ,(5, 6) ],
    .S04 : [(5, 6), (14, 6), (14, 11.7), (5, 11.7) ],
    .H01 : [(14, 0), (28.5, 0), (28.5, 11.7), (14, 11.7)],
    .S05 : [(0, 68), (5, 68), (5, 84), (0, 84)],
    .S06 : [(0, 79), (15.7, 79), (15.7, 84), (0, 84)]
]


let micDic0: [Position: [(CGFloat, CGFloat)]] = [
    .E02:[(0.6, 73.2), (5, 73.2), (5, 84), (0.6, 84)],
    .S08:[(5, 79), (11.6, 79), (11.6, 84), (5, 84)],
    .S09:[(11.6, 74), (16, 74), (16, 84), (11.6, 84)],
    .U01:[(7.7, 65), (16,65), (16, 74), (11.6, 74), (11.6, 79), (7.7, 79)]
]

enum Position: String {
    case A01
    case A02
    case A03
    case A04
    case A05
    case A06
    case A07
    case A08
    case A09
    case A10
    case A11
    case E01
    case E02
    case E03
    case R01
    case R02
    case R03
    case R04
    case R05
    case S01
    case S02
    case S03
    case S04
    case S05
    case S06
    case S07
    case S08
    case S09
    case U01
    case H01
    case H02
    case unknown
    
    // 남동북서 [180, 90, 0, 270]
    var adjacentCell: [[Position]] {
        switch self {
        case .A01:
            return [[.E01], [.R04], [.A02], [.R02]]
        case .A02:
            return [[.A01], [.unknown], [.A03], [.A08]]
        case .A03:
            return [[.A02], [.R01], [.A04], [.A08]]
        case .A04: // 보류
            return [[.A03], [.unknown], [.A05], [.A08, .A09]]
        case .A05:
            return [[.A04], [.R05], [.A06], [.A09]]
        case .A06:
            return [[.A05], [.unknown], [.A07], [.A09]]
        case .A07:
            return [[.A06], [.unknown], [.H02], [.A10]]
        case .A08:
            return [[.A02, .A03], [.A03, .A04], [.A09], [.unknown]]
        case .A09:
            return [[.A08], [.A04, .A05, .A06, .A07], [.A10, .A11, .A07], [.unknown]]
        case .A10:
            return [[.A09], [.A07], [.E03], [.A11, .E03]]
        case .A11:
            return [[.A09], [.A10], [.E03], [.E03]]
        case .E01:
            return [[.unknown], [.S01, .S02], [.A01], [.R03]]
        case .E02:
            return [[.unknown], [.unknown], [.unknown], [.S07, .S06]]
        case .E03:
            return [[.A11], [.unknown], [.unknown], [.unknown]]
        case .R01:
            return [[.unknown], [.unknown], [.unknown], [.A03, .A04, .A02]]
        case .R02:
            return [[.unknown], [.A01, .A02], [.unknown], [.unknown]]
        case .R03:
            return [[.unknown], [.E01], [.unknown], [.unknown]]
        case .R04:
            return [[.unknown], [.unknown], [.unknown], [.A01, .A02]]
        case .R05: // 남동북서
            return [[.A04, .A05], [.unknown], [.A05, .A06, .A07], [.A05, .A06, .A04]]
        case .S01: // 나가는것
            return [[.unknown], [.unknown], [.unknown], [.E01]]
        case .S02:
            return [[.unknown], [.S03], [.unknown], [.E01]]
        case .S03:
            return [[.unknown], [.unknown], [.unknown], [.S02, .S04]]
        case .S04:
            return [[.unknown], [.S03], [.unknown], [.H01]]
        case .S05:
            return [[.unknown], [.unknown], [.unknown], [.S06]]
        case .S06:
            return [[.S05], [.unknown], [.unknown], [.H02]]
        case .S07:
            return [[.unknown], [.E02], [.unknown], [.H02]]
        case .S08:
            return [[.unknown], [.E02], [.unknown], [.S09]]
        case .S09:
            return [[.U01], [.S08], [.unknown], [.unknown]]
        case .U01:
            return [[.unknown], [.unknown], [.S09], [.unknown]]
        case .H01:
            return [[.unknown], [.S04], [.unknown], [.unknown]]
        case .H02:
            return [[.A07], [.S07, .S06], [.unknown], [.unknown]]
        default:
            return []
        }
    }
    
}

//
//  Position.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/23/22.
//

import Foundation

enum Position {
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
    
    // 남동북서
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
            return [[.A02], [.A03, .A04], [.A09], [.unknown]]
        case .A09:
            return [[.A08], [.A04, .A05, .A06], [.A10, .A11], [.unknown]]
        case .A10:
            return [[.A09], [.A07], [.unknown], [.A11]]
        case .A11:
            return [[.A09], [.A10], [.E03], [.unknown]]
        case .E01:
            return [[.unknown], [.S01, .S02], [.A01], [.R03]]
        case .E02:
            return [[.unknown], [.unknown], [.unknown], [.S07, .S06]]
        case .E03:
            return [[.A11], [.unknown], [.unknown], [.unknown]]
        case .R01:
            return [[.unknown], [.unknown], [.unknown], [.A03]]
        case .R02:
            return [[.unknown], [.A01], [.unknown], [.unknown]]
        case .R03:
            return [[.unknown], [.E01], [.unknown], [.unknown]]
        case .R04:
            return [[.unknown], [.unknown], [.A01], [.unknown]]
        case .R05:
            return [[.unknown], [.unknown], [.A05], [.unknown]]
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

//
//  CSVService.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

/*
 This Singleton class is for make CSV file of Beacons
 */


import Foundation

class CSVService {
    
    static func createCSVFromBeaconDictionary(from dct: [String: Float]) -> String {
        var csvString = ""
        
        csvString = csvString.appending("\(String(describing: dct["001"] ?? -200.0)),\(String(describing: dct["002"] ?? -200.0)),\(String(describing: dct["003"] ?? -200.0)),\(String(describing: dct["004"] ?? -200.0)),\(String(describing: dct["005"]  ?? -200.0)),\(String(describing: dct["006"]  ?? -200.0)),\(String(describing: dct["007"]  ?? -200.0)),\(String(describing: dct["008"]  ?? -200.0)),\(String(describing: dct["009"]  ?? -200.0)),\(String(describing: dct["010"]  ?? -200.0)),\(String(describing: dct["011"]  ?? -200.0)),\(String(describing: dct["012"]  ?? -200.0)),\(String(describing: dct["013"]  ?? -200.0)),\(String(describing: dct["014"]  ?? -200.0)),\(String(describing: dct["015"]  ?? -200.0)),\(String(describing: dct["016"]  ?? -200.0)),\(String(describing: dct["017"]  ?? -200.0)),\(String(describing: dct["018"]  ?? -200.0)),\(String(describing: dct["019"]  ?? -200.0)),\(String(describing: dct["020"]  ?? -200.0)),\(String(describing: dct["021"]  ?? -200.0)),\(String(describing: dct["022"]  ?? -200.0))")
        
        return csvString
    }
    
    static func createLocationCSV(with locations: [String]) {
        
        var csvString = "\("estimated")\n"
        locations.forEach { csvString = csvString.appending("\($0)\n") }
        
        let fileManager = FileManager.default
        do {
            let path = try fileManager.url(for: .documentDirectory, in: .allDomainsMask, appropriateFor: nil, create: false)
            let fileURL = path.appendingPathComponent("estimating Location_\(Date()).csv")
            try csvString.write(to: fileURL, atomically: true, encoding: .utf8)
        } catch {
            print("error creating file")
        }
        
    }
    
    static func arrToCSV(arr: [String]) -> String {
        var result = ""
        for i in arr.indices{
            if i > 1 {
                result += "\(arr[i]),"
            }
        }
        if !result.isEmpty {
            result.removeLast()
        }
        return result
    }
    
    static func parseCSVAt(url:URL) -> [[String]] {
        var result = [[String]]()
        do {
            
            let data = try Data(contentsOf: url)
            let dataEncoded = String(data: data, encoding: .utf8)
            
            if let dataArr = dataEncoded?.components(separatedBy: "\n").map({$0.components(separatedBy: ",")}) {
                
                for item in dataArr {
                    result.append(item)
                }
            }
            
        } catch  {
            print("Error reading CSV file")
        }
        
        return result
    }
    
}

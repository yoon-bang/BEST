//
//  RssiInterpreter.swift
//  iBeacons
//
//  Created by Jung peter on 6/21/22.
//

import Foundation
import FirebaseMLModelDownloader
import TensorFlowLite

class RssiInterpreter {
    
    let conditions = ModelDownloadConditions(allowsCellularAccess: false)
    var interpreter: Interpreter?
    
    init() {
        
        ModelDownloader.modelDownloader()
            .getModel(name: modelName,
                      downloadType: .localModelUpdateInBackground,
                      conditions: conditions) { [weak self] result in
                switch (result) {
                case .success(let customModel):
                    do {
                        let interpreter = try Interpreter(modelPath: customModel.path)
                        self?.interpreter = interpreter
                    } catch {
                        print(error)
                    }
                case .failure(let error):
                    
                    print(error)
                }
        }
    }
    
    func classifyLocationOfUser(with csv: String) -> String? {
        
        var csv = csv
        var inputData = Data()
        
        guard let interpreter = interpreter else {
            print("no interpreter")
            return nil}
        
        let firstEle = Float(csv.split(separator: ",").first ?? "0.0") ?? 0.0
        let elementSize: Int = MemoryLayout.size(ofValue: firstEle)
        var bytes = [UInt8](repeating: 0, count: elementSize)
        
        for rssi in csv.split(separator: ",") {
            var rssi = Float(rssi) ?? 0.0
            memcpy(&bytes, &rssi, elementSize)
            inputData.append(&bytes, count: elementSize)
        }
        
        do {
            try interpreter.allocateTensors()
            try interpreter.copy(inputData, toInputAt: 0)
            try interpreter.invoke()
        } catch {
            print("firstError", error)
            return nil
        }
        
        do {
            let output = try interpreter.output(at: 0)
            let probabilities =
                    UnsafeMutableBufferPointer<Float32>.allocate(capacity: 31)
            
            output.data.copyBytes(to: probabilities)
            
            var labels = ["A01", "A10", "A11", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09","E01", "E02", "E03","H01","H02","R01","R02", "R03", "R04", "R05", "S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "U01"]
            
            let maxPro = probabilities.firstIndex(of: probabilities.max() ?? 0.0) ?? 1
            return labels[maxPro]
            
            
        } catch {
            print("second Error", error)
        }
        
        return "unknown"
    }

    
}


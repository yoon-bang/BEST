//
//  KalmanFilter.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import Foundation

class KalmanFilter {
    
    var R: Float // process noise(system noise)
    // How much noise can be expected from the systme itself
    
    var Q: Float // measurement noise
    // If the measurement can cause most of the noise, you shoud set this parameter to high number
    
    var stateVector: Float
    var controlVector: Float
    var measureVector: Float
    
    var x = Float.nan
    var cov: Float = 0.0
    var kalmanGain: Float = 0.0
    
    //init
    init(R: Float, Q: Float, stateVector: Float = 1, controlVector: Float = 0, measureVector: Float = 1) {
        self.R = R
        self.Q = Q
        self.stateVector = stateVector
        self.controlVector = controlVector
        self.measureVector = measureVector
    }
    
    // predict next value
    private func predict(u: Float = 0) -> Float {
        return (stateVector * x) + (controlVector * u)
    }
    
    // uncertainty of filter
    private func uncertainty() -> Float {
        return (square(x: measureVector) * cov) + R
    }
    
    //filter
    func filter(signal: Float, u: Float = 0) -> Float {
        if x.isNaN == true {
            self.x = (1 / measureVector) * signal
            self.cov = square(x: (1 / measureVector)) * self.Q
        } else {
            let prediction = predict(u: u)
            let uncertainty = uncertainty()
            
            // Kalman Gain
            kalmanGain = uncertainty * self.measureVector * (1 / (square(x: measureVector) * uncertainty + self.Q))
            
            // correction
            self.x = prediction + kalmanGain * (signal - (measureVector * prediction))
            self.cov = uncertainty - (kalmanGain * measureVector * uncertainty)
        }
        
        return self.x
    }
    
    private func square(x: Float) -> Float {
        return x * x
    }

}


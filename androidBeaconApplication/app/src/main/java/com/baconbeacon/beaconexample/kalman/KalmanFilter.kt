package com.baconbeacon.beaconexample.kalman

import kotlin.math.sqrt
import kotlin.properties.Delegates

class KalmanFilter(R: Float,
                   Q: Float,
                   stateVector: Float = 1f,
                   controlVector: Float = 0f,
                   measureVector: Float = 1f) {
    var R by Delegates.notNull<Float>()
    var Q by Delegates.notNull<Float>()

    var stateVector by Delegates.notNull<Float>()
    var controlVector by Delegates.notNull<Float>()
    var measureVector by Delegates.notNull<Float>()

    var x = Float.NaN
    var cov: Float = 0.0f

    init {
        this.R = R
        this.Q = Q
        this.stateVector = stateVector
        this.controlVector = controlVector
        this.measureVector = measureVector
    }

    // predict next value
    private fun predict(u: Float = 0f): Float {
        return (stateVector * x) + (controlVector * u)
    }

    // uncertainty of filter
    private fun uncertainty(): Float {
        return (sqrt(measureVector) * cov) + R
    }

    // filter
    fun filter(signal: Float, u: Float = 0f): Float {
        if (x.isNaN()) {
            this.x = (1 / measureVector) * signal
            this.cov = sqrt(1 / measureVector) * this.Q
        } else {
            var prediction = predict(u)
            var uncertainty = uncertainty()

            // Kalman Gain
            var kalmanGain =
                uncertainty * this.measureVector * (1 / ((sqrt(this.measureVector) * uncertainty) + this.Q))

            // correction
            this.x = prediction + kalmanGain * (signal - (measureVector * prediction))
            this.cov = uncertainty - (kalmanGain * measureVector * uncertainty)
        }
        return this.x
    }
}
// Copyright (c) 2024 Magic Tech Ltd

package fit.magic.cv.repcounter

import fit.magic.cv.PoseLandmarkerHelper
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class ExerciseRepCounterImpl : ExerciseRepCounter() {
    private var lastRepTimestamp: Long = 0L
    private var lastLegUsed: String = ""
    private var previousProgressValues = mutableListOf<Float>()

    override fun setResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        // process pose data in resultBundle
        //
        // use functions in base class incrementRepCount(), sendProgressUpdate(),
        // and sendFeedbackMessage() to update the UI

        val landmarks = resultBundle.poseLandmarks ?: return

        // Calculate angles for both legs
        val leftKneeAngle = calculateAngle(landmarks[LEFT_HIP], landmarks[LEFT_KNEE], landmarks[LEFT_ANKLE])
        val rightKneeAngle = calculateAngle(landmarks[RIGHT_HIP], landmarks[RIGHT_KNEE], landmarks[RIGHT_ANKLE])

        // Tracks progress and smoothens it, avoiding abrupt changes
        val rawProgress = calculateProgress(leftKneeAngle, rightKneeAngle)
        val smoothedProgress = calculateSmoothedProgress(rawProgress)
        sendProgressUpdate(smoothedProgress)

        // Provide feedback based on form whilst the user is attempting the exercise
        if (leftKneeAngle > 100 && lastLegUsed == "left") {
            sendFeedbackMessage("Go Lower on Left")
        } else if (rightKneeAngle > 100 && lastLegUsed == "right") {
            sendFeedbackMessage("Go Lower on Right")
        }

        // Detect reps as sets are being attempted
        if (isRepComplete(leftKneeAngle, rightKneeAngle)) {
            incrementRepCount()
            sendFeedbackMessage("Great job! Keep alternating legs.")
        }
    }

    private fun calculateAngle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Float {
        val ab = Point(b.x - a.x, b.y - a.y)
        val bc = Point(c.x - b.x, c.y - b.y)
        val dotProduct = ab.x * bc.x + ab.y * bc.y
        val magnitudeAB = sqrt(ab.x.pow(2) + ab.y.pow(2))
        val magnitudeBC = sqrt(bc.x.pow(2) + bc.y.pow(2))
        return acos(dotProduct / (magnitudeAB * magnitudeBC)) * (180 / Math.PI).toFloat()
    }

    private fun calculateProgress(leftAngle: Float, rightAngle: Float): Float {
        val targetAngle = 90f // Expected angle for a deep lunge
        val startAngle = 180f // Angle when standing upright
        val currentAngle = minOf(leftAngle, rightAngle)

        return ((startAngle - currentAngle) / (startAngle - targetAngle)).coerceIn(0f, 1f)
    }

    private fun calculateSmoothedProgress(newProgress: Float): Float {
        if (previousProgressValues.size >= 5) {
            previousProgressValues.removeAt(0)
        }
        previousProgressValues.add(newProgress)

        return previousProgressValues.sum() / previousProgressValues.size
    }

    private fun isRepComplete(leftAngle: Float, rightAngle: Float): Boolean {
        val currentTime = System.currentTimeMillis()

        // Ensure at least 1 second has passed between reps
        if (currentTime - lastRepTimestamp < 1000) {
            return false
        }

        // Check for left-leg lunge completion
        if (leftAngle <= 85 && rightAngle >= 175 && lastLegUsed != "left") {
            lastLegUsed = "left"
            lastRepTimestamp = currentTime
            return true
        }

        // Check for right-leg lunge completion
        if (rightAngle <= 85 && leftAngle >= 175 && lastLegUsed != "right") {
            lastLegUsed = "right"
            lastRepTimestamp = currentTime
            return true
        }

        return false
    }
}

data class Point(val x: Float, val y: Float)

// Assuming PoseLandmark is defined elsewhere with properties x and y
class PoseLandmark(val x: Float, val y: Float)
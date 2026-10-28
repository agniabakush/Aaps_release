package app.aaps.wear.data

import app.aaps.core.interfaces.rx.weardata.EventData

/**
 * Content comparison for the payloads the phone sends.
 *
 * Needed because [EventData.SingleBg] overrides `equals` to compare only `timeStamp` and `color`.
 * A reading that was corrected or smoothed keeps its timestamp, so `equals` would report "same"
 * while the displayed value changed. Everything the watch draws is compared here instead.
 *
 * Used to skip a DataStore write when nothing changed: a write re-encodes the whole store, which
 * also holds the watchface resources, and every write wakes all complications and repaints the
 * watchface.
 */
fun EventData.SingleBg.sameContentAs(other: EventData.SingleBg): Boolean =
    dataset == other.dataset &&
        timeStamp == other.timeStamp &&
        sgv == other.sgv &&
        sgvString == other.sgvString &&
        sgvLevel == other.sgvLevel &&
        glucoseUnits == other.glucoseUnits &&
        slopeArrow == other.slopeArrow &&
        delta == other.delta &&
        deltaDetailed == other.deltaDetailed &&
        avgDelta == other.avgDelta &&
        avgDeltaDetailed == other.avgDeltaDetailed &&
        deltaMgdl == other.deltaMgdl &&
        avgDeltaMgdl == other.avgDeltaMgdl &&
        high == other.high &&
        low == other.low &&
        color == other.color

/** [sameContentAs] applied element by element. */
fun List<EventData.SingleBg>.sameContentAs(other: List<EventData.SingleBg>): Boolean =
    size == other.size && indices.all { this[it].sameContentAs(other[it]) }

/**
 * Treatments compare by value, except the predictions, which are [EventData.SingleBg] and need the
 * content comparison above.
 */
fun EventData.TreatmentData.sameContentAs(other: EventData.TreatmentData): Boolean =
    temps == other.temps &&
        basals == other.basals &&
        boluses == other.boluses &&
        predictions.sameContentAs(other.predictions)

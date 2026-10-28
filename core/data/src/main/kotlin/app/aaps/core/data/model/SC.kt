package app.aaps.core.data.model

import java.util.TimeZone

/** Steps count values measured by a user smart watch or the like. */
data class SC(
    override var id: Long = 0,
    /** Duration milliseconds */
    var duration: Long,
    /** Milliseconds since the epoch. End of the sampling period, i.e. the value is
     *  sampled from timestamp-duration to timestamp. */
    var timestamp: Long,
    var steps5min: Int,
    var steps10min: Int,
    var steps15min: Int,
    var steps30min: Int,
    var steps60min: Int,
    var steps180min: Int,
    /** Source device that measured the steps count. */
    var device: String,
    var utcOffset: Long = TimeZone.getDefault().getOffset(timestamp).toLong(),
    override var version: Int = 0,
    override var dateCreated: Long = -1,
    override var isValid: Boolean = true,
    override var referenceId: Long? = null,
    override var ids: IDs = IDs()
) : HasIDs {

    fun contentEqualsTo(other: SC): Boolean {
        return this === other || (
            duration == other.duration &&
                timestamp == other.timestamp &&
                steps5min == other.steps5min &&
                steps10min == other.steps10min &&
                steps15min == other.steps15min &&
                steps30min == other.steps30min &&
                steps60min == other.steps60min &&
                steps180min == other.steps180min &&
                isValid == other.isValid)
    }

    companion object {

        /**
         * [duration] marking a row that is NOT a window total but a single raw increment — the steps
         * counted between two consecutive samples of a cumulative hardware counter, carried in
         * [steps5min].
         *
         * The phone-sensor sampler banks these so it can build its window totals by summing
         * non-overlapping increments; summing window totals instead makes the quantity feed on
         * itself. They are an implementation detail of that sampler: they are deliberately not a
         * window, so a consumer selecting a row by `duration == D minutes` never picks one up, and
         * the overview graph filters them out rather than drawing a second, shorter bar beside the
         * real one.
         */
        const val DURATION_RAW_INCREMENT_MS = 1L
    }
}
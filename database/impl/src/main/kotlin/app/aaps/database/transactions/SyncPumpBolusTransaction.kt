package app.aaps.database.transactions

import app.aaps.database.entities.Bolus

/**
 * Creates or updates the Bolus from pump synchronization
 *
 * @param updateExistingTimestamp overwrite the timestamp of a record that already exists under these
 *                                pump ids. False keeps the stored time and corrects only amount and
 *                                type, which a driver needs when it already stored a better time than
 *                                the pump reports.
 */
class SyncPumpBolusTransaction(
    private val bolus: Bolus,
    private val bolusType: Bolus.Type?, // extra parameter because field is not nullable in Bolus.class
    private val updateExistingTimestamp: Boolean = true
) : Transaction<SyncPumpBolusTransaction.TransactionResult>() {

    override fun run(): TransactionResult {
        bolus.interfaceIDs.pumpId ?: bolus.interfaceIDs.pumpType ?: bolus.interfaceIDs.pumpSerial ?: throw IllegalStateException("Some pump ID is null")
        val result = TransactionResult()
        val current = database.bolusDao.findByPumpIds(bolus.interfaceIDs.pumpId!!, bolus.interfaceIDs.pumpType!!, bolus.interfaceIDs.pumpSerial!!)
        if (current == null) {
            database.bolusDao.insertNewEntry(bolus)
            result.inserted.add(bolus)
        } else {
            val timestampChanged = updateExistingTimestamp && current.timestamp != bolus.timestamp
            if (
                timestampChanged ||
                current.amount != bolus.amount ||
                current.type != bolusType ?: current.type
            ) {
                if (updateExistingTimestamp) current.timestamp = bolus.timestamp
                current.amount = bolus.amount
                current.type = bolusType ?: current.type
                database.bolusDao.updateExistingEntry(current)
                result.updated.add(current)
            }
        }
        return result
    }

    class TransactionResult {

        val inserted = mutableListOf<Bolus>()
        val updated = mutableListOf<Bolus>()
    }
}
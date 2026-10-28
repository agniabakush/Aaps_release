package app.aaps.pump.apex.misc

import androidx.annotation.RawRes
import app.aaps.core.interfaces.notifications.Notification
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventDismissNotification
import app.aaps.core.interfaces.rx.events.EventNewNotification
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every notification card the Apex driver puts on screen goes through here.
 *
 * It stands in for `NotificationManager`, `NotificationId`, `NotificationHandle` and
 * `NotificationLevel`, none of which exist in this version of AAPS. Here a card is a plain
 * [Notification] sent on the bus, and the card's integer id is its whole identity: posting an id the
 * store already holds only refreshes that card's timestamp - `NotificationStore.add` returns before
 * it can make a second card, start a sound, or raise an Android notification.
 *
 * Two consequences the rest of the driver is built on.
 *
 * 1. "Is that card still on screen?" cannot be asked. This version has no read side to the
 *    notification store, so [isOnScreen] answers false and the caller simply posts the card again: a
 *    card that still stands is refreshed in place and silently, a card the user swiped away comes
 *    back. Answering true instead would mean a swiped card for an alarm that still stands never
 *    returns, which is the one outcome that must not happen.
 * 2. Two cards need two ids. The pump can stand ten alarms at once and each one has to keep its own
 *    text and its own sound, so alarm cards draw an id from a private pool ([postAlarmCard]) and
 *    give it back when the card comes down.
 *
 * Severity: this version has five levels and only [Notification.URGENT] gets the loud treatment
 * (long vibration and the alarm ringtone) when it is mirrored to an Android notification. The dev4
 * level `IMPORTANT` - "important, but not an alarm" - has no counterpart, so it maps to
 * [Notification.NORMAL] here. Mapping it to URGENT would turn a silent card into an audible one.
 */
@Singleton
class ApexNotifications @Inject constructor(
    private val rxBus: RxBus
) {

    /**
     * Names one posted card. Replaces dev4's `NotificationHandle`.
     *
     * Where dev4 keyed a card by an instance key and allowed many cards under one id, here the id is
     * the card, so a handle carries nothing else.
     */
    data class Handle(val id: Int)

    /** Alarm-card ids that no card holds right now. Guarded by this object's monitor. */
    private val freeAlarmIds = ArrayDeque((ALARM_CARD_FIRST until ALARM_CARD_FIRST + ALARM_CARD_SLOTS).toList())

    /** What was last posted under each id, so a card whose wording moved on really gets replaced. */
    private val posted = mutableMapOf<Int, PostedCard>()

    private data class PostedCard(val text: String, val level: Int, val soundRes: Int?)

    /**
     * Put a card with a fixed id on screen.
     *
     * [level] defaults to the severity that id always carries, the same rule dev4 had in
     * `NotificationId.defaultLevel`; pass one only where dev4 passed one. [soundRes] is a raw
     * resource, or null for a silent card.
     *
     * Posting the same card again is silent, so this is safe to call on every status frame: the
     * store keeps the entry it already has and only refreshes its timestamps, which rings nothing.
     * That is also why a card whose wording moved on - a falling reservoir, a bolus amount - has to
     * be taken down first, otherwise the old text would stay on screen for as long as the card does.
     * Only a real change does that, so the common re-post neither flickers nor restarts a siren.
     */
    @Synchronized
    fun post(id: Int, text: String, level: Int = defaultLevel(id), @RawRes soundRes: Int? = null): Handle {
        val standing = posted[id]
        // A ringing card is never taken down for a silent one, whatever else changed about it. Every
        // standing card is put up again on each status frame (isOnScreen is always false) and those
        // repeats carry no sound, and some of them recompute the level as they go - so any rule that
        // replaced on a difference alone would remove a card that carries a sound, and the store stops
        // the one alarm player with it. The siren would die one frame after it started.
        // A sound the standing card does NOT have still has to get through, because the store ignores
        // an add() for an id it already holds: that card has to come down first.
        // Known limit of the take-down: the two events reach the store on two separate subscriptions,
        // so their order is not guaranteed. If the new card is handled first the store swallows it as
        // a duplicate, and the take-down then leaves nothing on screen. Reversing the pair does not
        // help - it is symmetric. Ordering it properly would mean giving the replacement an id of its
        // own, which is what alarm cards do with their pool. Until then the loss is bounded: a card
        // that goes this way behaves exactly like one the user swiped, which the reactive version also
        // left down, and every card that is put up again on each status frame comes straight back.
        val ringing = standing?.soundRes != null
        val replace = standing != null && (soundRes != null || !ringing) &&
            (standing.text != text || standing.level != level || (soundRes != null && standing.soundRes != soundRes))
        if (replace) rxBus.send(EventDismissNotification(id))
        // On a re-post the store keeps the entry it already has, so what was recorded still describes
        // what the viewer sees - including its sound. Only a card that really changed is re-recorded.
        if (standing == null || replace) posted[id] = PostedCard(text, level, soundRes)
        rxBus.send(EventNewNotification(Notification(id, text, level).also { it.soundId = soundRes }))
        return Handle(id)
    }

    /**
     * Put one pump-alarm card on screen and give back the handle that takes down that very card.
     *
     * Pass [into] to post into the card's existing slot - that is what "put the card back" means, and
     * it is why a re-post lands on the same id instead of piling up a second card. Pass null for an
     * alarm that has no card yet; it takes the next free id from the pool.
     */
    @Synchronized
    fun postAlarmCard(text: String, level: Int, @RawRes soundRes: Int?, into: Handle?): Handle {
        // The overflow id cannot be reached: the pool holds one id per pump alarm slot and the caller
        // holds at most one card per standing alarm. If a bookkeeping bug ever empties the pool, an
        // alarm sharing a card is still far better than an alarm nobody is told about.
        val id = into?.id ?: freeAlarmIds.removeFirstOrNull() ?: ALARM_CARD_OVERFLOW
        return post(id, text, level, soundRes)
    }

    /** Take the card with this id down, and give an alarm-card id back to the pool. */
    @Synchronized
    fun dismiss(id: Int) {
        rxBus.send(EventDismissNotification(id))
        // Forget what stood here, so the next post of this id is a fresh card and not compared
        // against wording that is no longer on screen.
        val gone = posted.remove(id)
        // Guarded against a second dismiss of the same card, which would otherwise hand the same id
        // out twice and let two alarms share one card.
        if (id in ALARM_CARD_FIRST until ALARM_CARD_FIRST + ALARM_CARD_SLOTS && id !in freeAlarmIds) freeAlarmIds.addLast(id)
        if (gone?.soundRes != null) rearmSiren()
    }

    /**
     * Start the alarm player again for a card that is still standing.
     *
     * There is one player for the whole app, and the store stops it whenever it removes a card that
     * carries a sound. So taking down one ringing alarm silences every other ringing alarm with it -
     * an occlusion the user acknowledged on the pump would take the cartridge-empty siren with it.
     * dev4 answered this with `refreshAlarmSound()`, which promoted the next audible notification;
     * the same is done here by taking the survivor down and putting it straight back, which is the
     * only way to make the store start the player for an id it already holds.
     *
     * Any survivor will do - they share the one player - so the loudest is preferred only so the
     * shade copy keeps the right severity.
     */
    private fun rearmSiren() {
        val survivor = posted.entries
            .filter { it.value.soundRes != null }
            .minByOrNull { it.value.level } ?: return
        val card = survivor.value
        rxBus.send(EventDismissNotification(survivor.key))
        rxBus.send(EventNewNotification(Notification(survivor.key, card.text, card.level).also { it.soundId = card.soundRes }))
    }

    /** Take down the card this handle names. */
    fun dismiss(handle: Handle) = dismiss(handle.id)

    /**
     * Take down every alarm card and give the whole pool back.
     *
     * For the start and the end of the driver: handles die with the service while the notification
     * store does not, so cards left by a previous instance have to go by id.
     */
    @Synchronized
    fun clearAlarmCards() {
        for (id in ALARM_CARD_FIRST..ALARM_CARD_OVERFLOW) dismiss(id)
    }

    /**
     * Whether that card is still on screen. Always false - see the class comment.
     *
     * Kept as a call rather than a bare `false` at each site so the reason travels with it. Callers
     * are expected to post the card again on a false, which is silent when the card is still there.
     */
    fun isOnScreen(id: Int): Boolean = false

    /** Whether the card this handle names is still on screen. Always false - see [isOnScreen]. */
    fun isOnScreen(handle: Handle): Boolean = false

    /**
     * The severity an id always carries, standing in for dev4's `NotificationId.defaultLevel`.
     *
     * Everything not listed is [Notification.NORMAL]: that covers both the cards dev4 called NORMAL
     * and the ones it called IMPORTANT, which this version does not have.
     */
    private fun defaultLevel(id: Int) = when (id) {
        RESERVOIR_CRITICAL,
        BOLUS_UNCONFIRMED,
        PUMP_LOCKED,
        INVALID_OBJECT,
        Notification.PUMP_ERROR,
        Notification.PUMP_UNREACHABLE -> Notification.URGENT

        else                          -> Notification.NORMAL
    }

    companion object {

        /**
         * The driver's own notification ids, 900..918.
         *
         * Why this range cannot collide:
         * - the core's hand-assigned ids in [Notification] are one dense block running 0..97, and it
         *   grows upward one number at a time, so 900 leaves 800 numbers of room;
         * - `Notification.USER_MESSAGE` (1000) and `Notification.PERMISSION_EXTERNAL_STORAGE` (2000)
         *   are above the range;
         * - no module in this app posts a literal id, so [Notification] and the constants in this
         *   companion are the whole field.
         *
         * The one id in AAPS that is not from a fixed list is `NotificationUserMessage`, which uses
         * `text.hashCode()` and can therefore land on any number at all. Every hand-assigned id in the
         * app carries that same one-in-2^32 exposure, [Notification.PUMP_ERROR] included; a pump
         * driver cannot narrow it.
         */
        const val ALARM_CARD_FIRST = 900

        /**
         * One id per alarm the pump can stand at once.
         *
         * `StatusV1.alarms` reads exactly ten slots, so ten live cards is the most there can ever be.
         */
        const val ALARM_CARD_SLOTS = 10

        /** Only reachable through a bookkeeping bug - see [postAlarmCard]. */
        const val ALARM_CARD_OVERFLOW = 910

        /** dev4 `APEX_RESERVOIR_LOW`. */
        const val RESERVOIR_LOW = 911

        /** dev4 `APEX_RESERVOIR_CRITICAL`. */
        const val RESERVOIR_CRITICAL = 912

        /** dev4 `APEX_PERMISSION_MISSING`. */
        const val PERMISSION_MISSING = 913

        /** dev4 `APEX_PUMP_LINK_LOST`. The driver's own id, never the core's `PUMP_UNREACHABLE`. */
        const val PUMP_LINK_LOST = 914

        /** dev4 `APEX_BLUETOOTH_OFF`. */
        const val BLUETOOTH_OFF = 915

        /** dev4 `APEX_BOLUS_UNCONFIRMED`, for a bolus a person gave by hand. */
        const val BOLUS_UNCONFIRMED = 916

        /** dev4 `APEX_PASSWORD_REJECTED`. */
        const val PASSWORD_REJECTED = 917

        /**
         * The same warning about an SMB, on a card of its own.
         *
         * dev4 put both on one id and let it hold several cards at once. Here the id IS the card, so
         * one id would mean the quiet SMB line replaces the loud card about a hand-given bolus that
         * somebody is standing there waiting for. They get an id each: the loud card is never touched,
         * and a run of SMBs on a bad link still lands on this single id, so the screen cannot fill up.
         */
        const val BOLUS_UNCONFIRMED_SMB = 918

        /** dev4 `APEX_PUMP_LOCKED`. The core already carries this one. */
        const val PUMP_LOCKED = Notification.PUMP_IS_LOCKED

        /** dev4 `APEX_INVALID_OBJECT`. The core already carries this one. */
        const val INVALID_OBJECT = Notification.APEX_SERVICE_MSG
    }
}

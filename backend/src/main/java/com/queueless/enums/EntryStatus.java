package com.queueless.enums;

/**
 * Lifecycle status of a customer's queue entry.
 */
public enum EntryStatus {

    /** Customer is waiting in the queue. */
    WAITING,

    /** Staff has called this customer (announced their token). */
    CALLED,

    /** Customer is currently being served. */
    IN_SERVICE,

    /** Service has been completed successfully. */
    COMPLETED,

    /** Customer was skipped (didn't show up when called). */
    SKIPPED,

    /** Customer cancelled their queue entry. */
    CANCELLED
}

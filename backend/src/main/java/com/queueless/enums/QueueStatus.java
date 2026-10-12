package com.queueless.enums;

/**
 * Status of a Queue — whether it's accepting new customers or closed.
 *
 * A queue is OPEN when the business starts accepting customers
 * for a service, and CLOSED when they stop (end of day, or manually).
 */
public enum QueueStatus {

    /** Queue is accepting new customers. */
    OPEN,

    /** Queue is closed — no new customers can join. */
    CLOSED
}

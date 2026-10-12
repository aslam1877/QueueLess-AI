package com.queueless.enums;

/**
 * User authorization roles.
 */
public enum UserRole {

    /**
     * A person who joins queues and receives service.
     */
    CUSTOMER,

    /**
     * A person who owns a business, manages services and queues.
     */
    BUSINESS_OWNER,

    /**
     * A person employed by a business who can call customers
     * and manage queue operations.
     */
    STAFF
}

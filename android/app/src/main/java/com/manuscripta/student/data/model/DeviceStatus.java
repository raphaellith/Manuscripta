package com.manuscripta.student.data.model;

/**
 * Enum representing the status of a student device.
 */
public enum DeviceStatus {
    /**
     * Student is actively working on the assigned task.
     */
    ON_TASK,

    /**
     * Student has requested help.
     */
    HAND_RAISED,

    /**
     * Device is disconnected or offline.
     */
    DISCONNECTED,

    /**
     * Device screen is locked by the teacher.
     */
    LOCKED,

    /**
     * Device has been idle for a prolonged period.
     */
    IDLE
}

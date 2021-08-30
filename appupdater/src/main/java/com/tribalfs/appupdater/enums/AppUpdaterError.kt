package com.tribalfs.appupdater.enums

enum class AppUpdaterError {
    UPDATE_VARIES_BY_DEVICE,
    /**
     * No Internet connection available
     */
    NETWORK_NOT_AVAILABLE,

    /**
     * URL for the JSON file is not valid
     */
    JSON_URL_MALFORMED,

    /**
     * JSON file is invalid or is down
     */
    JSON_ERROR
}
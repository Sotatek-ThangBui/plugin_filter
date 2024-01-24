package com.plugin.filters.plugin_filters.config


val MSQRD_TOPIC =
    listOf(
        "all",
        "animal",
        "toonify",
        "masquerade",
        "beauty",
        "painting",
        "fantasy",
        "celebrity",
        "tattoo",
        "old",
        "baby",
        "test",

    )

const val ENABLE_ADMIN_MODE = false
const val IS_SHOW_INTERSTITIAL = !ENABLE_ADMIN_MODE
const val TELEGRAM_CHANNEL_ID = "-1001748098599"

const val ASSET_PREFIX = "assets://"
const val STICKER_TYPE_HAT = "Hat"
const val STICKER_TYPE_MASK = "Mask"
const val NUMBER_OF_ADS = 3
const val ADS_SETTING_PREF_NAME = "ADS_SETTING"
const val KEY_AD_BANNER_HOME = "banner_home"
const val KEY_AD_BANNER_EDITOR = "banner_editor"
const val KEY_AD_INTERSTITIAL_EDITOR = "interstitial_editor"
const val KEY_AD_REWARD_EDITOR = "reward_editor"
const val KEY_AD_NATIVE_EDITOR = "native_editor"
const val KEY_AD_NATIVE_LIST_TEMPLATE = "native_list_template"
const val KEY_APP_STORE_VERSION_CODE = "store_version_code"
const val KEY_APP_STORE_VERSION_NAME = "store_version_name"
const val KEY_EDITOR_AD_PERIOD = "editor_ad_period"
const val KEY_SAVED_IMAGE_PATH = "savedPath"
const val KEY_FCM_TOKEN = "fcm_token"

const val SHOW_REWARD_AD_PERIOD = 10
const val PRIVACY_URL = "https://sites.google.com/view/animeme-app/home"
const val INSTAGRAM_URL = "http://www.instagram.com/animeme.app"
const val SUPPORT_EMAIL = "animeme.app@gmail.com"
const val SAVED_IMAGE_FOLDER = "mangaverse"

const val TRACK_SETTING = "mgv_setting"
const val TRACK_ITEM_SETTING = "view_setting_list"

const val DATA_PREF_NAME = "DATA_PREF"
const val KEY_QUOTE_DATA = "quotes"

const val SAVE_BUBBLE_FOLDER = "Bubble"
const val SAVE_CANDIDATE_FACE_FOLDER = "CandidateFace"

const val CHECKED_FACE_PHOTO_PREF = "checkedFacePref"
const val KEY_CHECKED_FACE_PHOTO = "checkedFacePhotos"
const val KEY_SHOWN_SELFIE_GUIDE = "shownSelfieGuide"
const val KEY_TUTORIAL_COMPLETE = "tutorialComplete"
const val KEY_SHOWN_CHANGE_FACE_GUIDE = "shownChangeFaceGuide"
const val KEY_RECENT_USER_FACE = "recentUserFace"
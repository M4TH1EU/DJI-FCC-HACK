package ch.mathieubroillet.djiffchack


object Constants {
    private const val PACKAGE = "ch.mathieubroillet.djiffchack"
    const val INTENT_ACTION_GRANT_USB_PERMISSION = "$PACKAGE.USB_PERMISSION"

    // USB Device IDs for DJI N1 Remote Controllers
    const val VENDOR_ID_DJI = 11427
    const val PRODUCT_ID_STANDARD = 4128
    const val VENDOR_ID_ALTERNATE = 5840
    const val PRODUCT_ID_ALTERNATE = 2174

    // The "magic bytes" that enable FCC mode
    // The credits goes to @galbb from https://mavicpilots.com/threads/mavic-air-2-switch-to-fcc-mode-using-an-android-app.115027/
    val BYTES_1 = byteArrayOf(85, 13, 4, 33, 42, 31, 0, 0, 0, 0, 1, -122, 32)
    val BYTES_2 = byteArrayOf(85, 24, 4, 32, 2, 9, 0, 0, 64, 9, 39, 0, 2, 72, 0, -1, -1, 2, 0, 0, 0, 0, -127, 31)

    const val GITHUB_URL = "https://github.com/M4TH1EU/DJI-FCC-HACK"
}
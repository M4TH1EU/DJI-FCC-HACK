package ch.mathieubroillet.djiffchack


object Constants {
    private const val PACKAGE = "ch.mathieubroillet.djiffchack"
    const val INTENT_ACTION_GRANT_USB_PERMISSION = "$PACKAGE.USB_PERMISSION"

    // The "magic bytes" that enable FCC mode
    // The credits goes to @galbb from https://mavicpilots.com/threads/mavic-air-2-switch-to-fcc-mode-using-an-android-app.115027/
    val BYTES_1 = byteArrayOf(85, 13, 4, 33, 42, 31, 0, 0, 0, 0, 1, -122, 32)
    val BYTES_2 = byteArrayOf(85, 24, 4, 32, 2, 9, 0, 0, 64, 9, 39, 0, 2, 72, 0, -1, -1, 2, 0, 0, 0, 0, -127, 31)

    const val GITHUB_URL = "https://github.com/M4TH1EU/DJI-FCC-HACK"
}
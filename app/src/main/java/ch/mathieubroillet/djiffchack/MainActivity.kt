package ch.mathieubroillet.djiffchack

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ch.mathieubroillet.djiffchack.ui.theme.DJI_FCC_HACK_Theme
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbManager
    private var usbConnected by mutableStateOf(false)
    private var isPatching by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // Register receiver to detect USB plug/unplug events
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(Constants.INTENT_ACTION_GRANT_USB_PERMISSION)
        }
        ContextCompat.registerReceiver(
            this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Initial check for USB connection
        refreshUsbConnection()

        setContent {
            DJI_FCC_HACK_Theme {
                MainScreen(usbConnected, ::refreshUsbConnection, ::sendPatch, isPatching)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    /**
     * Refreshes the USB connection status
     */
    private fun refreshUsbConnection() {
        if (usbManager.deviceList.isNotEmpty()) {
            val device: UsbDevice = usbManager.deviceList.values.first()
            Log.d("USB_CONNECTION", device.vendorId.toString() + ":" + device.productId.toString())

            // Check to be sure the device is the initialized DJI Remote (and not another USB device)
            if (device.productId != 4128) {
                Log.d("USB_CONNECTION", "Device not supported ${device.productId}")
                usbConnected = false
                return
            }

            if (usbManager.openDevice(device) == null) {
                Log.d("USB_CONNECTION", "Requesting USB Permission")
                requestUsbPermission(device)
            } else {
                usbConnected = true
            }
        } else {
            usbConnected = false
        }
    }

    /**
     * Sends the FCC patch to the DJI remote via USB
     */
    private fun sendPatch(): Boolean {
        // At this point, we assume the USB device is connected and we have permission to access it
        if (!usbConnected) {
            Toast.makeText(this, "No USB device connected!", Toast.LENGTH_SHORT).show()
            return false
        }

        val probeTable = ProbeTable().apply {
            addProduct(11427, 4128, CdcAcmSerialDriver::class.java)

            // TODO: not sure which device this is, might be the DJI remote before it's connected
            //  to drone it seems to use a different device once connected to the drone
            // addProduct(5840, 2174, CdcAcmSerialDriver::class.java)
        }

        // Retrieve the custom device (DJI remote) with the correct driver from the probe table above
        val driver = UsbSerialProber(probeTable).probeDevice(usbManager.deviceList.values.first())
        val deviceConnection = usbManager.openDevice(driver.device)
        if (deviceConnection == null) {
            Log.e("USB_PATCH", "Error opening USB device")
            Toast.makeText(this, "Error opening USB device", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val deviceSerialPort = driver.ports.firstOrNull()
            if (deviceSerialPort == null) {
                Log.e("USB_PATCH", "Error opening USB port")
                Toast.makeText(this, "Error opening USB port", Toast.LENGTH_SHORT).show()
                return false
            }

            deviceSerialPort.open(deviceConnection)
            deviceSerialPort.setParameters(19200, 8, 1, UsbSerialPort.PARITY_NONE)
            deviceSerialPort.write(Constants.BYTES_1, 1000)
            deviceSerialPort.write(Constants.BYTES_2, 1000)

            Toast.makeText(this, "Patched successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }


        return true
    }

    /**
     * Requests USB permission for the device
     */
    private fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(Constants.INTENT_ACTION_GRANT_USB_PERMISSION).apply { setPackage(packageName) },
            PendingIntent.FLAG_MUTABLE
        )

        usbManager.requestPermission(device, permissionIntent)
    }


    /**
     * BroadcastReceiver to handle USB events
     */
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d("USB_EVENT", "USB Device Connected")
                    refreshUsbConnection()
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d("USB_EVENT", "USB Device Disconnected")
                    refreshUsbConnection()
                }

                Constants.INTENT_ACTION_GRANT_USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d("USB_EVENT", "USB Permission Granted")
                        refreshUsbConnection()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    usbConnected: Boolean,
    onRefresh: () -> Unit,
    onSendPatch: () -> Boolean,
    isPatching: Boolean = false
) {
    var buttonText by remember { mutableStateOf("Send FCC Patch") }
    var buttonEnabled by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    Scaffold(topBar = {
        TopAppBar(title = { Text("DJI FCC Hack") }, actions = {
            IconButton(onClick = onRefresh, enabled = !isPatching) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh USB Connection")
            }
        })
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Make the entire screen scrollable
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            Image(
                painter = painterResource(id = isSystemInDarkTheme().let { if (it) R.drawable.dji_light else R.drawable.dji_dark }),
                contentDescription = "DJI Logo",
                modifier = Modifier.size(75.dp),
            )

            // Disclaimer Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Disclaimer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This app is provided as-is and is not affiliated with DJI. Use at your own risk.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Instructions Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column {
                        listOf(
                            "Turn on the drone and remote and wait a few seconds for them to connect.",
                            "Connect your phone to the bottom USB port of the remote.",
                            "Click on 'Send FCC Patch'.",
                            "Disconnect your phone from the bottom USB port of the remote and connect it to the top USB port."
                        ).forEachIndexed { index, instruction ->
                            Text(
                                text = "${index + 1}. $instruction",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: You have to repeat the process every time you turn on the remote and/or drone.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // USB Connection Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (usbConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (usbConnected) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = "USB Status"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (usbConnected) "Remote Connected" else "Remote Not Connected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Send Patch Button
            Button(
                onClick = {
                    buttonEnabled = false
                    val result = onSendPatch()
                    buttonText = if (result) "Successfully patched" else "Error patching"

                    CoroutineScope(Dispatchers.Main).launch {
                        delay(5000)
                        buttonText = "Send FCC Patch"
                        buttonEnabled = true
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = usbConnected && !isPatching && buttonEnabled
            ) {
                Icon(Icons.Default.Build, contentDescription = "Patch")
                Spacer(Modifier.width(8.dp))
                Text(buttonText)
            }

            // Links
            Row {
                IconButton(onClick = { uriHandler.openUri(Constants.GITHUB_URL) }) {
                    Image(
                        painter = painterResource(id = isSystemInDarkTheme().let { if (it) R.drawable.github_light else R.drawable.github_dark }),
                        contentDescription = "GitHub",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Footer
            Row {
                Text(
                    text = "Made with ❤️ by ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Mathieu Broillet",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Text(
                text = "based on the work of @galbb on MavicPilots",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontStyle = FontStyle.Italic
            )
        }
    }
}


@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", apiLevel = 35, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewMainScreen() {
    DJI_FCC_HACK_Theme{
        MainScreen(usbConnected = true, onRefresh = {}, onSendPatch = { false }, isPatching = false)
    }
}
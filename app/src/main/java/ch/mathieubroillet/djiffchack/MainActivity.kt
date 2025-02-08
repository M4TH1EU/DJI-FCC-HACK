package ch.mathieubroillet.djiffchack

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ch.mathieubroillet.djiffchack.ui.theme.DJI_FCC_HACK_Theme
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber


class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbManager
    private var usbConnection by mutableStateOf(null as UsbDeviceConnection?)

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
            this,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Initial check for USB connection
        refreshUsbConnection()

        setContent {
            MainScreen(usbConnection != null, ::refreshUsbConnection, ::sendPatch)
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
            usbConnection = usbManager.openDevice(device)

            if (usbConnection == null) {
                Log.d("USB_CONNECTION", "Requesting USB Permission")
                requestUsbPermission(device)
            }
        } else {
            usbConnection = null
        }
    }

    /**
     * Handles sending the patch via USB communication
     */
    private fun sendPatch() {
        if (usbConnection == null) {
            Toast.makeText(this, "No USB device connected!", Toast.LENGTH_SHORT).show()
            return
        }

        for (device in usbManager.deviceList.values) {
            try {
                val probeTable = ProbeTable()
                probeTable.addProduct(11427, 4128, CdcAcmSerialDriver::class.java)
                probeTable.addProduct(5840, 2174, CdcAcmSerialDriver::class.java)

                val usbSerialProber = UsbSerialProber(probeTable)
                val usbSerialPort = usbSerialProber.probeDevice(device).ports.firstOrNull()

                if (usbSerialPort == null) {
                    Toast.makeText(this, "No serial port found", Toast.LENGTH_SHORT).show()
                    return
                }

                usbSerialPort.open(usbConnection)
                usbSerialPort.setParameters(19200, 8, 1, UsbSerialPort.PARITY_NONE)
                usbSerialPort.write(
                    byteArrayOf(85, 13, 4, 33, 42, 31, 0, 0, 0, 0, 1, -122, 32),
                    1000
                )
                usbSerialPort.write(
                    byteArrayOf(
                        85, 24, 4, 32, 2, 9, 0, 0, 64, 9, 39, 0, 2, 72, 0, -1, -1, 2, 0, 0, 0, 0,
                        -127, 31
                    ), 1000
                )
                usbSerialPort.close()
            } catch (e: Exception) {
                Log.e("USB_PATCH", "Error sending patch: ${e.message}")
                Toast.makeText(this, "Patch failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
fun MainScreen(usbConnected: Boolean, onRefresh: () -> Unit, onSendPatch: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DJI FCC Hack") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh USB Connection")
                    }
                    IconButton(onClick = { /* Open Settings */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.dji_innovations_logo),
                contentDescription = "DJI Logo",
                modifier = Modifier.size(100.dp)
            )

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
                    Image(
                        painter = painterResource(id = R.drawable.usb_c_port),
                        contentDescription = "USB Status",
                        modifier = Modifier.size(24.dp),
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
                onClick = onSendPatch,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = usbConnected
            ) {
                Icon(Icons.Default.Build, contentDescription = "Patch")
                Spacer(Modifier.width(8.dp))
                Text("Send FCC Patch")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    DJI_FCC_HACK_Theme {
        MainScreen(usbConnected = true, onRefresh = {}, onSendPatch = {})
    }
}
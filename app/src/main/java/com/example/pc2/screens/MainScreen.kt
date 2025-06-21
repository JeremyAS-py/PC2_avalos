package com.example.pc2.screens
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.pc2.*

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val db: FirebaseFirestore = Firebase.firestore

    var monto by remember { mutableStateOf("") }
    var monedaOrigen by remember { mutableStateOf("USD") }
    var monedaDestino by remember { mutableStateOf("EUR") }
    var resultado by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val monedas = listOf("USD", "EUR", "PEN", "GBP", "JPY")

    // Mapa más completo con todas las conversiones posibles
    val tasas = mapOf(
        "USD_EUR" to 0.92,
        "EUR_USD" to 1.09,
        "USD_PEN" to 3.8,
        "PEN_USD" to 0.26,
        "USD_GBP" to 0.78,
        "GBP_USD" to 1.28,
        "USD_JPY" to 143.0,
        "JPY_USD" to 0.007,
        "EUR_PEN" to 4.13,
        "PEN_EUR" to 0.24,
        "EUR_GBP" to 0.85,
        "GBP_EUR" to 1.18,
        "EUR_JPY" to 155.0,
        "JPY_EUR" to 0.0065,
        "GBP_PEN" to 4.87,
        "PEN_GBP" to 0.21,
        "GBP_JPY" to 183.0,
        "JPY_GBP" to 0.0055,
        "PEN_JPY" to 37.6,
        "JPY_PEN" to 0.027
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = "Conversor de Monedas",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = monto,
            onValueChange = { newValue ->
                // Solo permitir números y un punto decimal
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    monto = newValue
                }
            },
            label = { Text("Monto") },
            placeholder = { Text("Ingresa el monto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DropdownMenuMoneda(
                label = "De",
                selected = monedaOrigen,
                opciones = monedas,
                modifier = Modifier.weight(1f)
            ) { monedaOrigen = it }

            DropdownMenuMoneda(
                label = "A",
                selected = monedaDestino,
                opciones = monedas,
                modifier = Modifier.weight(1f)
            ) { monedaDestino = it }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (monto.isBlank()) {
                    Toast.makeText(context, "Por favor ingresa un monto", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (monedaOrigen == monedaDestino) {
                    Toast.makeText(context, "Selecciona monedas diferentes", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val clave = "${monedaOrigen}_${monedaDestino}"
                val tasa = tasas[clave]
                val valor = monto.toDoubleOrNull()

                if (tasa != null && valor != null && valor > 0) {
                    isLoading = true
                    val convertido = valor * tasa
                    resultado = "$monto $monedaOrigen = ${String.format("%.2f", convertido)} $monedaDestino"

                    // Guardar en Firebase
                    val currentUser = FirebaseAuthManager.getCurrentUser()
                    if (currentUser != null) {
                        val conversion = hashMapOf(
                            "uid" to currentUser.uid,
                            "fechaHora" to Timestamp.now(),
                            "monto" to valor,
                            "origen" to monedaOrigen,
                            "destino" to monedaDestino,
                            "resultado" to convertido,
                            "tasa" to tasa
                        )

                        db.collection("conversiones")
                            .add(conversion)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Conversión guardada", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        isLoading = false
                        Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Verifica el monto ingresado", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Convirtiendo..." else "Convertir")
        }

        if (resultado.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = resultado,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DropdownMenuMoneda(
    label: String,
    selected: String,
    opciones: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opcion,
                            color = if (opcion == selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(opcion)
                        expanded = false
                    }
                )
            }
        }
    }
}
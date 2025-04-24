@Composable
fun InvoiceItem(
    invoice: IOperation,
    onCancelClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    val isAnulado = invoice.operationStatus == "06"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                if (isAnulado) Color.Red.copy(alpha = 0.1f)
                else Color.Transparent
            ),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${invoice.serial}-${invoice.correlative}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = invoice.documentTypeReadable,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cliente: ${invoice.client.names}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: S/ ${invoice.totalAmount}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (isAnulado) {
                    Text(
                        text = "Anulado",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Button(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Anular")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPdfClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver PDF")
            }
        }
    }
} 
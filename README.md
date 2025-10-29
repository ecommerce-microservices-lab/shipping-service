# Shipping Service API - Resumen y Errores Detectados
## Prefijo
`/shipping-service`

## Endpoints

### Obtener todos los envios

* **Método:** GET
* **Ruta:** `/api/shippings`

Funciona bien


### Obtener envio por id

* **Método:** GET
* **Ruta:** `/api/shippings/{orderId}/{productId}`

No funciona, porque esta pasando un null en lugar de los parámetros reales

### Ejemplo de payload

```json
{
  "verificationTokenId": 1,
  "token": "abc123def456",
  "expireDate": "30-06-2025",
  "credential": {
    "credentialId": 1
  }
}
```

ci: trigger develop Wed Oct 29 11:04:13 -05 2025

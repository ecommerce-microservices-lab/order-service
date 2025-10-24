# CART-api

prefijo `/order-service`

Obtener todos los carritos

GET `/api/carts` 

Funciona bien

Obtener un carrito por id

GET `/api/carts/{cartId}` 

Crear carrito

POST `/api/carts` 

Maneja secundariamente las ordenenes lo cual esta mal, recibe el id como parametro entonces llega a sobreescribir, no tiene en cuenta si el usuario al que le crea el carrito existe realmente

Editar carrito

PUT `/api/carts` 

No se revisa, porque el carro solo tiene el id del usuario y no tiene sentido que se le cambie el carro de un usuario a otro, se elimina este endpoint

Eliminar carrito

DELETE `/api/carts`

Funciona bien

ejemplo de payload

```json
{
  "cartId": 1,
  "userId": 1
}
```

# Order API

Obtener todas las ordenes

GET `api/orders`

funciona bien

Obtener orden por id

GET `api/orders/{orderId}`

Crear orden

POST `api/orders`

Recibe el id entonces sobreescribe

Editar orden por body

PUT `api/orders`

No edita por referencia circular con cart

Editar orden por query

PUT `api/orders`

No edita por referencia circular con cart

Eliminar orden

DELETE `api/orders`

Funciona bien

Ejemplo de payload

```json
{
    "orderId": 2,
    "orderDate": "10-06-2025__13:12:22:606444",
    "orderDesc": "init",
    "orderFee": 5000.0,
    "cart": {
        "cartId": 2,
        "userId": 2
    }
}
```
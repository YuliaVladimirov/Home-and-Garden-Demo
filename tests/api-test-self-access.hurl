
# Login and store token
POST http://localhost:8080/auth/login
Authorization: None
Content-Type: application/json

{
  "email": "awilson@example.com",
  "password": "ClientPass1$trong"
}

HTTP/1.1 200
[Captures]
token: jsonpath "$.accessToken"



# Add wish list item
POST http://localhost:8080/wishlist/me
Authorization: Bearer {{token}}
Content-Type: application/json

{
    "productId": "16dadb2c-4653-45e7-a7ab-3049ba035bd5"
}

HTTP/1.1 201
[Captures]
wishListItemId: jsonpath "$.wishListItemId"
[Asserts]
jsonpath "$.wishListItemId" isString
jsonpath "$.addedAt" isIsoDate
jsonpath "$.product.productId" == "16dadb2c-4653-45e7-a7ab-3049ba035bd5"



# Get my wish list items
GET http://localhost:8080/users/me/wishListItems?size=5&page=0&order=desc
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].wishListItemId" == {{wishListItemId}}
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].product.productId" == "16dadb2c-4653-45e7-a7ab-3049ba035bd5"

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Remove my wish list item
DELETE http://localhost:8080/wishlist/me/{{wishListItemId}}
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Wishlist item with id: {{wishListItemId}}, has been removed from wishlist."



# Add cart item
POST http://localhost:8080/cart/me
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "productId": "16dadb2c-4653-45e7-a7ab-3049ba035bd5",
  "quantity": 7
}

HTTP/1.1 201
[Captures]
cartItemId: jsonpath "$.cartItemId"
[Asserts]
jsonpath "$.cartItemId" isString
jsonpath "$.quantity" == 7
jsonpath "$.addedAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate
jsonpath "$.product.productId" == "16dadb2c-4653-45e7-a7ab-3049ba035bd5"



# Get my cart items
GET http://localhost:8080/users/me/cartItems?size=5&page=0&order=asc&sortBy=quantity
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].cartItemId" == {{cartItemId}}
jsonpath "$.content[0].quantity" == 7
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate
jsonpath "$.content[0].product.productId" == "16dadb2c-4653-45e7-a7ab-3049ba035bd5"

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Update my cart item
PATCH http://localhost:8080/cart/me/{{cartItemId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "quantity": 3
}

HTTP/1.1 200
[Asserts]
jsonpath "$.cartItemId" == {{cartItemId}}
jsonpath "$.quantity" == 3
jsonpath "$.addedAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate
jsonpath "$.product.productId" == "16dadb2c-4653-45e7-a7ab-3049ba035bd5"



# Remove my cart item
DELETE http://localhost:8080/cart/me/{{cartItemId}}
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Cart item with id: {{cartItemId}}, has been removed from cart."



# Again add cart item (for creating an order)
POST http://localhost:8080/cart/me
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "productId": "16dadb2c-4653-45e7-a7ab-3049ba035bd5",
  "quantity": 7
}

HTTP/1.1 201



# Add order
POST http://localhost:8080/orders/me
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "firstName": "Ann",
  "lastName": "Wilson",
  "address": "Hauptstraße 123, Apt. 456",
  "zipCode": "12345",
  "city": "Stuttgart",
  "phone": "+491234567",
  "deliveryMethod": "CUSTOMER_PICKUP"
}

HTTP/1.1 201
[Captures]
orderId: jsonpath "$.orderId"
[Asserts]
jsonpath "$.orderId" isString
jsonpath "$.firstName" == "Ann"
jsonpath "$.lastName" == "Wilson"
jsonpath "$.address" == "Hauptstraße 123, Apt. 456"
jsonpath "$.zipCode" == "12345"
jsonpath "$.city"  == "Stuttgart"
jsonpath "$.phone" == "+491234567"
jsonpath "$.deliveryMethod" == "CUSTOMER_PICKUP"
jsonpath "$.orderStatus" == "CREATED"
jsonpath "$.createdAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Get my orders
GET http://localhost:8080/users/me/orders?size=5&page=0&order=desc&sortBy=createdAt
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].orderId" == {{orderId}}
jsonpath "$.content[0].firstName" == "Ann"
jsonpath "$.content[0].lastName" == "Wilson"
jsonpath "$.content[0].address" == "Hauptstraße 123, Apt. 456"
jsonpath "$.content[0].zipCode" == "12345"
jsonpath "$.content[0].city"  == "Stuttgart"
jsonpath "$.content[0].phone" == "+491234567"
jsonpath "$.content[0].deliveryMethod" == "CUSTOMER_PICKUP"
jsonpath "$.content[0].orderStatus" == "CREATED"
jsonpath "$.content[0].createdAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get my order by id
GET http://localhost:8080/orders/me/{{orderId}}
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.orderId" == {{orderId}}
jsonpath "$.firstName" == "Ann"
jsonpath "$.lastName" == "Wilson"
jsonpath "$.address" == "Hauptstraße 123, Apt. 456"
jsonpath "$.zipCode" == "12345"
jsonpath "$.city"  == "Stuttgart"
jsonpath "$.phone" == "+491234567"
jsonpath "$.deliveryMethod" == "CUSTOMER_PICKUP"
jsonpath "$.orderStatus" == "CREATED"
jsonpath "$.createdAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



#Get order status
GET http://localhost:8080/orders/me/{{orderId}}/status
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Order with id: {{orderId}} has status 'CREATED'."



# Get my order items
GET http://localhost:8080/orders/me/{{orderId}}/orderItems?size=5&page=0&order=asc&sortBy=priceAtPurchase
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].orderItemId" isString
jsonpath "$.content[0].quantity" == 7
jsonpath "$.content[0].priceAtPurchase" isFloat
jsonpath "$.content[0].product.productId" == "16dadb2c-4653-45e7-a7ab-3049ba035bd5"

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



#Update my order
PATCH http://localhost:8080/orders/me/{{orderId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{
    "firstName": "Anna",
    "lastName": "Wilson-Becker",
    "address": "Neustraße 456, Apt. 123",
    "zipCode": "54321",
    "city": "Berlin",
    "phone": "+497654321",
    "deliveryMethod": "COURIER_DELIVERY"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.orderId" == {{orderId}}
jsonpath "$.firstName" == "Anna"
jsonpath "$.lastName" == "Wilson-Becker"
jsonpath "$.address" == "Neustraße 456, Apt. 123"
jsonpath "$.zipCode" == "54321"
jsonpath "$.city"  == "Berlin"
jsonpath "$.phone" == "+497654321"
jsonpath "$.deliveryMethod" == "COURIER_DELIVERY"
jsonpath "$.orderStatus" == "CREATED"
jsonpath "$.createdAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Cancel order
PATCH http://localhost:8080/orders/me/{{orderId}}/cancel
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Order with id: {{orderId}} was canceled."



# Get my profile
GET http://localhost:8080/users/me/profile
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.userId" isString
jsonpath "$.email" == "awilson@example.com"
jsonpath "$.firstName" isString
jsonpath "$.lastName" isString
jsonpath "$.userRole" isString
jsonpath "$.isEnabled" isBoolean
jsonpath "$.isNonLocked" isBoolean
jsonpath "$.registeredAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Update my profil
PATCH http://localhost:8080/users/me/profile
Authorization: Bearer {{token}}
Content-Type: application/json

{
    "firstName": "Annette",
    "lastName": "Wilsons"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.userId" isString
jsonpath "$.email" == "awilson@example.com"
jsonpath "$.firstName" == "Annette"
jsonpath "$.lastName" == "Wilsons"
jsonpath "$.userRole" isString
jsonpath "$.isEnabled" isBoolean
jsonpath "$.isNonLocked" isBoolean
jsonpath "$.registeredAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Change my password
PATCH http://localhost:8080/users/me/change-password
Authorization: Bearer {{token}}
Content-Type: application/json

{
    "currentPassword": "ClientPass1$trong",
    "newPassword": "ClientPass1$trongNew",
    "confirmNewPassword": "ClientPass1$trongNew"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Password for user with email: awilson@example.com, has been successfully changed."



# Unregister my account
PATCH http://localhost:8080/users/me/unregister
Authorization: Bearer {{token}}
Content-Type: application/json

{
    "password": "ClientPass1$trongNew",
    "confirmPassword": "ClientPass1$trongNew"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "User with email: awilson@example.com, has been unregistered."


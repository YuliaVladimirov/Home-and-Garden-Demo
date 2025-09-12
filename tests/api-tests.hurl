# Hurl tests for public endpoints without authentication.
# Tests ensure that endpoints intended for open/public use return the expected status codes and response bodies (no authentication or authorization is required).

# Get active categories
GET http://localhost:8080/categories?size=5&page=0&order=desc&sortBy=categoryName
Authorization: None
Content-Type: application/json

HTTP/1.1 200
[Captures]
categoryId: jsonpath "$.content[0].categoryId"
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].categoryId" isString
jsonpath "$.content[0].categoryName" isString
jsonpath "$.content[0].categoryStatus" == "ACTIVE"
jsonpath "$.content[0].createdAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get category products
GET http://localhost:8080/categories/{{categoryId}}/products?minPrice=5.00&maxPrice=30.00&size=5&page=0&order=asc&sortBy=addedAt
Authorization: None
Content-Type: application/json

HTTP/1.1 200
[Captures]
productId: jsonpath "$.content[0].productId"
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].productId" isString
jsonpath "$.content[0].productName" isString
jsonpath "$.content[0].description" isString
jsonpath "$.content[0].listPrice" isFloat
jsonpath "$.content[0].currentPrice" > 5.00
jsonpath "$.content[0].currentPrice" < 30.00
jsonpath "$.content[0].productStatus" == "AVAILABLE"
jsonpath "$.content[0].imageUrl" isString
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get product by id
GET http://localhost:8080/products/{{productId}}
Authorization: None
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.productId" isString
jsonpath "$.productName" isString
jsonpath "$.description" isString
jsonpath "$.listPrice" isFloat
jsonpath "$.currentPrice" isFloat
jsonpath "$.productStatus" isString
jsonpath "$.imageUrl" isString
jsonpath "$.addedAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Register user
POST http://localhost:8080/auth/register
Authorization: None
Content-Type: application/json

{
    "email": "awilson@example.com",
    "password": "ClientPass1$trong",
    "confirmPassword": "ClientPass1$trong",
    "firstName": "Ann",
    "lastName": "Wilson"
}

HTTP/1.1 201
[Asserts]
jsonpath "$.userId" exists
jsonpath "$.email" == "awilson@example.com"
jsonpath "$.firstName" == "Ann"
jsonpath "$.lastName" == "Wilson"
jsonpath "$.userRole" == "CLIENT"
jsonpath "$.isEnabled" == true
jsonpath "$.isNonLocked" == true
jsonpath "$.registeredAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Login
POST http://localhost:8080/auth/login
Authorization: None
Content-Type: application/json

{
  "email": "awilson@example.com",
  "password": "ClientPass1$trong"
}

HTTP/1.1 200
[Captures]
refreshToken: jsonpath "$.refreshToken"

[Asserts]
jsonpath "$.type" == "Bearer"
jsonpath "$.accessToken" isString
jsonpath "$.refreshToken" isString



# Get new access token
POST http://localhost:8080/auth/token
Authorization: None
Content-Type: application/json

{
    "refreshToken": "{{refreshToken}}"
}

HTTP/1.1 200
[Captures]
accessToken: jsonpath "$.accessToken"
[Asserts]
jsonpath "$.type" == "Bearer"
jsonpath "$.accessToken" isString



# Hurl tests for self-access endpoints with authentication.
# Tests ensure that endpoints correctly return data only for the authenticated user, enforcing that users can access their own data but not data belonging to others.

# Add wish list item
POST http://localhost:8080/wishlist/me
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Wishlist item with id: {{wishListItemId}}, has been removed from wishlist."



# Add cart item
POST http://localhost:8080/cart/me
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Cart item with id: {{cartItemId}}, has been removed from cart."



# Again add cart item (for creating an order)
POST http://localhost:8080/cart/me
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "productId": "16dadb2c-4653-45e7-a7ab-3049ba035bd5",
  "quantity": 7
}

HTTP/1.1 201



# Add order
POST http://localhost:8080/orders/me
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Order with id: {{orderId}} has status 'CREATED'."



# Get my order items
GET http://localhost:8080/orders/me/{{orderId}}/orderItems?size=5&page=0&order=asc&sortBy=priceAtPurchase
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Order with id: {{orderId}} was canceled."



# Get my profile
GET http://localhost:8080/users/me/profile
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
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
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
    "password": "ClientPass1$trongNew",
    "confirmPassword": "ClientPass1$trongNew"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "User with email: awilson@example.com, has been unregistered."



# Hurl tests for admin-access endpoints with authentication.
# Tests ensure that endpoints restricted to administrators enforce proper authorization and return the expected responses only when accessed with valid admin credentials.

# Login admin and store token
POST http://localhost:8080/auth/login
Authorization: None
Content-Type: application/json

{
  "email": "michaelnguyen@example.com",
  "password": "AdminPass1$trong"
}

HTTP/1.1 200
[Captures]
accessToken: jsonpath "$.accessToken"



# Get categories by status
GET http://localhost:8080/categories/status?categoryStatus=active&size=5&page=0&order=desc&sortBy=categoryName
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].categoryId" isString
jsonpath "$.content[0].categoryName" isString
jsonpath "$.content[0].categoryStatus" == "ACTIVE"
jsonpath "$.content[0].createdAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Add category
POST http://localhost:8080/categories
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "categoryName": "Garden furniture"
}

HTTP/1.1 201
[Captures]
categoryId: jsonpath "$.categoryId"
[Asserts]
jsonpath "$.categoryId" isString
jsonpath "$.categoryName" == "Garden furniture"
jsonpath "$.categoryStatus" == "ACTIVE"
jsonpath "$.createdAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Update category
PATCH http://localhost:8080/categories/{{categoryId}}
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "categoryName": "Building Material"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.categoryId" == {{categoryId}}
jsonpath "$.categoryName" == "Building Material"
jsonpath "$.categoryStatus" == "ACTIVE"
jsonpath "$.createdAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Get products by status
GET http://localhost:8080/products/status?productStatus=out_of_stock&size=5&page=0&order=asc&sortBy=addedAt
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].productId" isString
jsonpath "$.content[0].productName" isString
jsonpath "$.content[0].description" isString
jsonpath "$.content[0].listPrice" isFloat
jsonpath "$.content[0].currentPrice" isFloat
jsonpath "$.content[0].productStatus" == "OUT_OF_STOCK"
jsonpath "$.content[0].imageUrl" isString
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get top products (best selling or most cancelled)
GET http://localhost:8080/products/top?status=paid&size=5&page=0
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].productId" isString
jsonpath "$.content[0].productName" isString
jsonpath "$.content[0].listPrice" isFloat
jsonpath "$.content[0].currentPrice" isFloat
jsonpath "$.content[0].productStatus" isString
jsonpath "$.content[0].imageUrl" isString
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate
jsonpath "$.content[0].totalAmount" isInteger

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get pending products (in certain order status)
GET http://localhost:8080/products/pending?orderStatus=on_the_way&days=5&size=5&page=0
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].productId" isString
jsonpath "$.content[0].productName" isString
jsonpath "$.content[0].listPrice" isFloat
jsonpath "$.content[0].currentPrice" isFloat
jsonpath "$.content[0].productStatus" isString
jsonpath "$.content[0].imageUrl" isString
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate
jsonpath "$.content[0].totalAmount" isInteger

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get profit by period
GET http://localhost:8080/products/profit?timeUnit=day&timePeriod=60
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.timeUnit" == "DAY"
jsonpath "$.timePeriod" == 60
jsonpath "$.profit" isNumber



# Add product
POST http://localhost:8080/products
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
    "categoryId": "{{categoryId}}",
    "productName": "Premium Fiber Cement Board",
    "description": "Durable, weather-resistant fiber cement board ideal for exterior and interior wall cladding",
    "listPrice": 25.99,
    "imageUrl": "https://example.com/images/premium_fiber_cement_board.jpg"

}

HTTP/1.1 201
[Captures]
productId: jsonpath "$.productId"
[Asserts]
jsonpath "$.productId" isString
jsonpath "$.productName" == "Premium Fiber Cement Board"
jsonpath "$.description" == "Durable, weather-resistant fiber cement board ideal for exterior and interior wall cladding"
jsonpath "$.listPrice" == 25.99
jsonpath "$.currentPrice" == 25.99
jsonpath "$.productStatus" == "AVAILABLE"
jsonpath "$.imageUrl" == "https://example.com/images/premium_fiber_cement_board.jpg"
jsonpath "$.addedAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Update product
PATCH http://localhost:8080/products/{{productId}}
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
    "productName": "Fiber Cement Board",
    "description": "Durable fiber cement board ideal for exterior and interior wall cladding",
    "listPrice": 20.99,
    "currentPrice": 15.99,
    "imageUrl": "https://example.com/images/fiber_cement_board.jpg"
}

HTTP/1.1 200
[Asserts]
jsonpath "$.productId" isString
jsonpath "$.productName" == "Fiber Cement Board"
jsonpath "$.description" == "Durable fiber cement board ideal for exterior and interior wall cladding"
jsonpath "$.listPrice" == 20.99
jsonpath "$.currentPrice" == 15.99
jsonpath "$.productStatus" == "AVAILABLE"
jsonpath "$.imageUrl" == "https://example.com/images/fiber_cement_board.jpg"
jsonpath "$.addedAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Set product status
PATCH http://localhost:8080/products/{{productId}}/status?productStatus=out_of_stock
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Status 'OUT_OF_STOCK' was set for the product with id: {{productId}}."



# Set category status
PATCH http://localhost:8080/categories/{{categoryId}}/status?categoryStatus=inactive
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Status 'INACTIVE' was set for category with id: {{categoryId}}."



# Get users by status
GET http://localhost:8080/users?isEnabled=true&isNonLocked=true&size=5&page=0&order=desc&sortBy=registeredAt
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Captures]
userId: jsonpath "$.content[0].userId"
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].userId" isString
jsonpath "$.content[0].email" isString
jsonpath "$.content[0].firstName" isString
jsonpath "$.content[0].lastName" isString
jsonpath "$.content[0].userRole" isString
jsonpath "$.content[0].isEnabled" == true
jsonpath "$.content[0].isNonLocked" == true
jsonpath "$.content[0].registeredAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get user by id
GET http://localhost:8080/users/{{userId}}
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.userId" isString
jsonpath "$.email" isString
jsonpath "$.firstName" isString
jsonpath "$.lastName" isString
jsonpath "$.userRole" isString
jsonpath "$.isEnabled" == true
jsonpath "$.isNonLocked" == true
jsonpath "$.registeredAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



# Get user's wish list items
GET http://localhost:8080/users/{{userId}}/wishListItems?size=5&page=0&order=desc
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].wishListItemId" isString
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].product" exists

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get user's cart items
GET http://localhost:8080/users/{{userId}}/cartItems?size=5&page=0&order=asc&sortBy=quantity
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].cartItemId" isString
jsonpath "$.content[0].quantity" isInteger
jsonpath "$.content[0].addedAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate
jsonpath "$.content[0].product" exists

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get user's orders
GET http://localhost:8080/users/{{userId}}/orders?size=5&page=0&order=desc&sortBy=createdAt
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Captures]
orderId: jsonpath "$.content[0].orderId"
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].orderId" isString
jsonpath "$.content[0].firstName" isString
jsonpath "$.content[0].lastName" isString
jsonpath "$.content[0].address" isString
jsonpath "$.content[0].zipCode" isString
jsonpath "$.content[0].city"  isString
jsonpath "$.content[0].phone" isString
jsonpath "$.content[0].deliveryMethod" isString
jsonpath "$.content[0].orderStatus" isString
jsonpath "$.content[0].createdAt" isIsoDate
jsonpath "$.content[0].updatedAt" isIsoDate

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Get user's order by id
GET http://localhost:8080/orders/{{orderId}}
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Captures]
status: jsonpath "$.orderStatus"
[Asserts]
jsonpath "$.orderId" == {{orderId}}
jsonpath "$.firstName" isString
jsonpath "$.lastName" isString
jsonpath "$.address" isString
jsonpath "$.zipCode" isString
jsonpath "$.city"  isString
jsonpath "$.phone" isString
jsonpath "$.deliveryMethod" isString
jsonpath "$.orderStatus" isString
jsonpath "$.createdAt" isIsoDate
jsonpath "$.updatedAt" isIsoDate



#Get user's order status
GET http://localhost:8080/orders/{{orderId}}/status
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Order with id: {{orderId}} has status '{{status}}'."



# Get my order items
GET http://localhost:8080/orders/{{orderId}}/orderItems?size=5&page=0&order=asc&sortBy=priceAtPurchase
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.content" isCollection
jsonpath "$.content[0].orderItemId" isString
jsonpath "$.content[0].quantity" isInteger
jsonpath "$.content[0].priceAtPurchase" isFloat
jsonpath "$.content[0].product" exists

jsonpath "$.pageable.pageSize" == 5
jsonpath "$.pageable.pageNumber" == 0
jsonpath "$.totalElements" isInteger
jsonpath "$.totalPages" isInteger



# Toggle order status
PATCH http://localhost:8080/orders/{{orderId}}/status
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" exists



# Set user's role
PATCH http://localhost:8080/users/{{userId}}/role?role=ADMINISTRATOR
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "UserRole ADMINISTRATOR was set for user with id: {{userId}}."



# Toggle user's lock state
PATCH http://localhost:8080/users/{{userId}}/toggle-lock
Authorization: Bearer {{accessToken}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" exists

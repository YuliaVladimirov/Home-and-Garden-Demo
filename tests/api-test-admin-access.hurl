
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
token: jsonpath "$.accessToken"



# Get categories by status
GET http://localhost:8080/categories/status?categoryStatus=active&size=5&page=0&order=desc&sortBy=categoryName
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.timeUnit" == "DAY"
jsonpath "$.timePeriod" == 60
jsonpath "$.profit" isNumber



# Add product
POST http://localhost:8080/products
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Status 'OUT_OF_STOCK' was set for the product with id: {{productId}}."



# Set category status
PATCH http://localhost:8080/categories/{{categoryId}}/status?categoryStatus=inactive
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Status 'INACTIVE' was set for category with id: {{categoryId}}."



# Get users by status
GET http://localhost:8080/users?isEnabled=true&isNonLocked=true&size=5&page=0&order=desc&sortBy=registeredAt
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "Order with id: {{orderId}} has status '{{status}}'."



# Get my order items
GET http://localhost:8080/orders/{{orderId}}/orderItems?size=5&page=0&order=asc&sortBy=priceAtPurchase
Authorization: Bearer {{token}}
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
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" exists



# Set user's role
PATCH http://localhost:8080/users/{{userId}}/role?role=ADMINISTRATOR
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" == "UserRole ADMINISTRATOR was set for user with id: {{userId}}."



# Toggle user's lock state
PATCH http://localhost:8080/users/{{userId}}/toggle-lock
Authorization: Bearer {{token}}
Content-Type: application/json

HTTP/1.1 200
[Asserts]
jsonpath "$.message" exists

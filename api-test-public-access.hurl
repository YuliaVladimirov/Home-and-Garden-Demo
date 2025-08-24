
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
[Asserts]
jsonpath "$.type" == "Bearer"
jsonpath "$.accessToken" isString


#### This is the Entity Relationship diagram for the application's database schema:

```mermaid
erDiagram
%%{init: {
  "theme": "default",
  "themeCSS": [
    ".er.relationshipLabel { fill: black; }",
    ".er.relationshipLabelBox { fill: white; }",
    ".er.entityBox { fill: lightgray}",
    "[id^=entity-users] .er.entityBox { fill: lightgreen;} ",
    "[id^=entity-categories] .er.entityBox { fill: powderblue;} ",
    "[id^=entity-products] .er.entityBox { fill: powderblue;} ",
    "[id^=entity-wish_list_items] .er.entityBox { fill: lightgreen;} ",
    "[id^=entity-cart_items] .er.entityBox { fill: lightgreen;} ",
    "[id^=entity-orders] .er.entityBox { fill: orange;} ",
    "[id^=entity-order_items] .er.entityBox { fill: orange;} "
    ]
}}%%

    users {
        user_id UUID PK
        email VARCHAR(100) UK "UNIQUE"
        password_hash VARCHAR(255)
        first_name VARCHAR(50)
        last_name VARCHAR(50)
        role VARCHAR(50) "CLIENT, ADMINISTRATOR"
        is_enabled BOOLEAN
        is_non_locked BOOLEAN
        registered_at TIMESTAMP
        updated_at TIMESTAMP "NULL"
        refresh_token VARCHAR(255) "NULL"
    }

    wish_list_items {
        wish_list_item_id UUID PK
        added_at TIMESTAMP
        product_id UUID FK
        user_id UUID FK
    }

    cart_items {
        cart_item_id UUID PK
        quantity INT
        added_at TIMESTAMP
        updated_at TIMESTAMP "NULL"
        product_id UUID FK
        user_id UUID FK
    }
    
    orders {
        order_id UUID PK
        first_name VARCHAR(50)
        last_name VARCHAR(50)
        address VARCHAR(255)
        zip_code VARCHAR(50)
        city VARCHAR(100)
        phone VARCHAR(50)
        delivery_method VARCHAR(50) "COURIER_DELIVERY,CUSTOMER_PICKUP"
        order_status VARCHAR(50) "CREATED,PAID, ON_THE_WAY, DELIVERED, CANCELED, RETURNED"
        created_at TIMESTAMP
        updated_at TIMESTAMP "NULL"
        user_id UUID FK
    }

    order_items {
        order_item_id UUID PK
        quantity INT
        price_at_purchase DECIMAL
        order_id UUID FK
        product_id UUID FK
    }
    
    categories {
        category_id UUID PK
        category_name VARCHAR(100)
        category_status VARCHAR(50) "ACTIVE, INACTIVE"
        created_at TIMESTAMP
        updated_at TIMESTAMP "NULL"
    } 
    
    products {
        product_id UUID PK
        product_name VARCHAR(255)
        description VARCHAR(255)
        list_price DECIMAL
        current_price DECIMAL
        image_url VARCHAR(255)
        added_at TIMESTAMP
        updated_at TIMESTAMP "NULL"
        product_status VARCHAR(50) "AVAILABLE, OUT_OF_STOCK, SOLD_OUT"
        category_id UUID FK
    }

    users ||--o{ wish_list_items : "1 → many"
    users ||--o{ cart_items : "1 → many"
    users ||--o{ orders : "1 → many"
    orders ||--o{ order_items : "1 → many"
    products ||--o{ wish_list_items : "1 → many"
    products ||--o{ cart_items : "1 → many"
    products ||--o{ order_items : "1 → many"
    categories ||--o{ products : "1 → many"

```

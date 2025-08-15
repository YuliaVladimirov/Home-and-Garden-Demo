--liquibase formatted sql


-- ========================================
-- USERS
-- ========================================

--changeset yulia:2025-08-15-create-users
CREATE TABLE users (user_id UUID PRIMARY KEY,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    role VARCHAR(50) NOT NULL CHECK (role IN ('CLIENT', 'ADMINISTRATOR')),
                    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    is_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
                    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NULL,
                    refresh_token VARCHAR(255) NULL,
                    password_reset_token VARCHAR(255) NULL);


--changeset yulia:2025-08-15-index-users-first-name
    CREATE INDEX index_users_first_name ON users(first_name);
--changeset yulia:2025-08-15-index-users-last-name
CREATE INDEX index_users_last_name ON users(last_name);
--changeset yulia:2025-08-15-index-users-role
CREATE INDEX index_users_role ON users(role);
--changeset yulia:2025-08-15-index-registered-at
CREATE INDEX index_users_registered_at ON users(registered_at);
--changeset yulia:2025-08-15-index-updated-at
CREATE INDEX index_users_updated_at ON users(updated_at);
--changeset yulia:2025-08-15-index-users-is-enabled
CREATE INDEX index_users_is_enabled ON users(is_enabled);
--changeset yulia:2025-08-15-index-users-is-non-locked
CREATE INDEX index_users_is_non_locked ON users(is_non_locked);


-- ========================================
-- CATEGORIES
-- ========================================

--changeset yulia:2025-08-15-create-categories
CREATE TABLE categories (category_id UUID PRIMARY KEY,
                         category_name VARCHAR(100) NOT NULL,
                         category_status VARCHAR(50) NOT NULL CHECK (category_status IN ('ACTIVE', 'INACTIVE')),
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NULL);

--changeset yulia:2025-08-15-index-categories-category_name
CREATE INDEX index_categories_name ON categories(category_name);
--changeset yulia:2025-08-15-index-categories-category_status
CREATE INDEX index_categories_category_status ON categories(category_status);
--changeset yulia:2025-08-15-index-categories-created-at
CREATE INDEX index_categories_created_at ON categories(created_at);
--changeset yulia:2025-08-15-index-categories-updated-at
CREATE INDEX index_categories_updated_at ON categories(updated_at);


-- ========================================
-- PRODUCTS
-- ========================================

--changeset yulia:2025-08-15-create-products
CREATE TABLE products (product_id UUID PRIMARY KEY,
                       product_name VARCHAR(255) NOT NULL,
                       description VARCHAR(255) NOT NULL,
                       list_price DECIMAL (10, 2) NOT NULL,
                       current_price DECIMAL (10, 2) NULL,
                       image_url VARCHAR(255) NULL,
                       added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NULL,
                       product_status VARCHAR(50) NOT NULL CHECK (product_status IN ('AVAILABLE', 'OUT_OF_STOCK', 'SOLD_OUT')),
                       category_id UUID NOT NULL,
                       CONSTRAINT foreign_key_product_category
                       FOREIGN KEY (category_id) REFERENCES categories(category_id));

--changeset yulia:2025-08-15-index-products-category-id
CREATE INDEX index_products_category_id ON products(category_id);
--changeset yulia:2025-08-15-index-products-product-name
CREATE INDEX index_products_product_name ON products(product_name);
--changeset yulia:2025-08-15-index-products-product-status
CREATE INDEX index_products_product_status ON products(product_status);
--changeset yulia:2025-08-15-index-products-list-price
CREATE INDEX index_products_list_price ON products(list_price);
--changeset yulia:2025-08-15-index-products-current-price
CREATE INDEX index_products_current_price ON products(current_price);
--changeset yulia:2025-08-15-index-products-added-at
CREATE INDEX index_products_added_at ON products(added_at);
--changeset yulia:2025-08-15-index-products-updated-at
CREATE INDEX index_products_updated_at ON products(updated_at);


-- ========================================
-- WISH LIST ITEMS
-- ========================================

--changeset yulia:2025-08-15-create-wish-list-items
CREATE TABLE wish_list_items (wish_list_item_id UUID PRIMARY KEY,
                              added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              product_id UUID NOT NULL,
                              user_id UUID NOT NULL,
                              CONSTRAINT foreign_key_wish_list_item_product
                                  FOREIGN KEY (product_id) REFERENCES products(product_id),
                              CONSTRAINT foreign_key_wish_list_item_user
                                  FOREIGN KEY (user_id) REFERENCES users(user_id));

--changeset yulia:2025-08-15-index-wish-list-items-user-id
CREATE INDEX index_wish_list_items_user_id ON wish_list_items(user_id);
--changeset yulia:2025-08-15-index-wish-list-items-product-id
CREATE INDEX index_wish_list_items_product_id ON wish_list_items(product_id);
--changeset yulia:2025-08-15-index-wish-list-items-added-at
CREATE INDEX index_wishlist_added_at ON wish_list_items(added_at);



-- ========================================
-- CART ITEMS
-- ========================================

--changeset yulia:2025-08-15-create-cart-items
CREATE TABLE cart_items (cart_item_id UUID PRIMARY KEY,
                         quantity INT NOT NULL CHECK (quantity > 0),
                         added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NULL,
                         product_id UUID NOT NULL,
                         user_id UUID NOT NULL,
                         CONSTRAINT foreign_key_cart_item_product
                             FOREIGN KEY (product_id) REFERENCES products(product_id),
                         CONSTRAINT foreign_key_cart_item_user
                             FOREIGN KEY (user_id) REFERENCES users(user_id));

--changeset yulia:2025-08-15-index-cart-items-user-id
CREATE INDEX index_cart_items_user_id ON cart_items(user_id);
--changeset yulia:2025-08-15-index-cart-items-product-id
CREATE INDEX index_cart_items_product_id ON cart_items(product_id);
--changeset yulia:2025-08-15-index-cart-items-added-at
CREATE INDEX index_cart_items_added_at ON cart_items(added_at);
--changeset yulia:2025-08-15-index-cart-items-quantity
CREATE INDEX index_cart_items_quantity ON cart_items(quantity);


-- ========================================
-- ORDERS
-- ========================================

--changeset yulia:2025-08-15-create-orders
CREATE TABLE orders (order_id UUID PRIMARY KEY,
                     first_name VARCHAR(50) NOT NULL,
                     last_name VARCHAR(50) NOT NULL,
                     address VARCHAR(255) NOT NULL,
                     zip_code VARCHAR(50) NOT NULL,
                     city VARCHAR(100) NOT NULL,
                     phone VARCHAR(50) NOT NULL,
                     delivery_method VARCHAR(50) NOT NULL CHECK (delivery_method IN ('COURIER_DELIVERY','CUSTOMER_PICKUP')),
                     order_status VARCHAR(50) NOT NULL CHECK (order_status IN ('CREATED','PAID', 'ON_THE_WAY', 'DELIVERED', 'CANCELED','RETURNED')),
                     created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                     updated_at TIMESTAMP NULL,
                     user_id UUID NOT NULL,
                     CONSTRAINT foreign_key_order_user
                         FOREIGN KEY (user_id) REFERENCES users(user_id));

--changeset yulia:2025-08-15-index-orders-users-id
CREATE INDEX index_orders_user_id ON orders(user_id);
--changeset yulia:2025-08-15-index-orders-order_status
CREATE INDEX index_orders_order_status ON orders(order_status);
--changeset yulia:2025-08-15-index-orders-created-at
CREATE INDEX index_orders_created_at ON orders(created_at);


-- ========================================
-- ORDER ITEMS
-- ========================================

--changeset yulia:2025-08-15-create-order-items
CREATE TABLE order_items (order_item_id UUID PRIMARY KEY,
                          quantity INT NOT NULL CHECK (quantity > 0),
                          price_at_purchase DECIMAL (10, 2) NOT NULL,
                          order_id UUID NOT NULL,
                          product_id UUID NOT NULL,
                          CONSTRAINT foreign_key_order_item_product
                              FOREIGN KEY (product_id) REFERENCES products(product_id),
                          CONSTRAINT foreign_key_order_item_order
                              FOREIGN KEY (order_id) REFERENCES orders(order_id));

--changeset yulia:2025-08-15-index-order-items-order-id
CREATE INDEX index_order_items_order_id ON order_items(order_id);
--changeset yulia:2025-08-15-index-order-items-product-id
CREATE INDEX index_order_items_product_id ON order_items(product_id);
--changeset yulia:2025-08-15-index-order-items-quantity
CREATE INDEX index_order_items_quantity ON order_items(quantity);
--changeset yulia:2025-08-15-index-order-items-price-at-purchase
CREATE INDEX index_order_items_price_at_purchase ON order_items(price_at_purchase);

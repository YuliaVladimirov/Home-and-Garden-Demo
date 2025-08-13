-- liquibase formatted sql

-- changeset yulia:create_table_users
CREATE TABLE users (user_id UUID PRIMARY KEY,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    role VARCHAR(50) CHECK (role IN ('CLIENT', 'ADMINISTRATOR')) NOT NULL,
                    is_enabled BOOLEAN NOT NULL,
                    is_non_locked BOOLEAN NOT NULL,
                    registered_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NULL,
                    refresh_token VARCHAR(255) NULL,
                    password_reset_token VARCHAR(255) NULL);

-- changeset yulia:create_table_categories
CREATE TABLE categories (category_id UUID PRIMARY KEY,
                         category_name VARCHAR(100) NOT NULL,
                         category_status VARCHAR(50) CHECK (category_status IN ('ACTIVE', 'INACTIVE')) NOT NULL,
                         created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP NULL);

-- changeset yulia:create_table_products
CREATE TABLE products (product_id UUID PRIMARY KEY,
                       product_name VARCHAR(255) NOT NULL,
                       description VARCHAR(255) NOT NULL,
                       list_price DECIMAL (10, 2) NULL,
                       current_price DECIMAL (10, 2) NULL,
                       image_url VARCHAR(255) NULL,
                       added_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NULL,
                       product_status VARCHAR(50) CHECK (product_status IN ('AVAILABLE', 'OUT_OF_STOCK', 'SOLD_OUT')) NOT NULL,
                       category_id UUID NOT NULL);

-- changeset yulia:create_table_wish_list_items
CREATE TABLE wish_list_items (wish_list_item_id UUID PRIMARY KEY,
                              added_at TIMESTAMP NOT NULL,
                              product_id UUID NOT NULL,
                              user_id UUID NOT NULL);

-- changeset yulia:create_table_cart_items
CREATE TABLE cart_items (cart_item_id UUID PRIMARY KEY,
                         quantity INT NOT NULL,
                         added_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP NULL,
                         product_id UUID NOT NULL,
                         user_id UUID NOT NULL);

-- changeset yulia:create_table_orders
CREATE TABLE orders (order_id UUID PRIMARY KEY,
                     first_name VARCHAR(50) NOT NULL,
                     last_name VARCHAR(50) NOT NULL,
                     address VARCHAR(255) NOT NULL,
                     zip_code VARCHAR(50) NOT NULL,
                     city VARCHAR(100) NOT NULL,
                     phone VARCHAR(50) NOT NULL,
                     delivery_method VARCHAR(50) CHECK (delivery_method IN ('COURIER_DELIVERY','CUSTOMER_PICKUP')) NOT NULL,
                     order_status VARCHAR(50) CHECK (order_status IN ('CREATED','PAID', 'ON_THE_WAY', 'DELIVERED', 'CANCELED','RETURNED')) NOT NULL,
                     created_at TIMESTAMP NULL,
                     updated_at TIMESTAMP NULL,
                     user_id UUID NOT NULL);

-- changeset yulia:create_table_order_items
CREATE TABLE order_items (order_item_id UUID PRIMARY KEY,
                          quantity INT NOT NULL,
                          price_at_purchase DECIMAL (10, 2) NOT NULL,
                          order_id UUID NOT NULL,
                          product_id UUID NOT NULL);


-- changeset yulia:create_foreign_key_products_categories
ALTER TABLE products ADD CONSTRAINT foreign_key_products_categories FOREIGN KEY (category_id) REFERENCES categories (category_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_wish_list_items_users
ALTER TABLE wish_list_items ADD CONSTRAINT foreign_key_wish_list_items_users FOREIGN KEY (user_id) REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_wish_list_items_products
ALTER TABLE wish_list_items ADD CONSTRAINT foreign_key_wish_list_items_products FOREIGN KEY (product_id) REFERENCES products (product_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_cart_items_users
ALTER TABLE cart_items ADD CONSTRAINT foreign_key_cart_items_users FOREIGN KEY (user_id) REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_cart_items_products
ALTER TABLE cart_items ADD CONSTRAINT foreign_key_cart_items_products FOREIGN KEY (product_id) REFERENCES products (product_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_orders_users
ALTER TABLE orders ADD CONSTRAINT foreign_key_orders_users FOREIGN KEY (user_id) REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_order_items_orders
ALTER TABLE order_items ADD CONSTRAINT foreign_key_order_items_orders FOREIGN KEY (order_id) REFERENCES orders (order_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- changeset yulia:create_foreign_key_order_items_products
ALTER TABLE order_items ADD CONSTRAINT foreign_key_order_items_products FOREIGN KEY (product_id) REFERENCES products (product_id) ON UPDATE CASCADE ON DELETE SET NULL;




-- changeset yulia:create_index_products_to_categories
CREATE INDEX foreign_key_products_categories ON products(category_id);


-- changeset yulia:create_index_wish_list_items_to_users
CREATE INDEX foreign_key_wish_list_items_users ON wish_list_items(user_id);
-- changeset yulia:create_index_wish_list_items_to_products
CREATE INDEX foreign_key_wish_list_items_products ON wish_list_items(product_id);


-- changeset yulia:create_index_cart_items_to_users
CREATE INDEX foreign_key_cart_items_users ON cart_items(user_id);
-- changeset yulia:create_index_cart_items_to_products
CREATE INDEX foreign_key_cart_items_products ON cart_items(product_id);


-- changeset yulia:create_index_orders_to_users
CREATE INDEX foreign_key_orders_users ON orders(user_id);
-- changeset yulia:create_index_order_items_to_orders
CREATE INDEX foreign_key_order_items_orders ON order_items(order_id);
-- changeset yulia:create_index_order_items_to_products
CREATE INDEX foreign_key_order_items_products ON order_items(product_id);
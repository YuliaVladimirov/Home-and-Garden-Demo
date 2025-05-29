-- liquibase formatted sql

-- changeset yulia:insert_categories
INSERT INTO categories (category_id, category_name, category_status, created_at, updated_at)
VALUES ('8ee5eee6-4f77-4e1a-bcb5-1ebffa3266d9','Fertilizer', 'ACTIVE', '2025-03-01 14:30:00.123456', '2025-03-01 14:30:00.123456'),
       ('908ae320-6b32-4ef1-bcbd-323bc96fc7ed','Protective product and septic tanks', 'ACTIVE', '2025-03-01 14:30:00.123456', '2025-03-01 14:30:00.123456'),
       ('2e32d4ad-f8bb-4146-839c-b48aa49fcdb1','Planting material', 'ACTIVE', '2025-03-01 14:30:00.123456', '2025-03-01 14:30:00.123456'),
       ('df0d2437-d304-482f-9bf5-4c1a358bc201','Tools and equipment', 'ACTIVE', '2025-03-01 14:30:00.123456', '2025-03-01 14:30:00.123456'),
       ('506b39a5-c663-4403-a7fc-f4f66ffc5505','Pots and planters', 'ACTIVE', '2025-03-01 14:30:00.123456', '2025-03-01 14:30:00.123456');
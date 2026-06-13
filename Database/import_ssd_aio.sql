DO $$
DECLARE
    aio_supplier_id BIGINT;
    admin_user_id BIGINT;
    po_id BIGINT;
    po_item_id BIGINT;
    prod_record RECORD;
    import_prc NUMERIC(20, 2);
    total_amt NUMERIC(20, 2) := 0;
BEGIN
    -- 1. Get or create Supplier AIO
    SELECT id INTO aio_supplier_id FROM suppliers WHERE UPPER(name) = 'AIO' LIMIT 1;
    IF aio_supplier_id IS NULL THEN
        INSERT INTO suppliers (name, email, phone, address, contact_person)
        VALUES ('AIO', 'contact@aio.vn', '19001234', 'AIO Distribution Center, Hanoi, Vietnam', 'AIO Sales Representative')
        RETURNING id INTO aio_supplier_id;
        RAISE NOTICE 'Created Supplier AIO with ID %', aio_supplier_id;
    ELSE
        RAISE NOTICE 'Supplier AIO exists with ID %', aio_supplier_id;
    END IF;

    -- 2. Get admin user ID
    SELECT id INTO admin_user_id FROM users WHERE username = 'admin' LIMIT 1;
    IF admin_user_id IS NULL THEN
        SELECT id INTO admin_user_id FROM users WHERE role = 'ADMIN' LIMIT 1;
    END IF;
    IF admin_user_id IS NULL THEN
        SELECT id INTO admin_user_id FROM users LIMIT 1;
    END IF;

    IF admin_user_id IS NULL THEN
        RAISE EXCEPTION 'No users found in database to associate with purchase order.';
    END IF;
    RAISE NOTICE 'Using creator User ID %', admin_user_id;

    -- 3. Create Purchase Order in DRAFT status
    INSERT INTO purchase_orders (supplier_id, created_by, status, total_amount, created_at)
    VALUES (aio_supplier_id, admin_user_id, 'DRAFT', 0, NOW())
    RETURNING id INTO po_id;
    RAISE NOTICE 'Created Purchase Order ID %', po_id;

    -- 4. Loop through all SSD products (category_id = 12)
    FOR prod_record IN SELECT id, price FROM products WHERE category_id = 12 LOOP
        -- Compute import price: 80% of selling price
        IF prod_record.price IS NULL THEN
            import_prc := 0;
        ELSE
            import_prc := ROUND(prod_record.price * 0.8, 2);
        END IF;

        -- Insert Purchase Order Item
        INSERT INTO purchase_order_items (purchase_order_id, product_id, quantity, import_price)
        VALUES (po_id, prod_record.id, 20, import_prc)
        RETURNING id INTO po_item_id;

        -- Insert Inventory Batch
        INSERT INTO inventory_batches (product_id, purchase_order_item_id, quantity, remaining_quantity, import_price, imported_at)
        VALUES (prod_record.id, po_item_id, 20, 20, import_prc, NOW());

        -- Update Product stock (increment by 20)
        UPDATE products SET stock = stock + 20 WHERE id = prod_record.id;

        -- Accumulate total amount
        total_amt := total_amt + (import_prc * 20);
    END LOOP;

    -- 5. Update Purchase Order total amount and set status to RECEIVED
    UPDATE purchase_orders 
    SET total_amount = total_amt, status = 'RECEIVED' 
    WHERE id = po_id;

    RAISE NOTICE 'Successfully completed SSD import from AIO. Total amount: %', total_amt;
END $$;

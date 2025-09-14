-- Only create the database if it does not exist
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'wallet_transfer') THEN
      CREATE DATABASE wallet_transfer;
   END IF;
END
$$;

-- Only create the user if it does not exist
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname =  'transfer_user') THEN
      CREATE USER transfer_user WITH ENCRYPTED PASSWORD 'transfer_pass';
   END IF;
END
$$;

DO $$
BEGIN
   IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relkind = 'S' AND relname = 'transfer_id_seq') THEN
      CREATE SEQUENCE transfer_id_seq
         START WITH 1
         INCREMENT BY 50
         NO MINVALUE
         NO MAXVALUE
         CACHE 1;
   END IF;
END
$$;

-- Only grant privileges if the user exists
GRANT ALL PRIVILEGES ON DATABASE wallet_transfer TO transfer_user;

-- Only create the table if it does not exist
CREATE TABLE IF NOT EXISTS TRANSFER (
    id BIGINT PRIMARY KEY DEFAULT nextval('transfer_id_seq'),
    transfer_id VARCHAR(255) NOT NULL UNIQUE,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

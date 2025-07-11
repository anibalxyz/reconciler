-- Types declarations

DROP TYPE IF EXISTS source_type CASCADE;
CREATE TYPE source_type AS ENUM (
	'BANK',
	'SYSTEM'
);

DROP TYPE IF EXISTS currency_type CASCADE;
CREATE TYPE currency_type AS ENUM (
  'UYU',
  'USD',
  'EUR',
  'BRL',
  'ARS',
  'CLP',
  'MXN',
  'COP',
  'PEN',
  'GBP'
);

DROP TYPE IF EXISTS tag_type CASCADE;
CREATE TYPE tag_type AS ENUM(
	'CATEGORY',
	'DISCREPANCY',
	'LOG_ACTION'
);

-- Tables declarations

DROP TABLE IF EXISTS tags CASCADE;
CREATE TABLE tags (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(7) NOT NULL DEFAULT '#CFDFEF',
    description TEXT NOT NULL,
    type tag_type NOT NULL
);

DROP TABLE IF EXISTS sources CASCADE;
CREATE TABLE sources (
	id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	code VARCHAR(30) NOT NULL UNIQUE,
	name VARCHAR(100) NOT NULL UNIQUE,
	type source_type NOT NULL
);

DROP TABLE IF EXISTS transactions CASCADE;
CREATE TABLE transactions (
	id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	source_id INT NOT NULL REFERENCES sources(id),
	transaction_date TIMESTAMPTZ NOT NULL,
	reference VARCHAR(255) NOT NULL,
	currency currency_type NOT NULL DEFAULT 'UYU',
	amount NUMERIC(15,2) NOT NULL,
	-- category_id references tags(id) where type = 'CATEGORY'
	category_id INT REFERENCES tags(id),
	description TEXT,
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT uq_transactions_source_reference UNIQUE(source_id, reference)
);

DROP TABLE IF EXISTS reconciliations CASCADE;
CREATE TABLE reconciliations (
	id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	left_transaction_id INT NOT NULL REFERENCES transactions(id),
	right_transaction_id INT NOT NULL REFERENCES transactions(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT uq_reconciliations_transactions UNIQUE(left_transaction_id, right_transaction_id)
);

DROP TABLE IF EXISTS reconciliation_discrepancies CASCADE;
CREATE TABLE reconciliation_discrepancies (
	reconciliation_id INT NOT NULL REFERENCES reconciliations(id),
	-- discrepancy_type_id references tags(id) where type = 'DISCREPANCY'
  	discrepancy_type_id INT NOT NULL REFERENCES tags(id),
  	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (reconciliation_id, discrepancy_type_id)
);

DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE users (
	id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	email VARCHAR(255) NOT NULL UNIQUE,
	password_hash VARCHAR(255) NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS reconciliation_logs CASCADE;
CREATE TABLE reconciliation_logs (
	id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	reconciliation_id INT NOT NULL REFERENCES reconciliations(id),
	reviewer_id INT NOT NULL REFERENCES users(id),
	-- action_id references tags(id) where type = 'LOG_ACTION'
	action_id INT NOT NULL REFERENCES tags(id),
	details TEXT,
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Test table

CREATE TYPE source AS ENUM ('system', 'bank');

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    source source NOT NULL,
    description TEXT,
    amount NUMERIC(10, 2) NOT NULL,
    date DATE NOT NULL
);

-- Test data
INSERT INTO transactions (source, description, amount, date) VALUES
('system', 'Compra supermercado', 1234.50, '2024-04-01'),
('system', 'Pago de sueldo', 20000.00, '2024-04-01'),
('bank', 'Compra supermercado', 1234.50, '2024-04-01'),
('bank', 'Transferencia recibida', 5000.00, '2024-04-02'),
('bank', 'Pago Netflix', 450.00, '2024-04-03');
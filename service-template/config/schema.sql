-- Base Relational Schema Template (SQLite Syntax)
-- Replace this file with your target database schema.sql

-- 1. Reference Categories
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    type TEXT CHECK(type IN ('Expense', 'Revenue', 'Asset', 'Liability')) NOT NULL,
    description TEXT
);

-- 2. Core Entities
CREATE TABLE IF NOT EXISTS items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category_id INTEGER,
    status TEXT NOT NULL DEFAULT 'Active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 3. Logs and Events
CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    event_date DATE NOT NULL,
    description TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'Info',
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 4. Financial / Operational Transactions
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER,
    category_id INTEGER NOT NULL,
    transaction_date DATE NOT NULL,
    amount REAL NOT NULL,
    notes TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

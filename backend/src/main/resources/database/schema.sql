-- Initial schema for Farm Manager AI

-- Fields Table
CREATE TABLE IF NOT EXISTS fields (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    size_acres REAL NOT NULL,
    soil_type TEXT,
    status TEXT DEFAULT 'Active' -- e.g., Active, Fallow, Under Maintenance
);

-- Crops/Planting Table
CREATE TABLE IF NOT EXISTS crops (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    crop_type TEXT NOT NULL, -- e.g., Corn, Wheat, Soybeans
    field_id INTEGER NOT NULL,
    planting_date DATE NOT NULL,
    expected_harvest_date DATE,
    actual_harvest_date DATE,
    expected_yield REAL, -- e.g., bushels per acre
    actual_yield REAL,   -- e.g., bushels per acre
    notes TEXT,
    FOREIGN KEY (field_id) REFERENCES fields(id)
);

-- Financial Transactions (Expenses & Revenue)
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL CHECK (type IN ('Expense', 'Revenue')), -- Expense or Revenue
    category TEXT NOT NULL, -- e.g., Seeds, Fertilizer, Labor, Sales, Equipment Purchase
    amount REAL NOT NULL,
    transaction_date DATE NOT NULL,
    description TEXT,
    field_id INTEGER, -- Optional: link to a specific field
    crop_id INTEGER,  -- Optional: link to a specific crop
    animal_id INTEGER, -- Optional: link to a specific animal
    FOREIGN KEY (field_id) REFERENCES fields(id),
    FOREIGN KEY (crop_id) REFERENCES crops(id),
    FOREIGN KEY (animal_id) REFERENCES animals(id)
);

-- View for backward compatibility with earlier schema that used an "expenses" table
CREATE VIEW IF NOT EXISTS expenses AS
SELECT amount, category, transaction_date AS expense_date FROM transactions WHERE type='Expense';

-- Equipment Table
CREATE TABLE IF NOT EXISTS equipment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT, -- e.g., Tractor, Harvester, Planter
    status TEXT DEFAULT 'Active', -- e.g., Active, Maintenance, Retired
    purchase_date DATE,
    last_maintenance_date DATE,
    notes TEXT
);

-- Animals Table (for Livestock)
CREATE TABLE IF NOT EXISTS animals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    species TEXT NOT NULL, -- e.g., Chicken, Cow, Rabbit
    breed TEXT,
    date_of_birth DATE,
    starting_weight_lbs REAL, -- for weight tracking
    current_weight_lbs REAL, -- for weight tracking
    status TEXT DEFAULT 'Active' -- e.g., Active, Sold, Retired, Requires Attention
);

-- Egg Collection Table
CREATE TABLE IF NOT EXISTS egg_collection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL, -- Link to the chicken
    collection_date DATE NOT NULL,
    quantity INTEGER NOT NULL,
    notes TEXT,
    FOREIGN KEY (animal_id) REFERENCES animals(id)
);

-- Milk Production Table
CREATE TABLE IF NOT EXISTS milk_production (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL, -- Link to the cow
    milking_date DATE NOT NULL,
    yield_gallons REAL NOT NULL,
    notes TEXT,
    FOREIGN KEY (animal_id) REFERENCES animals(id)
);

-- Health Records Table
CREATE TABLE IF NOT EXISTS health_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    animal_id INTEGER NOT NULL,
    incident_date DATE NOT NULL,
    diagnosis TEXT,
    treatment TEXT,
    cost REAL DEFAULT 0.0,
    status TEXT DEFAULT 'Resolved' -- e.g., Active, Resolved
);

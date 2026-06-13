-- Seed data for Farm Manager AI

-- Fields
INSERT INTO fields (name, size_acres, soil_type, status) VALUES
('North Pasture', 50.5, 'Loam', 'Active'),
('South Field', 120.0, 'Clay', 'Active'),
('Orchard', 10.2, 'Sandy Loam', 'Active'),
('West Meadow', 30.0, 'Loam', 'Fallow');

-- Crops/Planting
INSERT INTO crops (crop_type, field_id, planting_date, expected_harvest_date, actual_harvest_date, expected_yield, actual_yield, notes)
VALUES
('Corn', 2, '2023-05-15', '2023-10-01', '2023-09-28', 180.0, 175.5, 'Good year for corn.'),
('Wheat', 2, '2023-04-10', '2023-08-15', '2023-08-10', 60.0, 58.2, 'Drought affected wheat yield.'),
('Soybeans', 4, '2023-05-20', '2023-10-15', '2023-10-12', 50.0, 52.1, 'Soybeans performed well.'),
('Apples', 3, '2022-03-01', NULL, '2023-10-05', 40.0, 42.5, 'Early harvest this year.');

-- Animals (Chickens, Cows, Rabbits)
INSERT INTO animals (name, species, breed, date_of_birth, starting_weight_lbs, current_weight_lbs, status)
VALUES
('Cluckette', 'Chicken', 'Rhode Island Red', '2023-01-10', 4.5, 5.1, 'Active'),
('Henrietta', 'Chicken', 'Leghorn', '2023-01-10', 4.2, 4.8, 'Active'),
('Buttercup', 'Cow', 'Holstein', '2021-06-01', 1200.0, 1350.0, 'Active'),
('Daisy', 'Cow', 'Jersey', '2022-02-15', 1100.0, 1180.0, 'Active'),
('Thumper', 'Rabbit', 'New Zealand White', '2023-07-20', 3.0, 4.5, 'Active'),
('Snowball', 'Rabbit', 'Angora', '2023-07-20', 2.5, 3.2, 'Active');

-- Egg Collection (last 7 days for template 1 and this month for template 2)
INSERT INTO egg_collection (animal_id, collection_date, quantity)
VALUES
(1, date('now', '-1 day'), 5), (2, date('now', '-1 day'), 6),
(1, date('now', '-2 days'), 4), (2, date('now', '-2 days'), 5),
(1, date('now', '-3 days'), 6), (2, date('now', '-3 days'), 4),
(1, date('now', '-4 days'), 5), (2, date('now', '-4 days'), 6),
(1, date('now', '-5 days'), 4), (2, date('now', '-5 days'), 5),
(1, date('now', '-6 days'), 6), (2, date('now', '-6 days'), 4),
(1, date('now', '-7 days'), 5), (2, date('now', '-7 days'), 6),
-- Eggs for this month (template 2)
(1, date('now', 'start of month', '+5 days'), 6), (2, date('now', 'start of month', '+5 days'), 4);

-- Milk Production (this month for template 9)
INSERT INTO milk_production (animal_id, milking_date, yield_gallons)
VALUES
(3, date('now', 'start of month', '+2 days'), 8.5),
(4, date('now', 'start of month', '+2 days'), 7.2),
(3, date('now', 'start of month', '+8 days'), 9.0),
(4, date('now', 'start of month', '+8 days'), 7.5),
(3, date('now', 'start of month', '+15 days'), 8.8),
(4, date('now', 'start of month', '+15 days'), 7.0);

-- Health Records (for template 7)
INSERT INTO health_records (animal_id, incident_date, diagnosis, treatment, cost, status)
VALUES
(1, date('now', '-5 days'), 'Slightly lethargic', 'Rest and observation', 1.50, 'Resolved'),
(5, date('now', '-10 days'), 'Foot rot', 'Medicated bandage', 15.75, 'Active'),
(3, date('now', '-20 days'), 'Mastitis', 'Antibiotics', 75.00, 'Resolved');

-- Expenses (for template 4 and 5)
INSERT INTO transactions (type, category, amount, transaction_date, description)
VALUES
('Expense', 'Feed', 250.75, date('now', 'start of month', '-1 month'), 'Monthly chicken feed supply'),
('Expense', 'Feed', 600.50, date('now', 'start of month', '-1 month'), 'Cow feed delivery'),
('Expense', 'Feed', 30.20, date('now', 'start of month', '-1 month'), 'Rabbit pellets'),
('Expense', 'Seed', 1200.00, '2023-05-10', 'Corn seeds'),
('Expense', 'Labor', 500.00, '2023-05-12', 'Planting labor'),
('Expense', 'Fertilizer', 800.00, '2023-05-18', 'Corn fertilizer application'),
('Revenue', 'Sales', 1500.00, '2023-10-15', 'Sold 50 bushels of soybeans'),
('Revenue', 'Sales', 300.00, '2023-10-10', 'Sold 20 bushels of wheat'),
('Revenue', 'Sales', 600.00, '2023-10-20', 'Sold 10 crates of apples');

-- Equipment
INSERT INTO equipment (name, type, purchase_date, last_maintenance_date, status)
VALUES
('Big Red Tractor', 'Tractor', '2020-03-01', '2023-11-01', 'Active'),
('Combine Harvester', 'Harvester', '2021-05-10', '2023-09-15', 'Active'),
('Corn Planter', 'Planter', '2020-03-01', '2023-04-20', 'Active');

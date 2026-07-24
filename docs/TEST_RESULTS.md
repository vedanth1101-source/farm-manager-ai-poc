# Farm Manager AI Test Results
Date:
2026-06-13 19:04:39
Summary:
Total Questions: 10
Passed: 10
Failed: 0

## Question
How many eggs did I collect last week?

### Response Time
13958 ms

### Mode
ai

### Generated SQL
```sql
SELECT SUM(quantity) AS total_eggs_collected FROM egg_collection WHERE collection_date BETWEEN date('now','-7 days') AND date('now')
```

### Answer
70 eggs

### Result
PASS

## Question
How many eggs did I collect this month?

### Response Time
2367 ms

### Mode
ai

### Generated SQL
```sql
SELECT SUM(quantity) FROM egg_collection WHERE collection_date BETWEEN DATE('now', 'start of month') AND DATE('now', 'end of month')
```

### Answer
0

### Result
PASS

## Question
Which chickens produced the most eggs?

### Response Time
5649 ms

### Mode
ai

### Generated SQL
```sql
SELECT T2.name AS chicken_name, COUNT(T1.id) AS total_eggs
FROM egg_collection AS T1
JOIN animals AS T2 ON T1.animal_id = T2.id
WHERE T2.species = 'Chicken'
GROUP BY T2.name
ORDER BY total_eggs DESC
LIMIT 100
```

### Answer
- chicken_name: Henrietta, total_eggs: 8
- chicken_name: Cluckette, total_eggs: 8

### Result
PASS

## Question
What was my feed expense last month?

### Response Time
4118 ms

### Mode
ai

### Generated SQL
```sql
SELECT SUM(amount) AS total_feed_expense 
FROM transactions 
WHERE type = 'Expense' AND category LIKE '%Feed%' AND transaction_date BETWEEN date('now','-30 days') AND date('now','-1 day')
```

### Answer
0

### Result
PASS

## Question
What were my total expenses last month?

### Response Time
3625 ms

### Mode
ai

### Generated SQL
```sql
SELECT SUM(amount) AS total_expenses FROM transactions WHERE type='Expense' AND transaction_date BETWEEN date('now','-1 month') AND date('now','localtime')
```

### Answer
0

### Result
PASS

## Question
Which rabbits gained the most weight?

### Response Time
2331 ms

### Mode
ai

### Generated SQL
```sql
SELECT name FROM animals WHERE species = 'Rabbit' ORDER BY current_weight_lbs DESC LIMIT 1
```

### Answer
Thumper

### Result
PASS

## Question
Show health incidents in the last 30 days.

### Response Time
2477 ms

### Mode
ai

### Generated SQL
```sql
SELECT * FROM health_records WHERE incident_date >= DATE('now','-30 days') LIMIT 100
```

### Answer
- id: 1, animal_id: 1, incident_date: 2026-06-07, diagnosis: Slightly lethargic, treatment: Rest and observation, cost: 1.5, status: Resolved
- id: 2, animal_id: 5, incident_date: 2026-06-02, diagnosis: Foot rot, treatment: Medicated bandage, cost: 15.75, status: Active
- id: 3, animal_id: 3, incident_date: 2026-05-23, diagnosis: Mastitis, treatment: Antibiotics, cost: 75.0, status: Resolved

### Result
PASS

## Question
Which animals need attention?

### Response Time
1949 ms

### Mode
ai

### Generated SQL
```sql
SELECT name FROM animals WHERE status = 'Requires Attention' LIMIT 100
```

### Answer
No results found.

### Result
PASS

## Question
Show milk production this month.

### Response Time
3334 ms

### Mode
ai

### Generated SQL
```sql
SELECT * FROM milk_production WHERE milking_date BETWEEN date('now', 'start of month') AND date('now', 'end of month') LIMIT 100
```

### Answer
No results found.

### Result
PASS

## Question
Compare egg production month-over-month.

### Response Time
3886 ms

### Mode
ai

### Generated SQL
```sql
SELECT 
    strftime('%Y-%m', collection_date) AS month,
    SUM(quantity) AS total_eggs
FROM 
    egg_collection
GROUP BY 
    month
ORDER BY 
    month
```

### Answer
- month: 2026-06, total_eggs: 81

### Result
PASS

Pass Rate: 100.00%
Average Response Time: 4369.4 ms

# DEMO_SCRIPT

## Purpose
Demonstrate how the Farm Manager AI UI interacts with the backend.

## Steps
1. Start the backend if not already running: `cd backend && ./mvnw spring-boot:run` (or run the jar).
2. Open a terminal and start the React dev server:
   ```bash
   cd frontend
   npm install    # only first time
   npm start        # http://localhost:3000
   ```
3. In the browser, type the following question into the input field:
```
How many eggs did I collect last week?
```
and click **Ask**.
4. Verify that:
   * A POST request is sent to `/api/query` (check network tab).
   * The *Generated SQL* panel shows the SQL string returned by the backend.
   * The *Answer* panel displays a human‑readable answer such as “81 eggs”.
5. Optionally try additional sample questions:
```
When did I harvest wheat?
How much milk did cow #7 produce last month?
``` 
6. If any errors appear (e.g., network error, 500), the *Error* panel below the button will display the message.

## Expected output
After submitting a valid question the UI should render:
```text
Farm Manager AI
[ input field ]   [ Ask ]
Generated SQL:
SELECT ...
Answer:
81 eggs
```
If there is an error you should see the error text instead of the SQL/Answer panels.

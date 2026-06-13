# Automating regression tests for Farm Manager AI
#
# This guide explains how to run the Python test script and interpret results.
# It is intended for developers or CI pipelines that need quick validation after changes.
## Prerequisites
- The backend service must be up and listening on http://localhost:8080.
- An SQLite database should already exist under the project root; it is populated by the seed scripts.
- Python 3.8+ with `requests` installed. If using a virtual environment, activate it first.
## Install dependencies
```sh
pip install requests
```
## Test execution
Run the test script located in the tests directory:
```sh
echo "Running regression tests…"
pyscript C:\\Users\\VEDANTH\\farm-manager-ai\\tests\\test_questions.py
# If using a virtual environment you may need to activate it first or use ". ./venv/bin/activate".
```
The script will:
1. Retrieve all questions via GET /api/questions.
2. For each question, POST /api/query.
3. Record response status, generated SQL, answer, mode (if returned) and response time.
4. Write a detailed Markdown report to `TEST_RESULTS.md` in the project root.
## Expected output
At the end of execution you should see a line like:
```
Test results written to TEST_RESULTS.md
```
Open the generated file to review individual test cases, pass/fail status and overall statistics.
## Troubleshooting
- **Backend not running** – ensure the service is launched (e.g., `mvn spring-boot:run` or equivalent).
- **Connection refused** – verify you can reach http://localhost:8080 in a browser or via `curl http://localhost:8080`. If using Docker, check port mapping.
- **Endpoint unavailable** – the API path might have changed. Verify `/api/questions` and `/api/query` exist by inspecting application logs or codebase.
- **Slow response times** – a large workload may increase latency; check DB profiling if timing becomes an issue.
- **Missing SQL or answer** – indicates a problem in the query engine; review backend error logs for details.

Happy testing!

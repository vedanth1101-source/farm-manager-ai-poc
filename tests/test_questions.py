import requests
import time
import json
from datetime import datetime

def main():
    base_url = "http://localhost:8080"
    questions_endpoint = f"{base_url}/api/questions"
    query_endpoint = f"{base_url}/api/query"

    try:
        resp = requests.get(questions_endpoint, timeout=10)
        resp.raise_for_status()
        data = resp.json()
    except Exception as e:
        print(f"Failed to fetch questions: {e}")
        return

    # Handle response that is either a list of strings or list of objects with 'question' key
    if isinstance(data, list):
        # If first element is a dict with 'question', keep as is.
        if data and isinstance(data[0], dict) and 'question' in data[0]:
            questions_list = data
        else:
            # Assume plain strings
            questions_list = [{'question': str(q)} for q in data]
    else:
        print("Unexpected response format for /api/questions")
        return

    results = []
    for idx, q in enumerate(questions_list, 1):
        question_text = q.get('question', '') if isinstance(q, dict) else str(q)
        payload = {"question": question_text}
        start_time = time.perf_counter()
        try:
            r = requests.post(query_endpoint, json=payload, timeout=30)
            status_code = r.status_code
            elapsed_ms = int((time.perf_counter() - start_time) * 1000)
            if r.ok:
                body = r.json()
                sql = body.get("sql", "")
                answer = body.get("answer", "")
                mode = body.get("mode", "")
            else:
                body = {}
                sql = ""
                answer = ""
                mode = ""
        except Exception as e:
            status_code = None
            elapsed_ms = None
            sql = ""
            answer = ""
            mode = ""
            print(f"Error querying question {idx}: {e}")

        passed = status_code == 200 and bool(sql) and bool(answer)
        results.append({
            "question": question_text,
            "sql": sql,
            "answer": answer,
            "mode": mode,
            "response_time_ms": elapsed_ms if elapsed_ms is not None else "N/A",
            "status_code": status_code or "Error",
            "passed": passed,
        })

    total = len(results)
    passed_count = sum(1 for r in results if r["passed"])
    failed_count = total - passed_count
    avg_resp_time = (
        sum(r["response_time_ms"] for r in results if isinstance(r["response_time_ms"], int))
        / max(1, total)
    )
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    lines = []
    lines.append("# Farm Manager AI Test Results\n")
    lines.append(f"Date:\n{timestamp}\n")
    lines.append("Summary:\n")
    lines.append(f"Total Questions: {total}\n")
    lines.append(f"Passed: {passed_count}\n")
    lines.append(f"Failed: {failed_count}\n\n")

    for r in results:
        lines.append("## Question\n")
        lines.append(r["question"] + "\n\n")
        lines.append("### Response Time\n")
        rt = f"{r['response_time_ms']} ms" if isinstance(r['response_time_ms'], int) else str(r['response_time_ms'])
        lines.append(f"{rt}\n\n")

        mode_desc = r["mode"] or "Unknown"
        lines.append("### Mode\n")
        lines.append(mode_desc + "\n\n")

        if r["sql"]:
            lines.append("### Generated SQL\n")
            lines.append("```sql\n")
            lines.append(r["sql"] + "\n")
            lines.append("```\n\n")
        else:
            lines.append("### Generated SQL\n")
            lines.append("None\n\n")

        if r["answer"]:
            lines.append("### Answer\n")
            lines.append(r["answer"] + "\n\n")
        else:
            lines.append("### Answer\n")
            lines.append("None\n\n")

        lines.append("### Result\n")
        lines.append(("PASS" if r["passed"] else "FAIL") + "\n\n")

    lines.append(f"Pass Rate: {passed_count/total*100:.2f}%\n")
    lines.append(f"Average Response Time: {round(avg_resp_time,1)} ms\n")

    results_md = "".join(lines)
    try:
        with open("TEST_RESULTS.md", "w", encoding="utf-8") as f:
            f.write(results_md)
        print("Test results written to TEST_RESULTS.md")
    except Exception as e:
        print(f"Failed to write results file: {e}")

if __name__ == "__main__":
    main()

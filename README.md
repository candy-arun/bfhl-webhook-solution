# 📘 BFHL Webhook SQL Automation – Spring Boot (JAVA)

This project is my submission for the **BFHL Hiring Assignment – JAVA Webhook + SQL Automation**.

The application automatically:

1. Generates a webhook from BFHL API  
2. Retrieves webhook URL + JWT token  
3. Selects SQL question based on regNo (odd → Q1, even → Q2)  
4. Sends the final SQL query to the webhook using Authorization JWT  
5. Runs on application startup without any controllers

---

## 🚀 Technologies Used
- Java 17+  
- Spring Boot 3.x  
- RestTemplate (HTTP client)  
- Maven  
- CommandLineRunner  
- JWT Authorization Header  

---

# 📂 Project Workflow

### **1️⃣ On Startup**
The application sends:

```
POST https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA
```

Body:

```json
{
  "name": "YOUR_NAME",
  "regNo": "YOUR_REGNO",
  "email": "YOUR_EMAIL"
}
```

### **2️⃣ API Response**
The response returns:

- `webhook` → URL to submit the SQL answer  
- `accessToken` → JWT token  

### **3️⃣ Question Selection Logic**
Based on **last digit of regNo**:

- **Odd → Question 1 SQL**
- **Even → Question 2 SQL**

Both SQL queries are fully implemented in the code.

---

# 📘 SQL Queries Implemented

## ✅ **Question 1 – Highest Salary (Ignoring Day 1 Payments)**

```sql
WITH filtered_payments AS (
    SELECT p.PAYMENT_ID, p.EMP_ID, p.AMOUNT, p.PAYMENT_TIME
    FROM PAYMENTS p
    WHERE DAY(p.PAYMENT_TIME) <> 1
),
ranked AS (
    SELECT
        d.DEPARTMENT_NAME,
        fp.AMOUNT AS SALARY,
        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME,
        TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
        ROW_NUMBER() OVER (
            PARTITION BY d.DEPARTMENT_ID
            ORDER BY fp.AMOUNT DESC
        ) AS rn
    FROM filtered_payments fp
    JOIN EMPLOYEE e ON fp.EMP_ID = e.EMP_ID
    JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
)
SELECT DEPARTMENT_NAME, SALARY, EMPLOYEE_NAME, AGE
FROM ranked
WHERE rn = 1
ORDER BY DEPARTMENT_NAME;
```

---

## ✅ **Question 2 – Avg Age + Top 10 Names of Employees > 70,000**

```sql
SELECT 
    d.DEPARTMENT_NAME,
    AVG(TIMESTAMPDIFF(YEAR, e.DOB, CURDATE())) AS AVERAGE_AGE,
    GROUP_CONCAT(
        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME)
        ORDER BY e.EMP_ID
        LIMIT 10
    ) AS EMPLOYEE_LIST
FROM EMPLOYEE e
JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID
WHERE p.AMOUNT > 70000
GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME
ORDER BY d.DEPARTMENT_ID DESC;
```

---

# 📤 Submission Step

### **4️⃣ Submit SQL Query**
The app sends:

```
POST <webhook_url_from_step_1>
```

Headers:

```
Authorization: <accessToken>
Content-Type: application/json
```

Body:

```json
{
  "finalQuery": "SQL_QUERY_HERE"
}
```

---

# ▶️ How to Run

### 1. Build the JAR

```
mvn clean package -DskipTests
```

### 2. Run the JAR

```
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

If port 8080 is busy:

```
java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=9090
```

---

# 🧾 Files Included

- `WebhookFlowService.java` — Main logic  
- `BfhlApplication.java` — Spring Boot runner  
- `GenerateWebhookResponse.java` — DTO  
- `pom.xml` — Dependencies  
- `README.md` — Documentation  
- `target/demo-0.0.1-SNAPSHOT.jar` — Final JAR (submitted)  

---

# 🔗 Submission Links

| Type | Link |
|------|------|
| **GitHub Repository** | https://github.com/candy-arun/bfhl-webhook-solution |
| **RAW JAR File** | https://raw.githubusercontent.com/candy-arun/bfhl-webhook-solution/main/target/demo-0.0.1-SNAPSHOT.jar
 |

---

# ✔ Status
✅ Fully implemented  
✅ Both SQL queries included  
✅ Runs automatically on startup  
✅ No controllers used  
✅ Ready for final submission  

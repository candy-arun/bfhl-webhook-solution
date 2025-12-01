package com.example.bfhl;

import com.example.bfhl.dto.GenerateWebhookResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookFlowService {

    private final RestTemplate restTemplate;

    public WebhookFlowService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void runFlow() {

        try {
            // ======== STEP 1: Generate webhook ========
            String generateUrl =
                    "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> body = new HashMap<>();
            body.put("name", "YOUR_NAME_HERE");
            body.put("regNo", "YOUR_REGNO_HERE");
            body.put("email", "YOUR_EMAIL_HERE");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> req =
                    new HttpEntity<>(body, headers);

            ResponseEntity<GenerateWebhookResponse> genResp =
                    restTemplate.postForEntity(generateUrl, req, GenerateWebhookResponse.class);

            if (!genResp.getStatusCode().is2xxSuccessful() || genResp.getBody() == null) {
                System.out.println("Webhook generation FAILED.");
                return;
            }

            String webhookUrl = genResp.getBody().getWebhook();
            String accessToken = genResp.getBody().getAccessToken();

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token: " + accessToken);

            // ======== STEP 2: Choose question based on REG NO ========
            String regNo = body.get("regNo");
            int lastDigit = Character.getNumericValue(regNo.charAt(regNo.length() - 1));

            String finalQuery;

            if (lastDigit % 2 == 1) {
                // ================= QUESTION 1 (ODD REG NUMBER) =================
                finalQuery =
                        "WITH filtered_payments AS ( " +
                        "    SELECT p.PAYMENT_ID, p.EMP_ID, p.AMOUNT, p.PAYMENT_TIME " +
                        "    FROM PAYMENTS p " +
                        "    WHERE DAY(p.PAYMENT_TIME) <> 1 " +
                        "), " +
                        "ranked AS ( " +
                        "    SELECT " +
                        "        d.DEPARTMENT_NAME, " +
                        "        fp.AMOUNT AS SALARY, " +
                        "        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME, " +
                        "        TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
                        "        ROW_NUMBER() OVER ( " +
                        "            PARTITION BY d.DEPARTMENT_ID " +
                        "            ORDER BY fp.AMOUNT DESC " +
                        "        ) AS rn " +
                        "    FROM filtered_payments fp " +
                        "    JOIN EMPLOYEE e ON fp.EMP_ID = e.EMP_ID " +
                        "    JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                        ") " +
                        "SELECT DEPARTMENT_NAME, SALARY, EMPLOYEE_NAME, AGE " +
                        "FROM ranked " +
                        "WHERE rn = 1 " +
                        "ORDER BY DEPARTMENT_NAME;";
            } else {
                // ================= QUESTION 2 (EVEN REG NUMBER) =================
                finalQuery =
                        "SELECT " +
                        "    d.DEPARTMENT_NAME, " +
                        "    AVG(TIMESTAMPDIFF(YEAR, e.DOB, CURDATE())) AS AVERAGE_AGE, " +
                        "    GROUP_CONCAT( " +
                        "        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) " +
                        "        ORDER BY e.EMP_ID " +
                        "        LIMIT 10 " +
                        "    ) AS EMPLOYEE_LIST " +
                        "FROM EMPLOYEE e " +
                        "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                        "JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
                        "WHERE p.AMOUNT > 70000 " +
                        "GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME " +
                        "ORDER BY d.DEPARTMENT_ID DESC;";
            }

            // ======== STEP 3: Submit finalQuery ========
            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            submitHeaders.set("Authorization", accessToken);

            Map<String, String> finalBody = new HashMap<>();
            finalBody.put("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> submitReq =
                    new HttpEntity<>(finalBody, submitHeaders);

            ResponseEntity<String> submitResp =
                    restTemplate.postForEntity(webhookUrl, submitReq, String.class);

            System.out.println("Submission Status: " + submitResp.getStatusCode());
            System.out.println("Response: " + submitResp.getBody());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

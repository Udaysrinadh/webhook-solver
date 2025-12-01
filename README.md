# webhook-solver
Spring Boot app that on startup:
1. Sends a POST to generate a webhook.
2. Receives a webhook URL and accessToken.
3. Solves Question 2 (SQL) and posts the final SQL query to the webhook using the JWT token.

Included:
- src/main/java/com/example/webhooksolver/WebhookSolverApplication.java
- src/main/java/com/example/webhooksolver/WebhookService.java
- pom.xml
- src/main/resources/application.properties

Important:
- The final SQL query for Question 2 (from the provided PDF) is embedded in WebhookService.java.
- The SQL and problem statement are from the user-provided file; citation: see uploaded PDF. fileciteturn0file0

How to run:
- Build: mvn clean package
- Run: java -jar target/webhook-solver-0.0.1-SNAPSHOT.jar

Notes:
- No controllers are exposed; the flow runs on application startup (ApplicationReadyEvent).
- The app uses RestTemplate (synchronous) per requirements.
- If the target endpoints require a different payload or headers, update application.properties.
- 
<img width="1454" height="677" alt="image" src="https://github.com/user-attachments/assets/35cdadd1-9144-49b8-bacd-835b1c28c2a3" />

- <img width="1454" height="677" alt="image" src="https://github.com/user-attachments/assets/67bd6097-17df-47af-a73f-401f528154f4" />


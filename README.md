# Price Tracker REST API

A robust, multi-threaded Java Spring Boot application designed to automatically track product prices from various e-commerce websites and send email notifications when a desired price is reached.

-----

## Core Features

* **Automated Price Tracking**: A scheduler runs periodically to check the prices of all tracked products automatically.
* **Multi-Domain Support**: The application is designed to scrape multiple websites (e.g., Amazon, Flipkart) by using a dynamic, domain-specific CSS selector map.
* **Email Notifications**: Utilizes Spring Boot Mail to send real-time alerts to a user's email address when a product's price drops to or below their target price.
* **Intelligent Scraping**:
  * Handles shortened URLs (e.g., `amzn.in`) by following redirects to find the final product page.
  * Uses a multi-threaded, domain-specific queue to process products, preventing IP blocks by sending requests to the same site sequentially and politely.
  * Implements a robust retry mechanism to handle temporary network failures or blocks.
* **RESTful API**: Exposes clean and simple endpoints to add, check, and delete tracked products.

-----

## Technologies Used

* **Backend**: Java 17, Spring Boot 3
* **Data**: Spring Data JPA, PostgreSQL
* **Web Scraping**: Jsoup
* **Concurrency**: Java `ExecutorService`
* **Build Tool**: Maven
* **Utilities**: Lombok

-----

## Setup and Installation

Follow these steps to get the project running on your local machine.

### Prerequisites

* Java Development Kit (JDK) 17 or later
* Apache Maven
* A running PostgreSQL database instance

### 1\. Clone the Repository

Clone this project to your local machine.

```bash
git clone <your-repository-url>
cd price-tracker
```

### 2\. Configure the Application

Open the `src/main/resources/application.properties` file and update the following properties with your specific credentials.

**Database Configuration:**

```properties
# PostgreSQL Connection Settings
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database_name
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**Email Notification Configuration (using Gmail):**
To send emails, you must configure an SMTP server. This example uses Gmail.

```properties
# Spring Mail Properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-digit-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Important**: For `spring.mail.password`, you must use a 16-digit **App Password** generated from your Google Account settings. Your regular password will not work. You must have 2-Step Verification enabled to create an App Password.

### 3\. Build and Run the Application

Open a terminal in the project's root directory and run the following Maven command:

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

-----
## API Endpoints

The API provides endpoints to manage tracked products. The base URL is `/api/products`.

| Method | Path                 | Description                                                                                             | Request Body                                                              | Success Response                                      |
| :----- |:---------------------| :------------------------------------------------------------------------------------------------------ | :------------------------------------------------------------------------ | :---------------------------------------------------- |
| `POST` | `/add`               | Adds a new product to track. The URL is expanded, sanitized, and validated against supported domains. | `{ "url": "...", "targetPrice": ..., "userEmail": "..." }`                 | `201 CREATED` with the saved product DTO.               |
| `GET`  | `/all`               | Retrieves a list of all products currently being tracked.                                               | (None)                                                                    | `200 OK` with a list of product DTOs (can be empty).    |
| `GET`  | `/check-price?pid=1` | Manually triggers a price check for a single product by its ID.                                         | (None)                                                                    | `200 OK` with the current price as a number.            |
| `DELETE`| `/delete/{pid}`     | Stops tracking a product by its ID.                                                                     | (None)                                                                    | `200 OK` with no body.                                  |

_Note: Visit `localhost:8080/swagger-ui/index.html` after starting the service to get detailed interactive API documentation._

-----

## Limitations

### CAPTCHA and Anti-Bot Protection

This application uses **Jsoup** for web scraping, which is a powerful HTML parser. However, Jsoup **cannot run JavaScript or solve CAPTCHAs**.

Major e-commerce sites like Amazon have sophisticated anti-bot systems. If they detect too many rapid requests from a single IP, they will stop sending the product page and instead serve a CAPTCHA page to verify the user is human.

While this application has retry logic to handle temporary rate limits, it **cannot bypass a hard CAPTCHA block**. To reliably scrape sites with this level of protection, a more advanced tool like **Selenium** or **Playwright**, which automates a real web browser, would be required.

-----

## How to Extend

To add support for a new e-commerce website, you only need to make one change.

1.  Use your browser's "Inspect" tool to find the unique CSS selector for the price element on the new website.
2.  Open the `ProductService.java` file.
3.  Add a new entry to the `SELECTORS` map with the website's domain as the key and the CSS selector as the value.

<!-- end list -->

```java
// In ProductService.java
private static final Map<String, String> SELECTORS = Map.of(
    "www.amazon.in", ".a-price-whole",
    "www.flipkart.com", "._30jeq3._16Jk6d",
    "www.new-website.com", ".price-tag-class" // Add your new site here
);
```

The application will now automatically support scraping from this new domain.
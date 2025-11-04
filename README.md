# Nestly Backend

This is the backend service for the **Nestly** project.  
It is built with **Spring Boot** and connects to a **MySQL database** deployed on Railway.

## 🚀 Technologies Used
- Java 22 / Spring Boot
- MySQL (Railway)
- JWT Authentication
- PayPal Integration
- REST APIs with CORS support

## ⚙️ Environment Variables
The following variables are used in `application.properties`:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `PAYPAL_CLIENT_ID`
- `PAYPAL_CLIENT_SECRET`
- `FRONTEND_URL`

## 🌍 Deployment
The backend will be deployed on **Railway**, and the frontend on **Vercel**.
Make sure to set the correct environment variables in Railway before deploying.

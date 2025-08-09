# TumaGo – Fullstack Mobile Package Delivery Platform 📦🚚

**TumaGo** is a fullstack mobile logistics platform that connects clients who need to send packages with nearby drivers based on vehicle type (motorbike, van, or truck). It consists of a Django backend and two Android apps — one for drivers and another for clients.

---

## 🌍 Project Overview

TumaGo enables real-time package booking, tracking, and communication between clients and drivers. It offers live location updates, secure authentication, and seamless integration with maps, messaging, and cloud-based notifications.

---

## 🧩 Project Structure

TumaGo/
├── backend/ # Django REST API (with WebSocket support)
├── driver_app/ # Android app for delivery drivers
└── client_app/ # Android app for clients sending packages

---

## 🔧 Technologies Used

### 🖥️ Backend – Django
- Django 5 + Django REST Framework
- JWT Authentication (`djangorestframework_simplejwt`)
- WebSockets via Django Channels + Daphne
- Background tasks with Redis + Dramatiq
- MySQL (AWS RDS) for production database
- Google Maps & Firebase Admin SDK
- Docker + Docker Compose

### 📱 Android (Java) – Driver & Client Apps
- Retrofit2 + OkHttp for API communication
- Firebase Cloud Messaging (push notifications)
- Google Maps SDK, Places API, and Location Services
- WebSocket (Java-WebSocket)
- AndroidX Security for token storage
- MPAndroidChart for visual stats (optional)
- ConstraintLayout, AppCompat, and Jetpack libraries

---

## 🚀 Key Features

### ✅ Shared Functionality
- JWT-based login and role-based access
- Real-time location tracking with Google Maps
- Firebase push notifications
- WebSocket-powered chat between driver and client

### 👤 Client App
- Book deliveries by vehicle type
- Live track drivers and packages
- View delivery history and status
- Communicate with driver in real-time

### 🚚 Driver App
- Register with license verification
- Accept or reject package requests
- Navigation using Google Maps Directions API
- Update trip status (started, in transit, delivered)
- Live chat with clients

---

## 🙋‍♂️ Author

**Brandon Khumalo**  
🚀 Backend & Mobile Developer  
📫 [LinkedIn](https://www.linkedin.com/in/brandon-khumalo04) | [Email](mailto:brandonkhumz40@gmail.com)

**Live API URL**
[api demo](https://tumago.onrender.com/swagger/)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## ⭐️ Show your support

If you like this project, please give it a ⭐ and consider following me for more cool builds!
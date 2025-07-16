# TumaGo Client App 📦

**TumaGo Client** is the mobile application for customers who want to send packages using the **TumaGo** logistics platform. Users can register, choose a vehicle (motorbike, van, or truck), book a delivery, track their package in real-time, and communicate with drivers.

This app is built using **Java (Android)** and connects to a Django-based backend via REST APIs and WebSockets.

---

## 🚀 Features

- 📦 Book package deliveries by selecting vehicle type
- 📍 Live driver location tracking on Google Maps
- 🗺️ Route preview using Google Maps Directions API
- 📨 Real-time updates via Firebase Cloud Messaging
- 🔐 Secure login & JWT-based authentication
- 💬 Real-time chat with drivers using WebSockets
- 🧾 View delivery history and package status

---

## 🛠️ Tech Stack

### 📱 Android (Java)
- `Retrofit2` & `Gson` – API communication
- `Firebase Messaging` – Push notifications
- `Google Maps`, `Places API`, `Location Services` – Map and geolocation features
- `WebSocket` – Real-time communication (via `Java-WebSocket`)
- `AndroidX Security` – Secure storage for auth tokens
- `MPAndroidChart` – Charts for delivery analytics (optional)
- `ConstraintLayout`, `AppCompat`, etc. – Modern UI components

---

## 🔗 Backend Integration

The mobile app communicates with the [TumaGo Backend](https://github.com/Brandonkhumalo/TumaGo/tree/main/TumaGo_Backend/TumaGo) which is:
- Built with **Django & Django REST Framework**
- Uses **JWT Authentication**
- Supports **WebSockets** via **Django Channels + Daphne**
- Utilizes **Redis + Dramatiq** for background processing

---

## 📷 Screenshots

<!-- Add screenshots if available -->
| Home Screen | Package Tracker | Chat with Driver |
|-------------|------------------|------------------|
| *Coming Soon* | *Coming Soon* | *Coming Soon* |

---

## 🙋‍♂️ Author

**Brandon Khumalo**  
🚀 Backend & Mobile Developer  
📫 [LinkedIn](https://www.linkedin.com/in/brandon-khumalo04) | [Email](mailto:brandonkhumz40@gmail.com)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## ⭐️ Show your support

If you like this project, please give it a ⭐ and consider following me for more cool builds!

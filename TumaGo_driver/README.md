# TumaGo Driver App 🚚

**TumaGo Driver** is the mobile companion app for delivery drivers (motorbike, van, or truck) in the **TumaGo** logistics platform. Drivers can register, accept delivery requests, navigate routes using Google Maps, and communicate with clients in real-time.

This app is built using **Java (Android)** and connects to a Django-based backend via REST APIs and WebSockets.

---

## 🚀 Features

- 📍 Real-time location tracking
- 🧭 Google Maps navigation and Places Autocomplete
- 📨 Firebase Cloud Messaging (push notifications)
- 🔐 Secure login & JWT-based authentication
- ✅ Accept/reject delivery requests
- 💬 Real-time chat with clients using WebSockets
- 📦 Trip status updates (start, in-transit, delivered)
- 🔒 Secure license upload & verification

---

## 🛠️ Tech Stack

### 📱 Android (Java)
- `Retrofit2` & `Gson` – API communication
- `Firebase Messaging` – Push notifications
- `Google Maps`, `Places API`, `Location Services` – Map features
- `WebSocket` – Real-time messaging (via `Java-WebSocket`)
- `AndroidX Security` – Secure credential storage
- `MPAndroidChart` – Visual trip stats
- `ConstraintLayout`, `AppCompat`, etc. – Modern UI components

---

## 🔗 Backend Integration

The mobile app communicates with the [TumaGo Backend](https://github.com/Brandonkhumalo/TumaGo/tree/main/TumaGo_Backend/TumaGo) which is:
- Built with **Django & Django REST Framework**
- Uses **JWT Authentication**
- Supports **WebSockets** via **Django Channels + Daphne**
- Utilizes **Redis + Dramatiq** for background tasks

---

## 📷 Screenshots

<!-- Add screenshots if available -->
| Home Screen | Trip Details | Chat View |
|-------------|--------------|-----------|
| *Coming Soon* | *Coming Soon* | *Coming Soon* |

---

## 🙋‍♂️ Author

**Brandon Khumalo**  
🚀 Backend & Mobile Developer  
📫 [LinkedIn](www.linkedin.com/in/brandon-khumalo04) | [Email](mailto:brandonkhumz40@gmail.com)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## ⭐️ Show your support

If you like this project, please give it a ⭐ and consider following me for more cool builds!

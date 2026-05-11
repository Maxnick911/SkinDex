# SkinDex - Mobile Dermatology Assistant

SkinDex is a full-stack Android application developed as a bachelor thesis project for supporting dermatological image classification.

The application allows medical staff to manage patients, capture or upload skin images, and classify them locally on the device using a TensorFlow Lite model.

## Project Goal

The goal of SkinDex is to demonstrate how modern mobile development, backend systems, and machine learning can be combined into a practical healthcare-oriented application.

---

# Main Features

## Authentication

- Secure doctor registration
- Login / logout functionality
- JWT-based authentication
- Encrypted local token storage

## Patient Management
- Create patient profiles
- View patient history
- Delete patient profiles
- Store classified images linked to patients

## Image Classification

- Select image from gallery
- Capture image with camera
- Crop selected skin area
- Run local AI classification using TensorFlow Lite
- Display prediction probabilites in chart format

## Diagnosis History
- Save analyzed image to patient profile
- View previous classifications
- Delete saved results

---

# Disease Classes

The model can classify images into following categories:

1. Eczema  
2. Melanoma  
3. Atopic Dermatitis  
4. Basal Cell Carcinoma (BCC)  
5. Melanocytic Nevi (NV)  
6. Benign Keratosis-like Lesions (BKL)  
7. Psoriasis / Lichen Planus  
8. Seborrheic Keratoses / Benign Tumors  
9. Fungal Infections  
10. Viral Infections / Warts / Molluscum  
11. Normal Skin

---

# Machine Learning Model

The original trained model was converted into **TensorFlow Lite** format for mobile deployment.

Benefits of on-device inference:

- Faster prediction speed
- Smal application size
- Offline usage possible
- Improved privacy (images stay on device)

---

# Tech Stack

## Frontend (Android)

- Kotlin
- MVVM Architecture
- Android Jetpack
- Navigation Component
- RecyclerView
- Material Design 3
- Coroutines + StateFlow
- Hilt Dependency Injection
- Retrofit + Moshi
- Glide / Picasso
- MPAndroidChart
- CameraX
- TensorFlow Lite

## Backend

- Kotlin
- Ktor
- PostgreSQL
- Exposed ORM
- Flyway Migrations
- JWT Authentication
- BCrypt Password Hashing

---

# Architecture

```text
Android App
   ↓
REST API (Ktor Backend)
   ↓
PostgreSQL Database

TensorFlow Lite runs locally on the Android device
```

# Demo Video
[!Application Work]](https://youtu.be/nHkgqEz_OAQ)

# Project Structure



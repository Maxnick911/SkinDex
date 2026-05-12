# Skindex — AI Skin Disease Classification App

**Skindex** is a mobile Android application designed to assist dermatologists and general practitioners in classifying skin diseases using artificial intelligence.

## Features

- User registration and authentication (JWT-based)
- Doctor profile management
- Patient profile creation and management
- Skin lesion classification from images:
  - Real-time camera capture
  - Photo selection from gallery
  - Image cropping
- Visualization of prediction results using probability charts
- Saving classified images with diagnosis to patient records
- View and delete images in patient profiles

## Supported Conditions (11 classes)

- Normal Skin
- Melanocytic Nevi (NV)
- Basal Cell Carcinoma (BCC)
- Melanoma
- Warts, Molluscum and other Viral Infections
- Benign Keratosis-like Lesions (BKL)
- Psoriasis, Lichen Planus and related diseases
- Seborrheic Keratoses and other Benign Tumors
- Tinea Ringworm Candidiasis and other Fungal Infections
- Eczema
- Atopic Dermatitis

## Demo

[Watch Application Demonstration](https://drive.google.com/file/d/1tsp9T2EyuZn23nYTfOgPB3jXplSjIB2M)

## Technology Stack

### Frontend (Android)
- **Kotlin** + Jetpack Components
- CameraX
- Android Image Cropper
- TensorFlow Lite (optimized `.tflite` model for on-device inference)
- Retrofit + Moshi + OkHttp
- Hilt (Dependency Injection)
- MPAndroidChart
- Glide
- Kotlin Coroutines
- Navigation Component

### Backend
- **Ktor** (Netty)
- Exposed ORM + PostgreSQL
- Flyway (database migrations)
- JWT Authentication
- BCrypt password hashing

### Machine Learning
- Model trained in Jupyter Notebook (`skin_diseases_classification.ipynb`)
- Multi-stage training: Transfer Learning + Fine-tuning
- Trained on ~27,000 images
- Converted to **TensorFlow Lite** format for fast and lightweight on-device inference

## Local Deployment

### Prerequisites
- Docker and Docker Compose
- Android Studio

```bash
git clone https://github.com/Maxnick911/skindex.git
cd skindex

# Start backend and database
docker compose up --build -d
```
The backend will be available at **http://localhost:8080**.

Update the base URL in the Android application before running.

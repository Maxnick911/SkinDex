# SkinDex
Skindex is a mobile application designed for dermatologists and general practitioners to assist in the classification of skin diseases using artificial intelligence

## Features

- User registration und authentication (JWT-based)
- Doctor profile management
- Patient profile creation and management
- Skin lesion classification from images
- Visualization of prediction results with probability charts
- Saving classified images with diagnosis to patient records
- View and delete images in patient profiles

## Supported Conditions

1. Normal Skin
2. Melanocytic Nevi (NV)
3. Basal Cell Carcinoma (BCC)
4. Melanoma
5. Warts, Molluscum and other Viral Infections
6. Benign Keratosis-like Lesions (BKL)
7. Psoriasis, Lichen Planus and related diseases
8. Seborrheic Keratoses and other Benign Tumors
9. Tinea Ringworm Candidiasis and other Fungal Infections
10. Eczema
11. Atopic Dermatitis

# Demo

Watch the Application Work Demonstration: https://l1nk.dev/tu522jq

## Technology Stack

### Frontend (Android)

- Kotlin + Jetpack components
- CameraX
- Android Image Cropper
- TensorFlow Lite (optimized on-device model)
- Retrofit + Moshi + OkHttp
- Hilt for Dependency Injection
- MPAndroidChart
- Glide
- Kotlin Coroutines
- Navigation Component

### Backend

- Ktor
- Exposed + PostgreSQL
- Flyway migrations
- JWT Authentication
- BCrypt password hashing

### Machine Learning

- The model was trained in **.ipynb** file using a multi-stage approach with transfer learning of frozen base layers and fine tuning of top layers.
- Trained on approximately 27,000 images
- Final model is optimized and converted to **.tflite** format for mobile deployment, providing fast inference directly on the device.

## Local Deployment 

### Prerequisites
- Docker and Docker Compose
- Android Studio
```
git clone https://github.com/yourusername/skindex.git
cd skindex
docker compose up --build -d
```
The backend will be available at **http://localhost:8080**.

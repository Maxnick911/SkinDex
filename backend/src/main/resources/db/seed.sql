INSERT INTO users (role, name, email, password_hash) VALUES
    ('doctor', 'Dr. Ivan', 'ivan@hospital.com', '$2a$12$7Qz2i6g8b6y3j2k5n9m0vO6z7x8y9z0a1b2c3d4e5f6g7h8i9j0k'),
    ('patient', 'Anna', 'anna@gmail.com', '$2a$12$7Qz2i6g8b6y3j2k5n9m0vO6z7x8y9z0a1b2c3d4e5f6g7h8i9j0k'),
    ('admin', 'Admin', 'admin@system.com', '$2a$12$7Qz2i6g8b6y3j2k5n9m0vO6z7x8y9z0a1b2c3d4e5f6g7h8i9j0k')
ON CONFLICT (email) DO NOTHING;

INSERT INTO images (user_id, patient_id, file_path, quality_status) VALUES
    (1, 2, '/images/patient_anna_001.jpg', 'accepted')
ON CONFLICT DO NOTHING;

INSERT INTO diagnoses (image_id, diagnosis, probability, doctor_comment) VALUES
    (1, 'Меланома', 91.5, 'Рекомендується біопсія')
ON CONFLICT DO NOTHING;

INSERT INTO logs (user_id, action, details) VALUES
    (1, 'upload_image', 'Uploaded image for patient ID 2')
ON CONFLICT DO NOTHING;
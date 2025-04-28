INSERT INTO users (role, name, email, password_hash) VALUES
('doctor', 'Dr. Ivan', 'ivan@hospital.com', 'hashed_password_1'),
('patient', 'Anna', 'anna@gmail.com', 'hashed_password_2'),
('admin', 'Admin', 'admin@system.com', 'hashed_password_3');

INSERT INTO images (user_id, patient_id, file_path, quality_status) VALUES
(1, 2, '/images/patient_anna_001.jpg', 'accepted');

INSERT INTO diagnoses (image_id, diagnosis, probability, doctor_comment) VALUES
(1, 'Меланома', 91.5, 'Рекомендується біопсія');

INSERT INTO logs (user_id, action, details) VALUES
(1, 'upload_image', 'Uploaded image for patient ID 2');
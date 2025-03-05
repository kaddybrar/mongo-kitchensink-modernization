-- Insert sample data only if the table is empty
INSERT INTO member (name, email, phone_number)
SELECT 'John Doe', 'john.doe@example.com', '+11234567890'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'john.doe@example.com');

INSERT INTO member (name, email, phone_number)
SELECT 'Jane Smith', 'jane.smith@example.com', '+19876543210'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'jane.smith@example.com');

INSERT INTO member (name, email, phone_number)
SELECT 'Bob Johnson', 'bob.johnson@example.com', '+15551234567'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'bob.johnson@example.com'); 
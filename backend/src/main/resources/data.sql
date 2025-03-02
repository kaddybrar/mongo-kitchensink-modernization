-- Insert sample data only if the table is empty
INSERT INTO member (name, email, phone_number)
SELECT 'John Doe', 'john.doe@example.com', '123-456-7890'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'john.doe@example.com');

INSERT INTO member (name, email, phone_number)
SELECT 'Jane Smith', 'jane.smith@example.com', '987-654-3210'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'jane.smith@example.com');

INSERT INTO member (name, email, phone_number)
SELECT 'Bob Johnson', 'bob.johnson@example.com', '555-123-4567'
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'bob.johnson@example.com'); 
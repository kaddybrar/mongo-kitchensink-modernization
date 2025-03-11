CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_member_updated_at
    BEFORE UPDATE ON member
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column(); 
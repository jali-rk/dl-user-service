-- Create sequence for student registration numbers starting from 1001
CREATE SEQUENCE IF NOT EXISTS student_code_number_seq
    START WITH 1001
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

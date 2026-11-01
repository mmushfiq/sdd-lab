ALTER TABLE room
    ADD COLUMN cleaning_status VARCHAR(10) DEFAULT 'CLEAN' NOT NULL;

ALTER TABLE room
    ADD CONSTRAINT room_cleaning_status_ck CHECK (cleaning_status IN ('CLEAN', 'DIRTY'));

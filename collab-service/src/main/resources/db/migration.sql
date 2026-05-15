-- Make user_id nullable in collab_participants so email-based inserts succeed.
-- This column is a legacy artifact; identity is tracked via user_email instead.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'collab_participants'
          AND column_name = 'user_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE collab_participants ALTER COLUMN user_id DROP NOT NULL;
    END IF;
END$$;

ALTER TABLE profile_photo_jobs
ADD COLUMN IF NOT EXISTS callback_url VARCHAR(255);

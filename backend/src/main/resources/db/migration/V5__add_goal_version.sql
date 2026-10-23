-- Optimistic locking for goals: concurrent edits or status transitions of the same goal fail with
-- 409 instead of silently overwriting each other (D-14..D-16).
ALTER TABLE goals ADD COLUMN version bigint NOT NULL DEFAULT 0;

CREATE TABLE notification_queue (
  message_key varchar(240) PRIMARY KEY,
  kind varchar(40) NOT NULL CHECK (kind IN ('TASK','INTERVIEW','NEW_OPPORTUNITIES')),
  activity_id uuid, due_at timestamptz, new_count integer NOT NULL DEFAULT 0 CHECK (new_count >= 0),
  automatic boolean NOT NULL, created_at timestamptz NOT NULL, expires_at timestamptz,
  state varchar(16) NOT NULL CHECK
    (state IN ('PENDING','SENDING','SENT','REJECTED','UNCERTAIN','CANCELLED','EXPIRED')),
  updated_at timestamptz NOT NULL
);
CREATE INDEX notification_pending ON notification_queue(created_at, message_key)
  WHERE state = 'PENDING';
-- Java Instant.toString() uses UTC and groups fractional digits in threes.
INSERT INTO notification_queue
  (message_key, kind, activity_id, due_at, automatic, created_at, state, updated_at)
SELECT 'reminder:' || activity_id || ':' || kind || ':' ||
  to_char(due_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS') ||
  CASE WHEN extract(microseconds FROM due_at)::bigint % 1000000 = 0 THEN ''
       WHEN extract(microseconds FROM due_at)::bigint % 1000 = 0
         THEN '.' || to_char(due_at AT TIME ZONE 'UTC', 'MS')
       ELSE '.' || to_char(due_at AT TIME ZONE 'UTC', 'US') END || 'Z',
  kind, activity_id, due_at, false, updated_at,
  CASE WHEN state = 'SENDING' THEN 'UNCERTAIN' ELSE state END, updated_at
FROM notification_deliveries;

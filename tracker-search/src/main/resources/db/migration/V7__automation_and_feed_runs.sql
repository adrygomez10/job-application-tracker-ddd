ALTER TABLE search_profile
  ADD COLUMN auto_import boolean NOT NULL DEFAULT false,
  ADD COLUMN import_interval integer NOT NULL DEFAULT 60 CHECK (import_interval BETWEEN 15 AND 1440),
  ADD COLUMN auto_notifications boolean NOT NULL DEFAULT false,
  ADD COLUMN notify_tasks boolean NOT NULL DEFAULT true,
  ADD COLUMN notify_interviews boolean NOT NULL DEFAULT true,
  ADD COLUMN notify_opportunities boolean NOT NULL DEFAULT true;
CREATE TABLE feed_runs (
  id uuid PRIMARY KEY, origin varchar(16) NOT NULL CHECK (origin IN ('MANUAL','AUTOMATIC')),
  started_at timestamptz NOT NULL, finished_at timestamptz,
  state varchar(16) NOT NULL CHECK (state IN ('RUNNING','SUCCEEDED','FAILED','INTERRUPTED')),
  imported integer NOT NULL DEFAULT 0, unchanged integer NOT NULL DEFAULT 0
);
CREATE INDEX feed_runs_started ON feed_runs(started_at DESC);

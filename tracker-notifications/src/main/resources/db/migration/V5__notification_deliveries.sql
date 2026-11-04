CREATE TABLE notification_deliveries (
  activity_id uuid NOT NULL, kind varchar(40) NOT NULL, due_at timestamptz NOT NULL,
  state varchar(16) NOT NULL CHECK (state IN ('SENDING','SENT','REJECTED','UNCERTAIN')),
  updated_at timestamptz NOT NULL,
  PRIMARY KEY (activity_id, kind, due_at)
);

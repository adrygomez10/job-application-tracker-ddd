CREATE TABLE process_opportunity_history (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  process_id uuid NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
  previous_opportunity uuid REFERENCES opportunities(id),
  next_opportunity uuid NOT NULL REFERENCES opportunities(id),
  changed_at timestamptz NOT NULL DEFAULT now()
);

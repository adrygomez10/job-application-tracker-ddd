CREATE TABLE opportunities (
  id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
  employer varchar(160) NOT NULL, agency varchar(160) NOT NULL DEFAULT '', role varchar(180) NOT NULL,
  location varchar(160) NOT NULL DEFAULT '', work_mode varchar(32) NOT NULL, url varchar(2000) NOT NULL DEFAULT '',
  source_key varchar(240) NOT NULL UNIQUE, created_at timestamptz NOT NULL DEFAULT now(),
  salary_min numeric(14,2), salary_max numeric(14,2), currency varchar(3), salary_period varchar(8),
  CHECK (salary_min IS NULL OR salary_min >= 0), CHECK (salary_max IS NULL OR salary_max >= salary_min),
  CHECK ((salary_min IS NULL AND salary_max IS NULL AND currency IS NULL AND salary_period IS NULL)
    OR (salary_min IS NOT NULL AND currency IS NOT NULL AND salary_period IN ('YEAR','MONTH'))),
  CHECK (work_mode IN ('UNSPECIFIED','REMOTE','HYBRID','ONSITE'))
);
-- One provisional identity per legacy row: never merge ambiguous historical text.
INSERT INTO opportunities (id, employer, role, location, work_mode, url, source_key, created_at)
  SELECT id, company, role, location, work_mode, url, 'legacy:' || id, created_at FROM job_applications;
ALTER TABLE job_applications ADD COLUMN opportunity_id uuid REFERENCES opportunities(id);
UPDATE job_applications SET opportunity_id = id;
CREATE UNIQUE INDEX one_active_process_per_opportunity ON job_applications(opportunity_id)
  WHERE status IN ('APPLIED','IN_PROGRESS','OFFER_RECEIVED');
CREATE TABLE search_profile (
  id integer PRIMARY KEY CHECK (id = 1), version bigint NOT NULL DEFAULT 0,
  display_name varchar(120) NOT NULL, target_role varchar(180) NOT NULL DEFAULT '',
  location varchar(160) NOT NULL DEFAULT '', work_mode varchar(32) NOT NULL DEFAULT 'UNSPECIFIED',
  salary_min numeric(14,2), salary_max numeric(14,2), currency varchar(3), salary_period varchar(8),
  zone varchar(80) NOT NULL DEFAULT 'Europe/Madrid', external_notifications boolean NOT NULL DEFAULT false,
  quiet_from time NOT NULL DEFAULT '22:00', quiet_until time NOT NULL DEFAULT '08:00',
  CHECK (salary_min IS NULL OR salary_min >= 0), CHECK (salary_max IS NULL OR salary_max >= salary_min),
  CHECK ((salary_min IS NULL AND salary_max IS NULL AND currency IS NULL AND salary_period IS NULL)
    OR (salary_min IS NOT NULL AND currency IS NOT NULL AND salary_period IN ('YEAR','MONTH'))),
  CHECK (work_mode IN ('UNSPECIFIED','REMOTE','HYBRID','ONSITE'))
);
INSERT INTO search_profile (id, display_name) VALUES (1, 'Mi perfil');

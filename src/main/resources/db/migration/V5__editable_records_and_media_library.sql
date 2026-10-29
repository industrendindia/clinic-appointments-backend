CREATE TABLE payment_events(
  id bigserial PRIMARY KEY,
  clinic_id uuid NOT NULL REFERENCES clinics(id),
  payment_id uuid NOT NULL REFERENCES payments(id),
  actor_user_id uuid REFERENCES users(id),
  old_values jsonb NOT NULL,
  new_values jsonb NOT NULL,
  reason varchar(500) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE media_assets(
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  clinic_id uuid NOT NULL REFERENCES clinics(id),
  branch_id uuid REFERENCES branches(id),
  media_type varchar(30) NOT NULL CHECK(media_type IN ('PHOTO','REEL','STORY','SUCCESS_STORY')),
  title varchar(180) NOT NULL,
  caption varchar(1000),
  content_type varchar(100) NOT NULL,
  original_name varchar(255) NOT NULL,
  content bytea NOT NULL,
  featured boolean NOT NULL DEFAULT false,
  active boolean NOT NULL DEFAULT true,
  created_by uuid REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_media_assets_clinic ON media_assets(clinic_id,active,media_type,created_at DESC);
CREATE INDEX ix_payment_events_payment ON payment_events(payment_id,created_at DESC);

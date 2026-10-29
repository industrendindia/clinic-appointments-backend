CREATE TABLE products(
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  clinic_id uuid NOT NULL REFERENCES clinics(id),
  name varchar(160) NOT NULL,
  description varchar(500),
  price numeric(12,2) NOT NULL,
  stock_quantity int NOT NULL DEFAULT 0,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(clinic_id,name)
);

CREATE TABLE whatsapp_outbox(
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  clinic_id uuid NOT NULL REFERENCES clinics(id),
  recipient varchar(20) NOT NULL,
  template_type varchar(50) NOT NULL,
  payload jsonb NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'PENDING',
  created_at timestamptz NOT NULL DEFAULT now(),
  sent_at timestamptz
);

CREATE INDEX ix_products_clinic ON products(clinic_id,active);
CREATE INDEX ix_whatsapp_outbox_pending ON whatsapp_outbox(status,created_at);


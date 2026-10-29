ALTER TABLE practitioners ADD COLUMN IF NOT EXISTS phone varchar(20);
ALTER TABLE practitioners ADD COLUMN IF NOT EXISTS photo_data bytea;
ALTER TABLE practitioners ADD COLUMN IF NOT EXISTS photo_content_type varchar(80);
ALTER TABLE practitioners ADD COLUMN IF NOT EXISTS document_archive bytea;
ALTER TABLE practitioners ADD COLUMN IF NOT EXISTS document_archive_name varchar(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS adjustment numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS notes varchar(500);

CREATE UNIQUE INDEX IF NOT EXISTS ux_practitioner_availability
  ON practitioner_availability(practitioner_id,day_of_week,starts_at,ends_at);
CREATE INDEX IF NOT EXISTS ix_practitioner_services_service
  ON practitioner_services(service_id,practitioner_id);
CREATE INDEX IF NOT EXISTS ix_customer_services_service
  ON customer_services(service_id,customer_id);

ALTER TABLE rooms ADD CONSTRAINT rooms_type_check
  CHECK(room_type IN ('ASSESSMENT','THERAPY','COUNSELLING','GENERAL')) NOT VALID;
ALTER TABLE practitioner_availability ADD CONSTRAINT availability_time_check
  CHECK(starts_at < ends_at) NOT VALID;

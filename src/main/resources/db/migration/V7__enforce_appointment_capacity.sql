CREATE OR REPLACE FUNCTION enforce_appointment_capacity()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status <> 'BOOKED' THEN
    RETURN NEW;
  END IF;

  -- Serialize capacity decisions inside one branch. This prevents two requests
  -- from both observing the same practitioner, customer, or room as free.
  PERFORM pg_advisory_xact_lock(hashtextextended(NEW.branch_id::text, 0));

  IF EXISTS (
    SELECT 1 FROM appointments a
    WHERE a.id <> NEW.id
      AND a.clinic_id = NEW.clinic_id
      AND a.practitioner_id = NEW.practitioner_id
      AND a.status = 'BOOKED'
      AND a.starts_at < NEW.ends_at
      AND a.ends_at > NEW.starts_at
  ) THEN
    RAISE EXCEPTION 'Practitioner is already booked during this time'
      USING ERRCODE = '23P01';
  END IF;

  IF EXISTS (
    SELECT 1 FROM appointments a
    WHERE a.id <> NEW.id
      AND a.clinic_id = NEW.clinic_id
      AND a.customer_id = NEW.customer_id
      AND a.status = 'BOOKED'
      AND a.starts_at < NEW.ends_at
      AND a.ends_at > NEW.starts_at
  ) THEN
    RAISE EXCEPTION 'Customer already has an appointment during this time'
      USING ERRCODE = '23P01';
  END IF;

  IF EXISTS (
    SELECT 1 FROM appointments a
    WHERE a.id <> NEW.id
      AND a.clinic_id = NEW.clinic_id
      AND a.room_id = NEW.room_id
      AND a.status = 'BOOKED'
      AND a.starts_at < NEW.ends_at
      AND a.ends_at > NEW.starts_at
  ) THEN
    RAISE EXCEPTION 'Room is already booked during this time'
      USING ERRCODE = '23P01';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS appointments_capacity_guard ON appointments;
CREATE TRIGGER appointments_capacity_guard
BEFORE INSERT OR UPDATE OF practitioner_id, customer_id, room_id, starts_at, ends_at, status
ON appointments
FOR EACH ROW
EXECUTE FUNCTION enforce_appointment_capacity();

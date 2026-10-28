-- V1 shipped demonstration records for the first visual prototype. They must not
-- survive in an authenticated installation. Real clinics are created explicitly
-- through the platform-admin onboarding API.
DELETE FROM users WHERE username IN ('admin','owner@riseclinic.in','reception@riseclinic.in');
DELETE FROM services WHERE clinic_id = '10000000-0000-0000-0000-000000000001';
DELETE FROM rooms WHERE clinic_id = '10000000-0000-0000-0000-000000000001';
DELETE FROM branches WHERE clinic_id = '10000000-0000-0000-0000-000000000001';
DELETE FROM clinics WHERE id = '10000000-0000-0000-0000-000000000001';


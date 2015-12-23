ALTER TABLE oskari_users
   ALTER COLUMN user_name TYPE character varying(55);

ALTER TABLE oskari_role_oskari_user
   ALTER COLUMN user_name TYPE character varying(55);
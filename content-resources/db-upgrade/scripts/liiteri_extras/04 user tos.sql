alter table oskari_users add column tos_accepted timestamp with time zone;

update oskari_users set tos_accepted = current_timestamp where user_name not like '%@%';
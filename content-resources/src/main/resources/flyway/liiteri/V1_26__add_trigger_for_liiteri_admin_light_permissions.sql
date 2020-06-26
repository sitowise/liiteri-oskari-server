-- FUNCTION: public.procedure_copy_liiteri_admin_light_permission()

-- DROP FUNCTION public.procedure_copy_liiteri_admin_light_permission();

CREATE FUNCTION public.procedure_copy_liiteri_admin_light_permission()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100.0
    VOLATILE NOT LEAKPROOF 
AS $BODY$
BEGIN
	IF NEW.external_type = 'ROLE' AND NEW.external_id = (SELECT id::varchar FROM oskari_roles WHERE name = 'liiteri_admin_light') THEN
		INSERT INTO oskari_permission (oskari_resource_id, external_type, permission, external_id)
        VALUES (NEW.oskari_resource_id, NEW.external_type, NEW.permission, (SELECT id::varchar FROM oskari_roles WHERE name = 'liiteri_admin'));
	END IF;
    RETURN NEW;
END;

$BODY$;

ALTER FUNCTION public.procedure_copy_liiteri_admin_light_permission()
    OWNER TO oskari;

	
	
-- Trigger: trigger_copy_liiteri_admin_light_permission

-- DROP TRIGGER trigger_copy_liiteri_admin_light_permission ON public.oskari_permission;

CREATE TRIGGER trigger_copy_liiteri_admin_light_permission
    AFTER INSERT
    ON public.oskari_permission
    FOR EACH ROW
    EXECUTE PROCEDURE procedure_copy_liiteri_admin_light_permission();

CREATE FUNCTION procedure_user_layer_data_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        NEW.updated := current_timestamp;
    RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        NEW.created := current_timestamp;
    RETURN NEW;
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.procedure_user_layer_data_update() OWNER TO oskari;


CREATE FUNCTION procedure_user_layer_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        NEW.updated := current_timestamp;
    RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        NEW.created := current_timestamp;
    RETURN NEW;
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.procedure_user_layer_update() OWNER TO oskari;

CREATE TABLE gt_pk_metadata_table (
    table_schema character varying(32) NOT NULL,
    table_name character varying(32) NOT NULL,
    pk_column character varying(32) NOT NULL,
    pk_column_idx integer,
    pk_policy character varying(32),
    pk_sequence character varying(64)
);


ALTER TABLE public.gt_pk_metadata_table OWNER TO oskari;

ALTER TABLE public.my_places ALTER updated SET NOT NULL;

CREATE TABLE oskari_announcements (
    id integer NOT NULL,
    title character varying(255) NOT NULL,
    message character varying(255) NOT NULL,
    expiration_date date NOT NULL
);


ALTER TABLE public.oskari_announcements OWNER TO oskari;


CREATE SEQUENCE oskari_announcements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.oskari_announcements_id_seq OWNER TO oskari;


ALTER SEQUENCE oskari_announcements_id_seq OWNED BY oskari_announcements.id;


ALTER TABLE public.oskari_groupings_themes ADD unbound_layers boolean DEFAULT false NOT NULL;

ALTER TABLE public.oskari_workspaces_sharing RENAME COLUMN externaltypeid to externalid;

ALTER TABLE public.oskari_workspaces_sharing ADD email character varying(255);

ALTER TABLE public.oskari_workspaces_sharing ADD email_sent boolean;

CREATE VIEW portti_backendalert AS
 SELECT portti_backendstatus.id,
    portti_backendstatus.ts,
    portti_backendstatus.maplayer_id,
    portti_backendstatus.status,
    portti_backendstatus.statusmessage,
    portti_backendstatus.infourl,
    portti_backendstatus.statusjson
   FROM portti_backendstatus
  WHERE (((NOT (portti_backendstatus.status IS NULL)) AND (NOT ((portti_backendstatus.status)::text = 'UNKNOWN'::text))) AND (NOT ((portti_backendstatus.status)::text = 'OK'::text)));


ALTER TABLE public.portti_backendalert OWNER TO oskari;


CREATE VIEW portti_backendstatus_allknown AS
 SELECT portti_backendstatus.id,
    portti_backendstatus.ts,
    portti_backendstatus.maplayer_id,
    portti_backendstatus.status,
    portti_backendstatus.statusmessage,
    portti_backendstatus.infourl,
    portti_backendstatus.statusjson
   FROM portti_backendstatus;


ALTER TABLE public.portti_backendstatus_allknown OWNER TO oskari;

CREATE TABLE user_layer (
    id bigint NOT NULL,
    uuid character varying(64),
    layer_name character varying(256) NOT NULL,
    layer_desc character varying(256),
    layer_source character varying(256),
    publisher_name character varying(256),
    style_id bigint,
    created timestamp with time zone NOT NULL,
    updated timestamp with time zone,
    fields json
);


ALTER TABLE public.user_layer OWNER TO oskari;


CREATE TABLE user_layer_data (
    id bigint NOT NULL,
    user_layer_id bigint NOT NULL,
    uuid character varying(64),
    feature_id character varying(64),
    property_json json,
    geometry geometry NOT NULL,
    created timestamp with time zone NOT NULL,
    updated timestamp with time zone
);


ALTER TABLE public.user_layer_data OWNER TO oskari;


CREATE SEQUENCE user_layer_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_layer_data_id_seq OWNER TO oskari;


ALTER SEQUENCE user_layer_data_id_seq OWNED BY user_layer_data.id;



CREATE TABLE user_layer_style (
    id bigint NOT NULL,
    stroke_width integer,
    stroke_color character(7),
    fill_color character(7),
    dot_color character(7),
    dot_size integer,
    border_width integer,
    border_color character(7),
    dot_shape character varying(20) DEFAULT '8'::character varying NOT NULL,
    stroke_linejoin character varying(256),
    fill_pattern integer DEFAULT (-1),
    stroke_linecap character varying(256),
    stroke_dasharray character varying(256),
    border_linejoin character varying(256),
    border_dasharray character varying(256)
);


ALTER TABLE public.user_layer_style OWNER TO oskari;


CREATE VIEW user_layer_data_style AS
 SELECT ad.id,
    ad.uuid,
    ad.user_layer_id,
    a.layer_name,
    a.publisher_name,
    ad.feature_id,
    ad.created,
    ad.updated,
    ad.geometry,
    st.stroke_width,
    st.stroke_color,
    st.fill_color,
    st.dot_color,
    st.dot_size,
    st.dot_shape,
    st.border_width,
    st.border_color,
    st.fill_pattern,
    st.stroke_linejoin,
    st.stroke_linecap,
    st.stroke_dasharray,
    st.border_linejoin,
    st.border_dasharray
   FROM user_layer_data ad,
    user_layer a,
    user_layer_style st
  WHERE ((ad.user_layer_id = a.id) AND (a.style_id = st.id));


ALTER TABLE public.user_layer_data_style OWNER TO oskari;


CREATE SEQUENCE user_layer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_layer_id_seq OWNER TO oskari;


ALTER SEQUENCE user_layer_id_seq OWNED BY user_layer.id;



CREATE SEQUENCE user_layer_style_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_layer_style_id_seq OWNER TO oskari;


ALTER SEQUENCE user_layer_style_id_seq OWNED BY user_layer_style.id;



CREATE VIEW vuser_layer_data AS
 SELECT user_layer_data.id,
    user_layer_data.uuid,
    user_layer_data.user_layer_id,
    user_layer_data.feature_id,
    (user_layer_data.property_json)::text AS property_json,
    user_layer_data.created,
    user_layer_data.updated,
    user_layer_data.geometry
   FROM user_layer_data;


ALTER TABLE public.vuser_layer_data OWNER TO oskari;

ALTER TABLE ONLY oskari_announcements ALTER COLUMN id SET DEFAULT nextval('oskari_announcements_id_seq'::regclass);

ALTER TABLE ONLY user_layer ALTER COLUMN id SET DEFAULT nextval('user_layer_id_seq'::regclass);



ALTER TABLE ONLY user_layer_data ALTER COLUMN id SET DEFAULT nextval('user_layer_data_id_seq'::regclass);



ALTER TABLE ONLY user_layer_style ALTER COLUMN id SET DEFAULT nextval('user_layer_style_id_seq'::regclass);



ALTER TABLE ONLY oskari_announcements
    ADD CONSTRAINT "PK_OSKARI_ANNOUNCEMENTS" PRIMARY KEY (id);

ALTER TABLE ONLY gt_pk_metadata_table
    ADD CONSTRAINT gt_pk_metadata_table_table_schema_table_name_pk_column_key UNIQUE (table_schema, table_name, pk_column);

ALTER TABLE ONLY user_layer_data
    ADD CONSTRAINT "user_layer_data_pKey" PRIMARY KEY (id);



ALTER TABLE ONLY user_layer
    ADD CONSTRAINT user_layer_pkey PRIMARY KEY (id);



ALTER TABLE ONLY user_layer_style
    ADD CONSTRAINT user_layer_style_pkey PRIMARY KEY (id);

CREATE TRIGGER trigger_user_layer BEFORE INSERT OR UPDATE ON user_layer FOR EACH ROW EXECUTE PROCEDURE procedure_user_layer_update();



CREATE TRIGGER trigger_user_layer_update BEFORE INSERT OR UPDATE ON user_layer_data FOR EACH ROW EXECUTE PROCEDURE procedure_user_layer_data_update();

ALTER TABLE ONLY user_layer_data
    ADD CONSTRAINT user_layer_data_user_layer_fkey FOREIGN KEY (user_layer_id) REFERENCES user_layer(id) ON DELETE CASCADE;



ALTER TABLE ONLY user_layer
    ADD CONSTRAINT user_layer_style_id_fkey FOREIGN KEY (style_id) REFERENCES user_layer_style(id);



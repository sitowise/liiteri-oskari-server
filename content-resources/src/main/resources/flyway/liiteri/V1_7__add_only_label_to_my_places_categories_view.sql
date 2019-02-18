DROP VIEW "public"."my_places_categories";

CREATE VIEW "public"."my_places_categories" AS
  SELECT
    mp.id,
    mp.uuid,
    mp.category_id,
    mp.name,
    mp.attention_text,
    mp.place_desc,
    mp.created,
    mp.updated,
    mp.geometry,
    mp.only_label,
    c.category_name,
    c."default",
    c.stroke_width,
    c.stroke_color,
    c.fill_color,
    c.dot_color,
    c.dot_size,
    c.dot_shape,
    c.border_width,
    c.border_color,
    c.publisher_name,
    mp.link,
    mp.image_url,
    c.fill_pattern,
    c.stroke_linejoin,
    c.stroke_linecap,
    c.stroke_dasharray,
    c.border_linejoin,
    c.border_dasharray
  FROM my_places mp,
    categories c
  WHERE (mp.category_id = c.id);
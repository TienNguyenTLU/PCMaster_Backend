UPDATE products
SET specs = jsonb_strip_nulls(
  (
    specs 
    - 'chu_n_ch_ng_nh_n' 
    - 'lo_i_modular' 
    - 'chu_n_ngu_n' 
    - 'form_factor' 
    - 'c_ng_su_t' 
    - 'c_ng_su_t_t_i_a'
  )
  || jsonb_build_object(
    'efficiency_rating', COALESCE(specs -> 'chu_n_ch_ng_nh_n', specs -> 'efficiency_rating'),
    'modularity', COALESCE(specs -> 'lo_i_modular', specs -> 'modularity'),
    'dimensions', COALESCE(specs -> 'form_factor', specs -> 'dimensions'),
    'form_factor', COALESCE(specs -> 'chu_n_ngu_n', specs -> 'form_factor'),
    'wattage', 
      CASE 
        WHEN specs -> 'c_ng_su_t' IS NOT NULL THEN
          to_jsonb(NULLIF(SUBSTRING(specs ->> 'c_ng_su_t' FROM '^[0-9]+'), '')::integer)
        WHEN specs -> 'c_ng_su_t_t_i_a' IS NOT NULL THEN
          to_jsonb(NULLIF(SUBSTRING(specs ->> 'c_ng_su_t_t_i_a' FROM '^[0-9]+'), '')::integer)
        WHEN specs -> 'wattage' IS NOT NULL AND specs ->> 'wattage' ~ '[wW]' THEN
          to_jsonb(NULLIF(SUBSTRING(specs ->> 'wattage' FROM '^[0-9]+'), '')::integer)
        ELSE
          specs -> 'wattage'
      END
  )
)
WHERE specs ->> 'component_type' = 'PSU';

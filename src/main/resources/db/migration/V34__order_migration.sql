UPDATE code c
    SET flatorder = flatorder_new
    FROM (SELECT c.*,
                 row_number() OVER (PARTITION BY codescheme_id ORDER BY codevalue) AS flatorder_new
          FROM code c
         ) cc
    WHERE c.id = cc.id AND c.flatorder IS NULL;
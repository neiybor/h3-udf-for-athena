USING EXTERNAL FUNCTION h3_to_geo(h3Address VARCHAR) 
RETURNS ARRAY(DOUBLE)
LAMBDA '<ARN>'
SELECT h3_to_geo('8a3969ab2037fff')

# [43.55279969968375, 7.017036400377743]


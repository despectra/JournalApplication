//CP - comtent provider
INPUT: JSONArray 	response [ {k1:v1, k2:v2, .. , kn:vn},
						{k1:v1, k2:v2, .. , kn:vn},
						...
						{k1:v1, k2:v2, .. , kn:vn}],
		Table[] 	tables,
		String		tablesSelection,
		String[] 	tablesSelArgs,
		String[] 	tablesIdCols,
		String[] 	jsonIdCols,
		String[][]	tablesDataCols,
		String[][]	jsonDataCols
ALGORITHM:

Cursor existingRows = CP.query(table.name,
	all_local_and_remote_id_columns,
	tablesSelection,
	tablesSelArgs);

for (i = 0; i < tables.count; i++) {
	JSONArray currentJsonProjection = getJsonProj(response,
										{jsonIdCols[i]} + jsonDataCols[i] );

	for(j = 0; j < response.count; j++) {

	}



}

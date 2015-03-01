package edu.isi.wubble.physics.state;

public interface StateDatabaseI {
	public final String CREATE_SESSION = 
		"create table if not exists user_sessions (" +
		"session_id INTEGER PRIMARY KEY, " +
		"start_time REAL, " + 
		"end_time REAL, " +
		"end_offset_time INTEGER, " +
		"end_logical_time INTEGER " +
		")";
	
	public final String CREATE_ENTITY_TABLE = 
		"create table if not exists entity_table (" +
		"id INTEGER PRIMARY KEY, " + 
		"session_id INTEGER, " + 
		"name TEXT" +
		")";
	
	public final String CREATE_FLUENT_LOOKUP_TABLE =
		"create table if not exists lookup_fluent_table (" +
		"id INTEGER PRIMARY KEY, " +
		"name TEXT" +
		")";
	
	public final String CREATE_TAG_TABLE = 
		"create table if not exists tag_table (" +
		"id INTEGER PRIMARY KEY, " + 
		"session_id INTEGER, " + 
		"tag TEXT, " + 
		"start_logical_time INTEGER, " + 
		"end_logical_time INTEGER " +
		")";
	
	public final String CREATE_TAG_MAP_TABLE = 
		"create table if not exists tag_map_table (" +
		"id INTEGER, " + 
		"orig_session_id INTEGER, " + 
		"new_session_id INTEGER " +
		")";
	
	public final String CREATE_FLUENT_TABLE = 
		"create table if not exists fluent_STUB (" +
		"session_id INTEGER, " +
		"id INTEGER, " + 
		"fluent_id INTEGER, " +  // should be foreign keyed to fluent_table
		"entity_1 INTEGER, " +   // should be foreign keyed to entity_table
		"entity_2 INTEGER, " +   // should be foreign keyed to entity_table
		"value TEXT, " + 
		"start_time REAL, " +
		"start_logical_time INTEGER, " +
		"end_time REAL, " +
		"end_logical_time INTEGER, " +
		"PRIMARY KEY (id) " + 
		")";

	public final String CREATE_FLUENT_INDEX_1 = 
		"create index if not exists lookup_index_STUB_1 " + 
		"on fluent_STUB (session_id, fluent_id, entity_1)";
}

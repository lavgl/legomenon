create table if not exists uploading_status (
  id integer primary key,
  filename text not null,
  state text not null,
  state_info text,
  created_at integer not null default current_timestamp,
  updated_at integer not null default current_timestamp
);


alter table books add column upload_finished_at integer;

update books
   set upload_finished_at = created_at
where deleted_at is null;


drop table if exists uploading_progress;

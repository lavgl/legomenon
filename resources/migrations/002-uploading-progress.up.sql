create table if not exists uploading_progress (
  id integer primary key,
  current_percent integer not null
);

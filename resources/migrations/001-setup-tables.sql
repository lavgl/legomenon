create table books (
  id integer primary key,
  filename text not null,
  text text not null unique,
  text_hash text not null,
  created_at integer not null default current_timestamp,
  deleted_at integer
);

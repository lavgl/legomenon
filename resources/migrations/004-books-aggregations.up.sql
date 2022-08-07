create table if not exists books_aggregations (
  -- in-code generated uuid
  id text primary key,
  name text unique not null,
  created_at integer not null default current_timestamp,
  updated_at integer not null default current_timestamp,
  deleted_at integer
);

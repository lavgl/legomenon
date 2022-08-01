create table if not exists books (
  -- id is calculated as a book text hash
  id text primary key,
  filename text not null,
  user_entered_title text,
  text text not null unique,
  created_at integer not null default current_timestamp,
  used_at integer,
  deleted_at integer
);

--;;

create table if not exists lemma_count (
  id integer primary key,
  book_id text not null,
  lemma text not null,
  count integer not null
);

--;;

create table if not exists my_words (
  word text primary key,
  list text,
  deleted_at integer
);

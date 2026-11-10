-- MELOVA 1.0 · Supabase SQL setup
-- Run once, in a NEW Supabase project, in SQL Editor as project administrator.
-- All public data tables enable RLS and grants are restricted by operation/column.
-- Note: no service_role keys or privileged credentials belong in index.html.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null,
  display_name text not null default 'Foydalanuvchi',
  bio text not null default '',
  avatar_path text,
  created_at timestamptz not null default now(),
  constraint username_valid check (username ~ '^[a-zA-Z0-9_]{3,24}$'),
  constraint display_name_len check (char_length(display_name) between 1 and 64),
  constraint bio_len check (char_length(bio) <= 250),
  constraint avatar_owner check (avatar_path is null or split_part(avatar_path, '/', 1) = id::text)
);
create unique index if not exists profiles_username_ci on public.profiles (lower(username));

create table if not exists public.admins (
  id uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create or replace function public.is_admin()
returns boolean
language sql stable security invoker
set search_path = ''
as $$
  select exists (select 1 from public.admins where id = (select auth.uid()));
$$;

create schema if not exists private;
create or replace function private.create_profile_for_new_user()
returns trigger language plpgsql security definer
set search_path = ''
as $$
begin
  insert into public.profiles(id, username, display_name)
  values (
    new.id,
    'user_' || left(replace(new.id::text, '-', ''), 12),
    left(coalesce(nullif(trim(new.raw_user_meta_data->>'full_name'), ''), 'Foydalanuvchi'), 64)
  )
  on conflict (id) do nothing;
  return new;
end;
$$;
drop trigger if exists on_auth_user_created_melova on auth.users;
create trigger on_auth_user_created_melova
  after insert on auth.users for each row
  execute function private.create_profile_for_new_user();
-- Backfill any users who existed before this script.
insert into public.profiles(id, username, display_name)
select id, 'user_' || left(replace(id::text,'-',''),12),
       left(coalesce(nullif(trim(raw_user_meta_data->>'full_name'),''),'Foydalanuvchi'),64)
from auth.users
on conflict (id) do nothing;

create table if not exists public.tracks (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 120),
  artist text not null check (char_length(artist) between 1 and 100),
  genre text not null default 'Boshqa' check (char_length(genre) between 1 and 50),
  description text not null default '' check (char_length(description) <= 500),
  file_path text not null unique,
  file_size bigint not null check (file_size > 0 and file_size <= 20971520),
  mime_type text not null check (mime_type in ('audio/mpeg','audio/mp4','audio/x-m4a','audio/wav','audio/x-wav','audio/ogg')),
  status text not null default 'private' check (status in ('private','pending','approved','rejected')),
  copyright_attested_at timestamptz,
  review_note text,
  reviewed_at timestamptz,
  reviewed_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  constraint path_is_own_audio check (
    split_part(file_path,'/',1) = owner_id::text and
    file_path ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}\.(mp3|m4a|wav|ogg)$'
  )
);
create index if not exists tracks_status_created_idx on public.tracks(status,created_at desc);
create index if not exists tracks_owner_created_idx on public.tracks(owner_id,created_at desc);

-- An ordinary user cannot publish music, alter owner/path/size, or alter reviewed metadata.
-- Admins can approve/reject via normal authenticated UPDATE (authorized by RLS + trigger).
create or replace function private.guard_track_update()
returns trigger language plpgsql security invoker
set search_path = ''
as $$
begin
  if new.owner_id is distinct from old.owner_id or
     new.file_path is distinct from old.file_path or
     new.file_size is distinct from old.file_size or
     new.mime_type is distinct from old.mime_type or
     new.created_at is distinct from old.created_at then
    raise exception 'Yuklangan audio fayli va egasini o‘zgartirib bo‘lmaydi';
  end if;
  if (select public.is_admin()) then
    if new.status = 'approved' and old.status <> 'pending' then
      raise exception 'Faqat ko‘rib chiqilayotgan trek tasdiqlanishi mumkin';
    end if;
    return new;
  end if;
  if old.owner_id <> (select auth.uid()) then
    raise exception 'Ruxsat yo‘q';
  end if;
  if old.status = 'approved' then
    raise exception 'Tasdiqlangan trekni tahrirlab bo‘lmaydi';
  end if;
  if new.review_note is distinct from old.review_note or
     new.reviewed_at is distinct from old.reviewed_at or
     new.reviewed_by is distinct from old.reviewed_by then
    raise exception 'Tekshiruv ma’lumotlarini o‘zgartirish mumkin emas';
  end if;
  if new.status is distinct from old.status and not (
     (old.status in ('private','rejected') and new.status = 'pending' and new.copyright_attested_at is not null)
     or (old.status = 'pending' and new.status = 'private')
  ) then
    raise exception 'Statusni bu tarzda o‘zgartirish mumkin emas';
  end if;
  if new.copyright_attested_at is distinct from old.copyright_attested_at and
    new.copyright_attested_at is not null and new.copyright_attested_at > now() + interval '5 minutes' then
    raise exception 'Noto‘g‘ri sana';
  end if;
  return new;
end;
$$;
drop trigger if exists protect_track_melova on public.tracks;
create trigger protect_track_melova before update on public.tracks
for each row execute function private.guard_track_update();

create table if not exists public.favorites (
  user_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key(user_id,track_id)
);
create index if not exists favorites_track_idx on public.favorites(track_id);

create table if not exists public.playlists (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 90),
  description text not null default '' check (char_length(description) <= 300),
  is_public boolean not null default false,
  created_at timestamptz not null default now()
);
create index if not exists playlists_owner_idx on public.playlists(owner_id);
create index if not exists playlists_public_idx on public.playlists(is_public,created_at desc);

create table if not exists public.playlist_tracks (
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  added_at timestamptz not null default now(),
  primary key(playlist_id,track_id)
);
create index if not exists playlist_tracks_track_idx on public.playlist_tracks(track_id);

create table if not exists public.listening_history (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  played_at timestamptz not null default now()
);
create index if not exists history_user_time_idx on public.listening_history(user_id,played_at desc);

create table if not exists public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references auth.users(id) on delete cascade,
  track_id uuid not null references public.tracks(id) on delete cascade,
  reason text not null check (char_length(reason) between 10 and 600),
  state text not null default 'open' check (state in ('open','resolved')),
  created_at timestamptz not null default now()
);
create unique index if not exists reports_one_per_user_track on public.reports(reporter_id,track_id);
create index if not exists reports_state_idx on public.reports(state,created_at desc);

-- RLS on every exposed table.
alter table public.profiles enable row level security;
alter table public.admins enable row level security;
alter table public.tracks enable row level security;
alter table public.favorites enable row level security;
alter table public.playlists enable row level security;
alter table public.playlist_tracks enable row level security;
alter table public.listening_history enable row level security;
alter table public.reports enable row level security;

-- Revoke overly broad default grants Supabase may assign in public schema.
revoke all on public.profiles,public.admins,public.tracks,public.favorites,
  public.playlists,public.playlist_tracks,public.listening_history,public.reports
  from anon,authenticated;
revoke all on function public.is_admin() from public,anon,authenticated;
grant execute on function public.is_admin() to authenticated;

grant select on public.profiles to anon,authenticated;
grant update(username,display_name,bio,avatar_path) on public.profiles to authenticated;
grant select on public.admins to authenticated;
grant select on public.tracks to anon,authenticated;
grant insert,delete on public.tracks to authenticated;
grant update(title,artist,genre,description,status,copyright_attested_at,review_note,reviewed_at,reviewed_by)
  on public.tracks to authenticated;
grant select,insert,delete on public.favorites to authenticated;
grant select on public.playlists to anon,authenticated;
grant insert,delete on public.playlists to authenticated;
grant update(name,description,is_public) on public.playlists to authenticated;
grant select on public.playlist_tracks to anon,authenticated;
grant insert,delete on public.playlist_tracks to authenticated;
grant select,insert,delete on public.listening_history to authenticated;
grant usage,select on sequence public.listening_history_id_seq to authenticated;
grant select,insert on public.reports to authenticated;
grant update(state) on public.reports to authenticated;

create policy profiles_read on public.profiles for select to anon,authenticated using(true);
create policy profiles_update_own on public.profiles for update to authenticated
  using(id=(select auth.uid())) with check(id=(select auth.uid()));
create policy admins_read_self on public.admins for select to authenticated
  using(id=(select auth.uid()));

create policy tracks_public_read on public.tracks for select to anon,authenticated
  using(status='approved');
create policy tracks_own_or_admin_read on public.tracks for select to authenticated
  using(owner_id=(select auth.uid()) or ((select public.is_admin()) and status in ('pending','approved','rejected')));
create policy tracks_insert_own_private on public.tracks for insert to authenticated
  with check(owner_id=(select auth.uid()) and status='private' and
    review_note is null and reviewed_at is null and reviewed_by is null and copyright_attested_at is null);
create policy tracks_update_owner on public.tracks for update to authenticated
  using(owner_id=(select auth.uid())) with check(owner_id=(select auth.uid()));
create policy tracks_update_admin on public.tracks for update to authenticated
  using((select public.is_admin()) and status in ('pending','approved','rejected')) with check((select public.is_admin()));
create policy tracks_delete_private_or_admin on public.tracks for delete to authenticated
  using((owner_id=(select auth.uid()) and status in ('private','rejected')) or ((select public.is_admin()) and status in ('pending','approved','rejected')));

create policy favorites_read_own on public.favorites for select to authenticated
  using(user_id=(select auth.uid()));
create policy favorites_add_own on public.favorites for insert to authenticated
  with check(user_id=(select auth.uid()) and exists (
    select 1 from public.tracks t where t.id=track_id and
    (t.status='approved' or t.owner_id=(select auth.uid()))
  ));
create policy favorites_remove_own on public.favorites for delete to authenticated
  using(user_id=(select auth.uid()));

create policy playlists_public_read on public.playlists for select to anon,authenticated
  using(is_public);
create policy playlists_owner_read on public.playlists for select to authenticated
  using(owner_id=(select auth.uid()));
create policy playlists_insert_own on public.playlists for insert to authenticated
  with check(owner_id=(select auth.uid()));
create policy playlists_update_own on public.playlists for update to authenticated
  using(owner_id=(select auth.uid())) with check(owner_id=(select auth.uid()));
create policy playlists_delete_own on public.playlists for delete to authenticated
  using(owner_id=(select auth.uid()));

create policy playlist_tracks_read_public on public.playlist_tracks for select to anon,authenticated
  using(exists (select 1 from public.playlists p where p.id=playlist_id and p.is_public)
        and exists (select 1 from public.tracks t where t.id=track_id and t.status='approved'));
create policy playlist_tracks_read_owner on public.playlist_tracks for select to authenticated
  using(exists (select 1 from public.playlists p where p.id=playlist_id and p.owner_id=(select auth.uid())));
create policy playlist_tracks_insert_owner on public.playlist_tracks for insert to authenticated
  with check(
    exists (select 1 from public.playlists p where p.id=playlist_id and p.owner_id=(select auth.uid()))
    and exists (select 1 from public.tracks t where t.id=track_id and
      (t.status='approved' or t.owner_id=(select auth.uid())))
  );
create policy playlist_tracks_delete_owner on public.playlist_tracks for delete to authenticated
  using(exists (select 1 from public.playlists p where p.id=playlist_id and p.owner_id=(select auth.uid())));

create policy history_own_read on public.listening_history for select to authenticated
  using(user_id=(select auth.uid()));
create policy history_insert_own on public.listening_history for insert to authenticated
  with check(user_id=(select auth.uid()) and exists (
    select 1 from public.tracks t where t.id=track_id and
    (t.status='approved' or t.owner_id=(select auth.uid()))
  ));
create policy history_delete_own on public.listening_history for delete to authenticated
  using(user_id=(select auth.uid()));

create policy reports_own_or_admin_read on public.reports for select to authenticated
  using(reporter_id=(select auth.uid()) or (select public.is_admin()));
create policy reports_create_own on public.reports for insert to authenticated
  with check(reporter_id=(select auth.uid()) and state='open'
    and exists(select 1 from public.tracks t where t.id=track_id and t.status='approved'));
create policy reports_admin_update on public.reports for update to authenticated
  using((select public.is_admin())) with check((select public.is_admin()));

-- Storage buckets (audio stays private, avatars are public).
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('melova-audio','melova-audio',false,20971520,
  array['audio/mpeg','audio/mp4','audio/x-m4a','audio/wav','audio/x-wav','audio/ogg'])
on conflict(id) do update set public=false,file_size_limit=excluded.file_size_limit,
  allowed_mime_types=excluded.allowed_mime_types;
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('melova-avatars','melova-avatars',true,2097152,
  array['image/jpeg','image/png','image/webp'])
on conflict(id) do update set public=true,file_size_limit=excluded.file_size_limit,
  allowed_mime_types=excluded.allowed_mime_types;

create policy melova_audio_insert_own on storage.objects for insert to authenticated
  with check(bucket_id='melova-audio' and (storage.foldername(name))[1]=(select auth.uid())::text
    and name ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}\.(mp3|m4a|wav|ogg)$');
create policy melova_audio_read_approved on storage.objects for select to anon,authenticated
  using(bucket_id='melova-audio' and exists (
    select 1 from public.tracks t where t.file_path=name and t.status='approved'
  ));
create policy melova_audio_read_own_admin on storage.objects for select to authenticated
  using(bucket_id='melova-audio' and (
    (storage.foldername(name))[1]=(select auth.uid())::text or
    ((select public.is_admin()) and exists(select 1 from public.tracks t
      where t.file_path=name and t.status in ('pending','approved','rejected')))
  ));
create policy melova_audio_delete_own_admin on storage.objects for delete to authenticated
  using(bucket_id='melova-audio' and (
    ((select public.is_admin()) and exists(select 1 from public.tracks t
      where t.file_path=name and t.status in ('pending','approved','rejected'))) or
    ((storage.foldername(name))[1]=(select auth.uid())::text and not exists(
      select 1 from public.tracks t where t.file_path=name and t.status in ('approved','pending')
    ))
  ));
create policy melova_avatar_insert_own on storage.objects for insert to authenticated
  with check(bucket_id='melova-avatars' and (storage.foldername(name))[1]=(select auth.uid())::text
    and name ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}\.(jpg|jpeg|png|webp)$');
create policy melova_avatar_read on storage.objects for select to anon,authenticated
  using(bucket_id='melova-avatars');
create policy melova_avatar_delete_own on storage.objects for delete to authenticated
  using(bucket_id='melova-avatars' and (storage.foldername(name))[1]=(select auth.uid())::text);

-- IMPORTANT: NEVER put a service_role key into HTML.
-- Promote yourself AFTER registering/signing in once:
-- insert into public.admins(id)
-- select id from auth.users where email = 'YOUR_ACCOUNT_EMAIL' on conflict do nothing;
-- No self-promotion API exists for ordinary users.

-- JoyTube account metadata and user-authored library records.
-- Media files and downloads stay on the user's device. No YouTube account data,
-- credentials, playback state, comments, or provider watch history is stored here.

create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger language plpgsql set search_path = public as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

-- Supabase Auth owns the email and immutable auth user id. This table stores
-- JoyTube's public profile only; email is exposed through the Auth user record.
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique check (char_length(username) between 3 and 32 and username ~ '^[a-zA-Z0-9_]+$'),
  display_name text not null check (char_length(display_name) between 1 and 80),
  avatar_url text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.user_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  settings jsonb not null default '{}'::jsonb,
  theme text not null default 'glass' check (theme in ('glass', 'normal')),
  updated_at timestamptz not null default timezone('utc', now())
);

-- Metadata and pointers for media the user imported into JoyTube. The actual
-- media bytes and downloaded media are never uploaded to Supabase.
create table if not exists public.imported_assets (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 1 and 200),
  media_kind text not null check (media_kind in ('video', 'short', 'music', 'live')),
  -- Do not persist local content:// or file:// URIs; Android grants are device scoped.
  is_favourite boolean not null default false,
  view_count bigint not null default 0 check (view_count >= 0),
  download_count bigint not null default 0 check (download_count >= 0),
  last_viewed_at timestamptz,
  imported_at timestamptz not null default timezone('utc', now()),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.joytube_collections (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 80),
  description text not null default '' check (char_length(description) <= 500),
  visibility text not null default 'private' check (visibility in ('public', 'private')),
  is_favourite boolean not null default false,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.joytube_collection_items (
  id uuid primary key default gen_random_uuid(),
  collection_id uuid not null references public.joytube_collections(id) on delete cascade,
  owner_id uuid not null references auth.users(id) on delete cascade,
  imported_asset_id uuid not null references public.imported_assets(id) on delete cascade,
  position_index integer not null default 0 check (position_index >= 0),
  created_at timestamptz not null default timezone('utc', now()),
  unique (collection_id, imported_asset_id)
);

create table if not exists public.recent_imported_content (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  imported_asset_id uuid not null references public.imported_assets(id) on delete cascade,
  imported_at timestamptz not null default timezone('utc', now()),
  unique (owner_id, imported_asset_id)
);

create or replace function public.record_imported_asset_view(p_asset_id uuid)
returns void language plpgsql security invoker set search_path = public
as $$
begin
  update public.imported_assets
     set view_count = view_count + 1,
         last_viewed_at = timezone('utc', now()),
         updated_at = timezone('utc', now())
   where id = p_asset_id and owner_id = auth.uid();
  if not found then raise exception 'Imported item not found or not owned by caller'; end if;
end;
$$;

create or replace function public.record_imported_asset_download(p_asset_id uuid)
returns void language plpgsql security invoker set search_path = public
as $$
begin
  update public.imported_assets
     set download_count = download_count + 1,
         updated_at = timezone('utc', now())
   where id = p_asset_id and owner_id = auth.uid();
  if not found then raise exception 'Imported item not found or not owned by caller'; end if;
end;
$$;

create index if not exists idx_collections_owner on public.joytube_collections(owner_id);
create index if not exists idx_assets_owner on public.imported_assets(owner_id);
create index if not exists idx_recent_imports_owner on public.recent_imported_content(owner_id, imported_at desc);
create index if not exists idx_collection_items_owner on public.joytube_collection_items(owner_id);
create index if not exists idx_collection_items_asset on public.joytube_collection_items(imported_asset_id);
create index if not exists idx_recent_imports_asset on public.recent_imported_content(imported_asset_id);

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at before update on public.profiles
for each row execute function public.set_updated_at();
drop trigger if exists trg_imported_assets_updated_at on public.imported_assets;
create trigger trg_imported_assets_updated_at before update on public.imported_assets
for each row execute function public.set_updated_at();
drop trigger if exists trg_collections_updated_at on public.joytube_collections;
create trigger trg_collections_updated_at before update on public.joytube_collections
for each row execute function public.set_updated_at();

alter table public.profiles enable row level security;
alter table public.user_preferences enable row level security;
alter table public.imported_assets enable row level security;
alter table public.joytube_collections enable row level security;
alter table public.joytube_collection_items enable row level security;
alter table public.recent_imported_content enable row level security;

-- Profiles and settings are private to their account.
create policy "profiles_select_own" on public.profiles for select to authenticated using ((select auth.uid()) = id);
create policy "profiles_insert_own" on public.profiles for insert to authenticated with check ((select auth.uid()) = id);
create policy "profiles_update_own" on public.profiles for update to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
create policy "profiles_delete_own" on public.profiles for delete to authenticated using ((select auth.uid()) = id);

create policy "preferences_select_own" on public.user_preferences for select to authenticated using ((select auth.uid()) = user_id);
create policy "preferences_insert_own" on public.user_preferences for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "preferences_update_own" on public.user_preferences for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "preferences_delete_own" on public.user_preferences for delete to authenticated using ((select auth.uid()) = user_id);

-- Public collection metadata and items can be read by anyone. Private data is
-- visible only to its owner; inserts and writes always remain owner-only.
create policy "assets_select_owner_or_public_collection" on public.imported_assets for select to anon, authenticated
  using ((select auth.uid()) = owner_id or exists (
    select 1 from public.joytube_collection_items ci
    join public.joytube_collections c on c.id = ci.collection_id
    where ci.imported_asset_id = imported_assets.id and c.visibility = 'public'
  ));
create policy "assets_insert_owner" on public.imported_assets for insert to authenticated with check ((select auth.uid()) = owner_id);
create policy "assets_update_owner" on public.imported_assets for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id);
create policy "assets_delete_owner" on public.imported_assets for delete to authenticated using ((select auth.uid()) = owner_id);

create policy "collections_select_owner_or_public" on public.joytube_collections for select to anon, authenticated using ((select auth.uid()) = owner_id or visibility = 'public');
create policy "collections_insert_owner" on public.joytube_collections for insert to authenticated with check ((select auth.uid()) = owner_id);
create policy "collections_update_owner" on public.joytube_collections for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id);
create policy "collections_delete_owner" on public.joytube_collections for delete to authenticated using ((select auth.uid()) = owner_id);

create policy "collection_items_select_owner_or_public" on public.joytube_collection_items for select
  to anon, authenticated using ((select auth.uid()) = owner_id or exists (
    select 1 from public.joytube_collections c
    where c.id = collection_id and c.visibility = 'public'
  ));
create policy "collection_items_insert_owner" on public.joytube_collection_items for insert to authenticated
  with check ((select auth.uid()) = owner_id and exists (
    select 1 from public.joytube_collections c where c.id = collection_id and c.owner_id = (select auth.uid())
  ) and exists (
    select 1 from public.imported_assets a where a.id = imported_asset_id and a.owner_id = (select auth.uid())
  ));
create policy "collection_items_update_owner" on public.joytube_collection_items for update to authenticated
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id and exists (
    select 1 from public.joytube_collections c where c.id = collection_id and c.owner_id = (select auth.uid())
  ) and exists (
    select 1 from public.imported_assets a where a.id = imported_asset_id and a.owner_id = (select auth.uid())
  ));
create policy "collection_items_delete_owner" on public.joytube_collection_items for delete to authenticated using ((select auth.uid()) = owner_id);

create policy "recent_imports_select_own" on public.recent_imported_content for select to authenticated using ((select auth.uid()) = owner_id);
create policy "recent_imports_insert_own" on public.recent_imported_content for insert to authenticated with check ((select auth.uid()) = owner_id and exists (
  select 1 from public.imported_assets a where a.id = imported_asset_id and a.owner_id = (select auth.uid())
));
create policy "recent_imports_update_own" on public.recent_imported_content for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id);
create policy "recent_imports_delete_own" on public.recent_imported_content for delete to authenticated using ((select auth.uid()) = owner_id);

grant select on public.joytube_collections, public.joytube_collection_items, public.imported_assets to anon, authenticated;
grant select, insert, update, delete on public.profiles, public.user_preferences, public.imported_assets,
  public.joytube_collections, public.joytube_collection_items, public.recent_imported_content to authenticated;
revoke all on function public.record_imported_asset_view(uuid) from public, anon;
revoke all on function public.record_imported_asset_download(uuid) from public, anon;
grant execute on function public.record_imported_asset_view(uuid) to authenticated;
grant execute on function public.record_imported_asset_download(uuid) to authenticated;

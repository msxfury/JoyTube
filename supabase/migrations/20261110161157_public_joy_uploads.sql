-- Public Joy uploads are files explicitly selected and published by their owners.

alter table public.imported_assets
  add column if not exists visibility text not null default 'private'
    check (visibility in ('public', 'private')),
  add column if not exists description text not null default ''
    check (char_length(description) <= 500),
  add column if not exists creator_username text not null default '',
  add column if not exists storage_path text;

drop policy if exists "assets_select_owner_or_public_collection" on public.imported_assets;
create policy "assets_select_owner_or_public_or_collection" on public.imported_assets
  for select to anon, authenticated
  using (
    (select auth.uid()) = owner_id
    or (visibility = 'public' and storage_path is not null)
    or exists (
      select 1 from public.joytube_collection_items ci
      join public.joytube_collections c on c.id = ci.collection_id
      where ci.imported_asset_id = imported_assets.id and c.visibility = 'public'
    )
  );

-- Public means public downloads. Upload and delete remain owner-authenticated.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'joytube-public-media',
  'joytube-public-media',
  true,
  52428800,
  array['video/mp4', 'video/webm', 'video/x-matroska', 'audio/mpeg', 'audio/mp4', 'audio/wav', 'audio/x-wav']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "joytube_public_media_insert_owner" on storage.objects;
create policy "joytube_public_media_insert_owner" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'joytube-public-media'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

drop policy if exists "joytube_public_media_delete_owner" on storage.objects;
create policy "joytube_public_media_delete_owner" on storage.objects
  for delete to authenticated
  using (
    bucket_id = 'joytube-public-media'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

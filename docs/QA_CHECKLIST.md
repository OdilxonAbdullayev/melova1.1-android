# MELOVA Android QA Checklist

## Build
- [x] Project structure and main files generated
- [x] 3 embedded demo MP3 are valid audio, ffprobe durations ~45, ~48, ~50s
- [x] Static structure, permissions, config sanity checks pass
- [ ] GitHub Actions assembleDebug passes
- [ ] Android device test across API 26, 33, 35

## Playback
- [ ] Demo plays with no Supabase
- [ ] Audio continues when screen is locked
- [ ] Pause/resume from headset/media notification
- [ ] Queue reorder/remove, shuffle/repeat, seek and speed
- [ ] Invalid/missing signed audio URL surfaces understandable message
- [ ] 1-hour signed URL expiration on long queues, recovery strategy

## Data and security
- [ ] Public/anon key configured, never service_role
- [ ] Existing Supabase RLS policies remain untouched
- [ ] Email verification/login/logout and session refresh
- [ ] Google OAuth redirect URI / PKCE on Android
- [ ] Password recovery email callback and password update
- [ ] Favorites/playlist CRUD and multi-device sync
- [ ] Audio upload 20MB limit, upload errors, moderation RLS
- [ ] Reports and admin moderation only visible to authorized admins
- [ ] Test on slow network, airplane mode, invalid server URL

## Scale/performance
- [x] Search, library and favorites implemented with lazy list rendering
- [ ] Monitor network response for 350-track fetch limit and add paging
- [ ] Profile with 10k+ tracks using backend query paging and caching
- [ ] Test memory usage for 20MB uploads on low-RAM phones

## Note
This file is a transparent checklist, not a guarantee of zero bugs.

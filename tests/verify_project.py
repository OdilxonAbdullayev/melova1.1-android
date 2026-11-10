#!/usr/bin/env python3
"""Safe static checks: does not pretend to replace Android compilation or device QA."""
from pathlib import Path
import hashlib
import re
import json

root=Path(__file__).resolve().parents[1]
required=[
 'build.gradle.kts','settings.gradle.kts','app/build.gradle.kts',
 'app/src/main/AndroidManifest.xml',
 'app/src/main/java/uz/melova/app/MainActivity.kt',
 'app/src/main/java/uz/melova/app/playback/MelovaPlaybackService.kt',
 'app/src/main/java/uz/melova/app/data/MelovaApi.kt',
 'app/src/main/java/uz/melova/app/ui/MelovaApp.kt',
 '.github/workflows/android-apk.yml',
]
for n in required:
    p=root/n
    assert p.exists() and p.stat().st_size>50,n
for i in range(1,4):
    p=root/f'app/src/main/res/raw/demo_{i}.mp3'
    assert p.is_file() and p.stat().st_size>100000,p
    assert p.read_bytes()[:3]==b'ID3',p
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
assert 'FOREGROUND_SERVICE_MEDIA_PLAYBACK' in manifest
assert 'uzmelova' in manifest
assert 'MediaSessionService' in manifest
build=(root/'app/build.gradle.kts').read_text()
assert 'compileSdk = 36' in build
assert 'compose-bom:2025.08.00' in build
assert 'media3-session:1.11.1' in build
api=(root/'app/src/main/java/uz/melova/app/data/MelovaApi.kt').read_text()
for v in ['melova-audio','profiles','tracks','playlists','playlist_tracks','listening_history','reports','favorites','auth/v1','storage/v1']:
    assert v in api, v
assert 'sb_secret_' in api and 'service_role' in api
assert 'setFixedLengthStreamingMode' in api
assert '20*1024*1024' in api
# Delegated Compose State in the ViewModel requires these Kotlin operator extensions.
vm=(root/'app/src/main/java/uz/melova/app/model/MelovaViewModel.kt').read_text()
assert 'import androidx.compose.runtime.getValue' in vm
assert 'import androidx.compose.runtime.setValue' in vm
print('MELOVA static smoke checks: PASSED')

# Shared-project APK behavior: users must not have to provide their own server.
assert 'val preconfigured:Boolean' in api
assert 'if(preconfigured) bundledUrl' in api
assert 'check(!preconfigured)' in api
assert 'active_project_url' in api
profile=(root/'app/src/main/java/uz/melova/app/ui/AccountAndUpload.kt').read_text()
assert 'if (!vm.api.preconfigured) ProfileAction("Supabase ulanishi"' in profile
wf=(root/'.github/workflows/android-apk.yml').read_text()
assert 'MELOVA_SUPABASE_URL: ${{ secrets.MELOVA_SUPABASE_URL }}' in wf
assert 'MELOVA_SUPABASE_PUBLISHABLE_KEY: ${{ secrets.MELOVA_SUPABASE_PUBLISHABLE_KEY }}' in wf
assert 'python3 tests/verify_supabase_settings.py' in wf

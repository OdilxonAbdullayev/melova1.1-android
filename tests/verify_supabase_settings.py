#!/usr/bin/env python3
"""Fail early unless a non-secret Supabase public app configuration is supplied.

The APK exposes these fields publicly. Security requires server-side Supabase RLS.
Never print API keys to the GitHub Actions log.
"""
import base64
import json
import os
import re
import sys
from urllib.parse import urlparse


def validate(url: str, key: str) -> list[str]:
    errors = []
    url = url.strip().rstrip("/")
    key = key.strip()
    host = urlparse(url)
    if not re.fullmatch(r"https://[a-zA-Z0-9-]+\.supabase\.co", url) or host.port:
        errors.append("MELOVA_SUPABASE_URL: https://<project-ref>.supabase.co formatida kiriting.")
    if not key:
        errors.append("MELOVA_SUPABASE_PUBLISHABLE_KEY: Supabase publishable yoki anon key kerak.")
    elif key.startswith("sb_secret_") or "service_role" in key.lower():
        errors.append("Maxfiy service_role/sb_secret_ kalitini APK'ga qo'shish mumkin emas.")
    elif key.startswith("sb_publishable_"):
        if not re.fullmatch(r"sb_publishable_[A-Za-z0-9_-]+", key):
            errors.append("Publishable key formati noto'g'ri.")
    elif key.count(".") == 2:
        try:
            middle = key.split(".")[1]
            payload = json.loads(base64.urlsafe_b64decode(middle + "=" * (-len(middle) % 4)))
            if payload.get("role") != "anon":
                errors.append("JWT keyning roli 'anon' emas. Faqat anon key kerak.")
        except (ValueError, TypeError, UnicodeDecodeError):
            errors.append("Anon JWT keyni tekshirib bo'lmadi.")
    else:
        errors.append("Publishable (sb_publishable_) yoki legacy anon JWT keyni kiriting.")
    return errors

if __name__ == "__main__":
    errors = validate(os.environ.get("MELOVA_SUPABASE_URL", ""), os.environ.get("MELOVA_SUPABASE_PUBLISHABLE_KEY", ""))
    if errors:
        print("MELOVA avtomatik ulanishni sozlash kerak. GitHub → Settings → Secrets and variables → Actions:")
        for error in errors:
            print("- " + error)
        sys.exit(1)
    print("MELOVA: umumiy Supabase sozlamalari to'g'ri ko'rinishda. Keylar logga chiqarilmadi.")

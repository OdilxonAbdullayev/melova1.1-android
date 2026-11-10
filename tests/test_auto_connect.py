import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("verify_supabase_settings", ROOT / "tests" / "verify_supabase_settings.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class SupabaseConfigurationTests(unittest.TestCase):
    def test_valid_publishable_key(self):
        self.assertEqual([], module.validate("https://abc123.supabase.co/", "sb_publishable_123456789012345678901"))
    def test_reject_secret(self):
        self.assertTrue(module.validate("https://abc123.supabase.co", "sb_secret_123456789012345678901"))
    def test_reject_empty_key(self):
        self.assertTrue(module.validate("https://abc123.supabase.co", ""))
    def test_reject_insecure_url(self):
        self.assertTrue(module.validate("http://abc123.supabase.co", "sb_publishable_123456789012345678901"))
    def test_reject_wrong_host(self):
        self.assertTrue(module.validate("https://danger.example.com", "sb_publishable_123456789012345678901"))
    def test_reject_bad_jwt(self):
        self.assertTrue(module.validate("https://abc123.supabase.co", "not-a-key"))

if __name__ == "__main__": unittest.main()

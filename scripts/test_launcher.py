#!/usr/bin/env python3
"""Pruebas del lanzador. Ninguna inicia Docker ni escribe backend/.env real."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parent / "omarchy-launcher.py"
spec = importlib.util.spec_from_file_location("springfield_launcher", SCRIPT)
launcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(launcher)


class LauncherTest(unittest.TestCase):
    def write(self, text):
        temp = tempfile.TemporaryDirectory()
        target = Path(temp.name) / ".env"
        target.write_text(text)
        self.addCleanup(temp.cleanup)
        return target

    def good(self):
        return ("POSTGRES_USER=springfield_local\nPOSTGRES_PASSWORD=" + "p" * 25
                + "\nPOSTGRES_DB=springfield_archive\nSPRINGFIELD_ADMIN_TOKEN=" + "t" * 40 + "\n")

    def test_env_loader_reads_plain_text(self):
        cfg = launcher.load_env(self.write(self.good()))
        self.assertEqual(cfg["POSTGRES_DB"], "springfield_archive")
        self.assertEqual(cfg["SPRINGFIELD_ADMIN_TOKEN"], "t" * 40)

    def test_env_is_never_evaluated_as_shell(self):
        with self.assertRaises(launcher.LauncherError):
            launcher.load_env(self.write(self.good().replace("springfield_archive", "$(touch /tmp/unsafe)")))

    def test_rejects_short_admin_token(self):
        with self.assertRaises(launcher.LauncherError):
            launcher.load_env(self.write(self.good().replace("t" * 40, "weak")))

    def test_rejects_short_postgres_secret(self):
        with self.assertRaises(launcher.LauncherError):
            launcher.load_env(self.write(self.good().replace("p" * 25, "1234")))

    def test_rejects_missing_keys(self):
        with self.assertRaises(launcher.LauncherError):
            launcher.load_env(self.write("POSTGRES_USER=a\n"))

    def test_setup_creates_private_config_only_when_absent(self):
        import unittest.mock
        import os
        with tempfile.TemporaryDirectory() as folder:
            config = Path(folder) / ".env"
            with unittest.mock.patch.object(launcher, "ENV_FILE", config):
                launcher.setup()
                self.assertEqual(config.stat().st_mode & 0o777, 0o600)
                generated = config.read_text()
                self.assertIn("SPRINGFIELD_ADMIN_TOKEN=", generated)
                launcher.setup()
                self.assertEqual(config.read_text(), generated)

    def test_java_environment_uses_explicit_local_profile(self):
        cfg = launcher.load_env(self.write(self.good()))
        env = launcher.java_environment(cfg)
        self.assertEqual(env["SPRING_PROFILES_ACTIVE"], "local")
        self.assertTrue(env["SPRING_DATASOURCE_URL"].endswith("/springfield_archive"))


if __name__ == "__main__":
    unittest.main(verbosity=2)

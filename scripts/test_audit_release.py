from pathlib import Path
import tempfile
import unittest
from audit_release import inspect, REQUIRED

class ReleaseAuditTest(unittest.TestCase):
    def setup_root(self,path):
        for name in REQUIRED:
            file=path/name
            file.parent.mkdir(parents=True,exist_ok=True)
            file.write_text('Contenido de prueba, sin secretos.',encoding='utf8')

    def test_safe_source(self):
        with tempfile.TemporaryDirectory() as base:
            root=Path(base)
            self.setup_root(root)
            self.assertEqual(inspect(root,package=True),[])

    def test_rejects_real_env_and_artifacts(self):
        with tempfile.TemporaryDirectory() as base:
            root=Path(base)
            self.setup_root(root)
            (root/'backend/.env').write_text('TOKEN=[oculto]')
            (root/'target').mkdir()
            (root/'target/test.class').write_bytes(b'\x00')
            errors=inspect(root,package=True)
            self.assertTrue(any('secreto local' in e for e in errors))
            self.assertTrue(any('binario' in e or 'desarrollo' in e for e in errors))

    def test_detects_secret_signature_without_exposing_value(self):
        with tempfile.TemporaryDirectory() as base:
            root=Path(base)
            self.setup_root(root)
            secret='ghp_'+'A'*36
            (root/'frontend/leak.js').write_text('const token="'+secret+'";',encoding='utf8')
            errors=inspect(root)
            self.assertTrue(any('GitHub credential' in e for e in errors))
            self.assertTrue(all(secret not in e for e in errors))

if __name__=='__main__':unittest.main()

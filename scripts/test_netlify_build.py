"""No network access or production files modified: run Netlify build in a temp tree."""
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

SOURCE=Path(__file__).resolve().parent/'netlify-build.mjs'

@unittest.skipUnless(shutil.which('node'), 'Node is required for Netlify build tests')
class NetlifyBuildTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        (self.root/'scripts').mkdir()
        (self.root/'frontend/js').mkdir(parents=True)
        (self.root/'scripts/netlify-build.mjs').write_bytes(SOURCE.read_bytes())

    def build(self,origin=None):
        env=os.environ.copy();env.pop('SPRINGFIELD_BACKEND_ORIGIN',None)
        if origin is not None:env['SPRINGFIELD_BACKEND_ORIGIN']=origin
        return subprocess.run(['node','scripts/netlify-build.mjs'],cwd=self.root,env=env,
                              text=True,capture_output=True,check=False)

    def test_rejects_missing_config(self):
        result=self.build()
        self.assertNotEqual(result.returncode,0)
        self.assertFalse((self.root/'frontend/_headers').exists())
        self.assertFalse((self.root/'frontend/js/deployment-config.js').exists())

    def test_rejects_unsafe_urls(self):
        for invalid in ['http://api.example.com','https://localhost',
                        'https://api.example.com/path', 'https://user:pass@api.example.com',
                        'https://api.example.com:8443', 'https://api.example.com?token=secret',
                        'https://192.168.1.20', 'https://[::1]', 'https://api.internal',
                        'https://api.example.com/#fragment']:
            with self.subTest(origin=invalid):
                self.assertNotEqual(self.build(invalid).returncode,0)
        self.assertFalse((self.root/'frontend/_headers').exists())

    def test_success_generates_consistent_origin_and_csp(self):
        value='https://springfield-api.example.com'
        self.assertEqual(self.build(value).returncode,0)
        config=(self.root/'frontend/js/deployment-config.js').read_text()
        headers=(self.root/'frontend/_headers').read_text()
        self.assertIn(value,config)
        self.assertIn("connect-src 'self' "+value,headers)
        self.assertIn('img-src',headers)
        self.assertIn('media-src \'self\'',headers)
        self.assertNotIn('X-Admin-Token',config+headers)

if __name__=='__main__':unittest.main()

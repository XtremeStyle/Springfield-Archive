"""Pruebas locales del verificador de API; sin Internet ni PostgreSQL."""
import importlib.util
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import threading
import unittest

spec = importlib.util.spec_from_file_location('verify_apis', Path(__file__).with_name('verify-apis.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Handler(BaseHTTPRequestHandler):
    broken = False
    def log_message(self, *args):
        pass

    def do_GET(self):
        path = self.path.split('?')[0]
        if self.broken and path.endswith('/characters'):
            body = {'count': 2, 'pages': 1, 'results': 'INVALID'}
        elif path.startswith('/api/') and not path.startswith('/api/v1/'):
            parts = path.split('/')
            body = {'id': 1, 'name': 'Homer Simpson'} if len(parts) == 4 else {
                'count': 1, 'pages': 1, 'next': None, 'prev': None,
                'results': [{'id': 1, 'name': 'Homer Simpson'}]}
        elif path == '/actuator/health':
            body = {'status': 'UP'}
        elif path == '/api/v1/stats':
            body = {k: {'total': 1} for k in module.KINDS}
        elif path == '/api/v1/sync/status':
            body = [{'resource': k, 'status': 'completed'} for k in module.KINDS]
        elif path.startswith('/api/v1/'):
            body = {'slug': 'homer-simpson', 'name': 'Homer Simpson'} if path.count('/') >= 4 else {
                'content': [{'slug': 'homer-simpson', 'name': 'Homer Simpson'}], 'totalElements': 1,
                'size': 3, 'page': 0, 'totalPages': 1}
        else:
            self.send_error(404)
            return
        blob = json.dumps(body).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(blob)))
        self.end_headers()
        self.wfile.write(blob)


class VerifyApisTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f'http://127.0.0.1:{cls.server.server_port}'

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)

    def test_upstream_validates_pages_and_details(self):
        self.assertEqual(module.smoke_upstream(self.url, 2), 6)

    def test_local_validates_stats_and_all_catalogs(self):
        self.assertEqual(module.smoke_local(self.url, 2, True), 9)

    def test_malformed_external_page_is_rejected(self):
        Handler.broken = True
        try:
            with self.assertRaises(module.VerificationError):
                module.smoke_upstream(self.url, 2)
        finally:
            Handler.broken = False

    def test_unsynced_state_is_rejected_when_required(self):
        orig = module.request_json
        def wrapped(url, timeout):
            value = orig(url, timeout)
            if url.endswith('/sync/status'):
                value[0]['status'] = 'running'
            return value
        module.request_json = wrapped
        try:
            with self.assertRaises(module.VerificationError):
                module.smoke_local(self.url, 2, True)
        finally:
            module.request_json = orig

if __name__ == '__main__':
    unittest.main()

"""Pruebas sin red del verificador de catalogos."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

SPEC=importlib.util.spec_from_file_location('diag',Path(__file__).parent/'diagnostico_catalogos.py')
diag=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(diag)


class TestCatalogDiagnosis(unittest.TestCase):
    def test_normaliza_nombres_sin_confundir_diacriticos(self):
        self.assertEqual(diag.canonical(' El  Sr.  BÚRNS! '), 'elsrburns')
        self.assertEqual(diag.canonical('Moe’s Tavern'), 'moestavern')

    def test_detecta_si_el_backend_repite_un_nombre_ya_normalizado(self):
        def fake_page(base,entity,source,page):
            return {'content': [
                {'name':'Homer Simpson'},
                {'name':'HOMER SIMPSON!'},
            ], 'totalPages':1,'totalElements':2}
        with patch.object(diag,'get_page',side_effect=fake_page):
            self.assertFalse(diag.inspect('http://127.0.0.1:8080','characters',True))


if __name__=='__main__':unittest.main()

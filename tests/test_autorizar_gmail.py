import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from urllib.parse import parse_qs, urlsplit
from http.server import HTTPServer
import http.client
import threading
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("autorizar_gmail", Path(__file__).parents[1] / "scripts" / "autorizar_gmail.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class GmailSetupTest(unittest.TestCase):
    def test_pkce_state_e_permissoes_minimas(self):
        self.assertEqual(module.pkce_challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"), "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
        url = module.authorization_url("client-teste", "espectrocare@gmail.com", "state-teste", "verifier-teste")
        query = parse_qs(urlsplit(url).query)
        self.assertEqual(query["state"], ["state-teste"])
        self.assertEqual(query["access_type"], ["offline"])
        self.assertEqual(query["code_challenge_method"], ["S256"])
        self.assertNotIn("gmail.readonly", query["scope"][0])
        self.assertIn(module.GMAIL_SCOPE, query["scope"][0].split())
        self.assertEqual(query["redirect_uri"], [module.CALLBACK])

    def test_verifica_conta_antes_de_salvar(self):
        calls = []
        def request(url, data=None, token=None):
            calls.append((url, data, token))
            if url == module.TOKEN_URL:
                return {"access_token": "access-teste", "refresh_token": "refresh-teste", "scope": module.GMAIL_SCOPE}
            return {"email": "espectrocare@gmail.com", "email_verified": True}
        values = module.obtain_credentials("client", "secret", "code", "verifier", "espectrocare@gmail.com", request)
        self.assertEqual(values["GMAIL_REFRESH_TOKEN"], "refresh-teste")
        self.assertEqual(calls[0][1]["code_verifier"], "verifier")
        self.assertEqual(calls[1][2], "access-teste")
        self.assertEqual(values["EMAIL_PROVIDER"], "gmail")

    def test_conta_errada_ou_nao_verificada_falha(self):
        for identity in [{"email": "outra@teste.invalid", "email_verified": True}, {"email": "espectrocare@gmail.com", "email_verified": False}]:
            def request(url, **kwargs):
                return {"access_token": "access", "refresh_token": "refresh", "scope": module.GMAIL_SCOPE} if url == module.TOKEN_URL else identity
            with self.assertRaises(module.ConfigError):
                module.obtain_credentials("id", "secret", "code", "verifier", "espectrocare@gmail.com", request)

    def test_acesso_offline_ou_envio_nao_concedido_falha(self):
        for result in [{"access_token": "access", "scope": module.GMAIL_SCOPE}, {"access_token": "access", "refresh_token": "refresh", "scope": "openid email"}]:
            with self.assertRaises(module.ConfigError):
                module.obtain_credentials("id", "secret", "code", "verifier", "espectrocare@gmail.com", lambda *a, **k: result)

    def test_arquivo_privado_e_nao_sobrescreve(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "privado" / "gmail-render.env"
            module.save_credentials(output, {"EMAIL_PROVIDER": "gmail", "GMAIL_REFRESH_TOKEN": "refresh-teste"})
            original = output.read_bytes()
            if os.name != "nt": self.assertEqual(output.stat().st_mode & 0o777, 0o600)
            with self.assertRaises(module.ConfigError): module.save_credentials(output, {"GMAIL_REFRESH_TOKEN": "outro"})
            self.assertEqual(output.read_bytes(), original)

    def test_credencial_com_quebra_de_linha_nao_e_salva(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "gmail-render.env"
            with self.assertRaises(module.ConfigError): module.save_credentials(output, {"GMAIL_REFRESH_TOKEN": "refresh\nOUTRO=valor"})
            self.assertFalse(output.exists())

    def test_cliente_web_exige_callback_correto(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "client.json"
            path.write_text(json.dumps({"web": {"client_id": "id", "client_secret": "secret", "redirect_uris": [module.CALLBACK]}}))
            self.assertEqual(module.read_client(path), ("id", "secret"))
            path.write_text(json.dumps({"web": {"client_id": "id", "client_secret": "secret", "redirect_uris": []}}))
            with self.assertRaises(module.ConfigError): module.read_client(path)

    def test_callback_rejeita_state_errado_sem_registrar_codigo(self):
        result = {}
        with HTTPServer(("127.0.0.1", 0), module.make_handler("state-correto", result)) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
            try:
                conn = http.client.HTTPConnection("127.0.0.1", server.server_port)
                conn.request("GET", "/oauth2/callback?state=errado&code=segredo", headers={"Host": "127.0.0.1:8765"})
                response = conn.getresponse()
                self.assertEqual(response.status, 400)
                self.assertNotIn(b"segredo", response.read())
                self.assertEqual(result, {})
                conn.close()
                conn = http.client.HTTPConnection("127.0.0.1", server.server_port)
                conn.request("GET", "/oauth2/callback?state=state-correto&code=segredo", headers={"Host": "127.0.0.1:8765"})
                response = conn.getresponse()
                self.assertEqual(response.status, 200)
                self.assertEqual(response.getheader("Cache-Control"), "no-store")
                self.assertNotIn(b"segredo", response.read())
                self.assertEqual(result, {"code": "segredo"})
                conn.close()
            finally:
                server.shutdown(); thread.join(timeout=5)


if __name__ == "__main__":
    unittest.main()

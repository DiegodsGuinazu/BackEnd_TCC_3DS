#!/usr/bin/env python3
"""Autoriza a conta remetente localmente. Não envia e-mails e não imprime segredos."""
import argparse
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer

CALLBACK = "http://127.0.0.1:8765/oauth2/callback"
TOKEN_URL = "https://oauth2.googleapis.com/token"
USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo"
GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.send"
SCOPES = GMAIL_SCOPE + " openid email"


class ConfigError(Exception):
    """Mensagem segura para o operador; nunca incluir respostas brutas do Google."""


def pkce_challenge(verifier):
    return base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii")).digest()).rstrip(b"=").decode("ascii")


def authorization_url(client_id, email, state, verifier):
    return "https://accounts.google.com/o/oauth2/v2/auth?" + urllib.parse.urlencode({
        "client_id": client_id, "redirect_uri": CALLBACK, "response_type": "code",
        "scope": SCOPES, "access_type": "offline", "prompt": "consent", "login_hint": email,
        "state": state, "code_challenge": pkce_challenge(verifier), "code_challenge_method": "S256",
    })


def read_client(path):
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
        client = payload["web"]
        client_id, client_secret = client["client_id"], client["client_secret"]
        if not all(isinstance(v, str) and v and "\r" not in v and "\n" not in v for v in [client_id, client_secret]):
            raise ValueError()
        if CALLBACK not in client.get("redirect_uris", []):
            raise ConfigError("Cadastre exatamente " + CALLBACK + " no cliente Web do Google e baixe o JSON atualizado.")
        return client_id, client_secret
    except (OSError, ValueError, KeyError, TypeError):
        raise ConfigError("Não foi possível ler as credenciais. Use o JSON de um cliente OAuth do tipo Aplicativo da Web.") from None


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def request_json(url, data=None, token=None):
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        data = urllib.parse.urlencode(data).encode("ascii")
    if token is not None:
        if not isinstance(token, str) or not token or any(c.isspace() or ord(c) < 32 for c in token):
            raise ConfigError("Resposta de autorização inválida.")
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(url, data=data, headers=headers)
    try:
        # Nenhum redirecionamento recebe credenciais ou tokens.
        with urllib.request.build_opener(NoRedirect()).open(request, timeout=20) as response:
            result = json.loads(response.read(65537))
            if not isinstance(result, dict):
                raise ValueError()
            return result
    except urllib.error.HTTPError as error:
        raise ConfigError("Google recusou a solicitação (HTTP " + str(error.code) + "). Confira o projeto, escopos e autorização.") from None
    except (urllib.error.URLError, OSError, ValueError):
        raise ConfigError("Não foi possível concluir a comunicação segura com o Google.") from None


def obtain_credentials(client_id, client_secret, code, verifier, expected_email, request=request_json):
    result = request(TOKEN_URL, data={"client_id": client_id, "client_secret": client_secret,
        "code": code, "code_verifier": verifier, "redirect_uri": CALLBACK, "grant_type": "authorization_code"})
    access = result.get("access_token")
    refresh = result.get("refresh_token")
    if not access or not refresh or GMAIL_SCOPE not in str(result.get("scope", "")).split():
        raise ConfigError("O Google não concedeu envio e acesso offline. Confira a permissão e autorize novamente.")
    identity = request(USERINFO_URL, token=access)
    if identity.get("email_verified") is not True or str(identity.get("email", "")).lower() != expected_email.lower():
        raise ConfigError("A conta autorizada não corresponde à conta remetente esperada. Nada foi salvo.")
    return {"EMAIL_PROVIDER": "gmail", "GMAIL_CLIENT_ID": client_id,
            "GMAIL_CLIENT_SECRET": client_secret, "GMAIL_REFRESH_TOKEN": refresh,
            "GMAIL_SENDER": expected_email.lower()}


def save_credentials(output, values):
    if not all(isinstance(v, str) and v and "\r" not in v and "\n" not in v for v in values.values()):
        raise ConfigError("Credenciais inválidas. Nada foi salvo.")
    output.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    # Arquivo exclusivo: não sobrescreve nem segue um link já existente.
    try:
        fd = os.open(output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
            handle.write("\n".join(key + "=" + value for key, value in values.items()) + "\n")
    except OSError:
        raise ConfigError("Não foi possível criar o arquivo privado. Escolha um caminho novo em sua pasta pessoal.") from None


def make_handler(expected_state, result):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass  # O callback contém código OAuth na URL; nunca registrar acessos.

        def do_GET(self):
            parsed = urllib.parse.urlsplit(self.path)
            params = urllib.parse.parse_qs(parsed.query)
            state = params.get("state", [""])
            valid_state = len(state) == 1 and hmac.compare_digest(state[0].encode(), expected_state.encode())
            valid_host = self.headers.get("Host") == "127.0.0.1:8765"
            success = False
            if parsed.path == "/oauth2/callback" and valid_host and valid_state:
                code = params.get("code", [])
                if "error" in params:
                    result["error"] = True
                elif len(code) == 1 and code[0] and len(code[0]) <= 8192 and not result:
                    result["code"] = code[0]
                    success = True
            message = "Autorização recebida. Volte ao terminal para concluir." if success else "Solicitação não aceita. Confira o terminal ou tente novamente."
            body = ("<!doctype html><html lang='pt-BR'><meta charset='utf-8'><title>EspectroCare</title><p>" + message + "</p></html>").encode("utf-8")
            self.send_response(200 if success else 400)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Referrer-Policy", "no-referrer")
            self.send_header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(body)
    return Handler


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--client-json", required=True, type=Path, help="JSON privado baixado do cliente OAuth Web do Google")
    parser.add_argument("--email", default="espectrocare@gmail.com", help="Conta remetente que será autorizada")
    parser.add_argument("--output", type=Path, default=Path.home() / "EspectroCare-segredos" / "gmail-render.env")
    args = parser.parse_args(argv)
    try:
        email = args.email.strip().lower()
        if not email or "@" not in email or any(c.isspace() or ord(c) < 32 for c in email):
            raise ConfigError("Informe um endereço de e-mail válido.")
        output = args.output.expanduser().resolve()
        repo = Path(__file__).resolve().parents[1]
        if output.is_relative_to(repo):
            raise ConfigError("Salve as credenciais fora da pasta do repositório, em sua pasta pessoal.")
        if output.exists():
            raise ConfigError("O arquivo de saída já existe. Use --output com um nome novo; nada será sobrescrito.")
        client_id, client_secret = read_client(args.client_json.expanduser())
        state, verifier = secrets.token_urlsafe(32), secrets.token_urlsafe(64)
        result = {}
        # Somente loopback, sem endpoint no Render nem no banco.
        with HTTPServer(("127.0.0.1", 8765), make_handler(state, result)) as server:
            server.timeout = 1
            print("Abrindo o navegador para autorizar a conta remetente. Este processo não envia e-mails.")
            if not webbrowser.open(authorization_url(client_id, email, state, verifier)):
                raise ConfigError("Não foi possível abrir o navegador. Execute em seu computador com navegador disponível.")
            deadline = time.monotonic() + 300
            while not result and time.monotonic() < deadline:
                server.handle_request()
        if "error" in result:
            raise ConfigError("A autorização foi recusada ou cancelada. Nada foi salvo.")
        if "code" not in result:
            raise ConfigError("O prazo de autorização terminou. Execute novamente para tentar outra vez.")
        values = obtain_credentials(client_id, client_secret, result["code"], verifier, email)
        save_credentials(output, values)
        print("Credenciais salvas em arquivo privado: " + str(output))
        print("Copie os valores somente para Environment do backend no Render. Não envie o arquivo ao chat nem ao Git.")
        return 0
    except ConfigError as error:
        print(str(error), file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("Autorização cancelada. Nenhum segredo foi exibido.", file=sys.stderr)
        return 1
    except Exception:
        print("Não foi possível concluir. Confira a porta local 8765, os caminhos e a configuração do Google.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

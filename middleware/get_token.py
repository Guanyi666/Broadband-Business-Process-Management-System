#!/usr/bin/env python3
"""Fetch a BBPMS access token via the real RSA login flow (uses openssl for RSA).

Writes the access token to stdout. Pure stdlib + openssl (no cryptography lib).
Usage: python3 get_token.py [username] [password]   (default admin / 123456)
"""
import sys, os, base64, json, subprocess, tempfile, urllib.request

BASE = "http://localhost:8080"
OPENSSL = "openssl"

def http_json(path, method="GET", body=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode())

def rsa_encrypt_base64(pub_b64, plaintext):
    d = tempfile.mkdtemp()
    der = os.path.join(d, "pub.der")
    enc = os.path.join(d, "pw.enc")
    pw  = os.path.join(d, "pw.txt")
    with open(der, "wb") as f:
        f.write(base64.b64decode(pub_b64))
    with open(pw, "wb") as f:
        f.write(plaintext.encode())  # no trailing newline
    subprocess.run([OPENSSL, "pkeyutl", "-encrypt", "-pubin", "-inform", "DER",
                    "-inkey", der, "-pkeyopt", "rsa_padding_mode:pkcs1",
                    "-in", pw, "-out", enc], check=True)
    with open(enc, "rb") as f:
        return base64.b64encode(f.read()).decode()

def main():
    username = sys.argv[1] if len(sys.argv) > 1 else "admin"
    password = sys.argv[2] if len(sys.argv) > 2 else "123456"
    pub_b64 = http_json("/api/auth/public-key")["data"]
    enc = rsa_encrypt_base64(pub_b64, password)
    resp = http_json("/api/auth/login", method="POST",
                     body={"username": username, "password": enc})
    if resp.get("code") != 0:
        raise SystemExit("login failed: " + json.dumps(resp, ensure_ascii=False))
    print(resp["data"]["accessToken"])

if __name__ == "__main__":
    main()

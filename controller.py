#!/usr/bin/env python3
import json
import argparse
from urllib import request, parse
import sys

def send_alert(tv_ip: str, delay: int, message: str):
    url = f"http://{tv_ip}:8080/alert"
    data = json.dumps({
        "delaySeconds": delay,
        "message": message
    }).encode("utf-8")
    req = request.Request(url, data=data, headers={"Content-Type":"application/json"}, method="POST")
    try:
        with request.urlopen(req, timeout=5) as resp:
            print(resp.read().decode())
    except Exception as e:
        print(f"Hata: {e}", file=sys.stderr)
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Android TV Deprem Uyarı tetikleyici")
    parser.add_argument("--tv", default="192.168.1.104", help="TV IP adresi")
    parser.add_argument("--delay", type=int, default=5, help="Gecikme (saniye)")
    parser.add_argument("--message", default="Marmara Denizi, İstanbul yakınlarında 6.2 büyüklüğünde deprem.",
                        help="Gösterilecek mesaj")
    args = parser.parse_args()
    send_alert(args.tv, args.delay, args.message)

if __name__ == "__main__":
    main()

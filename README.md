# Android TV Deprem Uyarı Sistemi (PC'den Komutlu)

Bu proje, **PC (192.168.1.108)** üzerinden verilen komuttan **5 saniye sonra** **Android TV (192.168.1.104)** ekranında *kanal kapanmadan*, küçük bir **süslü uyarı** penceresi gösterir. Uyarı penceresi saniye sayar ve her saniye kısa bir uyarı sesi verir.

## Bileşenler
- **Android TV uygulaması (APK projesi)**: TV'de arka planda çalışır, 8080 portundan HTTP komutlarını dinler ve overlay uyarı oluşturur.
- **PC Python kontrol scripti (`controller.py`)**: TV IP'sine HTTP isteği göndererek uyarıyı tetikler.

## Ağ
- TV IP → `192.168.1.104`
- PC IP → `192.168.1.108`
- TV uygulaması port → `8080` (gerekirse değiştirilebilir)

## Android TV Uygulaması
- Overlay için `SYSTEM_ALERT_WINDOW` izni gerekir. İlk açılışta izin ekranına yönlendirir.
- Foreground servis, **NanoHTTPD** ile mini HTTP sunucusu başlatır.
- Endpoint:
  - `POST http://TV_IP:8080/alert` (JSON)
    ```json
    {
      "delaySeconds": 5,
      "message": "Marmara Denizi, İstanbul yakınlarında 6.2 büyüklüğünde deprem."
    }
    ```
  - `GET http://TV_IP:8080/alert?delay=5&message=...` da desteklenir.

### TV’de Kurulum
1. Projeyi Android Studio ile açın (`AndroidTVAlert/`).
2. TV’ye yükleyin (USB/ADB). Android TV’de **Ayarlar → Uygulamalar → Özel erişim → Diğer uygulamaların üzerinde çiz** iznini **TV Quake Alert** için **Açın**.
3. Uygulamayı başlatın. Ekranda **"Servis çalışıyor..."** yazısını görmelisiniz.
4. TV ve PC’nin aynı yerel ağda olduğundan emin olun.

> Not: Overlay bazı TV uygulamalarında sistem tarafından gizlenebilir. Çoğu Android TV uygulamasında `TYPE_APPLICATION_OVERLAY` çalışır; çalışmazsa TV ayarlarından reklam/overlay engellemesi varsa kapatın.

## PC Kontrol Scripti (`controller.py`)
TV’ye komut gönderir. Varsayılan 5 saniye sonra Marmara 6.2 mesajı gösterir.

### Kullanım
```bash
python3 controller.py --tv 192.168.1.104 --delay 5 \
  --message "Marmara Denizi, İstanbul yakınlarında 6.2 büyüklüğünde deprem."
```

### Örnek
```bash
python3 controller.py --tv 192.168.1.104
```

## Güvenlik
- TV uygulaması yerel ağda *herkese* açıktır. İsterseniz modem/firewall ile sadece **PC IP (192.168.1.108)**’e izin verin.
- Alternatif olarak, `OverlayService.SimpleHttpServer.serve` içinde basit bir shared-secret kontrolü ekleyebilirsiniz.

## Özelleştirme
- Uyarı konumunu `params.gravity`, `x`, `y` değerleri ile değiştirin.
- Ses şiddetini `ToneGenerator(AudioManager.STREAM_ALARM, 80)` ikinci parametre ile ayarlayın (0-100).
- Tasarımı `res/layout/overlay_alert.xml` ve `res/drawable/overlay_bg.xml` ile değiştirin.

## Hızlı Test
TV’de uygulama açıkken PC’den:
```bash
curl -X POST http://192.168.1.104:8080/alert \
  -H "Content-Type: application/json" \
  -d '{"delaySeconds":5,"message":"Marmara Denizi, İstanbul yakınlarında 6.2 büyüklüğünde deprem."}'
```

Başarılıysa 5 sn sonra TV’de küçük uyarı penceresi belirir, saniye sayar ve her saniye bip sesi verir.

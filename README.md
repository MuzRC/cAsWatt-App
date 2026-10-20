# ⚡ CasWatt - Android Charging Monitor & Widget

Aplikasi Android untuk mengesan arus masuk ($mA$), voltan ($V$), dan mengira kuasa cas sebenar ($W$) secara masa-nyata berserta Home Screen Widget.

## ✨ Ciri-Ciri Utama:
- **Bacaan Kuasa Sebenar (Watt):** Mengira $W = (V 	imes mA) / 1,000,000$ secara langsung dari cip pengurusan bateri Android.
- **Home Screen Widget:** Paparan Watt, status pengecasan, peratus bateri, dan arus terus di skrin utama telefon.
- **Auto-Start & Auto-Stop:** Servis pemantauan automatik berjalan bila kabel pengecas dicucuk, dan automatik berhenti bila kabel dicabut untuk menjimatkan bateri.
- **Ringan & Bebas Iklan:** Tiada kebergantungan luar yang berat.

## 🚀 Cara Jana APK Menggunakan GitHub Actions:
1. Buat satu Repository baru di GitHub (contoh: `CasWatt`).
2. Muat naik (*push* atau *upload*) semua fail dalam folder ini ke GitHub repository tersebut.
3. Sebaik sahaja fail ditolak ke branch `main`, GitHub Actions akan automatik mula membina (*compile*) aplikasi.
4. Pergi ke tab **Actions** di GitHub repo anda.
5. Tekan pada *Workflow run* terkini, dan di bahagian **Artifacts** di bawah sekali, muat turun fail **`CasWatt-Debug-APK`**.
6. *Unzip* dan pasang fail `.apk` terus ke telefon Android anda!

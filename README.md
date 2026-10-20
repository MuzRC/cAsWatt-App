# ⚡ CasWatt Pro v5.0.0 (Master Edition) - Multimeter & Diagnostik Bateri Pintar

Aplikasi pemantauan bateri dan kuasa pengecasan Android generasi baharu dengan ketepatan tahap multimeter, sifar bloatware, tanpa iklan, dan 100% offline. 

## 🚀 Ciri-Ciri Baharu dalam v5.0.0 (Master Edition):

1. **Diagnostik Kualiti Kabel & Pengecas (*Cable Resistance & Quality Tester*):**
   - Menilai kualiti kabel USB Type-C secara langsung berdasarkan daya hantar arus dan voltan (`Cemerlang`, `Baik`, `Sederhana`, atau `Kabel Lemah / Rintangan Tinggi`).
2. **Pengesan Sekatan Suhu (*Thermal Throttling Alert*):**
   - Mengesan secara automatik jika cip kuasa (*PMIC*) telefon menurunkan kuasa cas akibat suhu melebihi 40°C bagi melindungi sel bateri daripada degradasi haba.
3. **Smart ETA (Anggaran Masa Siap Cas Pintar):**
   - Mengira baki minit pengecasan secara dinamik mengikut keluk arus semasa:
     - ⏱️ Baki masa ke **80%** (Zon Selamat Litium).
     - ⏱️ Baki masa ke **100%** (Cas Penuh).
4. **Kalkulator Tenaga Sebenar (Wh) & Kos Elektrik (Tarif Sabah SESB):**
   - Menukar arus ($mAh$) dan voltan ($V$) kepada unit tenaga sebenar **Watt-jam ($Wh$)**.
   - Mengira anggaran kos elektrik setiap sesi pengecasan berdasarkan tarif asas elektrik SESB Sabah (RM 0.38/kWh).
5. **Log Terperinci Mengikut Sesi Cas (*Per-Session History*):**
   - Merekodkan setiap sesi sambungan kabel secara individu: Masa Mula, Masa Tamat, Durasi Penggunaan, Paras % Mula, Paras % Tamat, Watt Puncak, Tenaga Diserap, Kos Elektrik, dan Penarafan Kabel.
6. **Eksport CSV Lengkap (Excel / Sheets):**
   - Satu butang untuk mengeksport kedua-dua **Ringkasan Harian (10 Tahun)** dan **Log Terperinci Sesi** ke dalam fail `.csv` melalui perkongsian selamat `FileProvider`.
7. **Penggera Penepuan Penuh (*Trickle Charge Saturation*) & Penggera Bateri Rendah (20%):**
   - Penggera khas apabila bateri 100% mencapai fasa tepu sepenuhnya (arus masuk < 40mA).
   - Penggera peringatan bateri rendah (< 20%) semasa nyahcas untuk mengelakkan *deep discharge*.

## 🏆 Kredit Projek:
- 💡 **Pencetus Idea & Konsep:** Muz
- 🤖 **Pelaksana & Jurutera Kod:** Gemini 3.8
- 📍 **Asal Usul Inovasi:** Beaufort, Sabah

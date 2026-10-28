# ClearLag (Fabric 1.21.11)

Build:  ./gradlew build   -> build/libs/clearlag-1.0.0.jar
Taruh jar di folder mods/ server (butuh Fabric API).

Auto clear tiap 5 menit, peringatan tampil di atas hotbar (action bar).

Command (butuh OP level 2):
  /clearlag now      bersihkan sekarang
  /clearlag reload   reload config/clearlag.json
  /clearlag status   lihat sisa waktu

Config: config/clearlag.json
  intervalSeconds   jeda dalam detik (300 = 5 menit)
  warnAtSeconds     kapan peringatan muncul
  warnDisplay       "ACTIONBAR" atau "CHAT"
  resultDisplay     "ACTIONBAR" atau "CHAT"
  protectedItems    item yang tidak dihapus, mis. ["minecraft:diamond"]

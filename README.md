
# 🎴 MusRoyale

**MusRayale** Android (Kotlin/Java) teknologian garatutako plataforma digitala da, Euskal Herriko mus joko tradizionala gailu mugikorretara egokitzen duena. Proiektu honek denbora errealeko joko-esperientzia bat eskaintzen du, erabiltzaileek mundu osoko beste jokalari batzuekin edo lagunekin jokatzeko aukera izanik, edonon daudela ere.

---

## 🚀 Ezaugarri Nagusiak

* **⚡ Partida Azkarrak:** Aurkitu aurkariak berehala *matchmaking* automatikoarekin.
* **👥 Bikoteak:** Jokatu lagun batekin taldean partida publikoetan.
* **🔐 Partida Pribatuak:** Sortu kode bidezko gelak lagunarteko partidetarako.
* **📈 Estatistika Sistema:** Kontrolatu zure garaipenak, partidak eta saldoa zure profilean.
* **💬 Txata:** Komunikatu zure lagunekin denbora errealean.

---

## 🎮 Nola Jokatu (Gameplay)

Jokoa zerbitzariak sinkronizatutako txanda-ziklo batek gobernatzen du:

### 1. Hasiera eta Mus Fasea
Jokalari bakoitzak 4 karta jasotzen ditu.
* **Mus:** Jokalari guztiek "Mus" hautatzen badute, kartak aldatu ditzakezu.
* **Paso:** Norbaitek "Paso" hautatzen badu, musa moztu egiten da eta apustuen fasea hasten da.

### 2. Apustu Faseak (Lantzeak)
Apustu-sekuentzia hau gertatzen da:
1.  **Handia**
2.  **Txikia**
3.  **Pareak**
4.  **Jokoa/Puntua**

Zure txanda denean, interfazeak botoi dinamikoak gaituko ditu: `Envido`, `Órdago`, `Paso`, `Quiero`.

### 3. Puntuazioa
Zerbitzariak puntuak automatikoki kalkulatzen ditu eta markagailuan animazioen bidez erakusten dira. Puntu-muga (adibidez, 40 puntu) lortzean, partida amaitzen da.

---

## 🕹️ Joko Modalitateak

### ⚡ Partida Azkarra
Berehala jokatzeko aproposa.
1.  Sakatu **"Partida Azkarra"** botoia menu nagusian.
2.  Sistemak *matchmaking* automatikoa egingo du ausazko 4 jokalari elkartzeko (bikote moduan edo banaka, aukeratu denaren arabera).
3.  Lau jokalariak elkartzean, partida hasiko da.

### 👥 Bikoteak: Jokatu Lagun Batekin
Zure lagunekin taldea osatzea oso erraza da:
1.  Joan **"Lagunak"** atalera eta bilatu zure laguna.
2.  Bidali **gonbidapena**.
3.  Lagunak onartzen duenean, **"Bikoteak"** modua aukeratuz, biok taldekide gisa sartuko zarete gela berean.
4.  Sistemak beste bikote bat bilatuko du partida hasteko.

### 🔐 Partida Pribatua (Kodea)
Lagunarteko partidetarako kontrolatua.
1.  **Anfitrioia:** Sakatu "Partida Pribatua" eta **"Sortu Kodea"**. Pop-up batean agertuko den kodea lagunei pasatu.
2.  **Gonbidatuak:** Sakatu "Partida Pribatua" eta sartu kodea **input eremuan** anfitrioiaren gela pribatuan sartzeko.
3.  4 jokalari elkartzean, zerbitzariak partida hasiko du.

---

## 🛠️ Xehetasun Teknikoak

* **Frontend:** Android Studio (Kotlin/Java) Material Design interfazearekin.
* **Backend:** Sockets/API bidezko komunikazio asinkronoa denbora errealean.

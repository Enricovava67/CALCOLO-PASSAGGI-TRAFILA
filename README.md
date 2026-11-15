# Calcolo Passaggi Trafila – Android

App nativa Android (Java) per trafile multipasso a tiro rettilineo, acciaio a basso carbonio, lubrificazione a secco.

## Funzioni
- Ingresso: diametro vergella, diametro finale, 1–7 passaggi, velocità ultimo blocco in m/s.
- Proposta di riduzioni decrescenti (sezione) con minimo del 18%.
- Cursore per ogni passaggio, riduzione scelta bloccata e ricalcolo degli altri passaggi.
- Possibilità di sbloccare singole riduzioni e ripristinare il calcolo.
- Diametri visualizzati al centesimo; allungamenti; produttività teorica ed effettiva all'80% in kg/h.
- Salvataggio locale di una configurazione e richiamo successivo.

## Ottenere l'APK con GitHub
1. Crea un repository GitHub e carica **tutti i file e le cartelle di questo ZIP**, compresa `.github/workflows/android.yaml`.
2. Apri **Actions** → **Compila APK Android** → **Run workflow** (oppure attendi l'avvio automatico su push in main/master).
3. A build conclusa, apri l'esecuzione e scarica l'artifact **CalcoloPassaggiTrafila-APK**.
4. Estrai `app-debug.apk` dallo ZIP dell'artifact e installalo sul telefono Android.

La build debug è installabile per test personali, non è una release firmata per pubblicazione su Play Store.

## Nota tecnica
Le riduzioni sono percentuali di sezione, non di diametro. La distribuzione è matematica e orientativa, non una verifica meccanica della trafila. Il minimo del 18% è un vincolo imposto dall'utilizzatore. Il 34% è una soglia di avviso, non un massimo fisico.

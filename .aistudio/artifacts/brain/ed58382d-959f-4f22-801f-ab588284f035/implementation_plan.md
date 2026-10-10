# Implementatieplan: Discord Webhook voor #leaders-chat & Bestandsuploads

## Samenvatting
De gebruiker wil uitslagen van Ultieme Kooi en aanwezigheid direct vanuit de app naar Discord sturen in het kanaal **`#leaders-chat`** van de server *BLOOD THIRSTY TED ALLIANCE*. In het recente screenshot van de gebruiker werd het menu van `#leaders-chat` getoond met opties zoals *"Kanaal-ID kopiëren"* en *"Kopieer link"*. 

Dit plan bevat:
1. Een duidelijke uitleg en stappenplan hoe de Discord Webhook voor `#leaders-chat` aangemaakt wordt (inclusief hulp in de app).
2. Een prominente **Discord Koppeling & Delen** functie direct in het hoofdscherm van de app en in het screenshot-scanscherm.
3. Ondersteuning om met 1 klik Kooi-uitslagen (doelpunten, spelers, Kooi 1 & Kooi 2) en aanwezigheid als rijke Discord Embeds naar `#leaders-chat` te sturen.
4. Verbetering van het uploaden van bestanden en screenshots vanaf de telefoon met flexibele ondersteuning voor zowel de Android Photo Picker als de Documenten/Bestanden kiezer.

---

## Voorgestelde Wijzigingen

### 1. Discord Webhook Helper & Handleiding in de App
- In Discord levert *"Kopieer link"* of *"Kanaal-ID kopiëren"* een kanaalnummer op, maar een Webhook vereist een Webhook URL (formaat: `https://discord.com/api/webhooks/.../...`).
- We voegen een gebruiksvriendelijke instructiedialoog toe in de app met duidelijke stappen:
  - *Stap 1:* Ga in Discord naar Serverinstellingen of Kanaalinstellingen van `#leaders-chat`.
  - *Stap 2:* Tik op **Integraties** $\rightarrow$ **Webhooks** $\rightarrow$ **Nieuwe Webhook**.
  - *Stap 3:* Noem de webhook bijvoorbeeld "TED PRIVATE" en tik op **Webhook-URL kopiëren**.
  - *Stap 4:* Plak deze link in de TED PRIVATE app.
- De URL wordt persistent bewaard op het toestel.

### 2. Direct Uitslagen & Aanwezigheid Delen naar Discord
- **In `ScreenshotScannerDialog.kt`:** 
  - Na het scannen van Kooi 1 of Kooi 2 screenshots een directe actieknop: **"📤 Deel uitslag naar Discord #leaders-chat"**.
  - Stuurt een opgemaakte Discord Embed met:
    - Titel: `🛡️ [TED PRIVATE] Uitslag Ultieme Kooi 1 / 2`
    - Top scorers met doelpunten (bijv. willow: 4,7 mld, JustineValentina: 2,6 mld, Hans worst: 2,5 mld, etc.)
    - Status van de overige clanleden.
- **In `EventsScreen.kt`:**
  - Een directe knop in de actiebalk: **"Discord Delen"** met live status of er al een webhook geconfigureerd is.
  - Testknop om direct een welkomst- of testbericht naar `#leaders-chat` te sturen.

### 3. Verbeterde Bestandsupload vanaf de Telefoon
- Zorgen dat de gebruiker zowel via de galerij (`PickVisualMedia`) als via het volledige bestandsbeheer (`GetContent("image/*")`) afbeeldingen kan selecteren.
- Directe visuele feedback (bestandsnaam, miniatuur en scanstatus) wanneer een bestand van de telefoon wordt geselecteerd.

---

## Verificatieplan
1. **Compilatietest:** `compile_applet` uitvoeren om te verifiëren dat alle Compose-schermen en OkHttp Discord-aanroepen foutloos compileren.
2. **Webhook Test:** Verifiëren dat de Webhook URL validatie netjes controleert op `https://discord.com/api/webhooks/` en duidelijke foutmeldingen geeft bij verkeerde invoer.
3. **UI Test:** Controleren dat de Discord actieknoppen goed zichtbaar en makkelijk bereikbaar zijn op zowel mobiele schermen als breedbeeld.

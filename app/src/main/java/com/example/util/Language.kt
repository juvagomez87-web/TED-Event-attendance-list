package com.example.util

enum class Language(val code: String, val displayName: String, val flag: String) {
    EN("en", "English", "🇬🇧"),
    NL("nl", "Nederlands", "🇳🇱"),
    ES("es", "Español", "🇪🇸");

    companion object {
        fun fromCode(code: String): Language = entries.find { it.code == code } ?: NL
    }
}

object AppStrings {
    fun get(key: String, lang: Language): String {
        return translations[key]?.get(lang) ?: translations[key]?.get(Language.EN) ?: key
    }

    private val translations: Map<String, Map<Language, String>> = mapOf(
        // Navigation & App
        "app_title" to mapOf(
            Language.EN to "TED PRIVATE",
            Language.NL to "TED PRIVATE",
            Language.ES to "TED PRIVATE"
        ),
        "app_subtitle" to mapOf(
            Language.EN to "Event Attendance & Member Roster Manager",
            Language.NL to "Evenement Participatie & Leden Beheer",
            Language.ES to "Asistencia a Eventos y Gestión de Miembros"
        ),
        "nav_events" to mapOf(
            Language.EN to "Events",
            Language.NL to "Events",
            Language.ES to "Eventos"
        ),
        "nav_roster" to mapOf(
            Language.EN to "Roster",
            Language.NL to "Leden",
            Language.ES to "Miembros"
        ),
        "nav_stats" to mapOf(
            Language.EN to "Stats & Admin",
            Language.NL to "Statistieken",
            Language.ES to "Estadísticas"
        ),
        "nav_audit" to mapOf(
            Language.EN to "Audit Log",
            Language.NL to "Wijzigingen",
            Language.ES to "Historial"
        ),
        "nav_updates" to mapOf(
            Language.EN to "Updates & Alerts",
            Language.NL to "Updates & Berichten",
            Language.ES to "Avisos"
        ),

        // Actions
        "export_pdf" to mapOf(
            Language.EN to "Export PDF",
            Language.NL to "Exporteer PDF",
            Language.ES to "Exportar PDF"
        ),
        "import_pdf" to mapOf(
            Language.EN to "Import PDF",
            Language.NL to "Importeer PDF",
            Language.ES to "Importar PDF"
        ),
        "new_event" to mapOf(
            Language.EN to "New Event",
            Language.NL to "Nieuw Event",
            Language.ES to "Nuevo Evento"
        ),
        "add_member" to mapOf(
            Language.EN to "Add Member",
            Language.NL to "Lid Toevoegen",
            Language.ES to "Añadir Miembro"
        ),
        "all_present" to mapOf(
            Language.EN to "All Present",
            Language.NL to "Allen Aanwezig",
            Language.ES to "Todos Presentes"
        ),
        "all_absent" to mapOf(
            Language.EN to "All Absent",
            Language.NL to "Allen Afwezig",
            Language.ES to "Todos Ausentes"
        ),
        "reset" to mapOf(
            Language.EN to "Reset",
            Language.NL to "Herstellen",
            Language.ES to "Restablecer"
        ),
        "save" to mapOf(
            Language.EN to "Save",
            Language.NL to "Opslaan",
            Language.ES to "Guardar"
        ),
        "cancel" to mapOf(
            Language.EN to "Cancel",
            Language.NL to "Annuleren",
            Language.ES to "Cancelar"
        ),
        "delete" to mapOf(
            Language.EN to "Delete",
            Language.NL to "Verwijderen",
            Language.ES to "Eliminar"
        ),
        "search_members" to mapOf(
            Language.EN to "Search members...",
            Language.NL to "Zoek leden...",
            Language.ES to "Buscar miembros..."
        ),
        "search_events" to mapOf(
            Language.EN to "Search events...",
            Language.NL to "Zoek evenementen...",
            Language.ES to "Buscar eventos..."
        ),

        // Statuses
        "status_all" to mapOf(
            Language.EN to "All Statuses",
            Language.NL to "Alle Statussen",
            Language.ES to "Todos los Estados"
        ),
        "status_present" to mapOf(
            Language.EN to "Present",
            Language.NL to "Aanwezig",
            Language.ES to "Presente"
        ),
        "status_absent" to mapOf(
            Language.EN to "Absent",
            Language.NL to "Afwezig",
            Language.ES to "Ausente"
        ),
        "status_late" to mapOf(
            Language.EN to "Late",
            Language.NL to "Te laat",
            Language.ES to "Tarde"
        ),
        "status_excused" to mapOf(
            Language.EN to "Excused",
            Language.NL to "Afgemeld",
            Language.ES to "Excusado"
        ),
        "status_other_cage" to mapOf(
            Language.EN to "Other Cage",
            Language.NL to "Andere Kooi",
            Language.ES to "Otra Jaula"
        ),
        "in_kooi_1" to mapOf(
            Language.EN to "In Cage 1",
            Language.NL to "In Kooi 1",
            Language.ES to "En Jaula 1"
        ),
        "in_kooi_2" to mapOf(
            Language.EN to "In Cage 2",
            Language.NL to "In Kooi 2",
            Language.ES to "En Jaula 2"
        ),
        "scan_screenshot" to mapOf(
            Language.EN to "Scan Screenshot",
            Language.NL to "Screenshot Scannen",
            Language.ES to "Escanear Captura"
        ),
        "screenshot_interpreter" to mapOf(
            Language.EN to "Screenshot Interpreter",
            Language.NL to "Screenshot Interpreter",
            Language.ES to "Intérprete de Captura"
        ),

        // Participation & Stats
        "participation_rate" to mapOf(
            Language.EN to "Participation Rate",
            Language.NL to "Participatiegraad",
            Language.ES to "Tasa de Participación"
        ),
        "overall_attendance" to mapOf(
            Language.EN to "Overall Attendance",
            Language.NL to "Totale Aanwezigheid",
            Language.ES to "Asistencia Total"
        ),
        "total_events" to mapOf(
            Language.EN to "Total Events",
            Language.NL to "Totaal Events",
            Language.ES to "Total de Eventos"
        ),
        "total_members" to mapOf(
            Language.EN to "Total Members",
            Language.NL to "Totaal Leden",
            Language.ES to "Total de Miembros"
        ),
        "top_attendees" to mapOf(
            Language.EN to "Top Attendees",
            Language.NL to "Meest Actieve Leden",
            Language.ES to "Miembros Más Activos"
        ),
        "low_attendance" to mapOf(
            Language.EN to "Needs Attention (<50%)",
            Language.NL to "Aandacht Nodig (<50%)",
            Language.ES to "Requiere Atención (<50%)"
        ),
        "filter_period" to mapOf(
            Language.EN to "Period",
            Language.NL to "Periode",
            Language.ES to "Período"
        ),
        "filter_group" to mapOf(
            Language.EN to "Group / Role",
            Language.NL to "Groep / Rol",
            Language.ES to "Grupo / Rol"
        ),
        "period_all" to mapOf(
            Language.EN to "All Time",
            Language.NL to "Alles",
            Language.ES to "Todo el Tiempo"
        ),
        "period_7d" to mapOf(
            Language.EN to "Last 7 Days",
            Language.NL to "Laatste 7 Dagen",
            Language.ES to "Últimos 7 Días"
        ),
        "period_30d" to mapOf(
            Language.EN to "Last 30 Days",
            Language.NL to "Laatste 30 Dagen",
            Language.ES to "Últimos 30 Días"
        ),
        "period_month" to mapOf(
            Language.EN to "This Month",
            Language.NL to "Deze Maand",
            Language.ES to "Este Mes"
        ),

        // Member Detail
        "member_history" to mapOf(
            Language.EN to "Event Participation History",
            Language.NL to "Deelnamegeschiedenis",
            Language.ES to "Historial de Participación"
        ),
        "attended" to mapOf(
            Language.EN to "Attended",
            Language.NL to "Meegedaan",
            Language.ES to "Asistió"
        ),
        "missed" to mapOf(
            Language.EN to "Missed",
            Language.NL to "Niet meegedaan",
            Language.ES to "No asistió"
        ),

        // Audit Log
        "audit_title" to mapOf(
            Language.EN to "Who Modified Records (Audit Log)",
            Language.NL to "Wie Heeft Wijzigingen Aangebracht",
            Language.ES to "Quién Realizó Modificaciones"
        ),
        "audit_desc" to mapOf(
            Language.EN to "Full activity history showing which admin or user made modifications to files and attendance.",
            Language.NL to "Volledige geschiedenis van wie welke gegevens, statussen of notities heeft gewijzigd.",
            Language.ES to "Historial completo de qué administrador o usuario modificó datos, estados o notas."
        ),
        "modified_by" to mapOf(
            Language.EN to "Modified by",
            Language.NL to "Gewijzigd door",
            Language.ES to "Modificado por"
        ),

        // Notes
        "notes" to mapOf(
            Language.EN to "Notes & Comments",
            Language.NL to "Notities & Opmerkingen",
            Language.ES to "Notas y Comentarios"
        ),
        "add_note" to mapOf(
            Language.EN to "Add Note",
            Language.NL to "Notitie Toevoegen",
            Language.ES to "Añadir Nota"
        ),
        "note_placeholder" to mapOf(
            Language.EN to "Type real-time note or reason...",
            Language.NL to "Typ realtime notitie of reden...",
            Language.ES to "Escribe nota o motivo..."
        ),

        // Push Notifications & Reminders
        "reminders_title" to mapOf(
            Language.EN to "Notifications & Reminders",
            Language.NL to "Meldingen & Herinneringen",
            Language.ES to "Notificaciones y Recordatorios"
        ),
        "reminders_toggle" to mapOf(
            Language.EN to "Enable event reminders",
            Language.NL to "Schakel evenement herinneringen in",
            Language.ES to "Activar recordatorios de eventos"
        ),
        "post_update" to mapOf(
            Language.EN to "Publish Announcement / App Update",
            Language.NL to "Plaats Mededeling / Update",
            Language.ES to "Publicar Aviso / Actualización"
        ),
        "test_notification" to mapOf(
            Language.EN to "Send Test Push Notification",
            Language.NL to "Verstuur Test Pushmelding",
            Language.ES to "Enviar Notificación de Prueba"
        ),

        // Roles & Admin
        "admin_mode" to mapOf(
            Language.EN to "Admin Mode",
            Language.NL to "Beheerdersmodus",
            Language.ES to "Modo Administrador"
        ),
        "logged_in_as" to mapOf(
            Language.EN to "Active User",
            Language.NL to "Actieve Gebruiker",
            Language.ES to "Usuario Activo"
        ),
        "enter_pin" to mapOf(
            Language.EN to "Enter Admin PIN (Default: 1234)",
            Language.NL to "Voer Beheerders PIN in (Standaard: 1234)",
            Language.ES to "Ingrese PIN de Admin (Predeterminado: 1234)"
        ),
        "admin_unlocked" to mapOf(
            Language.EN to "Administrator permissions active",
            Language.NL to "Beheerdersrechten actief",
            Language.ES to "Permisos de administrador activos"
        ),

        // PDF Import & Export
        "pdf_import_title" to mapOf(
            Language.EN to "Import Data from PDF / Documents",
            Language.NL to "Importeer Gegevens uit PDF / Documenten",
            Language.ES to "Importar Datos desde PDF / Documentos"
        ),
        "pdf_import_instructions" to mapOf(
            Language.EN to "Select a PDF file or paste roster text from reports. The system will automatically parse events, members, ranks, and attendance statuses.",
            Language.NL to "Selecteer een PDF-bestand of plak rooster-tekst. Het systeem herkent automatisch events, leden, rangen en aanwezigheid.",
            Language.ES to "Seleccione un archivo PDF o pegue el texto del registro. El sistema analizará eventos, miembros, rangos y estados."
        ),
        "pick_pdf_file" to mapOf(
            Language.EN to "Select PDF File",
            Language.NL to "Kies PDF-Bestand",
            Language.ES to "Seleccionar Archivo PDF"
        ),
        "paste_or_sample" to mapOf(
            Language.EN to "Or Paste / Load Sample Data",
            Language.NL to "Of Plak / Laad Voorbeeld Gegevens",
            Language.ES to "O Pegar / Cargar Datos de Ejemplo"
        ),
        "parsed_preview" to mapOf(
            Language.EN to "Parsed Preview",
            Language.NL to "Herkende Gegevens",
            Language.ES to "Vista Previa Reconocida"
        ),
        "confirm_import" to mapOf(
            Language.EN to "Import & Apply to Roster",
            Language.NL to "Importeren & Toepassen op Roster",
            Language.ES to "Importar y Aplicar a Registros"
        ),
        "pdf_exported_success" to mapOf(
            Language.EN to "PDF report generated and ready to share!",
            Language.NL to "PDF rapport gegenereerd en klaar om te delen!",
            Language.ES to "¡Informe PDF generado y listo para compartir!"
        ),
        "offline_ready" to mapOf(
            Language.EN to "Offline mode active: All data saved locally",
            Language.NL to "Offline modus actief: Alle gegevens lokaal opgeslagen",
            Language.ES to "Modo sin conexión activo: Datos guardados localmente"
        )
    )
}

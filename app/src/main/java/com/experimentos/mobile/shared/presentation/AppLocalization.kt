package com.experimentos.mobile.shared.presentation

import androidx.compose.runtime.compositionLocalOf
import com.experimentos.mobile.mood.data.Mood

enum class AppLanguage(val code: String) {
    SPANISH("es"),
    ENGLISH("en"),
}

val LocalAppStrings = compositionLocalOf { AppStrings(AppLanguage.SPANISH) }

/** Centralizes user-facing translations so every mobile role shares one language state. */
class AppStrings(val language: AppLanguage) {
    val isEnglish: Boolean
        get() = language == AppLanguage.ENGLISH

    fun t(spanish: String): String = if (isEnglish) englishTranslations[spanish] ?: spanish else spanish

    fun languageLabel(): String = if (isEnglish) "English" else "Español"

    fun replyCountLabel(count: Int): String {
        val template = t(if (count == 1) "Ver {count} respuesta" else "Ver {count} respuestas")
        return template.replace("{count}", count.toString())
    }

    fun roleLabel(role: String): String = when (role) {
        "EMPLOYEE" -> t("Empleado")
        "HR_MEMBER" -> t("Miembro de Recursos Humanos")
        else -> role.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    fun moodLabel(mood: Mood): String = t(
        when (mood) {
            Mood.VERY_BAD -> "Muy mal"
            Mood.BAD -> "Mal"
            Mood.GOOD -> "Bien"
            Mood.VERY_GOOD -> "Muy bien"
        },
    )

    fun activityStatus(status: String): String = t(
        when (status) {
            "OPEN" -> "VOTACIÓN ABIERTA"
            "CLOSED" -> "CERRADA"
            else -> status
        },
    )

    fun surveyStatus(status: String): String = t(
        when (status) {
            "PUBLISHED" -> "Publicada"
            "CLOSED" -> "Cerrada"
            "DRAFT" -> "Borrador"
            else -> status
        },
    )

    fun reportStatus(status: String): String = t(
        when (status) {
            "NEW" -> "Nuevo"
            "IN_REVIEW" -> "En revisión"
            "ADDRESSED" -> "Atendido"
            "CLOSED" -> "Cerrado"
            else -> status
        },
    )

    fun translateMessage(message: String): String = if (!isEnglish) message else when {
        message == "Preferencias actualizadas." -> "Preferences updated."
        message == "Datos de cuenta actualizados." -> "Account details updated."
        message == "Completa todos los campos." -> "Complete all fields."
        message == "El nombre visible debe tener entre 2 y 100 caracteres." ->
            "The display name must be between 2 and 100 characters."
        message == "Las contraseñas no coinciden." -> "Passwords do not match."
        message == "La contraseña debe tener al menos 8 caracteres." -> "The password must contain at least 8 characters."
        message == "El enlace de recuperación no es válido o ya expiró. Solicita uno nuevo." ->
            "The recovery link is invalid or has expired. Request a new one."
        message == "La cuenta ya existe." -> "The account already exists."
        message == "El nombre de usuario ya está utilizado." -> "The username is already in use."
        message == "El correo electrónico ya está utilizado." -> "The email is already in use."
        message == "No se pudo conectar con el servidor. Verifica tu conexión." ->
            "Could not connect to the server. Check your connection."
        message == "Las credenciales no son válidas." -> "The credentials are not valid."
        message == "Revisa los datos ingresados." -> "Review the entered data."
        message == "No se pudo completar la operación. Intenta nuevamente." ->
            "The operation could not be completed. Try again."
        message == "No se pudo cargar tu estado de ánimo." -> "Your mood could not be loaded."
        message == "No se pudo guardar tu estado de ánimo." -> "Your mood could not be saved."
        message == "No se pudo cargar el resumen de bienestar." -> "The wellbeing summary could not be loaded."
        message == "No se pudo cargar el contenido administrable." -> "Managed content could not be loaded."
        message == "Completa el título y la pregunta." -> "Complete the title and question."
        message == "Encuesta creada como borrador." -> "Survey created as a draft."
        message == "Estado de la encuesta actualizado." -> "Survey status updated."
        message == "Escribe un título y al menos dos opciones." -> "Enter a title and at least two options."
        message == "Actividad creada y abierta para votación." -> "Activity created and open for voting."
        message == "Actividad cerrada." -> "Activity closed."
        message == "Comentario eliminado." -> "Comment deleted."
        message == "No se pudo eliminar el comentario." -> "The comment could not be deleted."
        message == "No se pudo crear la encuesta." -> "The survey could not be created."
        message == "No se pudo cambiar el estado de la encuesta." -> "The survey status could not be changed."
        message == "No se pudo crear la actividad." -> "The activity could not be created."
        message == "No se pudo cerrar la actividad." -> "The activity could not be closed."
        else -> message
    }

    private companion object {
        val englishTranslations = mapOf(
            "SafeSpace" to "SafeSpace",
            "Logo de SafeSpace" to "SafeSpace logo",
            "Bienestar laboral, en un espacio seguro." to "Workplace wellbeing, in a safe space.",
            "Bienvenido de nuevo" to "Welcome back",
            "Vuelve a tu espacio seguro para cuidar de ti." to "Return to your safe space and take care of yourself.",
            "Usuario o correo" to "Username or email",
            "ej. carlos o carlos@empresa.com" to "e.g. carlos or carlos@company.com",
            "Contraseña" to "Password",
            "Escribe tu contraseña" to "Enter your password",
            "¿Olvidaste tu contraseña?" to "Forgot your password?",
            "Recuperar acceso" to "Recover access",
            "Te ayudaremos a volver a tu espacio seguro." to "We will help you return to your safe space.",
            "Escribe tu usuario o correo y te enviaremos un enlace de recuperación." to
                "Enter your username or email and we will send you a recovery link.",
            "Enviar enlace" to "Send link",
            "Enviando…" to "Sending…",
            "Si la cuenta existe, recibirás instrucciones para recuperar el acceso." to
                "If the account exists, you will receive recovery instructions.",
            "Ya tengo el enlace" to "I already have the link",
            "Pega el token que recibiste para crear una nueva contraseña." to
                "Paste the token you received to create a new password.",
            "Token de recuperación" to "Recovery token",
            "Pega aquí tu token" to "Paste your token here",
            "Nueva contraseña" to "New password",
            "Cambiar contraseña" to "Change password",
            "Contraseña actualizada" to "Password updated",
            "Tu contraseña fue actualizada. Ya puedes iniciar sesión." to
                "Your password was updated. You can now sign in.",
            "Volver al inicio de sesión" to "Back to sign in",
            "Validando…" to "Validating…",
            "Iniciar sesión" to "Sign in",
            "Nombre visible" to "Display name",
            "Guardando…" to "Saving…",
            "Continuar" to "Continue",
            "¿No tienes cuenta?" to "Don't have an account?",
            "Regístrate" to "Sign up",
            "Crea tu cuenta" to "Create your account",
            "SafeSpace · Tu santuario digital." to "SafeSpace · Your digital sanctuary.",
            "Así te llamará la app en tus espacios personales." to
                "This is how the app will address you in your personal spaces.",
            "Nombre de usuario" to "Username",
            "ej. carlos.mendoza" to "e.g. carlos.mendoza",
            "Correo electrónico" to "Email address",
            "ej. carlos@empresa.com" to "e.g. carlos@company.com",
            "Mínimo 8 caracteres" to "At least 8 characters",
            "Confirmar contraseña" to "Confirm password",
            "Repite tu contraseña" to "Repeat your password",
            "Creando cuenta…" to "Creating account…",
            "Registrar cuenta" to "Create account",
            "Al registrarte aceptas nuestras condiciones de uso y política de privacidad." to
                "By signing up, you accept our terms of use and privacy policy.",
            "¿Ya tienes una cuenta?" to "Already have an account?",
            "Inicia sesión" to "Sign in",
            "Ocultar contraseña" to "Hide password",
            "Mostrar contraseña" to "Show password",
            "Próximamente" to "Coming soon",
            "¿Cómo te sientes hoy?" to "How are you feeling today?",
            "Muy mal" to "Very bad",
            "Mal" to "Bad",
            "Bien" to "Good",
            "Muy bien" to "Very good",
            "Registrado: Muy mal" to "Recorded: Very bad",
            "Registrado: Mal" to "Recorded: Bad",
            "Registrado: Bien" to "Recorded: Good",
            "Registrado: Muy bien" to "Recorded: Very good",
            "Registrado:" to "Recorded:",
            "Podrás responder nuevamente mañana." to "You can answer again tomorrow.",
            "Métricas de Bienestar" to "Wellbeing metrics",
            "Visualiza tu progreso a lo largo del tiempo con gráficos detallados." to
                "View your progress over time with detailed charts.",
            "Recursos Guiados" to "Guided resources",
            "Ejercicios de respiración y mindfulness para tu día a día." to
                "Breathing and mindfulness exercises for your day.",
            "Encuestas" to "Surveys",
            "Centro de Encuestas" to "Survey Center",
            "Tu opinión nos ayuda a construir un entorno laboral saludable. Participa en nuestras evaluaciones periódicas y ayúdanos a mejorar el bienestar del equipo." to
                "Your opinion helps us build a healthy workplace. Take part in our regular evaluations and help us improve team wellbeing.",
            "Encuestas diarias" to "Daily surveys",
            "Encuestas semanales" to "Weekly surveys",
            "Diarias" to "Daily",
            "Semanales" to "Weekly",
            "No hay encuestas diarias publicadas." to "No daily surveys are published.",
            "No hay encuestas semanales publicadas." to "No weekly surveys are published.",
            "Responde encuestas diarias y semanales y comparte tu experiencia." to
                "Answer daily and weekly surveys and share your experience.",
            "Breve chequeo de tu estado de ánimo y niveles de energía hoy. Toma menos de un minuto." to
                "A quick check-in on your mood and energy levels today. It takes less than a minute.",
            "Actividades semanales" to "Weekly activities",
            "Participa en votaciones y elige las actividades que más te interesen." to
                "Vote and choose the activities that interest you most.",
            "Comunica una situación de forma identificada o anónima." to
                "Report a situation with your identity shown or anonymously.",
            "Tu opinión nos ayuda a construir un ambiente laboral saludable. Comparte tu experiencia y ayuda a mejorar el bienestar del equipo." to
                "Your opinion helps us build a healthy workplace. Share your experience and help improve team wellbeing.",
            "Crear reporte" to "Create report",
            "Comenzar ahora" to "Start now",
            "Ver actividades" to "View activities",
            "Responde las preguntas de hoy y comparte cómo fue tu jornada." to
                "Answer today's questions and share how your workday went.",
            "Responde las encuestas disponibles y comparte tu experiencia." to
                "Answer the available surveys and share your experience.",
            "Comenzar ahora  →" to "Start now  →",
            "Ver progreso  →" to "View progress  →",
            "Actividad del fin de semana" to "Weekend activity",
            "Vota por la actividad que prefieres para nuestro próximo encuentro de equipo." to
                "Vote for your preferred activity for our next team gathering.",
            "Tu respuesta" to "Your answer",
            "Escribe tu respuesta..." to "Write your answer...",
            "Enviar respuesta" to "Submit answer",
            "Comentarios anónimos" to "Anonymous comments",
            "Ocultar" to "Hide",
            "Ver comentarios" to "View comments",
            "Enviar comentario" to "Send comment",
            "Eliminar comentario" to "Delete comment",
            "Esta acción no se puede deshacer." to "This action cannot be undone.",
            "Cerrar" to "Close",
            "Responder" to "Reply",
            "Respuesta" to "Reply",
            "Ver {count} respuesta" to "View {count} reply",
            "Ver {count} respuestas" to "View {count} replies",
            "Cerrar respuestas" to "Close replies",
            "Respuesta anónima" to "Anonymous reply",
            "Escribe una respuesta..." to "Write a reply...",
            "Publicar respuesta" to "Post reply",
            "VOTACIÓN ABIERTA" to "OPEN VOTE",
            "CERRADA" to "CLOSED",
            "Actividad abierta" to "Open activity",
            "abierta" to "open",
            "cerrada" to "closed",
            "Votar" to "Vote",
            "Cambiar voto" to "Change vote",
            "votos" to "votes",
            "Voto registrado. Puedes cambiarlo cuando quieras." to "Vote recorded. You can change it anytime.",
            "Gestión" to "Management",
            "Contenido del equipo" to "Team content",
            "Crea contenido y revisa el estado de tus encuestas y actividades." to
                "Create content and review the status of your surveys and activities.",
            "Actualizar inicio" to "Refresh home",
            "Actualizar contenido" to "Refresh content",
            "Actualizar resumen" to "Refresh summary",
            "Revisa respuestas, comentarios y estado de cada encuesta." to
                "Review responses, comments and the status of each survey.",
            "Consulta resultados actuales o finales y cierra las votaciones abiertas." to
                "Review current or final results and close open votes.",
            "Los borradores se pueden publicar y las publicadas se pueden cerrar." to
                "Drafts can be published and published surveys can be closed.",
            "Actividades" to "Activities",
            "Consulta las votaciones abiertas y el historial de actividades cerradas." to
                "View open votes and the history of closed activities.",
            "Cerrar encuesta" to "Close survey",
            "Las personas ya no podrán enviar nuevas respuestas a esta encuesta." to
                "People will no longer be able to submit new answers to this survey.",
            "Cerrar actividad" to "Close activity",
            "Ocultar comentarios" to "Hide comments",
            "Comentarios" to "Comments",
            "comentarios" to "comments",
            "Aún no hay comentarios." to "There are no comments yet.",
            "Comentario anónimo" to "Anonymous comment",
            "La actividad dejará de estar disponible para votación." to
                "The activity will no longer be available for voting.",
            "Encuesta" to "Survey",
            "Actividad" to "Activity",
            "Publicar encuesta" to "Publish survey",
            "Esta encuesta está cerrada y se conserva solo como historial." to
                "This survey is closed and kept for history only.",
            "Esta actividad está cerrada y se conserva solo como historial." to
                "This activity is closed and kept for history only.",
            "Nueva encuesta" to "New survey",
            "Título" to "Title",
            "Ej. Encuesta del día" to "e.g. Daily survey",
            "Nombre visible para el equipo" to "Name visible to the team",
            "Pregunta" to "Question",
            "¿Cómo fue tu jornada hoy?" to "How was your workday today?",
            "Frecuencia" to "Frequency",
            "Diaria" to "Daily",
            "Semanal" to "Weekly",
            "Permitir comentarios" to "Allow comments",
            "Los empleados podrán añadir comentarios anónimos." to "Employees can add anonymous comments.",
            "Crear encuesta" to "Create survey",
            "Cancelar" to "Cancel",
            "Nueva actividad" to "New activity",
            "Ej. Actividad del fin de semana" to "e.g. Weekend activity",
            "Descripción (opcional)" to "Description (optional)",
            "Añade contexto para la votación" to "Add context for the vote",
            "Opciones" to "Options",
            "Una opción por línea" to "One option per line",
            "Crear actividad" to "Create activity",
            "Confirmar" to "Confirm",
            "Mantener abierta" to "Keep open",
            "Resultados finales" to "Final results",
            "Resultados actuales" to "Current results",
            "Resultado final" to "Final result",
            "Aún no hay votos." to "There are no votes yet.",
            "Más votada" to "Most voted",
            "opciones" to "options",
            "mínimo 2" to "minimum 2",
            "Publicada" to "Published",
            "Borrador" to "Draft",
            "Crea una pregunta clara para que el equipo pueda responderla." to
                "Create a clear question for the team to answer.",
            "Las personas podrán añadir contexto a su respuesta." to
                "People will be able to add context to their answer.",
            "Crea una votación con al menos dos alternativas." to
                "Create a vote with at least two options.",
            "Perfil" to "Profile",
            "Abrir ajustes" to "Open settings",
            "Tu espacio personal de bienestar" to "Your personal wellbeing space",
            "Usuario" to "Username",
            "Nombre" to "Name",
            "ej. Carlos Mendoza" to "e.g. Carlos Mendoza",
            "Usa entre 2 y 100 caracteres." to "Use between 2 and 100 characters.",
            "Usa entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo." to
                "Use 3 to 50 characters: letters, numbers, dots, hyphens or underscores.",
            "Usa un correo válido para mantener tu cuenta segura." to
                "Use a valid email address to keep your account secure.",
            "No disponible" to "Not available",
            "Rol" to "Role",
            "Foto de perfil" to "Profile photo",
            "Cambiar foto de perfil" to "Change profile photo",
            "Configuración" to "Settings",
            "CUENTA" to "ACCOUNT",
            "Cambiar nombre" to "Change name",
            "Cambiar nombre de usuario" to "Change username",
            "Cambiar correo" to "Change email",
            "Cambiar contraseña" to "Change password",
            "Disponible próximamente" to "Available soon",
            "PREFERENCIAS" to "PREFERENCES",
            "Seleccionar idioma" to "Select language",
            "Español" to "Spanish",
            "Tema oscuro" to "Dark theme",
            "Activado" to "Enabled",
            "Cambiar entre claro y oscuro" to "Switch between light and dark",
            "ZONA DE PELIGRO" to "DANGER ZONE",
            "Cerrar sesión" to "Sign out",
            "Finalizar esta sesión en el dispositivo" to "End this session on the device",
            "Guardar" to "Save",
            "Editar nombre" to "Edit name",
            "Eliminar conversación" to "Delete conversation",
            "Opciones de conversación" to "Conversation options",
            "Nombre de la conversación" to "Conversation name",
            "Eliminar" to "Delete",
            "Historial de conversaciones" to "Conversation history",
            "Aún no hay conversaciones" to "No conversations yet",
            "Regresar" to "Back",
            "Volver" to "Back",
            "Encuesta Diaria" to "Daily Survey",
            "Actividad Semanal" to "Weekly Activity",
            "Regresar al Centro de Encuestas" to "Back to Survey Center",
            "Respuesta registrada" to "Response recorded",
            "Respondida" to "Answered",
            "Pendiente" to "Pending",
            "Respuestas" to "Responses",
            "respuestas" to "responses",
            "Comentarios habilitados" to "Comments enabled",
            "Solo respuestas" to "Responses only",
            "Cargando" to "Loading",
            "Sin encuestas" to "No surveys",
            "Aún no hay encuestas creadas." to "No surveys have been created yet.",
            "Aún no hay actividades creadas." to "No activities have been created yet.",
            "Escribe tu mensaje..." to "Write your message...",
            "Enviar mensaje" to "Send message",
            "Asistente" to "Assistant",
            "El asistente está escribiendo" to "The assistant is typing",
            "Orientación general; no sustituye la atención profesional." to
                "General guidance; it does not replace professional care.",
            "+ Nueva" to "+ New",
            "Hoy" to "Today",
            "Escribe algo cuando quieras comenzar la conversación." to
                "Write something whenever you want to start the conversation.",
            "Selecciona una conversación para continuar." to "Select a conversation to continue.",
            "Toca para abrir el chat" to "Tap to open the chat",
            "Nueva conversación" to "New conversation",
            "Reportes" to "Reports",
            "Gestión de Reportes" to "Report Management",
            "Detalle del reporte" to "Report details",
            "Actualizar reportes" to "Refresh reports",
            "Bandeja de entrada segura y confidencial." to "Secure and confidential inbox.",
            "Todos" to "All",
            "En revisión" to "In review",
            "Atendido" to "Addressed",
            "Ver detalle" to "View details",
            "Cambiar estado" to "Change status",
            "Información del reporte" to "Report information",
            "Los campos marcados son necesarios para poder revisar tu caso." to
                "Marked fields are required to review your case.",
            "Puesto de trabajo" to "Work area",
            "Selecciona tu área" to "Select your area",
            "Selecciona el área a la que perteneces" to "Select the area you belong to",
            "Título del problema" to "Problem title",
            "Describe brevemente el problema" to "Briefly describe the problem",
            "Descripción del problema" to "Problem description",
            "Escribe todos los detalles relevantes..." to "Write all relevant details...",
            "Prioridad" to "Priority",
            "Selecciona una prioridad" to "Select a priority",
            "Selecciona el nivel de prioridad." to "Select a priority level.",
            "Selecciona tu puesto de trabajo." to "Select your work area.",
            "Escribe un título para identificar el reporte." to "Enter a title to identify the report.",
            "Describe brevemente lo que ocurrió." to "Briefly describe what happened.",
            "Resume el caso en pocas palabras" to "Summarize the case in a few words",
            "Cuéntanos qué ocurrió y cómo podemos ayudarte..." to "Tell us what happened and how we can help...",
            "Nivel de prioridad" to "Priority level",
            "Selecciona qué tan urgente consideras este reporte." to "Select how urgent you consider this report.",
            "Tu nombre podrá ser visible para el equipo de RR. HH." to "Your name may be visible to the HR team.",
            "Usa este espacio para comunicar una situación real de forma respetuosa." to
                "Use this space to communicate a real situation respectfully.",
            "TI" to "IT",
            "Atención al cliente" to "Customer service",
            "Finanzas" to "Finance",
            "Recursos humanos" to "Human resources",
            "Operaciones" to "Operations",
            "Marketing" to "Marketing",
            "Baja" to "Low",
            "Normal" to "Normal",
            "Alta" to "High",
            "Urgente" to "Urgent",
            "Crítica" to "Critical",
            "Categoría o puesto" to "Category or work area",
            "Identidad del reporte" to "Report identity",
            "Identificado" to "Identified",
            "Cerrar detalle" to "Close details",
            "Fecha no disponible" to "Date unavailable",
            "Ahora" to "Now",
            "Ayer" to "Yesterday",
            "minuto" to "minute",
            "minutos" to "minutes",
            "hora" to "hour",
            "horas" to "hours",
            "días" to "days",
            "atrás" to "ago",
            "No se pudo cargar tu perfil." to "Your profile could not be loaded.",
            "No se pudieron actualizar las preferencias." to "Preferences could not be updated.",
            "No se pudieron actualizar tus datos." to "Your account details could not be updated.",
            "No se pudieron cargar las encuestas." to "Surveys could not be loaded.",
            "No se pudieron cargar los comentarios." to "Comments could not be loaded.",
            "Escribe una respuesta antes de enviarla." to "Write an answer before submitting it.",
            "Respuesta enviada correctamente." to "Response submitted successfully.",
            "Escribe un comentario antes de publicarlo." to "Write a comment before posting it.",
            "Comentario publicado." to "Comment posted.",
            "No se pudo marcar el comentario." to "The comment could not be marked.",
            "No se pudieron cargar las actividades." to "Activities could not be loaded.",
            "No se pudo registrar el voto." to "The vote could not be recorded.",
            "No se pudieron cargar tus conversaciones." to "Your conversations could not be loaded.",
            "No se pudo crear la conversación." to "The conversation could not be created.",
            "No se pudo cambiar el nombre." to "The name could not be changed.",
            "No se pudo eliminar la conversación." to "The conversation could not be deleted.",
            "Crea o selecciona una conversación primero." to "Create or select a conversation first.",
            "No se pudo enviar el mensaje." to "The message could not be sent.",
            "El asistente no está disponible en este momento. Intenta nuevamente." to
                "The assistant is unavailable right now. Try again.",
            "No se pudo cargar el historial." to "The history could not be loaded.",
            "Reporte enviado correctamente." to "Report submitted successfully.",
            "No se pudo enviar el reporte." to "The report could not be submitted.",
            "No se pudieron cargar los reportes." to "Reports could not be loaded.",
            "Estado del reporte actualizado." to "Report status updated.",
            "No se pudo actualizar el reporte." to "The report could not be updated.",
            "No se pudo cargar el resumen de bienestar." to "The wellbeing summary could not be loaded.",
            "No se pudo cargar tu estado de ánimo." to "Your mood could not be loaded.",
            "No se pudo guardar tu estado de ánimo." to "Your mood could not be saved.",
            "No se pudo cargar el contenido administrable." to "Managed content could not be loaded.",
            "No se pudo crear la encuesta." to "The survey could not be created.",
            "No se pudo cambiar el estado de la encuesta." to "The survey status could not be changed.",
            "No se pudo crear la actividad." to "The activity could not be created.",
            "No se pudo cerrar la actividad." to "The activity could not be closed.",
            "No se pudo conectar con el servidor." to "Could not connect to the server.",
            "La operación entra en conflicto con un registro existente." to
                "The operation conflicts with an existing record.",
            "Tu sesión ya no es válida. Inicia sesión nuevamente." to
                "Your session is no longer valid. Sign in again.",
            "No se encontró la información solicitada." to "The requested information was not found.",
            "No tienes permisos para realizar esta acción." to "You do not have permission to perform this action.",
            "El servidor no pudo completar la operación." to "The server could not complete the operation.",
            "El usuario o la contraseña no son válidos." to "The username or password is not valid.",
            "No se pudo enviar la respuesta." to "The answer could not be submitted.",
            "No se pudo publicar el comentario." to "The comment could not be posted.",
            "Ya respondiste esta encuesta." to "You already answered this survey.",
            "Esta encuesta ya no está disponible." to "This survey is no longer available.",
            "Actividad cerrada." to "Activity closed.",
            "Encuesta creada como borrador." to "Survey created as a draft.",
            "Estado de la encuesta actualizado." to "Survey status updated.",
            "Escribe un título y al menos dos opciones." to "Enter a title and at least two options.",
            "Completa el título y la pregunta." to "Complete the title and question.",
            "Enviar de forma anónima" to "Submit anonymously",
            "Tu identidad no se mostrará al equipo de RR. HH." to "Your identity will not be shown to HR.",
            "Enviar reporte" to "Submit report",
            "No hay reportes registrados." to "No reports registered.",
            "No hay reportes en este estado." to "There are no reports in this status.",
            "Detalles del reporte" to "Report details",
            "Descripción" to "Description",
            "Categoría" to "Category",
            "Reportado por" to "Reported by",
            "Anónimo" to "Anonymous",
            "Fecha" to "Date",
            "Estado" to "Status",
            "Nuevo" to "New",
            "Cerrado" to "Closed",
            "No hay actividades abiertas por ahora." to "There are no open activities right now.",
            "Todavía no hay encuestas publicadas." to "There are no published surveys yet.",
            "Elige una opción y consulta cómo avanza la votación." to
                "Choose an option and see how the vote is progressing.",
            "Reintentar" to "Try again",
            "Error" to "Error",
            "No se pudo cargar la información." to "The information could not be loaded.",
            "Resumen de Bienestar" to "Wellbeing Summary",
            "Estado Emocional General" to "Overall Emotional State",
            "Distribución de respuestas de la plantilla" to "Team response distribution",
            "Positivo" to "Positive",
            "Sin datos" to "No data",
            "Empleados Activos" to "Active Employees",
            "Total en el ecosistema" to "Total in the ecosystem",
            "Tasa de respuesta" to "Response rate",
            "Empleados activos" to "Active employees",
            "Bienestar" to "Wellbeing",
            "Miembro de Recursos Humanos" to "Human Resources member",
            "Empleado" to "Employee",
        )
    }
}

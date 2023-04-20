package no.nav.mulighetsrommet.slack

interface SlackNotifier {
    fun sendMessage(message: String)
}

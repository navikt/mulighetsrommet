package no.nav.mulighetsrommet.slack_notifier

interface SlackNotifier {
    fun sendMessage(message: String)
}

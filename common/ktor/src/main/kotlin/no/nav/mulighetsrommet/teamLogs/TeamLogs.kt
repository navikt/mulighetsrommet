package no.nav.mulighetsrommet.teamLogs

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.MarkerFactory

private val TeamLogsMarker: Marker = MarkerFactory.getMarker("TEAM_LOGS")

fun Logger.teamLogsInfo(message: String) = this.info(TeamLogsMarker, message)

fun Logger.teamLogsError(message: String) = this.error(TeamLogsMarker, message)

fun Logger.teamLogsError(message: String, throwable: Throwable) = this.error(TeamLogsMarker, message, throwable)

fun Logger.teamLogsError(format: String, obj: Any) = this.error(TeamLogsMarker, format, obj)

fun Logger.teamLogsWarn(message: String) = this.warn(TeamLogsMarker, message)

fun Logger.teamLogsWarn(message: String, obj: Any) = this.warn(TeamLogsMarker, message, obj)

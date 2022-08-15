package no.nav.mulighetsrommet.api.utils

fun replaceEnhetInQuery(query: String, enhetsId: String, fylkeId: String): String {
    /*return query.replace(
        oldValue = "%ENHET%",
        newValue = "&& ((\"${enhetsId}\" in enheter[]->nummer.current) || (enheter[0] == null && \"${fylkeId}\" == fylke->nummer.current))"
    )*/
    return "$query&\$enhetsId=$enhetsId&\$fylkeId=$fylkeId"
}

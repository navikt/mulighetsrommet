plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

base {
    archivesName = "mulighetsrommet-api-persistence"
}

dependencies {
    implementation(projects.mulighetsrommetApi.domain)
    implementation(projects.common.database)
    implementation(projects.common.databaseHelpers)

    testFixturesImplementation(projects.common.database)
    testFixturesImplementation(testFixtures(projects.common.database))
}

tasks.test {
    useJUnitPlatform()
}

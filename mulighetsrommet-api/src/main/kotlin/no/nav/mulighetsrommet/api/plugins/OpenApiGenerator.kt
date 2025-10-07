package no.nav.mulighetsrommet.api.plugins

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.OutputFormat
import io.github.smiley4.ktoropenapi.config.RequestConfig
import io.github.smiley4.ktoropenapi.config.RequestParameterConfig
import io.github.smiley4.ktoropenapi.config.SchemaOverwriteModule
import io.github.smiley4.schemakenerator.core.CoreSteps.handleNameAnnotation
import io.github.smiley4.schemakenerator.serialization.SerializationSteps.addJsonClassDiscriminatorProperty
import io.github.smiley4.schemakenerator.serialization.SerializationSteps.analyzeTypeUsingKotlinxSerialization
import io.github.smiley4.schemakenerator.swagger.SwaggerSteps
import io.github.smiley4.schemakenerator.swagger.SwaggerSteps.compileReferencingRoot
import io.github.smiley4.schemakenerator.swagger.SwaggerSteps.generateSwaggerSchema
import io.github.smiley4.schemakenerator.swagger.SwaggerSteps.handleSchemaAnnotations
import io.github.smiley4.schemakenerator.swagger.SwaggerSteps.mergePropertyAttributesIntoType
import io.github.smiley4.schemakenerator.swagger.data.RefType
import io.ktor.server.application.*
import io.swagger.v3.oas.models.media.Schema
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattAuthorizationRouteSelector
import no.nav.mulighetsrommet.api.routes.OpenApiSpec
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.PortableTextTypedObject
import no.nav.mulighetsrommet.model.ProblemDetail

fun RequestConfig.pathParameterUuid(name: String, block: RequestParameterConfig.() -> Unit = {}) {
    val uuidSchema = Schema<Any>().also {
        it.types = setOf("string")
        it.format = "uuid"
    }
    pathParameter(name, uuidSchema, block)
}

fun RequestConfig.queryParameterUuid(name: String, block: RequestParameterConfig.() -> Unit = {}) {
    val uuidSchema = Schema<Any>().also {
        it.types = setOf("string")
        it.format = "uuid"
    }
    queryParameter(name, uuidSchema, block)
}

fun Application.configureOpenApiGenerator() {
    install(OpenApi) {
        outputFormat = OutputFormat.YAML

        pathFilter = { _, urlParts ->
            val url = urlParts.joinToString("/", prefix = "/")
            OpenApiSpec.match(url) != null
        }

        specAssigner = { url, _ ->
            OpenApiSpec.match(url)?.specName
                ?: throw IllegalStateException("Failed to resolve OpenApiSpec for $url")
        }

        ignoredRouteSelectors = setOf(
            NavAnsattAuthorizationRouteSelector::class,
        )

        schemas {
            /**
             * Denne implementasjonen er basert på implementasjonen til [io.github.smiley4.ktoropenapi.config.SchemaGenerator.kotlinx].
             * Noen enddringer har blitt gjort fra "standard" implementasjon, samt noen ting er verdt å være klar over
             * ifm. generering av openapi-dokumentasjon:
             * - Noen steg har blitt fjernet da de uansett ikke er i bruk i dette prosjektet (f.eks. prosessering av noen annotasjoner, eller generering av titles)
             */
            generator = { type ->
                type
                    .analyzeTypeUsingKotlinxSerialization {
                        customModules.addAll(customSchemaOverwrites)
                    }
                    .addJsonClassDiscriminatorProperty()
                    .handleNameAnnotation()
                    .generateSwaggerSchema {
                        /**
                         * Optional constructor-felter blir required i generert skjema hvis selve typen ikke er nullable
                         */
                        optionals = SwaggerSteps.RequiredHandling.REQUIRED

                        /**
                         * Nullable felter blir required i generert skjema (men fortsatt nullable)
                         */
                        nullables = SwaggerSteps.RequiredHandling.REQUIRED

                        customModules.addAll(customSchemaOverwrites)
                    }
                    .handleSchemaAnnotations()
                    .mergePropertyAttributesIntoType()
                    .compileReferencingRoot(
                        /**
                         * Sørger for at "null" blir en del av gyldige typer for nullable felter i generert json schema
                         */
                        explicitNullTypes = true,
                        pathType = RefType.OPENAPI_SIMPLE,
                    )
            }
        }
    }
}

/**
 * Noen modeller trenger litt hjelp til å representeres riktig i OpenAPI.
 */
private val customSchemaOverwrites: List<SchemaOverwriteModule> = listOf(
    SchemaOverwriteModule(
        identifier = "UUIDCustom",
        schema = {
            Schema<Any>().also {
                it.types = setOf("string")
                it.format = "uuid"
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = "LocalDateTime",
        schema = {
            Schema<Any>().also {
                it.types = setOf("string")
                it.format = "date-time"
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = "LocalDate",
        schema = {
            Schema<Any>().also {
                it.types = setOf("string")
                it.format = "date"
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = "java.time.Instant",
        schema = {
            Schema<Any>().also {
                it.types = setOf("string")
                it.format = "date-time"
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = "kotlin.ByteArray",
        schema = {
            Schema<Any>().also {
                it.types = setOf("string")
                it.format = "binary"
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = Vedlegg::class.qualifiedName!!,
        schema = {
            Schema<Vedlegg>().apply {
                types = setOf("string")
                format = "binary"
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = "PortableTextTypedObject",
        schema = {
            Schema<PortableTextTypedObject>().apply {
                type = "object"
                addProperty(
                    "_type",
                    Schema<String>().apply {
                        types = setOf("string")
                    },
                )
                addProperty(
                    "_key",
                    Schema<String>().apply {
                        types = setOf("string")
                    },
                )
                required = listOf("_type")
                additionalProperties = Schema<Any>()
            }
        },
    ),
    SchemaOverwriteModule(
        identifier = "ProblemDetail",
        schema = {
            Schema<ProblemDetail>().apply {
                type = "object"
                addProperty(
                    "type",
                    Schema<String>().apply {
                        types = setOf("string")
                    },
                )
                addProperty(
                    "title",
                    Schema<String>().apply {
                        types = setOf("string")
                    },
                )
                addProperty(
                    "status",
                    Schema<String>().apply {
                        types = setOf("integer")
                    },
                )
                addProperty(
                    "detail",
                    Schema<String>().apply {
                        types = setOf("string")
                    },
                )
                addProperty(
                    "instance",
                    Schema<String>().apply {
                        types = setOf("string")
                    },
                )
                required = listOf("type", "title", "status", "detail")
                additionalProperties = Schema<Any>()
            }
        },
    ),
)

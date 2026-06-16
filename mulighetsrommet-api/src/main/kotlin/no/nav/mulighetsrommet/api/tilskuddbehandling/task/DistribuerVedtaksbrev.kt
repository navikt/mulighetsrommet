package no.nav.mulighetsrommet.api.tilskuddbehandling.task

/*
class DistribuerVedtaksbrev(
    private val db: ApiDatabase,
    private val dokdistClient: DokdistClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val vedtakId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler<TaskData>(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            distribuerDok(inst.data.vedtakId).onLeft { message ->
                throw Exception("Feil distribuering av vedtaksbrev for vedtak id=${inst.data.vedtakId}: $message")
            }
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(tilskuddBehandlingId: UUID, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(tilskuddBehandlingId))
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun distribuerDok(tilskuddBehandlingId: UUID): Either<DokdistError, DokdistResponse> = db.transaction {
        logger.info("Distribuerer journalpost for vedtak id: $tilskuddBehandlingId")

        val tilskudd = queries.tilskuddBehandling.getOrError(tilskuddBehandlingId)
        require(tilskudd.journalpost?.id != null) { "Vedtak med id=$tilskuddBehandlingId har ingen journalpostId, distribuering ikke mulig" }

        dokdistClient.distribuerJournalpost(
            journalpostId = tilskudd.journalpost.id,
            accessType = AccessType.M2M,
            distribusjonstype = DokdistRequest.DistribusjonsType.ANNET,
            adresse = null
        ).map { response ->
            queries.tilskuddBehandling.setJournalpostDistribueringId(tilskuddBehandlingId, response.bestillingsId)
            response
        }
    }

    fun hentUtenlandskArrangorAdresse(arrangorId: UUID): DokdistRequest.Adresse.UtenlandskPostadresse = db.session {
        val utenlandskArrangor = queries.arrangor.getUtenlandskArrangor(arrangorId)
        require(utenlandskArrangor != null) { "Utenlandsk arrangør med id=$arrangorId mangler informasjon, kunne ikke hente adresse" }
        DokdistRequest.Adresse.UtenlandskPostadresse(
            land = utenlandskArrangor.landKode,
            adresselinje1 = utenlandskArrangor.gateNavn,
            adresselinje2 = utenlandskArrangor.by,
            adresselinje3 = null,
        )
    }
}*/

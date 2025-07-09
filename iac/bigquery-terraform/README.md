# bigquery-terraform

Terraform-konfigurasjon for å flytte data fra applikasjonsspesifikke databaser i Google Cloud SQL til Google BigQuery
med Google Datastream. Konfigurasjonen er basert på følgende oppsett:

- [flex-google-bigquery-datastream](https://github.com/navikt/terraform-google-bigquery-datastream)
- [flex-bigquery-terraform](https://github.com/navikt/flex-bigquery-terraform)
- [amt-bigquery-terraform](https://github.com/navikt/amt-bigquery-terraform)

Formålet med oppsettet er å lage og dele [dataprodukter](https://docs.knada.io/dataprodukter/) via Nada.

*OBS! Husk å se over resultatet av `terraform plan` før du merger noe til main!*

### Deploy

Infrastrukturen deployes kontinuerlig via Github Actions når koden merges til `main`, evt. så er det også mulig å
deploye manuelt via workflow dispatch.

Det har blitt opprettet en egen IAM Service Account som autentiserer med GCP via Workload Identity Federation, oppsettet
er beskrevet under.

### Manuelle steg

Under er det beskrevet en del manuelle steg som må til for å få satt opp replikering fra en Postgres-instans til
BigQuery.

#### Manuelt oppsett database

Det må opprettes databasebruker for datastreamen, databasebrukeren må gis tilgang, og det må settes opp replikering.
Dette er godt beskrevet her: https://github.com/navikt/nada-datastream?tab=readme-ov-file#forutsetninger-for-bruk.

*OBS! Ikke sett opp replikeringen før man nærmer seg klar til å lese inn dataene fra datastreamen, hvis ikke fyller
disken seg opp!*

#### Opprette secret for datastream-bruker

Det må opprettes en secret i Google Secret Manager som inneholder brukernavn og passord for databasebrukeren for
datastreamen (som ble opprettet i steget over).
Dette gjør du manuelt i GCP-konsollet, eller med scriptet definert under.
Formatet på secret'en er som følger:

```json
{
  "username": "<brukernavn>",
  "password": "<passord>"
}
```

Følgende script ble brukt til å opprette secret'en (krever `gcloud`, `kubectl` og `jq`):

```sh
# Variabler avhengig av miljøet
PROJECT=team-mulighetsrommet-dev-a2d7 # team-mulighetsrommet-prod-5492
CLUSTER=dev-gcp # prod-gcp
SECRET_NAME=mr-api-datastream-credential

# Database secrets for the datastream postgres user
gcloud secrets create $SECRET_NAME --replication-policy user-managed --project $PROJECT --locations europe-north1

kubectl --context $CLUSTER get secret google-sql-mulighetsrommet-api-mulighetsrommet-api-db--184f872d -o json \
    | jq -r '.data | map_values(@base64d) | { username: .DB_DATASTREAM_USERNAME, password: .DB_DATASTREAM_PASSWORD }' \
    | gcloud secrets versions add mr-api-datastream-credentials --project $PROJECT --data-file=-
```

#### Manuelt oppsett for Terraform

Det er noen ressurser som må opprettes manuelt i hvert GCP-prosjekt for å få Terraform til å fungere:

- Bucket for Terraform-tilstand
- ServiceAccount for Terraform
    - Med tilstrekkelige permissions for ressurser som skal opprettes
    - Opprett key i json-format, og ta vare på filen som lastes ned hvis du ønsker å kjøre terraform lokalt

Følgende script ble brukt til å opprette ressursene:

```sh
#!/bin/bash

# Variabler avhengig av miljøet
PROJECT=team-mulighetsrommet-dev-a2d7 # team-mulighetsrommet-prod-5492
TERRAFORM_BUCKET=gs://mr-terraform-state-dev # gs://mr-terraform-state-prod

# Bucket for terraform state
gcloud storage buckets create $TERRAFORM_BUCKET --project $PROJECT --location europe-north1

# Service account with permissions
gcloud iam service-accounts create terraform --project $PROJECT --display-name "Terraform" --description "Gir terraform nok tilganger til å opprette nødvendige ressurser for datastream fra Postgres til BigQuery"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/bigquery.admin"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/cloudsql.viewer"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/compute.instanceAdmin"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/compute.networkAdmin"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/compute.securityAdmin"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/datastream.admin"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/iam.serviceAccountUser"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/secretmanager.secretAccessor"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/secretmanager.viewer"
gcloud projects add-iam-policy-binding $PROJECT --member="serviceAccount:terraform@$PROJECT.iam.gserviceaccount.com" --condition="None" --role="roles/storage.admin"
```

#### Kjør terraform

Hvis du skal kjøre terraform lokalt trenger du credentials til service-accounten til terraform.
Disse kan opprettes og lastes ned fra GCP via console og kjør deretter `export GOOGLE_CREDENTIALS=/path/to/<key>.json` (
`<key>.json` er filen som inneholder json-keyen for service accounten for Terraform) for å gjøre nøkkelen tilgjengelig
for terraform.

*OBS! Ikke sjekk inn denne filen i git og slett den gjerne lokalt og i GCP om den ikke lengre trengs.*

Hvis alt har blitt satt opp riktig kan terraform kjøres som følger:

```sh
# Initialize Terraform and download necessary providers and modules
terraform init

# Preview infrastructure changes without applying them
terraform plan

# Apply the planned changes to create, update, or delete infrastructure
terraform apply

# List all resources currently tracked in the Terraform state
terraform state list

# Show detailed information about a specific resource from the state
terraform state show <resource_name>

# Destroy all managed infrastructure resources. Be careful!
terraform destroy
```

## Eksponere tabeller via datastream
Lag migreringer som:
 - Gir lesetilgang  `datastream` til tabellen
 - Legger dem til publiseringen `ds_publication`

```postgresql
do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'datastream')
        then
            grant select on <tabell_navn> to "datastream";
            alter publication "ds_publication" add table <tabell_navn>;
        end if;
    end
$$;
```

Legg deretter til ønsket tabell under `postgresql_include_schemas` i datastream konfigurasjonen.

### La brukere/service kontoer få tilgang til BigQuery datasett

Eksterne utenfor teamet må gis rollen `roles/bigquery.metadataViewer` for å kunne se datasettet.\
Dette vil gi de tilgang til metadata, som tabellnavn, hvilke kolonner som finnes og hvilke type data som er tilgjengelig.\
>***OBS!* Alle med denne rollen vil da også få tilgang til å lese ut data fra `authorized_views`**

### Logger - Datastream og BigQuery

Loggene finner du i [GCP Observability Monitor](https://console.cloud.google.com/monitoring).\
[#team-valp-monitoring](https://nav-it.slack.com/archives/C04QSFWJTMW) får alerts når det er noe muffins i monitor, konfiguerert via [Alert Policies](https://console.cloud.google.com/monitoring/alerting/policies).

### Feilsøking

- **Hvorfor feiler `terraform apply` første gang det kjøres?**\
    Første gang `terraform apply` kjøres så kan det være at
    bigquery views ikke blir opprettet. Dette skjer fordi de er avhengig av at tabellene finnes i dataset'et, men det vil
    typisk ta litt tid før disse tabellene blir opprettet av datastreamen. Det burde fungere å kjøre `terraform apply` på
    nytt når tabellene har blitt replikert til BigQuery.
- **Hvorfor ser jeg ikke oppdatert data i BigQuery?**\
    Standard innstilling for `bigquery_table_freshness` er satt til
    1t, som betyr at det kan ta like lang tid før endringer i data er tilgjengelig i BigQuery. Dette kan endres i
    Datastream-innstillingene, men merk at dette også øker kostnadene av tjenesten.
- **Hvorfor ser jeg endringer ved `terraform plan` i GCP som ikke er sjekket inn i kode?**\
    Kan oppstår når andre applikasjoner har gitt roller til våre ressurser uten at vi vet om det. Vær spesielt obs på dette for prod.\
    Eks:
  - *En servicekonto til metabase har dukket opp i access roles for en gitt ressurs.*\
     I dette tilfellet var grunnen at dataproduktet vårt var delt til Metabase via Datamarkedsplassen.
- **Hvorfor får vi feilmeldingen `UNSUPPORTED_EVENTS_DISCARDED` i loggene?**
  - Datastream talker ikke endringer av datatypen på kolonner i streamede tabeller. Har vi nylig gjort en database migrering?\
    **Fiks**:
    - Drop kolonnen i truffet tabell via [BigQuery](https://console.cloud.google.com/bigquery)
    - Kjør en `Initiate Backfill` av samme tabell via [Datastream -> Objects fanen](https://console.cloud.google.com/datastream/streams)

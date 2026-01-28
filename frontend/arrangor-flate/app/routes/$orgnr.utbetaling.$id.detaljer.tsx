import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Box,
  Button,
  Heading,
  HelpText,
  HStack,
  InlineMessage,
  Link,
  Modal,
  Spacer,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorAvbrytStatus,
  ArrangorflateService,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
  FieldError,
  UtbetalingTypeDto,
} from "api-client";
import { useEffect, useState } from "react";
import {
  ActionFunction,
  LoaderFunction,
  MetaFunction,
  useFetcher,
  useLoaderData,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { PageHeading } from "~/components/common/PageHeading";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { getEnvironment } from "~/services/environment";
import { tekster } from "~/tekster";
import { deltakerOversiktLenke, pathTo } from "~/utils/navigation";
import { isValidationError, problemDetailResponse } from "~/utils/validering";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { FeilmeldingMedVarselTrekant } from "../../../mr-admin-flate/src/components/skjema/FeilmeldingMedVarseltrekant";
import { DataDetails } from "@mr/frontend-common";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrangorflateUtbetalingDto;
  deltakerlisteUrl: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetaling | Detaljer" },
    { name: "description", content: "Arrangørflate for detaljer om en utbetaling" },
  ];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<UtbetalingDetaljerSideData> => {
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());
  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [{ data: utbetaling, error: utbetalingError }] = await Promise.all([
    ArrangorflateService.getArrangorflateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError) {
    throw problemDetailResponse(utbetalingError);
  }

  return { utbetaling, deltakerlisteUrl };
};

interface ActionData {
  errors?: FieldError[];
  ok: boolean;
}

export const action: ActionFunction = async ({ request, params }) => {
  const { id } = params;
  if (!id) throw new Response("Mangler id", { status: 400 });

  const formData = await request.formData();
  const intent = formData.get("_action");

  if (intent === "avbryt") {
    const begrunnelse = formData.get("begrunnelse")?.toString();

    const [{ error }] = await Promise.all([
      ArrangorflateService.avbrytUtbetaling({
        path: { id },
        body: { begrunnelse: begrunnelse ?? null },
        headers: await apiHeaders(request),
      }),
    ]);

    if (error) {
      if (isValidationError(error)) return { ok: false, errors: error.errors };
      throw problemDetailResponse(error);
    }

    return { ok: true };
  }

  if (intent === "regenerer") {
    const [{ error }] = await Promise.all([
      ArrangorflateService.regenererUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
    ]);

    if (error) {
      if (isValidationError(error)) return { ok: false, errors: error.errors };
      throw problemDetailResponse(error);
    }

    return { ok: true };
  }

  throw new Response("Ukjent handling", { status: 400 });
};

export default function UtbetalingDetaljerSide() {
  const { utbetaling, deltakerlisteUrl } = useLoaderData<UtbetalingDetaljerSideData>();
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [deltakerModalOpen, setDeltakerModalOpen] = useState<boolean>(false);

  const regenererFetcher = useFetcher<ActionData>();

  const visNedlastingAvKvittering = [
    ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    ArrangorflateUtbetalingStatus.UTBETALT,
  ].includes(utbetaling.status);

  return (
    <VStack gap="4">
      <HStack gap="2" align="end" justify="space-between">
        <PageHeading
          title="Detaljer"
          tilbakeLenke={{
            navn: tekster.bokmal.tilbakeTilOversikt,
            url: pathTo.utbetalinger,
          }}
        />
        <Spacer />
        {visNedlastingAvKvittering && (
          <Link
            href={`/${utbetaling.arrangor.organisasjonsnummer}/utbetaling/${utbetaling.id}/detaljer/lastned?filename=${tekster.bokmal.utbetaling.pdfNavn(utbetaling.periode.start)}`}
            target="_blank"
          >
            <FilePdfIcon />
            Last ned som PDF (Åpner i ny fane)
          </Link>
        )}
      </HStack>
      <UtbetalingHeader utbetalingType={utbetaling.type} />
      <DataDetails entries={utbetaling.innsendingsDetaljer} />
      <Definisjonsliste
        title={"Utbetaling"}
        definitions={[
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
          {
            key: "Utbetales tidligst",
            value: formaterDato(utbetaling.utbetalesTidligstDato) ?? "-",
          },
        ]}
      />
      <SatsPerioderOgBelop
        pris={utbetaling.beregning.pris}
        satsDetaljer={utbetaling.beregning.satsDetaljer}
      />
      {utbetaling.kanViseBeregning && (
        <HStack gap="2">
          <Button variant="secondary" size="small" onClick={() => setDeltakerModalOpen(true)}>
            Se deltakelser
          </Button>
        </HStack>
      )}
      <Definisjonsliste
        title="Betalingsinformasjon"
        definitions={[
          {
            key: "Kontonummer",
            value: utbetaling.betalingsinformasjon?.kontonummer
              ? formaterKontoNummer(utbetaling.betalingsinformasjon.kontonummer)
              : "-",
          },
          {
            key: "KID-nummer",
            value: utbetaling.betalingsinformasjon?.kid || "-",
          },
        ]}
      />
      <Box
        background="bg-subtle"
        padding="6"
        borderRadius="medium"
        borderColor="border-subtle"
        borderWidth={"1 1 1 1"}
      >
        <UtbetalingStatusList utbetaling={utbetaling} />
      </Box>
      {utbetaling.kanAvbrytes !== ArrangorAvbrytStatus.HIDDEN && (
        <HStack gap="2" justify="start" align="center">
          <Button
            disabled={utbetaling.kanAvbrytes === ArrangorAvbrytStatus.DEACTIVATED}
            size="small"
            variant="danger"
            onClick={() => setAvbrytModalOpen(true)}
          >
            Avbryt
          </Button>
          <HelpText>
            Du kan avbryte en innsending frem til Nav har startet behandling av kravet. Om det ikke
            er mulig å avbryte innsendingen må du ta kontakt direkte med Nav.
          </HelpText>
        </HStack>
      )}
      {utbetaling.kanRegenereres && (
        <regenererFetcher.Form method="post">
          <input type="hidden" name="_action" value="regenerer" />
          <HStack gap="2" justify="start" align="center">
            <Button
              type="submit"
              size="small"
              variant="primary"
              loading={regenererFetcher.state !== "idle"}
            >
              Opprett krav på nytt
            </Button>
          </HStack>
        </regenererFetcher.Form>
      )}
      {utbetaling.regenerertId && (
        <HStack gap="2" justify="start" align="center">
          <InlineMessage status="info">
            Krav om utbetaling for denne perioden er opprettet på nytt. Du finner kravet på
            oversikten over aktive utbetalingskrav
          </InlineMessage>
        </HStack>
      )}
      <AvbrytModal open={avbrytModalOpen} setOpen={setAvbrytModalOpen} />
      <DeltakerModal
        utbetaling={utbetaling}
        deltakerlisteUrl={deltakerlisteUrl}
        open={deltakerModalOpen}
        setOpen={setDeltakerModalOpen}
      />
    </VStack>
  );
}

function UtbetalingHeader({ utbetalingType }: { utbetalingType: UtbetalingTypeDto }) {
  const tekst = utbetalingType.displayNameLong ?? utbetalingType.displayName;
  return (
    <HStack gap="2">
      <Heading level="3" size="medium">
        {tekst}
      </Heading>
      <UtbetalingTypeTag type={utbetalingType.displayName} />
    </HStack>
  );
}

interface DeltakerModalProps {
  utbetaling: ArrangorflateUtbetalingDto;
  deltakerlisteUrl: string;
  open: boolean;
  setOpen: (a: boolean) => void;
}

function DeltakerModal({ utbetaling, deltakerlisteUrl, open, setOpen }: DeltakerModalProps) {
  return (
    <Modal
      open={open}
      size="medium"
      header={{ heading: utbetaling.beregning.displayName }}
      onClose={() => setOpen(false)}
      width="80rem"
      closeOnBackdropClick
    >
      <Modal.Body>
        <VStack gap="2">
          {utbetaling.beregning.stengt.length > 0 && (
            <Alert variant={"info"}>
              {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
              <ul>
                {utbetaling.beregning.stengt.map(({ periode, beskrivelse }) => (
                  <li key={periode.start + periode.slutt}>
                    {formaterPeriode(periode)}: {beskrivelse}
                  </li>
                ))}
              </ul>
            </Alert>
          )}
          <DeltakelserTable
            beregning={utbetaling.beregning}
            advarsler={utbetaling.advarsler}
            deltakerlisteUrl={deltakerlisteUrl}
          />
          <SatsPerioderOgBelop
            pris={utbetaling.beregning.pris}
            satsDetaljer={utbetaling.beregning.satsDetaljer}
          />
        </VStack>
      </Modal.Body>
    </Modal>
  );
}

interface AvbrytModalProps {
  open: boolean;
  setOpen: (a: boolean) => void;
}

function AvbrytModal({ open, setOpen }: AvbrytModalProps) {
  const fetcher = useFetcher<ActionData>();
  const errors = fetcher.data?.errors || [];
  const rootError = errors.find((error) => error.pointer === "/")?.detail;

  function onClose() {
    setOpen(false);
  }

  useEffect(() => {
    if (fetcher.data?.ok) {
      setOpen(false);
    }
  }, [fetcher.data, setOpen]);

  return (
    <Modal
      open={open}
      size="medium"
      header={{ heading: "Avbryt innsending" }}
      onClose={onClose}
      width="50rem"
      closeOnBackdropClick
    >
      <Modal.Body>
        <fetcher.Form method="post">
          <input type="hidden" name="_action" value="avbryt" />
          <VStack gap="2">
            <Alert variant={"info"}>
              Hvis kravet avbrytes, vil det ikke behandles av Nav og det vil ikke utbetales noe. Det
              kan være aktuelt hvis dere oppdager noe feil i innsendingen.
              <br />
              <br />
              Dere kan selv starte en ny innsending med korrekte opplysninger etter at kravet er
              avbrutt. Vær oppmerksom på at et avbrutt krav fremdeles vil være arkivert hos Nav.
            </Alert>
            <Textarea
              name="begrunnelse"
              description="Oppgi årsaken til at behandlingen av kravet skal avbrytes. Begrunnelsen blir lagret hos Nav"
              label="Begrunnelse"
              error={errors.find((error) => error.pointer === "/begrunnelse")?.detail}
              maxLength={100}
            />
            <HStack gap="4" justify="end">
              <Button type="button" variant="tertiary" size="small" onClick={onClose}>
                Nei, takk
              </Button>
              <Button
                type="submit"
                loading={fetcher.state !== "idle"}
                size="small"
                variant="danger"
              >
                Ja, jeg vil avbryte
              </Button>
            </HStack>
            {rootError && <FeilmeldingMedVarselTrekant>{rootError}</FeilmeldingMedVarselTrekant>}
          </VStack>
        </fetcher.Form>
      </Modal.Body>
    </Modal>
  );
}

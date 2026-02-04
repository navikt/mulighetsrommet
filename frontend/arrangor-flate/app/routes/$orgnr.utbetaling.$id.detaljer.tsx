import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Box,
  Button,
  Heading,
  HelpText,
  HStack,
  InlineMessage,
  LocalAlert,
  Modal,
  Spacer,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorAvbrytStatus,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
  FieldError,
  UtbetalingTypeDto,
} from "api-client";
import { Suspense, useState } from "react";
import { MetaFunction } from "react-router";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { PageHeading } from "~/components/common/PageHeading";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { getEnvironment } from "~/services/environment";
import { tekster } from "~/tekster";
import { deltakerOversiktLenke, pathTo, useIdFromUrl } from "~/utils/navigation";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { FeilmeldingMedVarselTrekant } from "../../../mr-admin-flate/src/components/skjema/FeilmeldingMedVarseltrekant";
import { DataDetails } from "@mr/frontend-common";
import { Laster } from "~/components/common/Laster";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useDownloadUtbetalingPdf } from "~/hooks/useDownloadUtbetalingPdf";
import { useAvbrytUtbetaling } from "~/hooks/useAvbrytUtbetaling";
import { useRegenerUtbetaling } from "~/hooks/useRegenerUtbetaling";

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetaling | Detaljer" },
    { name: "description", content: "Arrangørflate for detaljer om en utbetaling" },
  ];
};

export default function UtbetalingDetaljerSide() {
  const id = useIdFromUrl();

  return (
    <Suspense fallback={<Laster tekst="Laster detaljer..." size="xlarge" />}>
      <UtbetalingDetaljerContent id={id} />
    </Suspense>
  );
}

function UtbetalingDetaljerContent({ id }: { id: string }) {
  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());
  const downloadPdf = useDownloadUtbetalingPdf(id);
  const regenererUtbetaling = useRegenerUtbetaling(id);

  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [deltakerModalOpen, setDeltakerModalOpen] = useState<boolean>(false);

  const visNedlastingAvKvittering = [
    ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    ArrangorflateUtbetalingStatus.UTBETALT,
  ].includes(utbetaling.status);

  const handleDownloadPdf = () => {
    downloadPdf.mutate({
      filename: tekster.bokmal.utbetaling.pdfNavn(utbetaling.periode.start),
    });
  };

  const handleRegenerer = () => {
    regenererUtbetaling.mutate();
  };

  return (
    <Box background="default" borderRadius="8" padding="space-32">
      <VStack gap="space-16">
        <HStack gap="space-8" align="end" justify="space-between">
          <PageHeading
            title="Detaljer"
            tilbakeLenke={{
              navn: tekster.bokmal.tilbakeTilOversikt,
              url: pathTo.utbetalinger,
            }}
          />
          <Spacer />
          {visNedlastingAvKvittering && (
            <Button
              variant="tertiary"
              size="small"
              onClick={handleDownloadPdf}
              loading={downloadPdf.isPending}
              icon={<FilePdfIcon aria-hidden />}
            >
              Last ned som PDF
            </Button>
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
          <HStack gap="space-8">
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
          background="neutral-soft"
          padding="space-24"
          borderRadius="4"
          borderColor="neutral-subtle"
          borderWidth="1"
        >
          <UtbetalingStatusList utbetaling={utbetaling} />
        </Box>
        {utbetaling.kanAvbrytes !== ArrangorAvbrytStatus.HIDDEN && (
          <HStack gap="space-8" justify="start" align="center">
            <Button
              data-color="danger"
              disabled={utbetaling.kanAvbrytes === ArrangorAvbrytStatus.DEACTIVATED}
              size="small"
              variant="primary"
              onClick={() => setAvbrytModalOpen(true)}
            >
              Avbryt
            </Button>
            <HelpText>
              Du kan avbryte en innsending frem til Nav har startet behandling av kravet. Om det
              ikke er mulig å avbryte innsendingen må du ta kontakt direkte med Nav.
            </HelpText>
          </HStack>
        )}
        {utbetaling.kanRegenereres && (
          <HStack gap="space-8" justify="start" align="center">
            <Button
              type="button"
              size="small"
              variant="primary"
              loading={regenererUtbetaling.isPending}
              onClick={handleRegenerer}
            >
              Opprett krav på nytt
            </Button>
          </HStack>
        )}
        {utbetaling.regenerertId && (
          <HStack gap="space-8" justify="start" align="center">
            <InlineMessage status="info">
              Krav om utbetaling for denne perioden er opprettet på nytt. Du finner kravet på
              oversikten over aktive utbetalingskrav
            </InlineMessage>
          </HStack>
        )}
        <AvbrytModal id={id} open={avbrytModalOpen} setOpen={setAvbrytModalOpen} />
        <DeltakerModal
          utbetaling={utbetaling}
          deltakerlisteUrl={deltakerlisteUrl}
          open={deltakerModalOpen}
          setOpen={setDeltakerModalOpen}
        />
      </VStack>
    </Box>
  );
}

function UtbetalingHeader({ utbetalingType }: { utbetalingType: UtbetalingTypeDto }) {
  const tekst = utbetalingType.displayNameLong ?? utbetalingType.displayName;
  return (
    <HStack gap="space-8">
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
        <VStack gap="space-8">
          {utbetaling.beregning.stengt.length > 0 && (
            <LocalAlert status="announcement" size="small">
              <LocalAlert.Header>
                <LocalAlert.Title as="h4">Stengte perioder</LocalAlert.Title>
              </LocalAlert.Header>
              <LocalAlert.Content>
                <BodyShort spacing>
                  {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
                </BodyShort>
                <ul>
                  {utbetaling.beregning.stengt.map(({ periode, beskrivelse }) => (
                    <li key={periode.start + periode.slutt}>
                      {formaterPeriode(periode)}: {beskrivelse}
                    </li>
                  ))}
                </ul>
              </LocalAlert.Content>
            </LocalAlert>
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
  id: string;
  open: boolean;
  setOpen: (a: boolean) => void;
}

function AvbrytModal({ id, open, setOpen }: AvbrytModalProps) {
  const avbrytUtbetaling = useAvbrytUtbetaling(id);
  const [begrunnelse, setBegrunnelse] = useState("");
  const [errors, setErrors] = useState<FieldError[]>([]);

  const rootError = errors.find((error) => error.pointer === "/")?.detail;

  function onClose() {
    setOpen(false);
    setErrors([]);
    setBegrunnelse("");
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const result = await avbrytUtbetaling.mutateAsync({
      begrunnelse: begrunnelse || null,
    });

    if (result.errors) {
      setErrors(result.errors);
    } else if (result.success) {
      onClose();
    }
  };

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
        <form onSubmit={handleSubmit}>
          <VStack gap="space-8">
            <LocalAlert status={"announcement"}>
              <LocalAlert.Header>
                <LocalAlert.Title>Hva betyr det å avbryte en innsending?</LocalAlert.Title>
              </LocalAlert.Header>
              Hvis kravet avbrytes, vil det ikke behandles av Nav og det vil ikke utbetales noe. Det
              kan være aktuelt hvis dere oppdager noe feil i innsendingen.
              <br />
              <br />
              Dere kan selv starte en ny innsending med korrekte opplysninger etter at kravet er
              avbrutt. Vær oppmerksom på at et avbrutt krav fremdeles vil være arkivert hos Nav.
            </LocalAlert>
            <Textarea
              name="begrunnelse"
              description="Oppgi årsaken til at behandlingen av kravet skal avbrytes. Begrunnelsen blir lagret hos Nav"
              label="Begrunnelse"
              value={begrunnelse}
              onChange={(e) => setBegrunnelse(e.target.value)}
              error={errors.find((error) => error.pointer === "/begrunnelse")?.detail}
              maxLength={100}
            />
            <HStack gap="space-16" justify="end">
              <Button type="button" variant="tertiary" size="small" onClick={onClose}>
                Nei, takk
              </Button>
              <Button
                data-color="danger"
                type="submit"
                loading={avbrytUtbetaling.isPending}
                size="small"
                variant="primary"
              >
                Ja, jeg vil avbryte
              </Button>
            </HStack>
            {rootError && <FeilmeldingMedVarselTrekant>{rootError}</FeilmeldingMedVarselTrekant>}
          </VStack>
        </form>
      </Modal.Body>
    </Modal>
  );
}

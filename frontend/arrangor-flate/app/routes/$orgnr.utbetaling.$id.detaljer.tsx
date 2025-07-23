import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import { Alert, Box, Button, Heading, HStack, Link, Modal, Spacer, VStack } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrFlateBeregning,
  ArrFlateUtbetaling,
  ArrFlateUtbetalingStatus,
  Periode,
  RelevanteForslag,
  UtbetalingType,
} from "api-client";
import { LoaderFunction, MetaFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { getBeregningDetaljer } from "~/utils/beregning";
import css from "../root.module.css";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { getTimestamp } from "~/utils/date";
import { problemDetailResponse } from "~/utils/validering";
import { deltakerOversiktLenke, pathByOrgnr } from "~/utils/navigation";
import { PageHeading } from "~/components/common/PageHeading";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import { getEnvironment } from "~/services/environment";
import { useRef } from "react";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrFlateUtbetaling;
  relevanteForslag: RelevanteForslag[];
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

  const [
    { data: utbetaling, error: utbetalingError },
    { data: relevanteForslag, error: relevanteForslagError },
  ] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getRelevanteForslag({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  if (relevanteForslagError || !relevanteForslag) {
    throw problemDetailResponse(relevanteForslagError);
  }

  return { utbetaling, relevanteForslag, deltakerlisteUrl };
};

export default function UtbetalingDetaljerSide() {
  const { utbetaling, relevanteForslag, deltakerlisteUrl } =
    useLoaderData<UtbetalingDetaljerSideData>();

  const innsendtTidspunkt = getTimestamp(utbetaling);

  const visNedlastingAvKvittering = [
    ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    ArrFlateUtbetalingStatus.UTBETALT,
  ].includes(utbetaling.status);

  return (
    <VStack gap="4" className={css.side}>
      <HStack gap="2" className="max-w-[1250px]" align="end" justify="space-between">
        <PageHeading
          title="Detaljer"
          tilbakeLenke={{
            navn: tekster.bokmal.tilbakeTilOversikt,
            url: pathByOrgnr(utbetaling.arrangor.organisasjonsnummer).utbetalinger,
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
      <UtbetalingHeader type={utbetaling.type} />
      <Definisjonsliste
        definitions={[
          {
            key: innsendtTidspunkt.title,
            value: innsendtTidspunkt.value,
          },
          { key: "Tiltaksnavn", value: utbetaling.gjennomforing.navn },
          { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
        ]}
      />
      <Definisjonsliste
        title={"Utbetaling"}
        headingLevel="3"
        definitions={[
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
          ...getBeregningDetaljer(utbetaling.beregning),
        ]}
      />
      <DeltakerModal
        periode={utbetaling.periode}
        beregning={utbetaling.beregning}
        relevanteForslag={relevanteForslag}
        deltakerlisteUrl={deltakerlisteUrl}
      />
      <Definisjonsliste
        title="Betalingsinformasjon"
        headingLevel="3"
        definitions={[
          {
            key: "Kontonummer",
            value: utbetaling.betalingsinformasjon.kontonummer
              ? formaterKontoNummer(utbetaling.betalingsinformasjon.kontonummer)
              : "-",
          },
          {
            key: "KID-nummer",
            value: utbetaling.betalingsinformasjon.kid || "-",
          },
        ]}
      />
      <Box
        background="bg-subtle"
        padding="6"
        borderRadius="medium"
        borderColor="border-subtle"
        borderWidth={"1 1 1 1"}
        className="max-w-[1250px]"
      >
        <UtbetalingStatusList utbetaling={utbetaling} />
      </Box>
    </VStack>
  );
}

function UtbetalingHeader({ type }: { type: UtbetalingType | undefined }) {
  const tekst = type ? getUtbetalingTypeNavn(type) : "Innsending";
  return (
    <HStack gap="2">
      <Heading level="3" size="medium">
        {tekst}
      </Heading>
      {type && <UtbetalingTypeTag type={type} />}
    </HStack>
  );
}

function getUtbetalingTypeNavn(type: UtbetalingType) {
  switch (type) {
    case UtbetalingType.KORRIGERING:
      return "Korrigering";
    case UtbetalingType.INVESTERING:
      return "Utbetaling for investering";
  }
}

interface DeltakerModalProps {
  periode: Periode;
  beregning: ArrFlateBeregning;
  relevanteForslag: RelevanteForslag[];
  deltakerlisteUrl: string;
}

function DeltakerModal({
  periode,
  beregning,
  relevanteForslag,
  deltakerlisteUrl,
}: DeltakerModalProps) {
  const modalRef = useRef<HTMLDialogElement>(null);
  if (beregning.type === "FRI") {
    return null;
  }
  return (
    <HStack gap="2">
      <Button variant="secondary" size="small" onClick={() => modalRef.current?.showModal()}>
        Åpne deltakerliste
      </Button>
      <Modal
        ref={modalRef}
        size="medium"
        header={{ heading: "Deltakere" }}
        onClose={() => modalRef.current?.close()}
        width="80rem"
      >
        <Modal.Body>
          {beregning.stengt.length > 0 && (
            <Alert variant={"info"}>
              {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
              <ul>
                {beregning.stengt.map(({ periode, beskrivelse }) => (
                  <li key={periode.start + periode.slutt}>
                    {formaterPeriode(periode)}: {beskrivelse}
                  </li>
                ))}
              </ul>
            </Alert>
          )}
          <DeltakelserTable
            periode={periode}
            beregning={beregning}
            relevanteForslag={relevanteForslag}
            deltakerlisteUrl={deltakerlisteUrl}
          />
          <Definisjonsliste definitions={getBeregningDetaljer(beregning)} className="my-2" />
        </Modal.Body>
      </Modal>
    </HStack>
  );
}

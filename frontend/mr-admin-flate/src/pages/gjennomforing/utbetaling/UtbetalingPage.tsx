import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { v4 as uuidv4 } from "uuid";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { utbetalingLinjeCompareFn } from "@/utils/Utils";
import {
  Besluttelse,
  BesluttTotrinnskontrollRequest,
  DelutbetalingRequest,
  FieldError,
  OpprettDelutbetalingerRequest,
  Rolle,
  TilsagnDto,
  TilsagnStatus,
  Toggles,
  UtbetalingDto,
  UtbetalingLinje,
  ValidationError,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import {
  Accordion,
  Alert,
  Button,
  CopyButton,
  Heading,
  HGrid,
  HStack,
  VStack,
} from "@navikt/ds-react";
import { useParams } from "react-router";
import {
  beregningQuery,
  tilsagnTilUtbetalingQuery,
  utbetalingHistorikkQuery,
  utbetalingQuery,
} from "./utbetalingPageLoader";

import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useEffect, useState } from "react";
import { UtbetalingTypeText } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import UtbetalingBeregningView from "@/components/utbetaling/beregning/UtbetalingBeregningView";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { AarsakerOgForklaring } from "../tilsagn/AarsakerOgForklaring";
import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { AvbrytUtbetalingButton } from "@/components/utbetaling/AvbrytUtbetalingButton";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { useQueryClient } from "@tanstack/react-query";
import MindreBelopModal from "@/components/utbetaling/MindreBelopModal";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useBesluttAvbrytelse } from "@/api/utbetaling/useBesluttAvbrytelse";

function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useApiSuspenseQuery(utbetalingHistorikkQuery(utbetalingId));
  const { data: utbetaling } = useApiSuspenseQuery(utbetalingQuery(utbetalingId));
  const { data: tilsagn } = useApiSuspenseQuery(tilsagnTilUtbetalingQuery(utbetalingId));
  const { data: beregning } = useApiSuspenseQuery(beregningQuery({ navEnheter: [] }, utbetalingId));

  // @todo: This is quickfix. Figure out why it scrolls to the bottom on page load as a part of the broader frontend improvements
  useEffect(() => {
    window.scrollTo(0, 0); // Reset scroll position to the top
  }, []);

  return {
    gjennomforing,
    ansatt,
    historikk,
    tilsagn,
    utbetaling: utbetaling.utbetaling,
    handlinger: utbetaling.handlinger,
    linjer: utbetaling.linjer.toSorted(utbetalingLinjeCompareFn),
    beregning,
  };
}

function genrererUtbetalingLinjer(tilsagn: TilsagnDto[]): UtbetalingLinje[] {
  return tilsagn
    .filter((t) => t.status === TilsagnStatus.GODKJENT)
    .map((t) => ({
      belop: 0,
      tilsagn: t,
      gjorOppTilsagn: false,
      id: uuidv4(),
    }))
    .toSorted(utbetalingLinjeCompareFn);
}

export function UtbetalingPage() {
  const { gjennomforingId, utbetalingId } = useParams();
  const { gjennomforing, ansatt, historikk, tilsagn, utbetaling, linjer, beregning, handlinger } =
    useUtbetalingPageData();
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);
  const [linjerState, setLinjerState] = useState<UtbetalingLinje[]>(() =>
    linjer.length === 0 ? genrererUtbetalingLinjer(tilsagn) : linjer,
  );
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const { data: enableAvbrytUtbetaling } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_MIGRERING_OKONOMI_AVBRYT_UTBETALING,
  );
  const queryClient = useQueryClient();
  const besluttAvbrytelseMutation = useBesluttAvbrytelse();
  const [avvisModalOpen, setAvvisModalOpen] = useState<boolean>(false);

  function besluttAvbrytelse(request: BesluttTotrinnskontrollRequest) {
    besluttAvbrytelseMutation.mutate(
      {
        id: utbetaling.id,
        body: {
          ...request,
        },
      },
      {
        onSuccess: async () => {
          setErrors([]);
          await queryClient.invalidateQueries({
            queryKey: ["utbetaling", utbetaling.id],
            refetchType: "all",
          });
        },
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjerState.map((linje) => {
      return {
        id: linje.id,
        belop: linje.belop,
        gjorOppTilsagn: linje.gjorOppTilsagn,
        tilsagnId: linje.tilsagn.id,
      };
    });

    const body: OpprettDelutbetalingerRequest = {
      utbetalingId: utbetaling.id,
      delutbetalinger: delutbetalingReq,
      begrunnelseMindreBetalt,
    };

    setErrors([]);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: ["utbetaling", utbetaling.id],
          refetchType: "all",
        });
      },
      onValidationError: (error: ValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  function utbetalesTotal(): number {
    return linjerState.reduce((acc, d) => acc + d.belop, 0);
  }

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforingId}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>Utbetalinger</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <BankNoteFillIcon color="#2AA758" className="w-10 h-10" />
        <Heading size="large" level="1">
          Utbetaling for {gjennomforing.navn}
        </Heading>
      </HStack>
      <ContentBox>
        <WhitePaddedBox>
          <VStack gap="4">
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
            <VStack
              gap="4"
              id="kostnadsfordeling"
              padding="4"
              className="border-gray-300 border-1 rounded-lg"
            >
              {utbetaling.status.type === "AVBRUTT" && (
                <AarsakerOgForklaring
                  heading="Utbetaling avbrutt"
                  tekster={[`Dato ${formaterDato(utbetaling.status.tidspunkt)}`]}
                  aarsaker={utbetaling.status.aarsaker}
                  forklaring={utbetaling.status.forklaring}
                />
              )}
              {utbetaling.status.type === "TIL_AVBRYTELSE" && (
                <AarsakerOgForklaring
                  heading="Utbetaling til avbrytelse"
                  tekster={[`Dato ${formaterDato(utbetaling.status.tidspunkt)}`]}
                  aarsaker={utbetaling.status.aarsaker}
                  forklaring={utbetaling.status.forklaring}
                />
              )}
              <HGrid columns="1fr 1fr 0.25fr">
                <VStack>
                  <Heading size="medium" level="2" spacing data-testid="utbetaling-til-utbetaling">
                    Til utbetaling
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Status"
                      verdi={<UtbetalingStatusTag status={utbetaling.status} />}
                    />

                    <MetadataHorisontal
                      header="Utbetalingsperiode"
                      verdi={formaterPeriode(utbetaling.periode)}
                    />
                    {utbetaling.type && (
                      <MetadataHorisontal
                        header="Type"
                        verdi={<UtbetalingTypeText type={utbetaling.type} />}
                      />
                    )}
                    <MetadataHorisontal
                      header="Dato innsendt"
                      verdi={formaterDato(utbetaling.godkjentAvArrangorTidspunkt)}
                    />
                    <MetadataHorisontal header="Innsendt av" verdi={utbetaling.innsendtAv} />
                    <MetadataHorisontal
                      header={utbetalingTekster.beregning.belop.label}
                      verdi={formaterNOK(utbetaling.belop)}
                    />
                    {utbetaling.beskrivelse && (
                      <MetadataHorisontal
                        header="Begrunnelse for utbetaling"
                        verdi={utbetaling.beskrivelse}
                      />
                    )}
                    {utbetaling.begrunnelseMindreBetalt && (
                      <MetadataHorisontal
                        header="Begrunnelse for mindre utbetalt"
                        verdi={utbetaling.begrunnelseMindreBetalt}
                      />
                    )}
                  </VStack>
                </VStack>
                <VStack gap="4">
                  <Heading size="medium" level="2">
                    Betalingsinformasjon
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Kontonummer"
                      verdi={utbetaling.betalingsinformasjon?.kontonummer}
                    />
                    <MetadataHorisontal
                      header="KID (valgfritt)"
                      verdi={utbetaling.betalingsinformasjon?.kid || "-"}
                    />
                  </VStack>
                  {utbetaling.journalpostId ? (
                    <>
                      <Heading size="medium" level="2">
                        Journalføring
                      </Heading>
                      <VStack gap="2">
                        <MetadataHorisontal
                          header="Journalpost-ID i Gosys"
                          verdi={
                            <HStack align="center">
                              <CopyButton
                                size="small"
                                variant="action"
                                copyText={utbetaling.journalpostId}
                                title="Kopier journalpost-ID"
                              />
                              {utbetaling.journalpostId}
                            </HStack>
                          }
                        />
                      </VStack>
                    </>
                  ) : null}
                </VStack>
                <HStack justify="end" align="start">
                  <EndringshistorikkPopover>
                    <ViewEndringshistorikk historikk={historikk} />
                  </EndringshistorikkPopover>
                </HStack>
              </HGrid>
              {utbetaling.status.type != "AVBRUTT" && (
                <>
                  <Accordion>
                    <Accordion.Item>
                      <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
                      <Accordion.Content>
                        {utbetalingId && (
                          <UtbetalingBeregningView
                            utbetalingId={utbetalingId}
                            beregning={beregning}
                          />
                        )}
                      </Accordion.Content>
                    </Accordion.Item>
                  </Accordion>
                  {tilsagn.every(
                    (t) => ![TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT].includes(t.status),
                  ) && (
                    <Alert variant="info">
                      Det finnes ingen godkjente tilsagn for utbetalingsperioden
                    </Alert>
                  )}
                  <UtbetalingLinjeView
                    utbetaling={utbetaling}
                    tilsagn={tilsagn}
                    linjer={linjerState}
                    setLinjer={setLinjerState}
                    roller={ansatt.roller}
                  />
                </>
              )}
              <VStack gap="2">
                <HStack justify="space-between">
                  {handlinger.avbryt && enableAvbrytUtbetaling ? (
                    <AvbrytUtbetalingButton utbetaling={utbetaling} setErrors={setErrors} />
                  ) : (
                    <div></div>
                  )}
                  <HStack gap="2" justify="end">
                    {handlinger.sendAvbrytIRetur && (
                      <Button
                        variant="secondary"
                        size="small"
                        type="button"
                        onClick={() => setAvvisModalOpen(true)}
                      >
                        Send i retur
                      </Button>
                    )}
                    {handlinger.godkjennAvbryt && (
                      <Button
                        size="small"
                        type="button"
                        onClick={() =>
                          besluttAvbrytelse({
                            besluttelse: Besluttelse.GODKJENT,
                            aarsaker: [],
                            forklaring: null,
                          })
                        }
                      >
                        Godkjenn avbrytelse
                      </Button>
                    )}
                  </HStack>
                  {handlinger.sendTilAttestering && (
                    <Button
                      size="small"
                      type="button"
                      onClick={() => {
                        if (utbetalesTotal() < utbetaling.belop) {
                          setMindreBelopModalOpen(true);
                        } else {
                          sendTilGodkjenning();
                        }
                      }}
                    >
                      Send til attestering
                    </Button>
                  )}
                </HStack>
                <VStack gap="2" align="end">
                  {errors.map((error) => (
                    <Alert variant="error" size="small">
                      {error.detail}
                    </Alert>
                  ))}
                </VStack>
              </VStack>
            </VStack>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
      <MindreBelopModal
        open={mindreBelopModalOpen}
        handleClose={() => setMindreBelopModalOpen(false)}
        onConfirm={() => {
          setMindreBelopModalOpen(false);
          sendTilGodkjenning();
        }}
        begrunnelseOnChange={(e: any) => setBegrunnelseMindreBetalt(e.target.value)}
        belopUtbetaling={utbetalesTotal()}
        belopInnsendt={utbetaling.belop}
      />
      <AarsakerOgForklaringModal<"ANNET">
        aarsaker={[{ value: "ANNET", label: "Annet" }]}
        header="Send i retur med forklaring"
        buttonLabel="Send i retur"
        open={avvisModalOpen}
        errors={errors}
        onClose={() => setAvvisModalOpen(false)}
        onConfirm={({ aarsaker, forklaring }) => {
          setErrors([]);
          besluttAvbrytelse({
            besluttelse: Besluttelse.AVVIST,
            aarsaker,
            forklaring,
          });
          setAvvisModalOpen(false);
        }}
      />
    </>
  );
}

function UtbetalingLinjeView({
  utbetaling,
  tilsagn,
  linjer,
  roller,
  setLinjer,
}: {
  utbetaling: UtbetalingDto;
  tilsagn: TilsagnDto[];
  linjer: UtbetalingLinje[];
  roller: Rolle[];
  setLinjer: React.Dispatch<React.SetStateAction<UtbetalingLinje[]>>;
}) {
  switch (utbetaling.status.type) {
    case "AVBRUTT":
    case "VENTER_PA_ARRANGOR":
      return null;
    case "RETURNERT":
    case "KLAR_TIL_BEHANDLING":
      if (roller.includes(Rolle.SAKSBEHANDLER_OKONOMI)) {
        return (
          <RedigerUtbetalingLinjeView
            setLinjer={setLinjer}
            tilsagn={tilsagn}
            utbetaling={utbetaling}
            linjer={linjer}
          />
        );
      } else {
        return null;
      }
    case "TIL_ATTESTERING":
    case "OVERFORT_TIL_UTBETALING":
      if (roller.includes(Rolle.ATTESTANT_UTBETALING)) {
        return <BesluttUtbetalingLinjeView utbetaling={utbetaling} linjer={linjer} />;
      } else {
        return null;
      }
  }
}

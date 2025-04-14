import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { useSlettTilsagn } from "@/api/tilsagn/useSlettTilsagn";
import { useTilsagnTilAnnullering } from "@/api/tilsagn/useTilsagnTilAnnullering";
import { useTilsagnTilOppgjor } from "@/api/tilsagn/useTilsagnTilOppgjor";
import { Header } from "@/components/detaljside/Header";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { TilsagnDetaljerFri } from "@/pages/gjennomforing/tilsagn/detaljer/TilsagnDetaljerFri";
import {
  isTilsagnForhandsgodkjent,
  isTilsagnFri,
} from "@/pages/gjennomforing/tilsagn/tilsagnUtils";
import {
  formaterDato,
  isValidationError,
  navnEllerIdent,
  tilsagnAarsakTilTekst,
} from "@/utils/Utils";
import {
  Besluttelse,
  BesluttTilsagnRequest,
  FieldError,
  Rolle,
  ProblemDetail,
  TilsagnAvvisningAarsak,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TilsagnTilAnnulleringRequest,
} from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import {
  EraserIcon,
  PencilFillIcon,
  PiggybankIcon,
  TrashFillIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import { ActionMenu, Alert, Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { AarsakerOgForklaring } from "../AarsakerOgForklaring";
import { TilsagnTag } from "../TilsagnTag";
import { TilsagnDetaljerForhandsgodkjent } from "./TilsagnDetaljerForhandsgodkjent";
import { aktiveTilsagnQuery, tilsagnHistorikkQuery, tilsagnQuery } from "./tilsagnDetaljerLoader";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";

function useTilsagnDetaljer() {
  const { gjennomforingId, tilsagnId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: tilsagnDetaljer } = useSuspenseQuery({ ...tilsagnQuery(tilsagnId) });
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useSuspenseQuery({ ...tilsagnHistorikkQuery(tilsagnId) });
  const { data: aktiveTilsagn } = useSuspenseQuery({
    ...aktiveTilsagnQuery(gjennomforingId),
  });
  return {
    ansatt,
    gjennomforing,
    historikk,
    ...tilsagnDetaljer.data,
    aktiveTilsagn: aktiveTilsagn?.data.filter((x) => x.id !== tilsagnDetaljer.data.tilsagn.id),
  };
}

const tilAnnuleringAarsaker = [
  TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
  TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
  TilsagnTilAnnulleringAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES,
  TilsagnTilAnnulleringAarsak.FEIL_ANNET,
].map((aarsak) => ({
  value: aarsak,
  label: tilsagnAarsakTilTekst(aarsak),
}));

export function TilsagnDetaljer() {
  const { gjennomforingId } = useParams();
  const {
    ansatt,
    gjennomforing,
    tilsagn,
    opprettelse,
    annullering,
    tilOppgjor,
    historikk,
    aktiveTilsagn,
  } = useTilsagnDetaljer();

  const besluttMutation = useBesluttTilsagn();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const tilOppgjorMutation = useTilsagnTilOppgjor();
  const slettMutation = useSlettTilsagn();

  const navigate = useNavigate();
  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
  const [tilOppgjorModalOpen, setTilOppgjorModalOpen] = useState<boolean>(false);
  const [slettTilsagnModalOpen, setSlettTilsagnModalOpen] = useState<boolean>(false);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const [avvisAnnulleringModalOpen, setAvvisAnnulleringModalOpen] = useState(false);
  const [avvisGjorOppModalOpen, setAvvisGjorOppModalOpen] = useState(false);
  const [error, setError] = useState<FieldError[]>([]);

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Tilsagnsoversikt",
      lenke: `/gjennomforinger/${gjennomforing.id}/tilsagn`,
    },
    {
      tittel: "Tilsagn",
    },
  ];

  function navigerTilTilsagnTabell() {
    navigate(`/gjennomforinger/${gjennomforingId}/tilsagn`);
  }

  function besluttTilsagn(request: BesluttTilsagnRequest) {
    besluttMutation.mutate(
      {
        id: tilsagn.id,
        body: {
          ...request,
        },
      },
      {
        onSuccess: navigerTilTilsagnTabell,
        onError: (error: ProblemDetail) => {
          if (isValidationError(error)) {
            setError(error.errors);
          } else {
            throw error;
          }
        },
      },
    );
  }

  function tilAnnullering(request: TilsagnTilAnnulleringRequest) {
    tilAnnulleringMutation.mutate(
      {
        id: tilsagn.id,
        aarsaker: request.aarsaker,
        forklaring: request.forklaring || null,
      },
      { onSuccess: navigerTilTilsagnTabell },
    );
  }

  function upsertTilOppgjor(request: TilsagnTilAnnulleringRequest) {
    tilOppgjorMutation.mutate(
      {
        id: tilsagn.id,
        aarsaker: request.aarsaker,
        forklaring: request.forklaring || null,
      },
      { onSuccess: navigerTilTilsagnTabell },
    );
  }

  function slettTilsagn() {
    slettMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilTilsagnTabell });
  }

  const visHandlingerMeny =
    ansatt.roller.includes(Rolle.SAKSBEHANDLER_OKONOMI) &&
    [TilsagnStatus.RETURNERT, TilsagnStatus.GODKJENT].includes(tilsagn.status);

  const handlingsMeny = (
    <HStack gap="2" justify={"end"}>
      <EndringshistorikkPopover>
        <ViewEndringshistorikk historikk={historikk.data} />
      </EndringshistorikkPopover>
      {visHandlingerMeny ? (
        <ActionMenu>
          <ActionMenu.Trigger>
            <Button variant="primary" size="small">
              Handlinger
            </Button>
          </ActionMenu.Trigger>
          <ActionMenu.Content>
            {tilsagn.status === TilsagnStatus.RETURNERT && (
              <>
                <ActionMenu.Item icon={<PencilFillIcon />}>
                  <Link className="no-underline" to="./rediger-tilsagn">
                    Rediger tilsagn
                  </Link>
                </ActionMenu.Item>
                <ActionMenu.Item
                  variant="danger"
                  onSelect={() => setSlettTilsagnModalOpen(true)}
                  icon={<TrashIcon />}
                >
                  Slett tilsagn
                </ActionMenu.Item>
              </>
            )}
            {tilsagn.status === TilsagnStatus.GODKJENT &&
              (tilsagn.belopGjenstaende === tilsagn.beregning.output.belop ? (
                <>
                  <ActionMenu.Item
                    variant="danger"
                    onSelect={() => setTilAnnulleringModalOpen(true)}
                    icon={<EraserIcon />}
                  >
                    Annuller tilsagn
                  </ActionMenu.Item>
                </>
              ) : (
                <>
                  <ActionMenu.Item
                    variant="danger"
                    onSelect={() => setTilOppgjorModalOpen(true)}
                    icon={<EraserIcon />}
                  >
                    Gjør opp tilsagn
                  </ActionMenu.Item>
                </>
              ))}
          </ActionMenu.Content>
        </ActionMenu>
      ) : null}
    </HStack>
  );

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <PiggybankIcon className="w-10 h-10" />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Tilsagn for {gjennomforing.navn} <TilsagnTag status={tilsagn.status} />
          </HStack>
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <VStack gap="6">
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} meny={handlingsMeny} />
            <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
            {annullering?.type === "TIL_BESLUTNING" && (
              <AarsakerOgForklaring
                heading="Tilsagnet annulleres"
                tekster={[
                  `${navnEllerIdent(annullering.behandletAv)} sendte tilsagnet til annullering den ${formaterDato(
                    annullering.behandletTidspunkt,
                  )} med følgende årsaker:`,
                ]}
                aarsaker={
                  annullering.aarsaker?.map((aarsak) =>
                    tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                  ) ?? []
                }
                forklaring={annullering.forklaring}
              />
            )}
            {annullering?.type === "BESLUTTET" &&
              annullering.besluttelse === "AVVIST" &&
              !tilOppgjor && (
                <AarsakerOgForklaring
                  heading="Annullering returnert"
                  tekster={[
                    `${navnEllerIdent(annullering.besluttetAv)} returnerte annullering den ${formaterDato(
                      annullering.behandletTidspunkt,
                    )} med følgende årsaker:`,
                  ]}
                  aarsaker={
                    annullering.aarsaker?.map((aarsak) =>
                      tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                    ) ?? []
                  }
                  forklaring={annullering.forklaring}
                />
              )}
            {tilOppgjor?.type === "TIL_BESLUTNING" && (
              <AarsakerOgForklaring
                heading="Tilsagnet gjøres opp"
                tekster={[
                  "Gjenstående beløp gjøres opp uten at det gjøres en utbetaling",
                  `${navnEllerIdent(tilOppgjor.behandletAv)} sendte tilsagnet til oppgjør den ${formaterDato(
                    tilOppgjor.behandletTidspunkt,
                  )} med følgende årsaker:`,
                ]}
                aarsaker={
                  tilOppgjor.aarsaker?.map((aarsak) =>
                    tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                  ) ?? []
                }
                forklaring={tilOppgjor.forklaring}
              />
            )}
            {tilOppgjor?.type === "BESLUTTET" && tilOppgjor.besluttelse === "AVVIST" && (
              <AarsakerOgForklaring
                heading="Oppgjør returnert"
                tekster={[
                  `${navnEllerIdent(tilOppgjor.besluttetAv)} returnerte oppgjør den ${formaterDato(
                    tilOppgjor.behandletTidspunkt,
                  )} med følgende årsaker:`,
                ]}
                aarsaker={
                  tilOppgjor.aarsaker?.map((aarsak) =>
                    tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                  ) ?? []
                }
                forklaring={tilOppgjor.forklaring}
              />
            )}

            <Box borderColor="border-subtle" padding="4" borderWidth="1" borderRadius="large">
              <ContentBox>
                <WhitePaddedBox>
                  <VStack gap="2">
                    {isTilsagnForhandsgodkjent(tilsagn) && (
                      <TilsagnDetaljerForhandsgodkjent
                        tilsagn={tilsagn}
                        annullering={annullering}
                        oppgjor={tilOppgjor}
                      />
                    )}
                    {isTilsagnFri(tilsagn) && (
                      <TilsagnDetaljerFri
                        tilsagn={tilsagn}
                        annullering={annullering}
                        oppgjor={tilOppgjor}
                      />
                    )}
                    <div>
                      <HStack gap="2" justify={"end"}>
                        {opprettelse.kanBesluttes && (
                          <HStack gap="2">
                            <Button
                              variant="secondary"
                              size="small"
                              type="button"
                              onClick={() => setAvvisModalOpen(true)}
                            >
                              Send i retur
                            </Button>
                            <Button
                              size="small"
                              type="button"
                              onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                            >
                              Godkjenn tilsagn
                            </Button>
                          </HStack>
                        )}
                        {annullering?.kanBesluttes && (
                          <HStack gap="2">
                            <Button
                              variant="secondary"
                              size="small"
                              type="button"
                              onClick={() => setAvvisAnnulleringModalOpen(true)}
                            >
                              Avslå annullering
                            </Button>
                            <Button
                              size="small"
                              variant="danger"
                              type="button"
                              onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                            >
                              Bekreft annullering
                            </Button>
                          </HStack>
                        )}
                        {tilOppgjor?.kanBesluttes && (
                          <HStack gap="2">
                            <Button
                              variant="secondary"
                              size="small"
                              type="button"
                              onClick={() => setAvvisAnnulleringModalOpen(true)}
                            >
                              Avslå oppgjør
                            </Button>
                            <Button
                              size="small"
                              variant="danger"
                              type="button"
                              onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                            >
                              Bekreft oppgjør
                            </Button>
                          </HStack>
                        )}
                      </HStack>
                      <AarsakerOgForklaringModal<TilsagnTilAnnulleringAarsak>
                        aarsaker={tilAnnuleringAarsaker}
                        header="Annuller tilsagn med forklaring"
                        buttonLabel="Send til godkjenning"
                        open={tilAnnulleringModalOpen}
                        onClose={() => setTilAnnulleringModalOpen(false)}
                        onConfirm={({ aarsaker, forklaring }) =>
                          tilAnnullering({ aarsaker, forklaring })
                        }
                      />
                      <AarsakerOgForklaringModal<TilsagnTilAnnulleringAarsak>
                        aarsaker={[
                          {
                            value: TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
                            label: "Arrangør har ikke sendt krav",
                          },
                          { value: TilsagnTilAnnulleringAarsak.FEIL_ANNET, label: "Annet" },
                        ]}
                        header="Gjør opp tilsagn med forklaring"
                        buttonLabel="Send til godkjenning"
                        open={tilOppgjorModalOpen}
                        onClose={() => setTilOppgjorModalOpen(false)}
                        onConfirm={({ aarsaker, forklaring }) =>
                          upsertTilOppgjor({ aarsaker, forklaring })
                        }
                      />
                      <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
                        aarsaker={[
                          {
                            value: TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER,
                            label: "Feil i antall plasser",
                          },
                          {
                            value: TilsagnAvvisningAarsak.FEIL_KOSTNADSSTED,
                            label: "Feil kostnadssted",
                          },
                          { value: TilsagnAvvisningAarsak.FEIL_PERIODE, label: "Feil periode" },
                          { value: TilsagnAvvisningAarsak.FEIL_BELOP, label: "Feil beløp" },
                          { value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" },
                        ]}
                        header="Send i retur med forklaring"
                        buttonLabel="Send i retur"
                        open={avvisModalOpen}
                        onClose={() => setAvvisModalOpen(false)}
                        onConfirm={({ aarsaker, forklaring }) => {
                          besluttTilsagn({
                            besluttelse: Besluttelse.AVVIST,
                            aarsaker,
                            forklaring,
                          });
                          setAvvisModalOpen(false);
                        }}
                      />
                      <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
                        aarsaker={[{ value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" }]}
                        header="Send i retur med forklaring"
                        buttonLabel="Send i retur"
                        open={avvisAnnulleringModalOpen}
                        onClose={() => setAvvisAnnulleringModalOpen(false)}
                        onConfirm={({ aarsaker, forklaring }) => {
                          besluttTilsagn({
                            besluttelse: Besluttelse.AVVIST,
                            aarsaker,
                            forklaring,
                          });
                          setAvvisAnnulleringModalOpen(false);
                        }}
                      />
                      <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
                        aarsaker={[{ value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" }]}
                        header="Send i retur med forklaring"
                        buttonLabel="Send i retur"
                        open={avvisGjorOppModalOpen}
                        onClose={() => setAvvisGjorOppModalOpen(false)}
                        onConfirm={({ aarsaker, forklaring }) => {
                          besluttTilsagn({
                            besluttelse: Besluttelse.AVVIST,
                            aarsaker,
                            forklaring,
                          });
                          setAvvisGjorOppModalOpen(false);
                        }}
                      />
                      <VarselModal
                        headingIconType="warning"
                        headingText="Slette tilsagnet?"
                        open={slettTilsagnModalOpen}
                        handleClose={() => setTilOppgjorModalOpen(false)}
                        body={
                          <p>
                            Er du sikker på at du vil slette tilsagnet?
                            <br /> Denne operasjonen kan ikke angres
                          </p>
                        }
                        primaryButton={
                          <Button variant="danger" onClick={slettTilsagn} icon={<TrashFillIcon />}>
                            Ja, jeg vil slette tilsagnet
                          </Button>
                        }
                        secondaryButton
                        secondaryButtonHandleAction={() => setTilOppgjorModalOpen(false)}
                      />
                    </div>
                    {error.find((f) => f.pointer === "/") && (
                      <Alert className="self-end" variant="error" size="small">
                        {error.find((f) => f.pointer === "/")!.detail}
                      </Alert>
                    )}
                  </VStack>
                </WhitePaddedBox>
              </ContentBox>
            </Box>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
      <WhitePaddedBox>
        <VStack gap="4">
          <Heading size="medium">Aktive tilsagn</Heading>
          {aktiveTilsagn.length > 0 ? (
            <TilsagnTabell tilsagn={aktiveTilsagn} />
          ) : (
            <Alert variant="info">Det finnes ingen aktive tilsagn for dette tiltaket</Alert>
          )}
        </VStack>
      </WhitePaddedBox>
    </main>
  );
}

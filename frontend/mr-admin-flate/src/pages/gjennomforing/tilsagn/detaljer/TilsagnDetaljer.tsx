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
import { formaterDato, tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  Besluttelse,
  BesluttTilsagnRequest,
  NavAnsattRolle,
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
import { ActionMenu, Alert, BodyShort, Box, Button, Heading, HStack } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { AarsakerOgForklaring } from "../AarsakerOgForklaring";
import { TilsagnTag } from "../TilsagnTag";
import { TilsagnDetaljerForhandsgodkjent } from "./TilsagnDetaljerForhandsgodkjent";
import { tilsagnHistorikkQuery, tilsagnQuery } from "./tilsagnDetaljerLoader";

function useTilsagnDetaljer() {
  const { gjennomforingId, tilsagnId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: tilsagnDetaljer } = useSuspenseQuery({ ...tilsagnQuery(tilsagnId) });
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useSuspenseQuery({ ...tilsagnHistorikkQuery(tilsagnId) });
  return { ansatt, gjennomforing, historikk, ...tilsagnDetaljer.data };
}

export function TilsagnDetaljer() {
  const { gjennomforingId } = useParams();
  const { ansatt, gjennomforing, tilsagn, opprettelse, annullering, tilOppgjor, historikk } =
    useTilsagnDetaljer();

  const besluttMutation = useBesluttTilsagn();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const tilOppgjorMutation = useTilsagnTilOppgjor();
  const slettMutation = useSlettTilsagn();
  const navigate = useNavigate();
  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
  const [tilOppgjorModalOpen, setTilOppgjorModalOpen] = useState<boolean>(false);
  const [slettTilsagnModalOpen, setSlettTilsagnModalOpen] = useState<boolean>(false);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);

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
    ansatt.roller.includes(NavAnsattRolle.SAKSBEHANDLER_OKONOMI) &&
    [TilsagnStatus.RETURNERT, TilsagnStatus.GODKJENT].includes(tilsagn.status);

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
        <Box background="bg-default" padding={"5"}>
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
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          {opprettelse.type === "BESLUTTET" && opprettelse.besluttelse === Besluttelse.AVVIST && (
            <AarsakerOgForklaring
              heading="Tilsagnet ble returnert"
              tekst={`${opprettelse.behandletAv} returnerte tilsagnet den ${formaterDato(
                opprettelse.behandletTidspunkt,
              )} med følgende årsaker:`}
              aarsaker={
                opprettelse.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
                ) ?? []
              }
              forklaring={opprettelse.forklaring}
            />
          )}
          {annullering?.type === "TIL_BESLUTNING" && (
            <AarsakerOgForklaring
              heading="Tilsagnet ble annullerert"
              tekst={`${annullering.behandletAv} sendte tilsagnet til annullering den ${formaterDato(
                annullering.behandletTidspunkt,
              )} med følgende årsaker:`}
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
              tekst={`${tilOppgjor.behandletAv} sendte tilsagnet til oppgjør den ${formaterDato(
                tilOppgjor.behandletTidspunkt,
              )} med følgende årsaker:`}
              aarsaker={
                tilOppgjor.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                ) ?? []
              }
              forklaring={tilOppgjor.forklaring}
            />
          )}
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
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
              {besluttMutation.error ? (
                <BodyShort spacing>
                  <Alert variant="error">Klarte ikke lagre beslutning</Alert>
                </BodyShort>
              ) : null}
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
                      onClick={() =>
                        besluttTilsagn({
                          besluttelse: Besluttelse.AVVIST,
                          aarsaker: [],
                          forklaring: null,
                        })
                      }
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
                      onClick={() =>
                        besluttTilsagn({
                          besluttelse: Besluttelse.AVVIST,
                          aarsaker: [],
                          forklaring: null,
                        })
                      }
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
                aarsaker={[
                  {
                    value: TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
                    label: "Arrangør har ikke sendt krav",
                  },
                  {
                    value: TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
                    label: "Feilregistrering",
                  },
                  {
                    value: TilsagnTilAnnulleringAarsak.GJENNOMFORING_AVBRYTES,
                    label: "Gjennomføring skal avbrytes",
                  },
                  { value: TilsagnTilAnnulleringAarsak.FEIL_ANNET, label: "Annet" },
                ]}
                header="Annuller tilsagn med forklaring"
                buttonLabel="Send til godkjenning"
                open={tilAnnulleringModalOpen}
                onClose={() => setTilAnnulleringModalOpen(false)}
                onConfirm={({ aarsaker, forklaring }) => tilAnnullering({ aarsaker, forklaring })}
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
                onConfirm={({ aarsaker, forklaring }) => upsertTilOppgjor({ aarsaker, forklaring })}
              />
              <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
                aarsaker={[
                  {
                    value: TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER,
                    label: "Feil i antall plasser",
                  },
                  { value: TilsagnAvvisningAarsak.FEIL_KOSTNADSSTED, label: "Feil kostnadssted" },
                  { value: TilsagnAvvisningAarsak.FEIL_PERIODE, label: "Feil periode" },
                  { value: TilsagnAvvisningAarsak.FEIL_BELOP, label: "Feil beløp" },
                  { value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" },
                ]}
                header="Send i retur med forklaring"
                buttonLabel="Send i retur"
                open={avvisModalOpen}
                onClose={() => setAvvisModalOpen(false)}
                onConfirm={({ aarsaker, forklaring }) =>
                  besluttTilsagn({
                    besluttelse: Besluttelse.AVVIST,
                    aarsaker,
                    forklaring,
                  })
                }
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
          </Box>
        </Box>
      </ContentBox>
    </main>
  );
}

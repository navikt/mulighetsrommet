import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { useSlettTilsagn } from "@/api/tilsagn/useSlettTilsagn";
import { useTilsagnTilAnnullering } from "@/api/tilsagn/useTilsagnTilAnnullering";
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
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
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
import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { AvvistAlert, TilAnnulleringAlert, TilFrigjoringAlert } from "../AarsakerAlert";
import { TilsagnTag } from "../TilsagnTag";
import { TilsagnDetaljerForhandsgodkjent } from "./TilsagnDetaljerForhandsgodkjent";
import { tilsagnHistorikkQuery, tilsagnQuery } from "./tilsagnDetaljerLoader";
import { useTilsagnTilFrigjoring } from "@/api/tilsagn/useTilsagnTilFrigjoring";

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
  const { ansatt, gjennomforing, tilsagn, opprettelse, annullering, frigjoring, historikk } =
    useTilsagnDetaljer();

  const besluttMutation = useBesluttTilsagn();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const tilFrigjoringMutation = useTilsagnTilFrigjoring();
  const slettMutation = useSlettTilsagn();
  const navigate = useNavigate();
  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
  const [tilFrigjoringModalOpen, setTilFrigjoringModalOpen] = useState<boolean>(false);
  const slettTilsagnModalRef = useRef<HTMLDialogElement>(null);
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
    if (tilsagn) {
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
  }

  function tilAnnullering(request: TilsagnTilAnnulleringRequest) {
    if (tilsagn) {
      tilAnnulleringMutation.mutate(
        {
          id: tilsagn.id,
          aarsaker: request.aarsaker,
          forklaring: request.forklaring || null,
        },
        { onSuccess: navigerTilTilsagnTabell },
      );
    }
  }

  function tilFrigjoring(request: TilsagnTilAnnulleringRequest) {
    if (tilsagn) {
      tilFrigjoringMutation.mutate(
        {
          id: tilsagn.id,
          aarsaker: request.aarsaker,
          forklaring: request.forklaring || null,
        },
        { onSuccess: navigerTilTilsagnTabell },
      );
    }
  }

  function slettTilsagn() {
    if (tilsagn) {
      slettMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilTilsagnTabell });
    }
  }

  function visBesluttKnapp(endretAv?: string): boolean {
    return Boolean(
      (tilsagn.status === TilsagnStatus.TIL_GODKJENNING ||
        tilsagn.status === TilsagnStatus.TIL_ANNULLERING ||
        tilsagn.status === TilsagnStatus.TIL_FRIGJORING) &&
        ansatt?.navIdent !== endretAv &&
        ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER),
    );
  }

  const visHandlingerMeny =
    tilsagn.status === TilsagnStatus.RETURNERT ||
    (tilsagn.status === TilsagnStatus.GODKJENT &&
      ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV));

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
                        onSelect={() => slettTilsagnModalRef.current?.showModal()}
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
                          onSelect={() => setTilFrigjoringModalOpen(true)}
                          icon={<EraserIcon />}
                        >
                          Frigjør tilsagn
                        </ActionMenu.Item>
                      </>
                    ))}
                </ActionMenu.Content>
              </ActionMenu>
            ) : null}
          </HStack>
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          {tilsagn.status === TilsagnStatus.RETURNERT && (
            <AvvistAlert
              header="Tilsagnet ble returnert"
              tidspunkt={opprettelse.besluttetTidspunkt}
              aarsaker={
                opprettelse.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
                ) ?? []
              }
              forklaring={opprettelse.forklaring}
              navIdent={opprettelse.besluttetAv}
            />
          )}
          {tilsagn.status === TilsagnStatus.TIL_ANNULLERING && annullering && (
            <TilAnnulleringAlert annullering={annullering} />
          )}
          {tilsagn.status === TilsagnStatus.TIL_FRIGJORING && frigjoring && (
            <TilFrigjoringAlert frigjoring={frigjoring} />
          )}
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
            {isTilsagnForhandsgodkjent(tilsagn) && (
              <TilsagnDetaljerForhandsgodkjent tilsagn={tilsagn} annullering={annullering} />
            )}
            {isTilsagnFri(tilsagn) && (
              <TilsagnDetaljerFri tilsagn={tilsagn} annullering={annullering} />
            )}
            <div>
              {besluttMutation.error ? (
                <BodyShort spacing>
                  <Alert variant="error">Klarte ikke lagre beslutning</Alert>
                </BodyShort>
              ) : null}
              <HStack gap="2" justify={"end"}>
                {tilsagn.status === TilsagnStatus.TIL_GODKJENNING &&
                  visBesluttKnapp(opprettelse.behandletAv) && (
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
                {tilsagn.status === TilsagnStatus.TIL_ANNULLERING &&
                  annullering &&
                  visBesluttKnapp(annullering.behandletAv) && (
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
                {tilsagn.status === TilsagnStatus.TIL_FRIGJORING &&
                  frigjoring &&
                  visBesluttKnapp(frigjoring.behandletAv) && (
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
                        Avslå frigjøring
                      </Button>
                      <Button
                        size="small"
                        variant="danger"
                        type="button"
                        onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                      >
                        Bekreft frigjøring
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
                header="Frigjør tilsagn med forklaring"
                buttonLabel="Send til godkjenning"
                open={tilFrigjoringModalOpen}
                onClose={() => setTilFrigjoringModalOpen(false)}
                onConfirm={({ aarsaker, forklaring }) => tilFrigjoring({ aarsaker, forklaring })}
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
                modalRef={slettTilsagnModalRef}
                handleClose={() => slettTilsagnModalRef.current?.close()}
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
                secondaryButtonHandleAction={() => slettTilsagnModalRef.current?.close()}
              />
            </div>
          </Box>
        </Box>
      </ContentBox>
    </main>
  );
}

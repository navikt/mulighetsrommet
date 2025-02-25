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
import { LoaderData } from "@/types/loader";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  Besluttelse,
  BesluttTilsagnRequest,
  NavAnsattRolle,
  TilsagnAvvisningAarsak,
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
import { useRef, useState } from "react";
import { Link, useLoaderData, useNavigate } from "react-router";
import { AvvistAlert, TilAnnulleringAlert } from "../AarsakerAlert";
import { TilsagnTag } from "../TilsagnTag";
import { TilsagnDetaljerForhandsgodkjent } from "./TilsagnDetaljerForhandsgodkjent";
import { tilsagnDetaljerLoader } from "./tilsagnDetaljerLoader";

export function TilsagnDetaljer() {
  const { gjennomforing, tilsagn, ansatt, historikk } =
    useLoaderData<LoaderData<typeof tilsagnDetaljerLoader>>();

  const besluttMutation = useBesluttTilsagn();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const slettMutation = useSlettTilsagn();
  const navigate = useNavigate();
  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
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
    navigate(`/gjennomforinger/${gjennomforing.id}/tilsagn`);
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
          forklaring: request.forklaring,
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

  function visBesluttKnapp(endretAv: string): boolean {
    return (
      (tilsagn.status.type === "TIL_GODKJENNING" || tilsagn.status.type === "TIL_ANNULLERING") &&
      ansatt?.navIdent !== endretAv &&
      ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER)
    );
  }

  const visHandlingerMeny =
    tilsagn.status.type === "RETURNERT" ||
    (tilsagn.status.type === "GODKJENT" &&
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
              <ViewEndringshistorikk historikk={historikk} />
            </EndringshistorikkPopover>
            {visHandlingerMeny ? (
              <ActionMenu>
                <ActionMenu.Trigger>
                  <Button variant="primary" size="small">
                    Handlinger
                  </Button>
                </ActionMenu.Trigger>
                <ActionMenu.Content>
                  {tilsagn.status.type === "RETURNERT" && (
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
                  {tilsagn.status.type === "GODKJENT" && (
                    <>
                      <ActionMenu.Item
                        variant="danger"
                        onSelect={() => setTilAnnulleringModalOpen(true)}
                        icon={<EraserIcon />}
                      >
                        Annuller tilsagn
                      </ActionMenu.Item>
                    </>
                  )}
                </ActionMenu.Content>
              </ActionMenu>
            ) : null}
          </HStack>
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          {tilsagn.status.type === "RETURNERT" && (
            <AvvistAlert
              header="Tilsagnet ble returnert"
              tidspunkt={tilsagn.status.opprettelse.besluttetTidspunkt}
              aarsaker={
                tilsagn.status.opprettelse.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
                ) ?? []
              }
              forklaring={tilsagn.status.opprettelse.forklaring}
              navIdent={tilsagn.status.opprettelse.besluttetAv}
            />
          )}
          {tilsagn.status.type === "TIL_ANNULLERING" && (
            <TilAnnulleringAlert status={tilsagn.status} />
          )}
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
            {isTilsagnForhandsgodkjent(tilsagn) && (
              <TilsagnDetaljerForhandsgodkjent tilsagn={tilsagn} />
            )}
            {isTilsagnFri(tilsagn) && <TilsagnDetaljerFri tilsagn={tilsagn} />}
            <div>
              {besluttMutation.error ? (
                <BodyShort spacing>
                  <Alert variant="error">Klarte ikke lagre beslutning</Alert>
                </BodyShort>
              ) : null}
              <HStack gap="2" justify={"end"}>
                {tilsagn.status.type === "TIL_GODKJENNING" &&
                  visBesluttKnapp(tilsagn.status.opprettelse.behandletAv) && (
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
                {tilsagn.status.type === "TIL_ANNULLERING" &&
                  visBesluttKnapp(tilsagn.status.annullering.behandletAv) && (
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
              </HStack>
              <AarsakerOgForklaringModal<TilsagnTilAnnulleringAarsak>
                aarsaker={[
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

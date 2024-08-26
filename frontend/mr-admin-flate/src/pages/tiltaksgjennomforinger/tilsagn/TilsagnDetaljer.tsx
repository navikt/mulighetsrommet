import { Alert, BodyShort, Button, Heading, HStack, Tag } from "@navikt/ds-react";
import { NavAnsattRolle, TilsagnBesluttelse } from "@mr/api-client";
import { Link, useMatch, useNavigate, useParams } from "react-router-dom";
import { useTiltaksgjennomforingById } from "../../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Bolk } from "../../../components/detaljside/Bolk";
import { Header } from "../../../components/detaljside/Header";
import { Metadata } from "../../../components/detaljside/Metadata";
import { TiltaksgjennomforingIkon } from "../../../components/ikoner/TiltaksgjennomforingIkon";
import { Laster } from "../../../components/laster/Laster";
import { Brodsmule, Brodsmuler } from "../../../components/navigering/Brodsmuler";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { formaterDato, formaterTall } from "../../../utils/Utils";
import { DetaljerContainer } from "../../DetaljerContainer";
import { DetaljerInfoContainer } from "../../DetaljerInfoContainer";
import { useGetTilsagnById } from "./useGetTilsagnById";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { VarselModal } from "@/components/modal/VarselModal";
import { useRef } from "react";
import { TrashFillIcon } from "@navikt/aksel-icons";
import { useAnnullerTilsagn } from "@/api/tilsagn/useAnnullerTilsagn";
import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";

export function TilsagnDetaljer() {
  const { avtaleId, tiltaksgjennomforingId } = useParams();
  const { data: tilsagn } = useGetTilsagnById();
  const besluttMutation = useBesluttTilsagn();
  const annullerMutation = useAnnullerTilsagn();
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: ansatt } = useHentAnsatt();
  const navigate = useNavigate();
  const annullerModalRef = useRef<HTMLDialogElement>(null);

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtaledetaljer",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Tiltaksgjennomføring",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
    {
      tittel: "Tilsagn",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
  ];

  function navigerTilGjennomforing() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn`);
  }

  function besluttTilsagn(besluttelse: TilsagnBesluttelse) {
    if (tilsagn) {
      besluttMutation.mutate(
        {
          id: tilsagn.id,
          requestBody: {
            besluttelse,
          },
        },
        {
          onSuccess: navigerTilGjennomforing,
        },
      );
    }
  }

  function annullerTilsagn() {
    if (tilsagn) {
      annullerMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilGjennomforing });
    }
  }

  function tag() {
    if (tilsagn?.besluttelse?.utfall === TilsagnBesluttelse.GODKJENT) {
      return (
        <Tag variant="success" size="small">
          Godkjent
        </Tag>
      );
    } else if (tilsagn?.besluttelse?.utfall === TilsagnBesluttelse.AVVIST) {
      return (
        <Tag variant="warning" size="small">
          Avvist
        </Tag>
      );
    } else if (tilsagn?.annullertTidspunkt) {
      return (
        <Tag variant="neutral" size="small">
          Annullert
        </Tag>
      );
    } else {
      return (
        <Tag variant="info" size="small">
          Til beslutning
        </Tag>
      );
    }
  }

  if (!tiltaksgjennomforing || !tilsagn) {
    return <Laster tekst="Laster tilsagn..." />;
  }

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Tilsagn for {tiltaksgjennomforing.navn} {tag()}
          </HStack>
        </Heading>
      </Header>
      <ContainerLayout>
        <Link to={`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn`}>
          Tilbake til tilsagn
        </Link>
        <DetaljerContainer>
          <DetaljerInfoContainer withBorderRight={false}>
            <Bolk>
              <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.navn} />
              <Metadata
                header="Arrangør"
                verdi={`${tiltaksgjennomforing.arrangor.navn} -  ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
              />
            </Bolk>
            <Bolk>
              <Metadata header="Periodestart" verdi={formaterDato(tilsagn.periodeStart)} />
              <Metadata header="Periodeslutt" verdi={formaterDato(tilsagn.periodeSlutt)} />
            </Bolk>
            <Bolk>
              <Metadata
                header="Kostnadssted"
                verdi={`${tilsagn.kostnadssted.navn} - ${tilsagn.kostnadssted.enhetsnummer}`}
              />
              <Metadata header="Beløp" verdi={`${formaterTall(tilsagn.beregning.belop)} kr`} />
            </Bolk>
            <Bolk>
              <Metadata header="Løpenummer" verdi={tilsagn.lopenummer} />
            </Bolk>
            {besluttMutation.error ? (
              <BodyShort spacing>
                <Alert variant="error">Klarte ikke lagre beslutning</Alert>
              </BodyShort>
            ) : null}
            <HStack gap="2" justify={"space-between"}>
              {!tilsagn?.besluttelse &&
              ansatt?.navIdent !== tilsagn.opprettetAv &&
              ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER) ? (
                <GodkjennAvvisTilsagnButtons
                  onGodkjennTilsagn={() => besluttTilsagn(TilsagnBesluttelse.GODKJENT)}
                  onAvvisTilsagn={() => besluttTilsagn(TilsagnBesluttelse.AVVIST)}
                />
              ) : (
                <div></div>
              )}
              {!tilsagn.annullertTidspunkt && (
                <Button
                  variant="danger"
                  size="small"
                  onClick={() => annullerModalRef.current?.show()}
                >
                  Annuller
                </Button>
              )}
            </HStack>
            <VarselModal
              headingIconType="warning"
              headingText="Annuller tilsagn?"
              modalRef={annullerModalRef}
              handleClose={() => annullerModalRef.current?.close()}
              body={null}
              primaryButton={
                <Button variant="danger" onClick={annullerTilsagn} icon={<TrashFillIcon />}>
                  Ja, jeg vil annullere
                </Button>
              }
              secondaryButton
              secondaryButtonHandleAction={() => annullerModalRef.current?.close()}
            />
          </DetaljerInfoContainer>
        </DetaljerContainer>
      </ContainerLayout>
    </main>
  );
}

interface GodkjennAvvisButtonProps {
  onGodkjennTilsagn: () => void;
  onAvvisTilsagn: () => void;
}

function GodkjennAvvisTilsagnButtons({
  onGodkjennTilsagn,
  onAvvisTilsagn,
}: GodkjennAvvisButtonProps) {
  return (
    <HStack gap="2">
      <Button size="small" type="button" onClick={onGodkjennTilsagn}>
        Godkjenn tilsagn
      </Button>
      <Button variant="secondary" size="small" type="button" onClick={onAvvisTilsagn}>
        Avvis tilsagn
      </Button>
    </HStack>
  );
}

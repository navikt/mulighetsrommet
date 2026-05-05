import { BodyShort, Box, CopyButton, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import {
  GjennomforingOppstartstype,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltak,
} from "@api-client";
import { formaterDato, utledLopenummerFraTiltaksnummer } from "@/utils/Utils";
import { Faglenker } from "./Faglenker";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  innsatsgrupper: VeilederflateInnsatsgruppe[];
  tiltak: VeilederflateTiltak;
}

const SidemenyInfo = ({ innsatsgrupper, tiltak }: Props) => {
  const { tiltakstype } = tiltak;

  const minimumInnsatsgruppe = innsatsgrupper
    .filter((innsatsgruppe) => (tiltakstype.innsatsgrupper ?? []).includes(innsatsgruppe.nokkel))
    .reduce((prev, current) => (prev.order < current.order ? prev : current));

  const tiltaksnummer = "tiltaksnummer" in tiltak ? tiltak.tiltaksnummer : null;
  const lopenummer = "lopenummer" in tiltak ? tiltak.lopenummer : null;
  const arrangor = "arrangor" in tiltak ? tiltak.arrangor : null;

  return (
    <Box asChild padding="space-20" borderRadius="0 0 8 8" background="neutral-soft" id="sidemeny">
      <VStack gap="space-24">
        {lopenummer && (
          <SidemenyInfoContainer
            tittel="Løpenummer"
            innhold={
              <HStack gap="space-4" align="center" justify="space-between">
                <BodyShort size="small">{lopenummer}</BodyShort>
                <CopyButton data-color="accent" size="xsmall" copyText={lopenummer} />
              </HStack>
            }
          />
        )}
        {tiltaksnummer && (
          <SidemenyInfoContainer
            tittel="Tiltaksnummer i Arena"
            innhold={
              <HStack gap="space-4" align="center" justify="space-between">
                <BodyShort size="small">{utledLopenummerFraTiltaksnummer(tiltaksnummer)}</BodyShort>
                <CopyButton
                  data-color="accent"
                  size="xsmall"
                  copyText={utledLopenummerFraTiltaksnummer(tiltaksnummer)}
                />
              </HStack>
            }
          />
        )}
        <SidemenyInfoContainer tittel="Tiltakstype" innhold={tiltakstype.navn} />
        {arrangor && <SidemenyInfoContainer tittel="Arrangør" innhold={arrangor.selskapsnavn} />}
        <SidemenyInfoContainer tittel="Min. innsatsgruppe" innhold={minimumInnsatsgruppe.tittel} />
        <TiltakVarighetInfo tiltak={tiltak} />
        {tiltakstype.faglenker && (
          <SidemenyInfoContainer
            tittel="Regelverk og rutiner"
            innhold={<Faglenker faglenker={[...tiltakstype.faglenker]} />}
          />
        )}
      </VStack>
    </Box>
  );
};

function TiltakVarighetInfo({ tiltak }: { tiltak: VeilederflateTiltak }) {
  const elementer =
    !isTiltakGruppe(tiltak) || tiltak.oppstart === GjennomforingOppstartstype.LOPENDE
      ? [
          {
            tittel: "Oppstart",
            innhold: "Løpende",
          },
        ]
      : [
          {
            tittel: "Oppstart",
            innhold: "Felles",
          },
          tiltak.sluttdato
            ? {
                tittel: "Varighet",
                innhold: `${formaterDato(tiltak.oppstartsdato)} - ${formaterDato(tiltak.sluttdato)}`,
              }
            : {
                tittel: "Oppstartsdato",
                innhold: formaterDato(tiltak.oppstartsdato),
              },
        ];

  return elementer.map(({ tittel, innhold }) => (
    <SidemenyInfoContainer key={tittel} tittel={tittel} innhold={innhold} />
  ));
}

const SidemenyInfoContainer = ({ tittel, innhold }: { tittel: string; innhold: ReactNode }) => (
  <HStack gap="space-4" align="start" justify="space-between" wrap={false}>
    <BodyShort size="small" className="font-bold text-left">
      {tittel}
    </BodyShort>
    {typeof innhold === "string" ? (
      <BodyShort size="small" align="end">
        {innhold}
      </BodyShort>
    ) : (
      innhold
    )}
  </HStack>
);
export default SidemenyInfo;

import { BodyShort, Box, CopyButton, HStack, VStack } from "@navikt/ds-react";
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
    <Box padding="space-20" background="neutral-soft" id="sidemeny">
      <VStack gap="space-24">
        {lopenummer && (
          <HStack align="center" justify="space-between">
            <BodyShort size="small" className="font-bold text-left">
              Løpenummer
            </BodyShort>
            <HStack align="center" gap="space-4">
              <BodyShort size="small">{lopenummer}</BodyShort>
              <CopyButton data-color="accent" size="xsmall" copyText={lopenummer} />
            </HStack>
          </HStack>
        )}
        {tiltaksnummer && (
          <HStack align="center" justify="space-between">
            <BodyShort size="small" className="font-bold text-left">
              Tiltaksnummer i Arena
            </BodyShort>
            <HStack align="center" gap="space-4">
              <BodyShort size="small">{utledLopenummerFraTiltaksnummer(tiltaksnummer)}</BodyShort>
              <CopyButton
                data-color="accent"
                size="xsmall"
                copyText={utledLopenummerFraTiltaksnummer(tiltaksnummer)}
              />
            </HStack>
          </HStack>
        )}
        <HStack align="center" justify="space-between">
          <BodyShort size="small" className="font-bold text-left">
            Tiltakstype
          </BodyShort>
          <BodyShort size="small">{tiltakstype.navn} </BodyShort>
        </HStack>
        {arrangor && (
          <HStack align="center" justify="space-between">
            <BodyShort size="small" className="font-bold text-left">
              Arrangør
            </BodyShort>
            <BodyShort size="small">{arrangor.selskapsnavn}</BodyShort>
          </HStack>
        )}
        <HStack align="center" justify="space-between">
          <BodyShort
            title="Minimum krav innsatsgruppe"
            size="small"
            className="font-bold text-left"
          >
            <abbr title="Minimum">Min</abbr>. innsatsgruppe
          </BodyShort>
          <BodyShort size="small">{minimumInnsatsgruppe.tittel}</BodyShort>
        </HStack>
        <TiltakVarighetInfo tiltak={tiltak} />
        {tiltakstype.faglenker && (
          <HStack align="start" justify="space-between">
            <BodyShort size="small" className="font-bold text-left">
              Regelverk og rutiner
            </BodyShort>
            <Faglenker
              faglenker={[
                ...tiltakstype.faglenker,
                {
                  id: "avslag-og-klage",
                  navn: "Avslag og klage",
                  url: "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Klage-p%C3%A5-arbeidsmarkedstiltak.aspx",
                  beskrivelse: null,
                },
                {
                  id: "tiltak-hos-familie",
                  navn: "Tiltak hos familie/nærstående",
                  url: "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Rutine.aspx",
                  beskrivelse: null,
                },
              ]}
            />
          </HStack>
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
    <HStack align="center" justify="space-between" key={tittel}>
      <BodyShort size="small" className="font-bold text-left">
        {tittel}
      </BodyShort>
      <BodyShort size="small">{innhold}</BodyShort>
    </HStack>
  ));
}

export default SidemenyInfo;

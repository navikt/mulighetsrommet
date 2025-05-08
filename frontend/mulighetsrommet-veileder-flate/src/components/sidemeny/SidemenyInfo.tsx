import { BodyShort, Box } from "@navikt/ds-react";
import {
  GjennomforingOppstartstype,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltak,
} from "@mr/api-client-v2";
import { formaterDato, utledLopenummerFraTiltaksnummer } from "@/utils/Utils";
import Kopiknapp from "../kopiknapp/Kopiknapp";
import RegelverkInfo from "./RegelverkInfo";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  innsatsgrupper: VeilederflateInnsatsgruppe[];
  tiltak: VeilederflateTiltak;
}

const SidemenyInfo = ({ innsatsgrupper, tiltak }: Props) => {
  const { tiltakstype, stedForGjennomforing } = tiltak;

  const minimumInnsatsgruppe = innsatsgrupper
    .filter((innsatsgruppe) => (tiltakstype.innsatsgrupper ?? []).includes(innsatsgruppe.nokkel))
    .reduce((prev, current) => (prev.order < current.order ? prev : current));

  const tiltaksnummer = "tiltaksnummer" in tiltak ? tiltak.tiltaksnummer : null;
  const arrangor = "arrangor" in tiltak ? tiltak.arrangor : null;

  return (
    <Box padding="5" background="bg-subtle" className="max-w-[380px]" id="sidemeny">
      {tiltaksnummer && (
        <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
          <BodyShort size="small" className="font-bold text-left">
            Tiltaksnummer
          </BodyShort>
          <div className="flex items-start justify-end gap-1">
            <BodyShort size="small">{utledLopenummerFraTiltaksnummer(tiltaksnummer)}</BodyShort>
            <Kopiknapp
              kopitekst={utledLopenummerFraTiltaksnummer(tiltaksnummer)}
              dataTestId="knapp_kopier"
            />
          </div>
        </div>
      )}

      {stedForGjennomforing && (
        <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
          <BodyShort size="small" className="font-bold text-left">
            Sted for gjennomføring
          </BodyShort>
          <BodyShort size="small">{stedForGjennomforing}</BodyShort>
        </div>
      )}

      <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
        <BodyShort size="small" className="font-bold text-left">
          Tiltakstype
        </BodyShort>
        <BodyShort size="small">{tiltakstype.navn} </BodyShort>
      </div>

      {arrangor && (
        <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
          <BodyShort size="small" className="font-bold text-left">
            Arrangør
          </BodyShort>
          <BodyShort size="small">{arrangor.selskapsnavn}</BodyShort>
        </div>
      )}

      <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
        <BodyShort title="Minimum krav innsatsgruppe" size="small" className="font-bold text-left">
          <abbr title="Minimum">Min</abbr>. innsatsgruppe
        </BodyShort>
        <BodyShort size="small">{minimumInnsatsgruppe.tittel}</BodyShort>
      </div>

      <TiltakVarighetInfo tiltak={tiltak} />

      {tiltakstype.regelverkLenker && (
        <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
          <BodyShort size="small" className="font-bold text-left">
            Regelverk og rutiner
          </BodyShort>
          <div className="space-y-4 last:mb-0">
            <RegelverkInfo
              regelverkLenker={[
                ...tiltakstype.regelverkLenker,
                {
                  _id: "klage",
                  regelverkLenkeNavn: "Avslag og klage",
                  regelverkUrl:
                    "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Klage-p%C3%A5-arbeidsmarkedstiltak.aspx",
                },
                {
                  _id: "vurdering",
                  regelverkLenkeNavn: "Tiltak hos familie/nærstående",
                  regelverkUrl:
                    "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Rutine.aspx",
                },
              ]}
            />
          </div>
        </div>
      )}
    </Box>
  );
};

function TiltakVarighetInfo({ tiltak }: { tiltak: VeilederflateTiltak }) {
  const { tittel, innhold } =
    !isTiltakGruppe(tiltak) || tiltak.oppstart === GjennomforingOppstartstype.LOPENDE
      ? {
          tittel: "Oppstart",
          innhold: "Løpende",
        }
      : tiltak.sluttdato
        ? {
            tittel: "Varighet",
            innhold: `${formaterDato(tiltak.oppstartsdato)} - ${formaterDato(tiltak.sluttdato)}`,
          }
        : {
            tittel: "Oppstart",
            innhold: formaterDato(tiltak.oppstartsdato),
          };

  return (
    <div className="flex justify-between min-h-[40px] mb-2 text-right last:mb-0 xl:mb-0 xl:p-0 xl:not-last:mb-4">
      <BodyShort size="small" className="font-bold text-left">
        {tittel}
      </BodyShort>
      <BodyShort size="small">{innhold}</BodyShort>
    </div>
  );
}

export default SidemenyInfo;

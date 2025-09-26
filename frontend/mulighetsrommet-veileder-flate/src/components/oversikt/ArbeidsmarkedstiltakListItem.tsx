import { paginationAtom } from "@/core/atoms";
import { formaterDato, utledOppstart } from "@/utils/Utils";
import { ChevronRightIcon, PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { BodyShort, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { useAtomValue } from "jotai";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";
import { VisningsnavnForTiltak } from "./VisningsnavnForTiltak";
import { DeltMedBrukerDto, VeilederflateTiltak } from "@api-client";
import { isTiltakGruppe, isTiltakMedArrangor } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak;
  index: number;
  deltMedBruker?: DeltMedBrukerDto;
}

export function ArbeidsmarkedstiltakListItem({ tiltak, index, deltMedBruker }: Props) {
  const pageData = useAtomValue(paginationAtom);

  const paginationUrl = `#pagination=${encodeURIComponent(JSON.stringify({ ...pageData }))}`;

  const id = isTiltakGruppe(tiltak) ? tiltak.id : tiltak.sanityId;
  const oppstart = utledOppstart(tiltak);

  return (
    <li
      className={classNames(
        "list-none w-full bg-white rounded-[4px] text-medium hover:bg-gray-50 [&_a]:text-black",
        {
          harDeltMedBruker: "border border-solid border-[rgba(7,26,54,0.21)]",
        },
      )}
      id={`list_element_${index}`}
      data-testid={`gjennomforing_${kebabCase(tiltak.tiltakstype.navn)}`}
    >
      <Lenke className={`text-[#000000]`} to={`../tiltak/${id}${paginationUrl}`}>
        {deltMedBruker && <TiltakDeltMedBrukerInfo deltMedBruker={deltMedBruker} />}
        <div
          className={`grid grid-cols-[0_40%_1fr_2%] [grid-template-areas:'status_navn_metadata_ikon'] lg:grid-areas-[status_navn_navn_ikon_metadata_metadata_metadata]  items-start justify-start grid-rows-[auto] lg:items-center min-h-[4rem] gap-8 p-3`}
        >
          {isTiltakGruppe(tiltak) && !tiltak.apentForPamelding && (
            <PadlockLockedFillIcon
              className={`[grid-area:status] w-6 h-auto text-black`}
              title="Tiltaket er stengt for påmelding"
            />
          )}

          <div className={`flex flex-col [grid-area:navn]`}>
            <VStack>
              <VisningsnavnForTiltak tiltakstypeNavn={tiltak.tiltakstype.navn} navn={tiltak.navn} />
            </VStack>
          </div>

          <div
            className={`grid [grid-area:metadata] grid-cols-[repeat(auto-fill,minmax(5rem,15rem))] gap-[5px] lg:gap-4 justify-[initial] lg:justify-evenly`}
          >
            {isTiltakMedArrangor(tiltak) && tiltak.arrangor.selskapsnavn ? (
              <BodyShort size="small" title={tiltak.arrangor.selskapsnavn}>
                {tiltak.arrangor.selskapsnavn}
              </BodyShort>
            ) : (
              <div />
            )}

            <BodyShort size="small" title={oppstart} className="truncate">
              {oppstart}
            </BodyShort>
          </div>

          <ChevronRightIcon
            className={`[grid-area:ikon] w-6 h-auto text-black`}
            title="Detaljer om tiltaket"
          />
        </div>
      </Lenke>
    </li>
  );
}

interface TiltakDeltMedBrukerInfoProps {
  deltMedBruker: DeltMedBrukerDto;
}

function TiltakDeltMedBrukerInfo({ deltMedBruker }: TiltakDeltMedBrukerInfoProps) {
  const date = new Date(deltMedBruker.deling.tidspunkt);

  const formatertDeltMedBrukerDato = date.toLocaleDateString("nb-NO", {
    weekday: "long",
    day: "numeric",
    month: "numeric",
    year: "numeric",
  });

  return (
    <div className={`bg-surface-action-subtle py-1.5 px-3`}>
      <BodyShort title={formatertDeltMedBrukerDato} size="small">
        Delt i dialogen {formaterDato(date)}
      </BodyShort>
    </div>
  );
}

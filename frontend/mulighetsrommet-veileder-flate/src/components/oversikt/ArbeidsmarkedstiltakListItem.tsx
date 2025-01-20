import { paginationAtom } from "@/core/atoms";
import { formaterDato } from "@/utils/Utils";
import { ChevronRightIcon, PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { BodyShort, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { useAtomValue } from "jotai";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";
import { VisningsnavnForTiltak } from "./VisningsnavnForTiltak";
import { DelMedBruker, GjennomforingOppstartstype, VeilederflateTiltak } from "@mr/api-client-v2";
import styles from "./ArbeidsmarkedstiltakListItem.module.scss";
import {
  isTiltakEnkeltplass,
  isTiltakGruppe,
  isTiltakMedArrangor,
} from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak;
  index: number;
  delMedBruker?: DelMedBruker;
}

export function ArbeidsmarkedstiltakListItem({ tiltak, index, delMedBruker }: Props) {
  const pageData = useAtomValue(paginationAtom);

  const datoSidenSistDelt = delMedBruker && formaterDato(new Date(delMedBruker.createdAt!));
  const paginationUrl = `#pagination=${encodeURIComponent(JSON.stringify({ ...pageData }))}`;

  const formatertDeltMedBrukerDato = delMedBruker?.createdAt
    ? new Date(delMedBruker.createdAt).toLocaleDateString("nb-NO", {
        weekday: "long",
        day: "numeric",
        month: "numeric",
        year: "numeric",
      })
    : "Dato mangler";

  const id = isTiltakGruppe(tiltak) ? tiltak.id : tiltak.sanityId;
  const oppstart = utledOppstart(tiltak);

  return (
    <li
      className={classNames(styles.list_element, {
        harDeltMedBruker: styles.list_element_border,
      })}
      id={`list_element_${index}`}
      data-testid={`gjennomforing_${kebabCase(tiltak.tiltakstype.navn)}`}
    >
      <Lenke to={`../tiltak/${id}${paginationUrl}`}>
        {datoSidenSistDelt ? (
          <div className={styles.delt_med_bruker_rad}>
            <BodyShort title={formatertDeltMedBrukerDato} size="small">
              Delt i dialogen {datoSidenSistDelt}
            </BodyShort>
          </div>
        ) : null}

        <div className={styles.gjennomforing_container}>
          {isTiltakGruppe(tiltak) && !tiltak.apentForPamelding && (
            <PadlockLockedFillIcon
              className={styles.status}
              title="Tiltaket er stengt for påmelding"
            />
          )}

          <div className={classNames(styles.flex, styles.navn)}>
            <VStack>
              <VisningsnavnForTiltak tiltakstypeNavn={tiltak.tiltakstype.navn} navn={tiltak.navn} />
            </VStack>
          </div>

          <div className={classNames(styles.infogrid, styles.metadata)}>
            {isTiltakMedArrangor(tiltak) ? (
              <BodyShort size="small" title={tiltak.arrangor.selskapsnavn} className={styles.muted}>
                {tiltak.arrangor.selskapsnavn}
              </BodyShort>
            ) : (
              <div />
            )}

            <BodyShort size="small" title={oppstart} className={styles.truncate}>
              {oppstart}
            </BodyShort>
          </div>

          <ChevronRightIcon className={styles.ikon} title="Detaljer om tiltaket" />
        </div>
      </Lenke>
    </li>
  );
}

function utledOppstart(tiltak: VeilederflateTiltak) {
  if (isTiltakEnkeltplass(tiltak)) {
    return "Løpende oppstart";
  }

  switch (tiltak.oppstart) {
    case GjennomforingOppstartstype.FELLES:
      return formaterDato(tiltak.oppstartsdato);
    case GjennomforingOppstartstype.LOPENDE:
      return "Løpende oppstart";
  }
}

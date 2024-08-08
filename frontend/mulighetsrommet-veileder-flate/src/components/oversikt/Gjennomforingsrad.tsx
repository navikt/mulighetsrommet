import { paginationAtom } from "@/core/atoms";
import { formaterDato } from "@/utils/Utils";
import { ChevronRightIcon, PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { BodyShort, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { useAtomValue } from "jotai";
import {
  DelMedBruker,
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { Lenke } from "mulighetsrommet-frontend-common/components/lenke/Lenke";
import { kebabCase } from "mulighetsrommet-frontend-common/utils/TestUtils";
import styles from "./Gjennomforingsrad.module.scss";
import { VisningsnavnForTiltak } from "./VisningsnavnForTiltak";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  index: number;
  delMedBruker?: DelMedBruker;
}

const visOppstartsdato = (oppstart: TiltaksgjennomforingOppstartstype, oppstartsdato?: string) => {
  switch (oppstart) {
    case TiltaksgjennomforingOppstartstype.FELLES:
      return formaterDato(oppstartsdato!);
    case TiltaksgjennomforingOppstartstype.LOPENDE:
      return "Løpende oppstart";
  }
};

export function Gjennomforingsrad({ tiltaksgjennomforing, index, delMedBruker }: Props) {
  const pageData = useAtomValue(paginationAtom);
  const { id, sanityId, navn, arrangor, tiltakstype, oppstart, oppstartsdato, apentForInnsok } =
    tiltaksgjennomforing;

  const datoSidenSistDelt = delMedBruker && formaterDato(new Date(delMedBruker.createdAt!!));
  const paginationUrl = `#pagination=${encodeURIComponent(JSON.stringify({ ...pageData }))}`;

  return (
    <li
      className={classNames(styles.list_element, {
        harDeltMedBruker: styles.list_element_border,
      })}
      id={`list_element_${index}`}
      data-testid={`tiltaksgjennomforing_${kebabCase(navn)}`}
    >
      <Lenke to={`../tiltak/${id ?? sanityId}${paginationUrl}`}>
        {datoSidenSistDelt ? (
          <div className={styles.delt_med_bruker_rad}>
            <BodyShort
              title={`${new Date(delMedBruker?.createdAt!!).toLocaleDateString("nb-NO", {
                weekday: "long",
                day: "numeric",
                month: "numeric",
                year: "numeric",
              })}`}
              size="small"
            >
              Delt med bruker {datoSidenSistDelt}
            </BodyShort>
          </div>
        ) : null}
        <div className={styles.gjennomforing_container}>
          {!apentForInnsok && (
            <PadlockLockedFillIcon
              className={styles.status}
              title="Tiltaket er stengt for innsøking"
            />
          )}

          <div className={classNames(styles.flex, styles.navn)}>
            <VStack>
              <VisningsnavnForTiltak navn={navn} tiltakstype={tiltakstype} arrangor={arrangor} />
            </VStack>
          </div>

          <div className={classNames(styles.infogrid, styles.metadata)}>
            <BodyShort size="small" title={tiltakstype.navn}>
              {tiltakstype.navn}
            </BodyShort>
            <BodyShort
              size="small"
              title={visOppstartsdato(oppstart, oppstartsdato)}
              className={styles.truncate}
            >
              {visOppstartsdato(oppstart, oppstartsdato)}
            </BodyShort>
          </div>

          <ChevronRightIcon className={styles.ikon} title="Detaljer om tiltaket" />
        </div>
      </Lenke>
    </li>
  );
}

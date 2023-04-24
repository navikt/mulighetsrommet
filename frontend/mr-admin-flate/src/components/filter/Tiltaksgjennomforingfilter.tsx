import { Button, Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
<<<<<<< HEAD
  TiltaksgjennomforingAvslutningsstatus, TiltaksgjennomforingStatus,
=======
  Norg2Type,
  TiltaksgjennomforingAvslutningsstatus,
>>>>>>> 360dad96db54a8b90d1973c74479278fce5ee2c2
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { ChangeEvent, useState } from "react";
import {
  useFeatureToggles,
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
} from "../../api/features/feature-toggles";
import { paginationAtom, tiltaksgjennomforingfilter } from "../../api/atoms";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { resetPaginering } from "../../utils/Utils";
import styles from "./Filter.module.scss";
import { OpprettTiltaksgjennomforingModal } from "../tiltaksgjennomforinger/opprett/OpprettTiltaksgjennomforingModal";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";

export function Tiltaksgjennomforingfilter() {
  const [sokefilter, setSokefilter] = useAtom(tiltaksgjennomforingfilter);
  const [, setPage] = useAtom(paginationAtom);
  const { data: enheter } = useAlleEnheter();
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1
  );
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const features = useFeatureToggles();
  const visOpprettTiltaksgjennomforingKnapp =
    features.isSuccess &&
    features.data[OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE];

  return (
    <>
      <div className={styles.filter_container}>
        <div className={styles.filter_left}>
          <Search
            label="Søk etter tiltaksgjennomføring"
            hideLabel
            size="small"
            variant="simple"
            onChange={(search: string) =>
              setSokefilter({ ...sokefilter, search })
            }
            value={sokefilter.search}
            aria-label="Søk etter tiltaksgjennomføring"
            data-testid="filter_sokefelt"
          />
          <Select
            label="Filtrer på fylke"
            hideLabel
            size="small"
            value={sokefilter.fylkesenhet}
            data-testid="filter_tiltaksgjennomforing_fylkesenhet"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setSokefilter({
                ...sokefilter,
                enhet: "",
                fylkesenhet: e.currentTarget.value,
              });
            }}
          >
            <option value="">Alle fylker</option>
            {enheter
              ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
              ?.sort()
              ?.map((enhet) => (
                <option key={enhet.enhetNr} value={enhet.enhetNr}>
                  {enhet.navn}
                </option>
              ))}
          </Select>
          <Select
            label="Filtrer på enhet"
            hideLabel
            size="small"
            value={sokefilter.enhet}
            data-testid="filter_tiltaksgjennomforing_enhet"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setSokefilter({
                ...sokefilter,
                enhet: e.currentTarget.value,
              });
            }}
          >
            <option value="">Alle enheter</option>
            {enheter
              ?.filter((enhet) => {
                const erLokalEllerTiltaksenhet =
                  enhet.type === Norg2Type.LOKAL ||
                  enhet.type === Norg2Type.TILTAK;
                const enheterFraFylke =
                  sokefilter.fylkesenhet === ""
                    ? true
                    : sokefilter.fylkesenhet === enhet.overordnetEnhet;
                return erLokalEllerTiltaksenhet && enheterFraFylke;
              })
              ?.map((enhet) => (
                <option key={enhet.enhetNr} value={enhet.enhetNr}>
                  {enhet.navn} - {enhet.enhetNr}
                </option>
              ))}
          </Select>
          <Select
            label="Filtrer på tiltakstype"
            hideLabel
            size="small"
            value={sokefilter.tiltakstype}
            data-testid="filter_tiltaksgjennomforing_tiltakstype"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setSokefilter({
                ...sokefilter,
                tiltakstype: e.currentTarget.value,
              });
            }}
          >
            <option value="">Alle tiltakstyper</option>
            {tiltakstyper?.data?.map((tiltakstype) => (
              <option key={tiltakstype.id} value={tiltakstype.id}>
                {tiltakstype.navn}
              </option>
            ))}
          </Select>
        </div>
        <div className={styles.filter_right}>
          <Select
            label="Filtrer på status"
            hideLabel
            size="small"
            value={sokefilter.status}
            data-testid="filter_tiltaksgjennomforing_status"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setSokefilter({
                ...sokefilter,
                status: e.currentTarget
                  .value as TiltaksgjennomforingStatus,
              });
            }}
          >
            <option value={TiltaksgjennomforingStatus.GJENNOMFORES}>
              Gjennomføres
            </option>
            <option value={TiltaksgjennomforingStatus.AVSLUTTET}>
              Avsluttet
            </option>
            <option value={TiltaksgjennomforingStatus.AVBRUTT}>Avbrutt</option>
            <option value={TiltaksgjennomforingStatus.AVLYST}>Avlyst</option>
            <option value={TiltaksgjennomforingStatus.APENT_FOR_INNSOK}>
              Åpent for innsøk
            </option>
            <option value="">Alle statuser</option>
          </Select>
          {visOpprettTiltaksgjennomforingKnapp && (
            <>
              <Button size="small" onClick={() => setModalOpen(true)}>
                Opprett ny gjennomføring
              </Button>

              <OpprettTiltaksgjennomforingModal
                modalOpen={modalOpen}
                onClose={() => setModalOpen(false)}
              />
            </>
          )}
        </div>
      </div>
    </>
  );
}

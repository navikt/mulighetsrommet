import { Button, Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  Norg2Type,
  TiltaksgjennomforingStatus,
  Tiltakstypestatus,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { ChangeEvent, useState } from "react";
import {
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { paginationAtom, tiltaksgjennomforingfilter } from "../../api/atoms";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { inneholderUrl, resetPaginering } from "../../utils/Utils";
import styles from "./Filter.module.scss";
import { OpprettTiltaksgjennomforingModal } from "../modal/OpprettTiltaksgjennomforingModal";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import { arenaKodeErAftEllerVta } from "../../utils/tiltakskoder";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
  avtale?: Avtale;
}

export function Tiltaksgjennomforingfilter({ skjulFilter, avtale }: Props) {
  const [sokefilter, setSokefilter] = useAtom(tiltaksgjennomforingfilter);
  const [, setPage] = useAtom(paginationAtom);
  const { data: enheter } = useAlleEnheter();
  const { data: virksomheter } = useVirksomheter(VirksomhetTil.TILTAKSGJENNOMFORING);
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1
  );
  const [opprettModal, setOpprettModalOpen] = useState<boolean>(false);
  const [modalOpen, setModalOpen] = useState<boolean>(false);

  const features = useFeatureToggles();
  const visOpprettTiltaksgjennomforingKnapp =
    features.isSuccess &&
    features.data[OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE] &&
    inneholderUrl("/avtaler/");

  const erAFTellerVTA = arenaKodeErAftEllerVta(avtale?.tiltakstype.arenaKode);

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
            className={styles.form_field}
          />
          <Select
            label="Filtrer på region"
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
            <option value="">Alle regioner</option>
            {enheter
              ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
              ?.sort()
              ?.map((enhet) => (
                <option key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
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
                <option key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
                  {enhet.navn} - {enhet.enhetsnummer}
                </option>
              ))}
          </Select>
          {skjulFilter?.tiltakstype ? null : (
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
          )}
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
                status: e.currentTarget.value as TiltaksgjennomforingStatus,
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
          <Select
            label="Filtrer på arrangør"
            hideLabel
            size="small"
            value={sokefilter.arrangorOrgnr}
            data-testid="filter_tiltaksgjennomforing_arrangor"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setSokefilter({
                ...sokefilter,
                arrangorOrgnr: e.currentTarget.value,
              });
            }}
          >
            <>
              <option value="">Alle arrangører</option>
              {virksomheter?.map(v =>
                <option key={v.organisasjonsnummer} value={v.organisasjonsnummer}>{v.navn}</option>
              )}
            </>
          </Select>
        </div>
        <div className={styles.filter_right}>
          {avtale && (
            <>
              {visOpprettTiltaksgjennomforingKnapp && (
                <>
                  <Button
                    size="small"
                    onClick={() => setOpprettModalOpen(true)}
                  >
                    Opprett ny gjennomføring
                  </Button>

                  <OpprettTiltaksgjennomforingModal
                    modalOpen={opprettModal}
                    avtale={avtale}
                    onClose={() => setOpprettModalOpen(false)}
                  />
                </>
              )}
              {erAFTellerVTA && (
                <>
                  <Button
                    size="small"
                    onClick={() => setModalOpen(true)}
                    variant="secondary"
                  >
                    Legg til gjennomføring
                  </Button>
                  <LeggTilGjennomforingModal
                    modalOpen={modalOpen}
                    onClose={() => setModalOpen(false)}
                  />
                </>
              )}
            </>
          )}
        </div>
      </div>
    </>
  );
}

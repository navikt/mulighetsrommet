import { Button, Search, Select } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtalestatus,
  Norg2Type,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { avtaleFilter, avtalePaginationAtom } from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import {
  OPPRETT_AVTALE_ADMIN_FLATE,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { resetPaginering } from "../../utils/Utils";
import OpprettAvtaleModal from "../avtaler/OpprettAvtaleModal";
import styles from "./Filter.module.scss";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
}

export function Avtalefilter(props: Props) {
  const [filter, setFilter] = useAtom(avtaleFilter);
  const { data: enheter } = useAlleEnheter();
  const { data: tiltakstyper } = useTiltakstyper(
    { status: Tiltakstypestatus.AKTIV },
    1
  );
  const { data } = useAvtaler();
  const [, setPage] = useAtom(avtalePaginationAtom);
  const searchRef = useRef<HTMLDivElement | null>(null);
  const [modalOpen, setModalOpen] = useState<boolean>(false);

  const features = useFeatureToggles();
  const visOpprettAvtaleknapp =
    features.isSuccess && features.data[OPPRETT_AVTALE_ADMIN_FLATE];

  useEffect(() => {
    // Hold fokus på søkefelt dersom bruker skriver i søkefelt
    if (filter.sok !== "") {
      searchRef?.current?.focus();
    }
  }, [data]);

  return (
    <>
      <div className={styles.filter_container}>
        <div className={styles.filter_left}>
          <Search
            ref={searchRef}
            label="Søk etter avtale"
            hideLabel
            size="small"
            variant="simple"
            onChange={(sok: string) => {
              setFilter({ ...filter, sok });
            }}
            value={filter.sok}
            aria-label="Søk etter avtale"
            data-testid="filter_avtale_sokefelt"
          />
          <Select
            label="Filtrer på statuser"
            hideLabel
            size="small"
            value={filter.status}
            data-testid="filter_avtale_status"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setFilter({
                ...filter,
                status: e.currentTarget.value as Avtalestatus,
              });
            }}
          >
            <option value="Aktiv">Aktiv</option>
            <option value="Planlagt">Planlagt</option>
            <option value="Avsluttet">Avsluttet</option>
            <option value="Avbrutt">Avbrutt</option>
            <option value="">Alle statuser</option>
          </Select>
          <Select
            label="Filtrer på region"
            hideLabel
            size="small"
            value={filter.fylkeenhet}
            data-testid="filter_avtale_fylke"
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              resetPaginering(setPage);
              setFilter({ ...filter, fylkeenhet: e.currentTarget.value });
            }}
          >
            <option value="">Alle regioner</option>
            {enheter
              ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
              ?.map((enhet) => (
                <option key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
                  {enhet.navn} - {enhet.enhetsnummer}
                </option>
              ))}
          </Select>
          {props.skjulFilter?.tiltakstype ? null : (
            <Select
              label="Filtrer på tiltakstype"
              hideLabel
              size="small"
              value={filter.tiltakstype}
              data-testid="filter_avtale_tiltakstype"
              onChange={(e: ChangeEvent<HTMLSelectElement>) => {
                resetPaginering(setPage);
                setFilter({ ...filter, tiltakstype: e.currentTarget.value });
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
        </div>
        <div className={styles.filter_right}>
          {visOpprettAvtaleknapp && (
            <>
              <Button
                onClick={() => setModalOpen(true)}
                data-testid="registrer-ny-avtale"
                size="small"
              >
                Registrer avtale
              </Button>
              <OpprettAvtaleModal
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

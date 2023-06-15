import { Button, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Norg2Type,
  Tiltakstypestatus,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { useEffect, useRef, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  avtaleFilter,
  AvtaleFilterProps,
  avtalePaginationAtom,
  defaultAvtaleFilter,
} from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import {
  OPPRETT_AVTALE_ADMIN_FLATE,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { resetPaginering, valueOrDefault } from "../../utils/Utils";
import OpprettAvtaleModal from "../avtaler/OpprettAvtaleModal";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./Filter.module.scss";
import { RESET } from "jotai/vanilla/utils";
import { faro } from "@grafana/faro-web-sdk";
import { useNavigate } from "react-router-dom";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
}

export function Avtalefilter(props: Props) {
  const [filter, setFilter] = useAtom(avtaleFilter);
  const navigate = useNavigate();

  const form = useForm<AvtaleFilterProps>({
    defaultValues: {
      ...filter,
    },
  });
  const { register } = form;

  const { data: enheter } = useAlleEnheter();
  const { data: tiltakstyper } = useTiltakstyper(
    { status: Tiltakstypestatus.AKTIV },
    1
  );
  const { data } = useAvtaler();
  const { data: leverandorer } = useVirksomheter(VirksomhetTil.AVTALE);
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

  useEffect(() => {
    // Reset filter når vi unmounter
    return () => setFilter(RESET);
  }, []);

  const regionOptions = () => {
    const alleOptions = { value: "", label: "Alle regioner" };
    const regionOptions = enheter
      ? enheter
          ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
          ?.map((enhet) => ({
            value: enhet.enhetsnummer,
            label: enhet.navn,
          }))
      : [];
    return [alleOptions, ...regionOptions];
  };

  const tiltakstypeOptions = () => {
    const alleOptions = { value: "", label: "Alle tiltakstyper" };
    const tiltakstypeOptions = tiltakstyper
      ? tiltakstyper?.data?.map((tiltakstype) => ({
          label: tiltakstype.navn,
          value: tiltakstype.id,
        }))
      : [];
    return [alleOptions, ...tiltakstypeOptions];
  };
  const leverandorOptions = () => {
    const alleOptions = { value: "", label: "Alle leverandører" };
    const leverandorOptions = leverandorer
      ? leverandorer?.map(({ navn: label, organisasjonsnummer: value }) => ({
          label,
          value,
        }))
      : [];
    return [alleOptions, ...leverandorOptions];
  };

  return (
    <FormProvider {...form}>
      <form className={styles.avtaleform}>
        <div className={styles.filter_container}>
          <div className={styles.filtrering}>
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
              className={styles.form_field}
            />
            <SokeSelect
              size="small"
              placeholder="Filtrer på statuser"
              label="Filtrer på statuser"
              hideLabel
              {...register("status")}
              className={styles.form_field}
              onChange={(e) => {
                setFilter({
                  ...filter,
                  status: valueOrDefault(e, defaultAvtaleFilter.status),
                });
              }}
              options={[
                { value: "Aktiv", label: "Aktiv" },
                { value: "Planlagt", label: "Planlagt" },
                { value: "Avsluttet", label: "Avsluttet" },
                { value: "Avbrutt", label: "Avbrutt" },
                { value: "", label: "Alle statuser" },
              ]}
            />

            <SokeSelect
              size="small"
              label="Filtrer på region"
              hideLabel
              placeholder="Filtrer på region"
              {...register("navRegion")}
              className={styles.form_field}
              onChange={(e) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  navRegion: valueOrDefault(e, defaultAvtaleFilter.navRegion),
                });
              }}
              options={regionOptions()}
            />
            {props.skjulFilter?.tiltakstype ? null : (
              <SokeSelect
                size="small"
                label="Filtrer på tiltakstype"
                placeholder="Filtrer på tiltakstype"
                hideLabel
                {...register("tiltakstype")}
                className={styles.form_field}
                onChange={(e) => {
                  resetPaginering(setPage);
                  setFilter({
                    ...filter,
                    tiltakstype: valueOrDefault(
                      e,
                      defaultAvtaleFilter.tiltakstype
                    ),
                  });
                }}
                options={tiltakstypeOptions()}
              />
            )}
            <SokeSelect
              size="small"
              label="Filtrer på leverandør"
              placeholder="Filtrer på leverandør"
              hideLabel
              {...register("leverandor_orgnr")}
              className={styles.form_field}
              onChange={(e) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  leverandor_orgnr: valueOrDefault(
                    e,
                    defaultAvtaleFilter.leverandor_orgnr
                  ),
                });
              }}
              options={leverandorOptions()}
            />
          </div>

          <div className={styles.knapperad}>
            {visOpprettAvtaleknapp && (
              <>
                <Button
                  onClick={() => {
                    faro?.api?.pushEvent(
                      "Bruker trykket på 'Registrer avtale'-knapp"
                    );
                    setModalOpen(true);
                  }}
                  data-testid="registrer-ny-avtale"
                  size="small"
                  type="button"
                  className={styles.registrer_avtale_knapp}
                >
                  Registrer avtale
                </Button>
                <OpprettAvtaleModal
                  modalOpen={modalOpen}
                  onClose={() => setModalOpen(false)}
                  onSuccess={(id) => navigate(`/avtaler/${id}`)}
                />
              </>
            )}
          </div>
          <div className={styles.tabs}>TABS</div>
        </div>
      </form>
    </FormProvider>
  );
}

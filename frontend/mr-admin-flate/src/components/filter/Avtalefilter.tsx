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
  AvtaleFilterProps,
  avtaleFilter,
  avtalePaginationAtom,
} from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import {
  OPPRETT_AVTALE_ADMIN_FLATE,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { resetPaginering } from "../../utils/Utils";
import OpprettAvtaleModal from "../avtaler/OpprettAvtaleModal";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./Filter.module.scss";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
}

export function Avtalefilter(props: Props) {
  const [filter, setFilter] = useAtom(avtaleFilter);

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

  const regionOptions = () => {
    const alleOptions = { value: "", label: "Alle regioner" };
    const regionOptions = enheter
      ? enheter
          ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
          ?.map((enhet) => ({
            value: enhet.enhetsnummer,
            label: `${enhet.navn} - ${enhet.enhetsnummer}`,
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
      <form>
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
              className={styles.form_field}
            />
            <SokeSelect
              placeholder="Filtrer på statuser"
              label="Filtrer på statuser"
              hideLabel
              {...register("status")}
              className={styles.form_field}
              onChange={(e) => setFilter({ ...filter, status: e })}
              options={[
                { value: "Aktiv", label: "Aktiv" },
                { value: "Planlagt", label: "Planlagt" },
                { value: "Avsluttet", label: "Avsluttet" },
                { value: "Avbrutt", label: "Avbrutt" },
                { value: "", label: "Alle statuser" },
              ]}
            />

            <SokeSelect
              label="Filtrer på region"
              hideLabel
              placeholder="Filtrer på region"
              {...register("navRegion")}
              className={styles.form_field}
              onChange={(e) => {
                resetPaginering(setPage);
                setFilter({ ...filter, navRegion: e });
              }}
              options={regionOptions()}
            />
            {props.skjulFilter?.tiltakstype ? null : (
              <SokeSelect
                label="Filtrer på tiltakstype"
                placeholder="Filtrer på tiltakstype"
                hideLabel
                {...register("tiltakstype")}
                className={styles.form_field}
                onChange={(e) => {
                  resetPaginering(setPage);
                  setFilter({ ...filter, tiltakstype: e });
                }}
                options={tiltakstypeOptions()}
              />
            )}
            <SokeSelect
              label="Filtrer på leverandør"
              placeholder="Filtrer på leverandør"
              hideLabel
              {...register("leverandor_orgnr")}
              className={styles.form_field}
              onChange={(e) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  leverandor_orgnr: e,
                });
              }}
              options={leverandorOptions()}
            />
          </div>
          <div className={styles.filter_right}>
            {visOpprettAvtaleknapp && (
              <>
                <Button
                  onClick={() => setModalOpen(true)}
                  data-testid="registrer-ny-avtale"
                  size="small"
                  type="button"
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
      </form>
    </FormProvider>
  );
}

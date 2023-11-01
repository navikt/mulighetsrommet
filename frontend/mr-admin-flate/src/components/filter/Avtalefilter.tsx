import { faro } from "@grafana/faro-web-sdk";
import { Button, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { NavEnhetType, Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { useEffect, useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  avtaleFilter,
  AvtaleFilterProps,
  avtalePaginationAtom,
  defaultAvtaleFilter,
} from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { resetPaginering, valueOrDefault } from "../../utils/Utils";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import styles from "./Filter.module.scss";
import { FilterTag } from "./FilterTag";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";

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
  const { register, setValue } = form;

  const { data: enheter } = useNavEnheter();
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
      kategori: "",
    },
    1,
  );
  const { data: avtaler } = useAvtaler();
  const { data: leverandorer } = useVirksomheter(VirksomhetTil.AVTALE);
  const [, setPage] = useAtom(avtalePaginationAtom);
  const searchRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // Hold fokus på søkefelt dersom bruker skriver i søkefelt
    if (filter.sok !== "") {
      searchRef?.current?.focus();
    }
  }, [avtaler]);

  const regionOptions = () => {
    const alleOptions = {
      value: "",
      label: "Alle regioner",
    };
    const regionOptions = enheter
      ? enheter
          ?.filter((enhet) => enhet.type === NavEnhetType.FYLKE)
          ?.map((enhet) => ({
            value: enhet.enhetsnummer,
            label: enhet.navn,
          }))
      : [];
    return [alleOptions, ...regionOptions];
  };

  const tiltakstypeOptions = () => {
    const alleOptions = {
      value: "",
      label: "Alle tiltakstyper",
    };
    const tiltakstypeOptions = tiltakstyper
      ? tiltakstyper?.data?.map((tiltakstype) => ({
          label: tiltakstype.navn,
          value: tiltakstype.id,
        }))
      : [];
    return [alleOptions, ...tiltakstypeOptions];
  };
  const leverandorOptions = () => {
    const alleOptions = {
      value: "",
      label: "Alle leverandører",
    };
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
                setFilter({
                  ...filter,
                  sok,
                });
              }}
              value={filter.sok}
              aria-label="Søk etter avtale"
              className={styles.form_field}
            />
            <ControlledSokeSelect
              size="small"
              placeholder="Filtrer på statuser"
              label="Filtrer på statuser"
              hideLabel
              {...register("status")}
              className={styles.form_field}
              onChange={(e) => {
                setFilter({
                  ...filter,
                  status: valueOrDefault(e.target.value, defaultAvtaleFilter.status),
                });
              }}
              options={[
                {
                  value: "Aktiv",
                  label: "Aktiv",
                },
                {
                  value: "Planlagt",
                  label: "Planlagt",
                },
                {
                  value: "Avsluttet",
                  label: "Avsluttet",
                },
                {
                  value: "Avbrutt",
                  label: "Avbrutt",
                },
                {
                  value: "",
                  label: "Alle statuser",
                },
              ]}
            />

            <ControlledSokeSelect
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
                  navRegion: valueOrDefault(e.target.value, defaultAvtaleFilter.navRegion),
                });
              }}
              options={regionOptions()}
            />
            {props.skjulFilter?.tiltakstype ? null : (
              <ControlledSokeSelect
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
                    tiltakstype: valueOrDefault(e.target.value, defaultAvtaleFilter.tiltakstype),
                  });
                }}
                options={tiltakstypeOptions()}
              />
            )}
            <ControlledSokeSelect
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
                    e.target.value,
                    defaultAvtaleFilter.leverandor_orgnr,
                  ),
                });
              }}
              options={leverandorOptions()}
            />
          </div>

          <div className={styles.knapperad}>
            <Lenkeknapp
              to={`/avtaler/skjema`}
              variant="primary"
              handleClick={() => {
                faro?.api?.pushEvent("Bruker trykket på 'Opprett ny avtale'-knapp");
              }}
            >
              Opprett ny avtale
            </Lenkeknapp>
          </div>
          <div className={styles.tags_container}>
            {filter.status && (
              <FilterTag
                label={filter.status}
                onClick={() => {
                  setFilter({
                    ...filter,
                    status: "",
                  });
                  setValue("status", "");
                }}
              />
            )}
            {filter.navRegion && (
              <FilterTag
                label={enheter?.find((e) => e.enhetsnummer === filter.navRegion)?.navn}
                onClick={() => {
                  setFilter({
                    ...filter,
                    navRegion: defaultAvtaleFilter.navRegion,
                  });
                  setValue("navRegion", defaultAvtaleFilter.navRegion);
                }}
              />
            )}
            {filter.tiltakstype && (
              <FilterTag
                label={tiltakstyper?.data?.find((t) => t.id === filter.tiltakstype)?.navn}
                onClick={() => {
                  setFilter({
                    ...filter,
                    tiltakstype: defaultAvtaleFilter.tiltakstype,
                  });
                  setValue("tiltakstype", defaultAvtaleFilter.tiltakstype);
                }}
              />
            )}
            {filter.leverandor_orgnr && (
              <FilterTag
                label={
                  leverandorer?.find((l) => l.organisasjonsnummer === filter.leverandor_orgnr)?.navn
                }
                onClick={() => {
                  setFilter({
                    ...filter,
                    leverandor_orgnr: defaultAvtaleFilter.leverandor_orgnr,
                  });
                  setValue("leverandor_orgnr", defaultAvtaleFilter.leverandor_orgnr);
                }}
              />
            )}
            {(filter.status !== defaultAvtaleFilter.status ||
              filter.navRegion ||
              filter.tiltakstype ||
              filter.leverandor_orgnr) && (
              <Button
                type="button"
                size="small"
                variant="tertiary"
                onClick={() => {
                  setFilter(defaultAvtaleFilter);
                  setValue("status", defaultAvtaleFilter.status);
                  setValue("navRegion", defaultAvtaleFilter.navRegion);
                  setValue("tiltakstype", defaultAvtaleFilter.tiltakstype);
                  setValue("leverandor_orgnr", defaultAvtaleFilter.leverandor_orgnr);
                }}
              >
                Tilbakestill filter
              </Button>
            )}
          </div>
        </div>
      </form>
    </FormProvider>
  );
}
